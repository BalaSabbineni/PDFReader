package com.pdfreader.service;

import com.pdfreader.controller.dto.PdfDocumentResponse;
import com.pdfreader.controller.dto.PdfTextResponse;
import com.pdfreader.controller.dto.PdfTextResponse.PageContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfService {

    private static final String ROOT = "pdfs/";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final S3Service s3Service;

    // ── Public API (all scoped to userId) ────────────────────────────────────

    public List<PdfDocumentResponse> listAll(String userId) {
        return s3Service.listFiles(userPrefix(userId)).stream()
                .filter(obj -> obj.key().endsWith(".pdf"))
                .map(this::toResponse)
                .sorted(Comparator.comparing(PdfDocumentResponse::getUploadedAt).reversed())
                .toList();
    }

    public PdfDocumentResponse upload(MultipartFile file, String title, String userId) throws IOException {
        validatePdf(file);

        String id = UUID.randomUUID().toString();
        String s3Key = userPrefix(userId) + id + "/" + file.getOriginalFilename();

        int pageCount = countPages(file.getInputStream());

        Map<String, String> metadata = new HashMap<>();
        metadata.put("id", id);
        metadata.put("title", title != null && !title.isBlank() ? title : stripExtension(file.getOriginalFilename()));
        metadata.put("originalFilename", file.getOriginalFilename());
        metadata.put("pageCount", String.valueOf(pageCount));
        metadata.put("uploadedAt", LocalDateTime.now().format(FMT));

        s3Service.uploadFile(file, s3Key, metadata);
        log.info("Uploaded PDF: {} (id={}, user={})", metadata.get("title"), id, userId);

        HeadObjectResponse head = s3Service.getMetadata(s3Key);
        return toResponseFromHead(s3Key, file.getSize(), head);
    }

    public PdfTextResponse extractText(String id, String userId) throws IOException {
        String s3Key = findKeyById(userId, id);

        try (InputStream inputStream = s3Service.downloadFile(s3Key);
             PDDocument pdf = Loader.loadPDF(new RandomAccessReadBuffer(inputStream))) {

            PDFTextStripper stripper = new PDFTextStripper();
            List<PageContent> pages = new ArrayList<>();

            for (int i = 1; i <= pdf.getNumberOfPages(); i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String text = stripper.getText(pdf).trim();
                if (!text.isBlank()) {
                    pages.add(new PageContent(i, text));
                }
            }

            HeadObjectResponse head = s3Service.getMetadata(s3Key);
            String title = head.metadata().getOrDefault("title", "Untitled");

            return PdfTextResponse.builder()
                    .id(id)
                    .title(title)
                    .totalPages(pdf.getNumberOfPages())
                    .pages(pages)
                    .build();
        }
    }

    public PdfDocumentResponse getMetadata(String id, String userId) {
        String s3Key = findKeyById(userId, id);
        HeadObjectResponse head = s3Service.getMetadata(s3Key);
        return toResponseFromHead(s3Key, head.contentLength(), head);
    }

    public void delete(String id, String userId) {
        String s3Key = findKeyById(userId, id);
        s3Service.deleteFile(s3Key);
        log.info("Deleted PDF id={} user={}", id, userId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String userPrefix(String userId) {
        return ROOT + userId + "/";
    }

    private String findKeyById(String userId, String id) {
        return s3Service.listFiles(userPrefix(userId) + id + "/").stream()
                .filter(obj -> obj.key().endsWith(".pdf"))
                .map(S3Object::key)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("PDF not found: " + id));
    }

    private PdfDocumentResponse toResponse(S3Object obj) {
        HeadObjectResponse head = s3Service.getMetadata(obj.key());
        return toResponseFromHead(obj.key(), obj.size(), head);
    }

    private PdfDocumentResponse toResponseFromHead(String s3Key, long fileSize, HeadObjectResponse head) {
        Map<String, String> meta = head.metadata();
        return PdfDocumentResponse.builder()
                .id(meta.getOrDefault("id", extractIdFromKey(s3Key)))
                .title(meta.getOrDefault("title", "Untitled"))
                .fileName(meta.getOrDefault("originalfilename", s3Key))
                .fileSize(fileSize)
                .pageCount(Integer.parseInt(meta.getOrDefault("pagecount", "0")))
                .uploadedAt(parseDate(meta.get("uploadedat")))
                .downloadUrl(s3Service.generatePresignedUrl(s3Key))
                .build();
    }

    private String extractIdFromKey(String s3Key) {
        // key = pdfs/{userId}/{pdfId}/{filename}
        String[] parts = s3Key.split("/");
        return parts.length >= 3 ? parts[2] : s3Key;
    }

    private LocalDateTime parseDate(String value) {
        if (value == null) return LocalDateTime.now();
        try { return LocalDateTime.parse(value, FMT); } catch (Exception e) { return LocalDateTime.now(); }
    }

    private void validatePdf(MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("File is empty");
        String ct = file.getContentType();
        if (ct == null || !ct.equals("application/pdf"))
            throw new IllegalArgumentException("Only PDF files are allowed");
    }

    private int countPages(InputStream inputStream) {
        try (PDDocument pdf = Loader.loadPDF(new RandomAccessReadBuffer(inputStream))) {
            return pdf.getNumberOfPages();
        } catch (IOException e) {
            log.warn("Could not count pages: {}", e.getMessage());
            return 0;
        }
    }

    private String stripExtension(String fileName) {
        if (fileName == null) return "Untitled";
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}

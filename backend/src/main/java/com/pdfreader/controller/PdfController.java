package com.pdfreader.controller;

import com.pdfreader.controller.dto.PdfDocumentResponse;
import com.pdfreader.controller.dto.PdfTextResponse;
import com.pdfreader.service.PdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pdfs")
@RequiredArgsConstructor
public class PdfController {

    private final PdfService pdfService;

    @GetMapping
    public ResponseEntity<List<PdfDocumentResponse>> listAll() {
        return ResponseEntity.ok(pdfService.listAll(currentUserId()));
    }

    @PostMapping("/upload")
    public ResponseEntity<PdfDocumentResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title) throws IOException {
        return ResponseEntity.ok(pdfService.upload(file, title, currentUserId()));
    }

    @GetMapping("/{id}/text")
    public ResponseEntity<PdfTextResponse> getText(@PathVariable String id) throws IOException {
        return ResponseEntity.ok(pdfService.extractText(id, currentUserId()));
    }

    @GetMapping("/{id}/metadata")
    public ResponseEntity<PdfDocumentResponse> getMetadata(@PathVariable String id) {
        return ResponseEntity.ok(pdfService.getMetadata(id, currentUserId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable String id) {
        pdfService.delete(id, currentUserId());
        return ResponseEntity.ok(Map.of("message", "Deleted successfully", "id", id));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, String>> handleIOException(IOException ex) {
        return ResponseEntity.internalServerError().body(Map.of("error", "Failed to process PDF: " + ex.getMessage()));
    }

    private String currentUserId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}

package com.pdfreader.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PdfDocumentResponse {
    private String id;
    private String title;
    private String fileName;
    private long fileSize;
    private int pageCount;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime uploadedAt;
    private String downloadUrl;
}

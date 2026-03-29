package com.pdfreader.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PdfDocument {
    private String id;
    private String title;
    private String fileName;
    private String s3Key;
    private long fileSize;
    private int pageCount;
    private LocalDateTime uploadedAt;
}

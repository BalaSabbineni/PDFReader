package com.pdfreader.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdfTextResponse {
    private String id;
    private String title;
    private int totalPages;
    private List<PageContent> pages;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageContent {
        private int pageNumber;
        private String text;
    }
}

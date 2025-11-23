package com.example.vacancyscraper.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class ParseBatchRequest {

    @NotEmpty
    private List<String> urls;

    public ParseBatchRequest() {
    }

    public ParseBatchRequest(List<String> urls) {
        this.urls = urls;
    }

    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }
}

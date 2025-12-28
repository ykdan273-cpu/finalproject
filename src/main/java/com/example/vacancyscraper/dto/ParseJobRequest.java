package com.example.vacancyscraper.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public class ParseJobRequest {

    @NotBlank
    private String url;

    @PositiveOrZero
    private Long delayMs;

    public ParseJobRequest() {
    }

    public ParseJobRequest(String url, Long delayMs) {
        this.url = url;
        this.delayMs = delayMs;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Long getDelayMs() {
        return delayMs;
    }

    public void setDelayMs(Long delayMs) {
        this.delayMs = delayMs;
    }
}

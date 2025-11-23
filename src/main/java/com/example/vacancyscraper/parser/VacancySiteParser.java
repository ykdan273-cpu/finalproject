package com.example.vacancyscraper.parser;

import com.example.vacancyscraper.model.Vacancy;

public interface VacancySiteParser {
    boolean supports(String url);

    Vacancy parse(String url, String html);
}

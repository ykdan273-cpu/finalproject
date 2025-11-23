package com.example.vacancyscraper.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ParsersSupportsTest {

    @Test
    void supportsCorrectSites() {
        assertTrue(new HhVacancyParser().supports("https://hh.ru/vacancy/123"));
        assertTrue(new SuperJobVacancyParser().supports("https://www.superjob.ru/vakansii/123.html"));
        assertTrue(new HabrVacancyParser().supports("https://career.habr.com/vacancies/1000"));
        assertTrue(new RabotaRuVacancyParser().supports("https://www.rabota.ru/vacancy/555"));
    }
}

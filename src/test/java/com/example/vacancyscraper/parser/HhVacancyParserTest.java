package com.example.vacancyscraper.parser;

import com.example.vacancyscraper.model.Vacancy;
import com.example.vacancyscraper.model.VacancySource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HhVacancyParserTest {

    private final HhVacancyParser parser = new HhVacancyParser();

    @Test
    void parsesBasicFieldsFromHtml() {
        String html = """
                <html>
                  <head><meta itemprop="datePosted" content="2024-01-10"/></head>
                  <body>
                    <h1 data-qa="vacancy-title">Java Developer</h1>
                    <a data-qa="vacancy-company-name">Acme Corp</a>
                    <span data-qa="vacancy-salary">200 000 ₽</span>
                    <p data-qa="vacancy-view-location">Москва</p>
                    <div data-qa="vacancy-description">Spring, REST</div>
                  </body>
                </html>
                """;

        Vacancy vacancy = parser.parse("https://hh.ru/vacancy/123", html);

        assertEquals("Java Developer", vacancy.getTitle());
        assertEquals("Acme Corp", vacancy.getCompany());
        assertEquals("200 000 ₽", vacancy.getSalary());
        assertEquals("Москва", vacancy.getCity());
        assertEquals("Spring, REST", vacancy.getRequirements());
        assertEquals(VacancySource.HH, vacancy.getSource());
        assertNotNull(vacancy.getPublishedDate());
    }
}

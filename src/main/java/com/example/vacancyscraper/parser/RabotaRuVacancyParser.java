package com.example.vacancyscraper.parser;

import com.example.vacancyscraper.model.Vacancy;
import com.example.vacancyscraper.model.VacancySource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class RabotaRuVacancyParser implements VacancySiteParser {
    @Override
    public boolean supports(String url) {
        return url != null && url.contains("rabota.ru/vacancy");
    }

    @Override
    public Vacancy parse(String url, String html) {
        Document doc = Jsoup.parse(html);
        String title = ParserUtils.text(doc, "h1, h1.vacancy-title", "не указано");
        String company = ParserUtils.text(doc, "div.company-name, a.company-link", "не указано");
        String salary = ParserUtils.text(doc, "div.salary, span.salary-value", "не указано");
        String city = ParserUtils.text(doc, "div.location, span.address", "не указано");
        String requirements = ParserUtils.text(doc, "div.vacancy-description, div#vacancy-description", "не указано");
        LocalDate publishedDate = ParserUtils.parseDate(ParserUtils.attr(doc, "meta[itemprop=datePosted]", "content", ""));

        return new Vacancy(title, company, salary, requirements, city, publishedDate, VacancySource.RABOTA, url);
    }
}

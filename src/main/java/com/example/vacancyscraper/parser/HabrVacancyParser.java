package com.example.vacancyscraper.parser;

import com.example.vacancyscraper.model.Vacancy;
import com.example.vacancyscraper.model.VacancySource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class HabrVacancyParser implements VacancySiteParser {
    @Override
    public boolean supports(String url) {
        return url != null && url.contains("career.habr.com/vacancies");
    }

    @Override
    public Vacancy parse(String url, String html) {
        Document doc = Jsoup.parse(html);
        String title = ParserUtils.text(doc, "h1.page-title__title, h1", "не указано");
        String company = ParserUtils.text(doc, "div.company_name a, a.company_link", "не указано");
        String salary = ParserUtils.text(doc, "div.vacancy-salary, span.salary", "не указано");
        String city = ParserUtils.text(doc, "div.location, span.location", "не указано");
        String requirements = ParserUtils.text(doc, "div.vacancy-description, div#job_show_right_column", "не указано");
        LocalDate publishedDate = ParserUtils.parseDate(ParserUtils.attr(doc, "meta[itemprop=datePosted]", "content", ""));

        return new Vacancy(title, company, salary, requirements, city, publishedDate, VacancySource.HABR, url);
    }
}

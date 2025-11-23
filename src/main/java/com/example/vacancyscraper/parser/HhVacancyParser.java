package com.example.vacancyscraper.parser;

import com.example.vacancyscraper.model.Vacancy;
import com.example.vacancyscraper.model.VacancySource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class HhVacancyParser implements VacancySiteParser {
    @Override
    public boolean supports(String url) {
        return url != null && url.contains("hh.ru/vacancy");
    }

    @Override
    public Vacancy parse(String url, String html) {
        Document doc = Jsoup.parse(html);
        String title = ParserUtils.text(doc, "h1[data-qa=vacancy-title], h1.bloko-header-section-1", "не указано");
        String company = ParserUtils.text(doc, "a[data-qa=vacancy-company-name], span[data-qa=bloko-header-2]", "не указано");
        String salary = ParserUtils.text(doc, "span[data-qa=vacancy-salary], span[data-qa=vacancy-salary-compensation-type-net]", "не указано");
        String city = ParserUtils.text(doc, "p[data-qa=vacancy-view-location], span[data-qa=vacancy-view-raw-address]", "не указано");
        String requirements = ParserUtils.text(doc, "div[data-qa=vacancy-description]", "не указано");
        LocalDate publishedDate = ParserUtils.parseDate(ParserUtils.attr(doc, "meta[itemprop=datePosted]", "content", ""));

        return new Vacancy(title, company, salary, requirements, city, publishedDate, VacancySource.HH, url);
    }
}

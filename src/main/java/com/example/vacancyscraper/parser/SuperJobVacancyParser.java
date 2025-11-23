package com.example.vacancyscraper.parser;

import com.example.vacancyscraper.model.Vacancy;
import com.example.vacancyscraper.model.VacancySource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class SuperJobVacancyParser implements VacancySiteParser {
    @Override
    public boolean supports(String url) {
        return url != null && url.contains("superjob.ru/vakansii");
    }

    @Override
    public Vacancy parse(String url, String html) {
        Document doc = Jsoup.parse(html);
        String title = ParserUtils.text(doc, "h1, h1[data-qa=vacancy-title]", "не указано");
        String company = ParserUtils.text(doc, "a._6a68x._1h3Zg, div[data-qa=vacancy-company-name]", "не указано");
        String salary = ParserUtils.text(doc, "span[data-qa=vacancy-salary], span._1OuF_, span._1h3Zg", "не указано");
        String city = ParserUtils.text(doc, "span[data-qa=vacancy-view-raw-address], span._1P6qU", "не указано");
        String requirements = ParserUtils.text(doc, "div[data-qa=vacancy-description], div.f-test-vacancy-description", "не указано");
        LocalDate publishedDate = ParserUtils.parseDate(ParserUtils.attr(doc, "meta[itemprop=datePosted]", "content", ""));

        return new Vacancy(title, company, salary, requirements, city, publishedDate, VacancySource.SUPERJOB, url);
    }
}

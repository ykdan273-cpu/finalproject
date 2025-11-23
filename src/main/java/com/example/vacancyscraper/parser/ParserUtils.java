package com.example.vacancyscraper.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class ParserUtils {
    private ParserUtils() {
    }

    public static String text(Document doc, String cssQuery, String fallback) {
        Element element = doc.selectFirst(cssQuery);
        if (element != null) {
            String value = element.text();
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return fallback;
    }

    public static String attr(Document doc, String cssQuery, String attr, String fallback) {
        Element element = doc.selectFirst(cssQuery);
        if (element != null) {
            String value = element.attr(attr);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return fallback;
    }

    public static LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateString.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception ignored) {
            return null;
        }
    }
}

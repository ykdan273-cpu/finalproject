package com.example.vacancyscraper.analytics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SalaryParser {

    private static final Pattern NUMBER = Pattern.compile("(\\d+[\\s\\d]*)");

    private SalaryParser() {
    }

    public static SalaryRange parse(String salaryText) {
        if (salaryText == null || salaryText.isBlank()) {
            return new SalaryRange(null, null);
        }
        List<Integer> numbers = extractNumbers(salaryText);
        if (numbers.isEmpty()) {
            return new SalaryRange(null, null);
        }
        String normalized = salaryText.toLowerCase(Locale.ROOT);
        if (numbers.size() >= 2) {
            int from = Math.min(numbers.get(0), numbers.get(1));
            int to = Math.max(numbers.get(0), numbers.get(1));
            return new SalaryRange(from, to);
        }
        Integer single = numbers.get(0);
        if (normalized.contains("до")) {
            return new SalaryRange(null, single);
        }
        return new SalaryRange(single, null);
    }

    private static List<Integer> extractNumbers(String salaryText) {
        Matcher matcher = NUMBER.matcher(salaryText);
        List<Integer> numbers = new ArrayList<>();
        while (matcher.find()) {
            String raw = matcher.group(1).replace(" ", "");
            try {
                numbers.add(Integer.parseInt(raw));
            } catch (NumberFormatException ignored) {
            }
        }
        return numbers;
    }
}

package com.example.vacancyscraper.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Vacancy {
    private String title;
    private String company;
    private String salary;
    private String requirements;
    private String city;
    private LocalDate publishedDate;
    private VacancySource source;
    private String url;

    public Vacancy() {
    }

    public Vacancy(String title, String company, String salary, String requirements,
                   String city, LocalDate publishedDate, VacancySource source, String url) {
        this.title = title;
        this.company = company;
        this.salary = salary;
        this.requirements = requirements;
        this.city = city;
        this.publishedDate = publishedDate;
        this.source = source;
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getSalary() {
        return salary;
    }

    public void setSalary(String salary) {
        this.salary = salary;
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public LocalDate getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(LocalDate publishedDate) {
        this.publishedDate = publishedDate;
    }

    public VacancySource getSource() {
        return source;
    }

    public void setSource(VacancySource source) {
        this.source = source;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

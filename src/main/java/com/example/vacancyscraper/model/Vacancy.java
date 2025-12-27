package com.example.vacancyscraper.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "vacancies")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Vacancy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    private String title;
    private String company;
    private String salary;

    @Column(length = 4000)
    private String requirements;
    private String city;
    private LocalDate publishedDate;

    @Enumerated(EnumType.STRING)
    private VacancySource source;

    @Column(unique = true, nullable = false)
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

    public Long getId() {
        return id;
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

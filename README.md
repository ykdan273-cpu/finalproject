# Парсер вакансий (Spring Boot 3, Java 17)

Сервис парсит вакансии с hh.ru/superjob/habr/rabota, отдает результаты через REST/Swagger, считает метрики в Prometheus/Grafana, пишет трейсы в Jaeger.

## Стек
- Java 17, Spring Boot 3.3
- JPA/Hibernate (PostgreSQL/H2), MongoDB
- Micrometer + Prometheus + Grafana
- Jsoup, HttpClient (HTTP/1.1 keep-alive)
- Кэш: Caffeine (по умолчанию) или Redis


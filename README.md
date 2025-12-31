# Парсер вакансий (Spring Boot 3, Java 17)

Сервис парсит вакансии с hh.ru/superjob/habr/rabota, отдает результаты через REST/Swagger, считает метрики в Prometheus/Grafana, пишет трейсы в Jaeger.

https://disk.360.yandex.ru/i/n1Omh-jh5YpRxw - презентация 
## Стек
- Java 17, Spring Boot 3.3
- использовал opentelemetry-javaagent.jar
- JPA/Hibernate (PostgreSQL/H2), MongoDB
- Micrometer + Prometheus + Grafana
- Jsoup, HttpClient (HTTP/1.1 keep-alive)
- Кэш: Caffeine 


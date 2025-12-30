# Парсер вакансий (Spring Boot 3, Java 17)

Сервис парсит вакансии с hh.ru/superjob/habr/rabota, отдает результаты через REST/Swagger, считает метрики в Prometheus/Grafana, пишет трейсы в Jaeger.

## Стек
- Java 17, Spring Boot 3.3
- JPA/Hibernate (PostgreSQL/H2), MongoDB
- Micrometer + Prometheus + Grafana
- OpenTelemetry OTLP → Jaeger
- Jsoup, HttpClient (HTTP/1.1 keep-alive)
- Кэш: Caffeine (по умолчанию) или Redis

## Запуск
```bash
mvn spring-boot:run
```
Переключить хранилище: `-Dvacancy.storage.backend=jpa` (PostgreSQL/H2) или `mongo`  
Переключить кэш на Redis: `-Dspring.cache.type=redis --spring.data.redis.host=localhost`

Swagger: http://localhost:8080/swagger-ui/index.html  
Actuator/health: http://localhost:8080/actuator/health  
Prometheus scrape: http://localhost:8080/actuator/prometheus

## API (главное)
- `POST /parse` — парсинг одной ссылки
- `POST /parse-batch` — парсинг списка ссылок
- `GET /answer` — выборка вакансий (фильтры city/source, sortBy), кэшируется
- `GET /stats` — служебная статистика

## Хранилище
`Vacancy` (title, company, salary, requirements, city, publishedDate, source, url).  
JPA: индекс по url (unique), индекс по city+publishedDate.  
Mongo: коллекция `vacancies` с аналогичными полями.  
Профиль postgres/mongo управляется через `vacancy.storage.backend`.

## Кэш
- Caffeine по умолчанию: `spring.cache.type=caffeine`, `spring.cache.caffeine.spec=maximumSize=1000,expireAfterWrite=10m`
- Redis вариант: запусти Redis и добавь `--spring.cache.type=redis --spring.data.redis.host=localhost`
- Кэшируется GET `/answer` (ключ: city, source, sortBy, parallel), инвалидация на сохранении.

## Наблюдаемость
- Micrometer/Prometheus: HTTP метрики, кастомные `vacancy_parsing_success_total`, `vacancy_parsing_error_total`, `vacancy_parsing_time_seconds`, сохраненные в БД (`vacancy_persistence_saved_total`).
- Grafana: дашборды throughput/latency, success/error, GC/heap.
- OpenTelemetry: spans `vacancy.fetchAndParse` с тегами parser/url/status, события `http.send` и `parse.html`; экспорт OTLP на 4317 → Jaeger. Настройка в `application.properties` (`otel.resource.attributes=service.name=vacancy-parser`).

## JFR/JMC
Профилировка через JDK Flight Recorder:
```
jcmd <PID> JFR.start name=hot settings=profile duration=300s filename=recording_hot.jfr maxsize=200m maxage=5m dumponexit=true
```
Анализ в JDK Mission Control (горячие места: TLS/crypto, HeapByteBuffer; GC паузы короткие ~13 ms).

## Бенчмарки (JMH, свежие)
- ParseBenchmark (разбор зарплаты):
  - for: ~432 µs/op
  - stream: ~456 µs/op
  - parallelStream: ~111 µs/op
- ThreadedParseBenchmark:
  - platform threads: ~0.82–0.88 ms/op
  - virtual threads: ~0.36 ms/op
- DeliveryBenchmark:
  - REST polling: ~55.3 ms/op
  - WebSocket push: ~58.8 ms/op
- VacancyParserBenchmark (реальный HH парсер):
  - parseSingle: ~36.8 µs/op
  - parseBatchParallel (200 URL): ~3.06 ms/op
  - endToEndWithStore: ~7.49 ms/op

Запуск JMH (пример):
```
mvn -DskipTests test-compile exec:java \
  -Dexec.classpathScope=test \
  -Dexec.mainClass=org.openjdk.jmh.Main \
  -Dexec.args="\"-Djmh.separateClasspathJAR=false\" \"-f\" \"0\" \".*VacancyParserBenchmark.*\""
```

## Нагрузочное воспроизведение (для метрик)
Пример PowerShell нагрузки (batch + single):
```powershell
Get-Job | Stop-Job | Remove-Job
$batch='{"urls":["https://hh.ru/vacancy/128763453","https://hh.ru/vacancy/109654425?utm_medium=cpc_hh&utm_source=clickmehhru&utm_campaign=979123&utm_local_campaign=1427257&utm_content=1024271"]}'
1..3 | % { Start-Job { while ($true) {
  iwr "http://localhost:8080/parse-batch" -Method Post -Body $using:batch -ContentType "application/json" | Out-Null
  iwr "http://localhost:8080/parse" -Method Post -Body '{"url":"https://hh.ru/vacancy/128763453"}' -ContentType "application/json" | Out-Null
  iwr "http://localhost:8080/answer?city=mos" | Out-Null
}}}
```

## Выбор алгоритмов/структур
- parallelStream + virtual threads → максимальная утилизация CPU и скрытие I/O.
- Индекс по url (JPA) обеспечивает уникальность и O(log n) вставку.
- Ограничение параллельности (Semaphore) → контролируемое число исходящих соединений/TLS рукопожатий.
- Кэш на Caffeine/Redis (O(1) доступ, неблокирующий).


## парсер вакансий (Spring Boot)

проект, который получает реальные страницы вакансий (hh.ru, superjob.ru, career.habr.com, rabota.ru), разбирает их через Jsoup, сохраняет результат в файл и предоставляет Swagger-UI.

### Запуск
- Требования: Java 17, Maven.
- Собрать и запустить: `mvn spring-boot:run` (или через main )
- Swagger-UI: http://localhost:8080/swagger-ui/index.html



###  проверка через Swagger
1. Запустить приложение.
2. Открыть Swagger-UI.
3. `POST /parse` — вставить одну ссылку, получить `Vacancy`.
4. `POST /parse-batch` — вставить 3–4 ссылки на разные сайты, убедиться, что задачи идут параллельно (лог + скорость).
5. `GET /answer` — посмотреть сохранённые вакансии; попробовать `parallel=true` для демонстрации `parallelStream`.
6. `GET /stats` — увидеть счётчики и время последнего batch/шедулера.

### Примечания по парсерам
- Парсеры используют реальный `HttpClient` с нормальным User-Agent из настроек.
- Jsoup ищет типичные CSS-селектора; если элемент не найден, поле заполняется «не указано», чтобы не падать .
### Cache setup and benchmarks
- Hot path cached: the filtered vacancy listing `GET /answer` now uses `@Cacheable` with key `(city, source, sortBy, parallel)`. TTL is 10 minutes; writes via `VacancyPersistenceService.saveWithLock` evict the cache to keep data fresh.
- Default mode (local Caffeine): `spring.cache.type=caffeine` with `spring.cache.caffeine.spec=maximumSize=1000,expireAfterWrite=10m`. Run with `mvn spring-boot:run`.
- Distributed mode (Redis): start Redis (e.g. `docker run --name vacancy-redis -p 6379:6379 -d redis:7-alpine`) and run the app with `-Dspring-boot.run.arguments="--spring.cache.type=redis --spring.data.redis.host=localhost"`. TTL is controlled by `spring.cache.redis.time-to-live=10m`.
- Two-instance check: because `VACANCY_DB_URL` defaults to `jdbc:h2:file:./data/vacancies-db;AUTO_SERVER=TRUE`, two Spring Boot processes can share the same DB file. Start one at `--server.port=8080`, another at `--server.port=8081` with the same cache type. With Caffeine each instance hits the DB on first request; with Redis the second instance serves warm data from Redis.
- Quick timing check (PowerShell): `Measure-Command { curl http://localhost:8080/answer?sortBy=title -UseBasicParsing | Out-Null }` for a cold call, then repeat for a warm call. For heavier load, use `wrk -t2 -c20 -d30s http://localhost:8080/answer?sortBy=title`.

## Storage comparison (PostgreSQL vs MongoDB)
- Key entity: `Vacancy` (title, company, salary, requirements, city, publishedDate, source, url). Typical pattern: many reads with city/source filters and periodic batch writes from parsers; url is the natural business key.
- Backends: JPA/Hibernate (default) via `vacancy.storage.backend=jpa`; MongoDB via `vacancy.storage.backend=mongo` with `VACANCY_MONGO_URI` / `VACANCY_MONGO_DB`. Convenience profiles: `-Dspring.profiles.active=postgres` or `mongo`.
- Benchmark: synthetic workload using embedded PostgreSQL + embedded MongoDB. Run `mvn -q -Dtest=VacancyStorageBenchmarkTest test` (first run downloads binaries, takes ~30–60s). Operations: single insert, batch insert (400 docs), read by url, filtered read by city + 7‑day window.
- Results on Windows (local run): PostgreSQL — single insert 185 ms, batch insert 90 ms, readByUrl 49 ms, filtered read 39 ms (~44 rows), ~1.4k write ops/s. MongoDB — single insert 38 ms, batch insert 43 ms, readByUrl 22 ms, filtered read 5 ms (~44 rows), ~5k write ops/s and ~8.8k filtered rows/s.
- Observations: Mongo shows better insert and filtered read latency for the document shape, while PostgreSQL offers stronger transactional semantics and is better suited when you need consistent joins/locking (the API still uses pessimistic locking when `backend=jpa`). Choose MongoDB for fast ingestion + flexible schema, PostgreSQL for strict constraints and transactional workflows.

## File I/O benchmark (RandomAccessFile vs FileChannel vs MemoryMapped)
- Формат хранения: бинарный, фиксированный размер записи 64 байта (id, timestamp, salary, companyHash, cityHash, score, offset + padding). Тестовые наборы: 10k / 50k / 100k записей.
- Методы: RandomAccessFile, FileChannel+ByteBuffer, memory-mapped (FileChannel.map). Для RandomAccessFile и mmap замерен случайный доступ (5k обращений).
- Как запустить: `mvn -q -Dtest=FileIOBenchmarkTest test` — вывод таблицы в консоль (`target/surefire-reports/...FileIOBenchmarkTest.txt`).
- Пример результатов (Windows, JDK21): для 100k записей — write/read/random(ms): RAF 398/218/27; FileChannel 158/14/0; mmap 147/15/2. Пиковая память по Runtime ~24 MB.
- Замечания: memory-mapped файлы быстро пишут/читают последовательные блоки и дают быстрый random-access, но требуют аккуратного закрытия (unmap) на Windows. RandomAccessFile удобен для точечных обновлений, но медленнее при массовой записи; FileChannel с буфером показывает лучшую последовательную запись/чтение без проблем блокировок.

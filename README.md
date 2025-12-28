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

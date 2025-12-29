package com.example.vacancyscraper.storage;

import com.example.vacancyscraper.model.Vacancy;
import com.example.vacancyscraper.model.VacancySource;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Synthetic latency/throughput comparison for Vacancy persistence in PostgreSQL vs MongoDB.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VacancyStorageBenchmarkTest {

    private static final Logger log = LoggerFactory.getLogger(VacancyStorageBenchmarkTest.class);

    private EmbeddedPostgres embeddedPostgres;
    private MongodExecutable mongodExecutable;
    private MongodProcess mongodProcess;
    private MongoClient mongoClient;

    private BenchmarkStore postgresStore;
    private BenchmarkStore mongoStore;

    @BeforeAll
    void setUp() throws Exception {
        embeddedPostgres = EmbeddedPostgres.builder()
                .start();
        DataSource dataSource = embeddedPostgres.getPostgresDatabase();
        postgresStore = new PostgresStore(dataSource);

        int mongoPort = Network.freeServerPort(Network.getLocalHost());
        MongodConfig mongoConfig = MongodConfig.builder()
                .version(Version.Main.PRODUCTION)
                .net(new Net(mongoPort, Network.localhostIsIPv6()))
                .build();
        mongodExecutable = MongodStarter.getDefaultInstance().prepare(mongoConfig);
        mongodProcess = mongodExecutable.start();
        mongoClient = MongoClients.create("mongodb://localhost:" + mongoPort);
        mongoStore = new MongoStore(mongoClient, "vacancies-benchmark");
    }

    @AfterAll
    void tearDown() throws Exception {
        if (postgresStore != null) {
            postgresStore.close();
        }
        if (mongoStore != null) {
            mongoStore.close();
        }
        if (mongoClient != null) {
            mongoClient.close();
        }
        if (mongodProcess != null) {
            mongodProcess.stop();
        }
        if (mongodExecutable != null) {
            mongodExecutable.stop();
        }
        if (embeddedPostgres != null) {
            embeddedPostgres.close();
        }
    }

    @Test
    @DisplayName("Compare insert/read patterns between PostgreSQL and MongoDB")
    void benchmarkStores() {
        int datasetSize = 800;
        int batchSize = 400;
        String cityFilter = "City-1";
        LocalDate filterDate = LocalDate.now().minusDays(3);
        List<Vacancy> dataset = TestDataFactory.generate(datasetSize);

        StorageMetrics postgresMetrics = measure(postgresStore, dataset, batchSize, filterDate, cityFilter);
        StorageMetrics mongoMetrics = measure(mongoStore, dataset, batchSize, filterDate, cityFilter);

        logMetrics(postgresMetrics);
        logMetrics(mongoMetrics);

        assertNotNull(postgresMetrics);
        assertNotNull(mongoMetrics);
    }

    private StorageMetrics measure(BenchmarkStore store,
                                   List<Vacancy> dataset,
                                   int batchSize,
                                   LocalDate filterDate,
                                   String city) {
        store.reset();

        Vacancy single = dataset.get(0);
        long singleInsertMs = measureMs(() -> store.insertOne(single));

        List<Vacancy> batch = new ArrayList<>(dataset.subList(1, batchSize + 1));
        long batchInsertMs = measureMs(() -> store.insertBatch(batch));

        String targetUrl = batch.get(batchSize / 2).getUrl();
        long readByUrlMs = measureMs(() -> assertNotNull(store.findByUrl(targetUrl)));

        List<Vacancy> filtered = new ArrayList<>();
        long filteredReadMs = measureMs(() -> filtered.addAll(store.filter(city, filterDate.minusDays(3), filterDate.plusDays(3))));

        double writeThroughput = (batchSize + 1) / ((singleInsertMs + batchInsertMs) / 1000.0);
        double filterThroughput = filtered.isEmpty() ? 0.0 : filtered.size() / (filteredReadMs / 1000.0);

        return new StorageMetrics(store.name(), singleInsertMs, batchInsertMs, readByUrlMs, filteredReadMs, writeThroughput, filterThroughput, filtered.size());
    }

    private long measureMs(ThrowingRunnable runnable) {
        Instant start = Instant.now();
        try {
            runnable.run();
        } catch (Exception e) {
            throw new IllegalStateException("Benchmark step failed", e);
        }
        return Duration.between(start, Instant.now()).toMillis();
    }

    private void logMetrics(StorageMetrics metrics) {
        log.info("[{}] single insert={} ms, batch insert={} ms, readByUrl={} ms, filtered read={} ms ({} rows). Write throughput ~{} ops/s, filter throughput ~{} rows/s",
                metrics.backend(),
                metrics.singleInsertMs(),
                metrics.batchInsertMs(),
                metrics.readByUrlMs(),
                metrics.filteredReadMs(),
                metrics.filteredCount(),
                Math.round(metrics.writeOpsPerSecond()),
                Math.round(metrics.filterOpsPerSecond()));
    }

    private interface BenchmarkStore extends AutoCloseable {
        String name();

        void reset();

        void insertOne(Vacancy vacancy) throws Exception;

        void insertBatch(List<Vacancy> vacancies) throws Exception;

        Vacancy findByUrl(String url) throws Exception;

        List<Vacancy> filter(String city, LocalDate from, LocalDate to) throws Exception;

        @Override
        default void close() throws Exception {
        }
    }

    private static class PostgresStore implements BenchmarkStore {

        private final JdbcTemplate jdbcTemplate;

        PostgresStore(DataSource dataSource) {
            this.jdbcTemplate = new JdbcTemplate(dataSource);
            reset();
        }

        @Override
        public String name() {
            return "postgresql";
        }

        @Override
        public void reset() {
            jdbcTemplate.execute("DROP TABLE IF EXISTS vacancies");
            jdbcTemplate.execute("CREATE TABLE vacancies (" +
                    "id SERIAL PRIMARY KEY, " +
                    "title VARCHAR(255), " +
                    "company VARCHAR(255), " +
                    "salary VARCHAR(255), " +
                    "requirements TEXT, " +
                    "city VARCHAR(255), " +
                    "published_date DATE, " +
                    "source VARCHAR(50), " +
                    "url VARCHAR(1024) NOT NULL UNIQUE)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_vacancies_city_date ON vacancies(city, published_date)");
        }

        @Override
        public void insertOne(Vacancy vacancy) {
            jdbcTemplate.update("INSERT INTO vacancies (title, company, salary, requirements, city, published_date, source, url) VALUES (?,?,?,?,?,?,?,?)",
                    vacancy.getTitle(),
                    vacancy.getCompany(),
                    vacancy.getSalary(),
                    vacancy.getRequirements(),
                    vacancy.getCity(),
                    toSqlDate(vacancy.getPublishedDate()),
                    vacancy.getSource() == null ? null : vacancy.getSource().name(),
                    vacancy.getUrl());
        }

        @Override
        public void insertBatch(List<Vacancy> vacancies) {
            jdbcTemplate.batchUpdate("INSERT INTO vacancies (title, company, salary, requirements, city, published_date, source, url) VALUES (?,?,?,?,?,?,?,?)",
                    new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            Vacancy vacancy = vacancies.get(i);
                            ps.setString(1, vacancy.getTitle());
                            ps.setString(2, vacancy.getCompany());
                            ps.setString(3, vacancy.getSalary());
                            ps.setString(4, vacancy.getRequirements());
                            ps.setString(5, vacancy.getCity());
                            ps.setDate(6, toSqlDate(vacancy.getPublishedDate()));
                            ps.setString(7, vacancy.getSource() == null ? null : vacancy.getSource().name());
                            ps.setString(8, vacancy.getUrl());
                        }

                        @Override
                        public int getBatchSize() {
                            return vacancies.size();
                        }
                    });
        }

        @Override
        public Vacancy findByUrl(String url) {
            List<Vacancy> result = jdbcTemplate.query("SELECT * FROM vacancies WHERE url = ? LIMIT 1", this::mapVacancy, url);
            return result.isEmpty() ? null : result.get(0);
        }

        @Override
        public List<Vacancy> filter(String city, LocalDate from, LocalDate to) {
            return jdbcTemplate.query(
                    "SELECT * FROM vacancies WHERE city = ? AND published_date BETWEEN ? AND ?",
                    this::mapVacancy,
                    city,
                    toSqlDate(from),
                    toSqlDate(to));
        }

        private Vacancy mapVacancy(ResultSet rs, int rowNum) throws SQLException {
            Vacancy vacancy = new Vacancy();
            vacancy.setId(rs.getLong("id"));
            vacancy.setTitle(rs.getString("title"));
            vacancy.setCompany(rs.getString("company"));
            vacancy.setSalary(rs.getString("salary"));
            vacancy.setRequirements(rs.getString("requirements"));
            vacancy.setCity(rs.getString("city"));
            Date published = rs.getDate("published_date");
            vacancy.setPublishedDate(published == null ? null : published.toLocalDate());
            String source = rs.getString("source");
            vacancy.setSource(source == null ? null : VacancySource.valueOf(source));
            vacancy.setUrl(rs.getString("url"));
            return vacancy;
        }

        private Date toSqlDate(LocalDate date) {
            return date == null ? null : Date.valueOf(date);
        }
    }

    private static class MongoStore implements BenchmarkStore {

        private final MongoCollection<Document> collection;

        MongoStore(MongoClient client, String databaseName) {
            MongoDatabase database = client.getDatabase(databaseName);
            this.collection = database.getCollection("vacancies");
            reset();
        }

        @Override
        public String name() {
            return "mongodb";
        }

        @Override
        public void reset() {
            collection.drop();
            collection.createIndex(Indexes.ascending("url"), new IndexOptions().unique(true));
            collection.createIndex(Indexes.compoundIndex(Indexes.ascending("city"), Indexes.descending("publishedDate")));
        }

        @Override
        public void insertOne(Vacancy vacancy) {
            collection.insertOne(toDocument(vacancy));
        }

        @Override
        public void insertBatch(List<Vacancy> vacancies) {
            collection.insertMany(vacancies.stream().map(this::toDocument).collect(Collectors.toList()));
        }

        @Override
        public Vacancy findByUrl(String url) {
            Document document = collection.find(Filters.eq("url", url)).first();
            return document == null ? null : toVacancy(document);
        }

        @Override
        public List<Vacancy> filter(String city, LocalDate from, LocalDate to) {
            Bson filter = Filters.and(
                    Filters.eq("city", city),
                    Filters.gte("publishedDate", toDate(from)),
                    Filters.lte("publishedDate", toDate(to))
            );
            FindIterable<Document> documents = collection.find(filter);
            List<Vacancy> result = new ArrayList<>();
            for (Document document : documents) {
                result.add(toVacancy(document));
            }
            return result;
        }

        private Document toDocument(Vacancy vacancy) {
            Document document = new Document();
            document.append("title", vacancy.getTitle());
            document.append("company", vacancy.getCompany());
            document.append("salary", vacancy.getSalary());
            document.append("requirements", vacancy.getRequirements());
            document.append("city", vacancy.getCity());
            document.append("publishedDate", toDate(vacancy.getPublishedDate()));
            document.append("source", vacancy.getSource() == null ? null : vacancy.getSource().name());
            document.append("url", vacancy.getUrl());
            return document;
        }

        private Vacancy toVacancy(Document document) {
            Vacancy vacancy = new Vacancy();
            vacancy.setTitle(document.getString("title"));
            vacancy.setCompany(document.getString("company"));
            vacancy.setSalary(document.getString("salary"));
            vacancy.setRequirements(document.getString("requirements"));
            vacancy.setCity(document.getString("city"));
            java.util.Date published = document.getDate("publishedDate");
            vacancy.setPublishedDate(published == null ? null :
                    published.toInstant().atZone(ZoneOffset.UTC).toLocalDate());
            String source = document.getString("source");
            vacancy.setSource(source == null ? null : VacancySource.valueOf(source));
            vacancy.setUrl(document.getString("url"));
            return vacancy;
        }

        private java.util.Date toDate(LocalDate localDate) {
            return localDate == null ? null : java.util.Date.from(localDate.atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        }
    }

    private record StorageMetrics(String backend,
                                  long singleInsertMs,
                                  long batchInsertMs,
                                  long readByUrlMs,
                                  long filteredReadMs,
                                  double writeOpsPerSecond,
                                  double filterOpsPerSecond,
                                  long filteredCount) {
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static class TestDataFactory {
        private static final List<String> CITIES = List.of("City-0", "City-1", "City-2", "City-3");
        private static final Random RANDOM = new Random(42);

        static List<Vacancy> generate(int count) {
            List<Vacancy> vacancies = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                vacancies.add(single(i));
            }
            return vacancies;
        }

        private static Vacancy single(int idx) {
            String title = "Java Developer " + idx;
            String company = "Company " + (idx % 15);
            String salary = (80_000 + RANDOM.nextInt(80_000)) + "-" + (120_000 + RANDOM.nextInt(120_000));
            String requirements = "Java, Spring, REST, SQL #" + idx;
            String city = CITIES.get(idx % CITIES.size());
            LocalDate publishedDate = LocalDate.now().minusDays(idx % 14);
            VacancySource source = VacancySource.values()[idx % VacancySource.values().length];
            String url = "https://jobs.example.com/" + source.name().toLowerCase(Locale.ROOT) + "/" + idx;
            return new Vacancy(title, company, salary, requirements, city, publishedDate, source, url);
        }
    }
}

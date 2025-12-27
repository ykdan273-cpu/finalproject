package com.example.vacancyscraper.storage;

import com.example.vacancyscraper.model.Vacancy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface VacancyRepository extends JpaRepository<Vacancy, Long> {

    Optional<Vacancy> findByUrl(String url);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vacancy v where v.url = :url")
    Optional<Vacancy> findByUrlForUpdate(@Param("url") String url);
}

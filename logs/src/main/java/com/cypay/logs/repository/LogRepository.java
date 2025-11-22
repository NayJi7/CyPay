package com.cypay.logs.repository;

import com.cypay.logs.model.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LogRepository extends JpaRepository<LogEntry, Long> {
    List<LogEntry> findByActeur(String acteur);

}


package com.cypay.logs.controller;

import com.cypay.logs.dto.LogSummary;
import com.cypay.logs.model.LogEntry;
import com.cypay.logs.agent.SupervisorAgent;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/logs")
public class LogController {

    private final SupervisorAgent logSupervisor;

    public LogController(SupervisorAgent logSupervisor) {
        this.logSupervisor = logSupervisor;

        // 🔥 On démarre l’acteur dès que le contrôleur est instancié
        this.logSupervisor.demarrer();
    }

    /**
     * GET /logs/all
     * → Récupère tous les logs (Spring + JPA)
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllLogs() {
        try {
            // 👇 On envoie un message à l’acteur pour tracer la requête
            logSupervisor.envoyerObjet("GET_ALL");

            // 👇 On récupère les données en direct via Spring
            List<LogEntry> logs = logSupervisor.getAllLogs();

            List<LogSummary> summaries = logs.stream()
                    .map(log -> new LogSummary(
                            log.getId(),
                            log.getActeur(),
                            log.getNiveau(),
                            log.getMessage(),
                            log.getLogTime()))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(summaries);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la récupération des logs : " + e.getMessage());
        }
    }

    /**
     * GET /logs/actor/{acteur}
     * → Récupère les logs par acteur
     */
    @GetMapping("/actor/{acteur}")
    public ResponseEntity<?> getLogsByActor(@PathVariable String acteur) {
        try {
            // 👇 Log de la requête dans ton framework
            logSupervisor.envoyerObjet("GET_ACTOR:" + acteur);

            List<LogEntry> logs = logSupervisor.getLogsByActeur(acteur);

            List<LogSummary> summaries = logs.stream()
                    .map(log -> new LogSummary(
                            log.getId(),
                            log.getActeur(),
                            log.getNiveau(),
                            log.getMessage(),
                            log.getLogTime()))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(summaries);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la récupération des logs : " + e.getMessage());
        }
    }
}

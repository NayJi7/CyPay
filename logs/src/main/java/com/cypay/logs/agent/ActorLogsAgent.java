package com.cypay.logs.agent;

import com.cypay.framework.acteur.Acteur;
import com.cypay.framework.acteur.ActeurLogger;
import com.cypay.logs.model.LogEntry;
import com.cypay.logs.repository.LogRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActorLogsAgent extends Acteur<Object> implements LogAgent {

    private final LogRepository logRepository;

    public ActorLogsAgent(LogRepository logRepository) {
        super("ActorLogsAgent");
        this.logger = new ActeurLogger(
                "ActorLogsAgent",
                true,
                "jdbc:postgresql://db.yldotyunksweuovyknzg.supabase.co:5432/postgres",
                "postgres",
                "Cypay.Cytech"
        );
        this.logRepository = logRepository;
    }

    @PostConstruct
    public void startAgent() {
        demarrer();
    }

    @Override
    protected void traiterMessage(Object message) {
        getLogger().info("Message reçu : " + message);

        if (message instanceof String acteur) {
            List<LogEntry> logs = getLogs(acteur);
            getLogger().info("Logs récupérés pour l'acteur [" + acteur + "] : " + logs.size());
        } else {
            getLogger().info("Message non reconnu : " + message);
        }
    }

    @Override
    public List<LogEntry> getLogs(String acteur) {
        if (acteur == null || acteur.isEmpty()) {
            getLogger().info("Aucun acteur fourni → retour liste vide");
            return List.of();
        }

        List<LogEntry> logs = logRepository.findByActeur(acteur);
        getLogger().info("Récupération logs pour acteur : " + acteur + " → " + logs.size());
        return logs;
    }
}

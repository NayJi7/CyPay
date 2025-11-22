package com.cypay.logs.agent;

import com.cypay.framework.acteur.Acteur;
import com.cypay.framework.acteur.ActeurLogger;
import com.cypay.logs.model.LogEntry;
import com.cypay.logs.repository.LogRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AllLogsAgent extends Acteur<Object> implements LogAgent {

    private final LogRepository logRepository;

    public AllLogsAgent(LogRepository logRepository) {
        super("AllLogsAgent");
        this.logger = new ActeurLogger(
                "AllLogsAgent",
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

        // Si message = "GET_ALL", récupérer et loguer tous les logs
        if (message instanceof String cmd && cmd.equals("GET_ALL")) {
            List<LogEntry> logs = getLogs(null);
            getLogger().info("Total logs en base : " + logs.size());
        } else {
            getLogger().info("Message non reconnu : " + message);
        }
    }

    @Override
    public List<LogEntry> getLogs(String acteur) {
        List<LogEntry> logs = logRepository.findAll();
        getLogger().info("Récupération de tous les logs → " + logs.size());
        return logs;
    }
}

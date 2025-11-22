package com.cypay.logs.agent;

import com.cypay.framework.acteur.Acteur;
import com.cypay.framework.acteur.ActeurLogger;
import com.cypay.logs.model.LogEntry;
import com.cypay.logs.repository.LogRepository;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;
import java.util.List;

/**
 * SupervisorAgent "paresseux" : démarre les agents seulement à la demande
 */
@Component
public class SupervisorAgent extends Acteur<Object> {

    private final LogRepository logRepository;

    // Références aux agents gérées dynamiquement
    private final AtomicReference<AllLogsAgent> allLogsAgentRef = new AtomicReference<>();
    private final AtomicReference<ActorLogsAgent> actorLogsAgentRef = new AtomicReference<>();

    public SupervisorAgent(LogRepository logRepository) {
        super("LogsSupervisorActor");
        this.logger = new ActeurLogger(
                "LogsSupervisorActor",
                true,
                "jdbc:postgresql://db.yldotyunksweuovyknzg.supabase.co:5432/postgres",
                "postgres",
                "Cypay.Cytech"
        );
        this.logRepository = logRepository;
    }

    @Override
    protected void traiterMessage(Object message) {
        getLogger().info("Message reçu : " + message);

        if (!(message instanceof String cmd)) {
            getLogger().info("Message non reconnu : " + message);
            return;
        }

        // Démarrage paresseux des agents
        if (cmd.equals("GET_ALL")) {
            AllLogsAgent allAgent = allLogsAgentRef.updateAndGet(existing -> {
                if (existing == null) {
                    AllLogsAgent agent = new AllLogsAgent(logRepository);
                    agent.demarrer();
                    return agent;
                }
                return existing;
            });
            envoyerVers(allAgent, "GET_ALL");

        } else if (cmd.startsWith("GET_ACTOR:")) {
            String acteur = cmd.substring("GET_ACTOR:".length());
            ActorLogsAgent actorAgent = actorLogsAgentRef.updateAndGet(existing -> {
                if (existing == null) {
                    ActorLogsAgent agent = new ActorLogsAgent(logRepository);
                    agent.demarrer();
                    return agent;
                }
                return existing;
            });
            envoyerVers(actorAgent, acteur);

        } else {
            getLogger().info("Commande non reconnue : " + cmd);
        }
    }
}

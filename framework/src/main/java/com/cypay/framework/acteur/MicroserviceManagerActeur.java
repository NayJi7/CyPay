package com.cypay.framework.acteur;

import com.cypay.framework.acteur.Acteur;
import com.cypay.framework.acteur.Message;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MicroserviceManagerActeur extends Acteur<Object> {

    private final Map<Integer, Process> processes = new HashMap<>();
    private final Map<Integer, String> services = new HashMap<>();

    public MicroserviceManagerActeur() {
        super("MicroserviceManager");
    }

    @Override
    protected void traiterMessage(Object message) {
        if (message instanceof StartRequest req) {
            lancerMicroservice(req);
        }
        else if (message instanceof StopRequest req) {
            arreterMicroservice(req.port());
        }
        else if (message instanceof ListRequest) {
            afficherMicroservices();
        }
    }

    // ========== LANCEMENT DU MICROSERVICE ==========
    private void lancerMicroservice(StartRequest req) {
        try {
            int port = req.port();
            String path = req.path();
            String name = req.name();
            logger.info("[START] Démarrage du microservice " + name + " sur le port " + port);
            ProcessBuilder builder = new ProcessBuilder(
                    "java",
                    "-jar",
                    path,
                    "--server.port=" + port
            );
            builder.redirectErrorStream(true);
            Process process = builder.start();
            processes.put(port, process);
            services.put(port, name);
            logger.info("[SUCCESS] Microservice démarré : " + name + " (port " + port + ")");
        } catch (IOException e) {
            logger.erreur("[ERROR] Erreur lors du démarrage du microservice", e);
        }
    }


    // ========== ARRET DU MICROSERVICE ==========
    private void arreterMicroservice(int port) {
        Process p = processes.get(port);
        if (p == null) {
            logger.info("[WARN] Aucun microservice sur le port " + port);
            return;
        }
        logger.info("[STOP] Arrêt du microservice " + services.get(port) + " sur le port " + port);
        p.destroy();
        processes.remove(port);
        services.remove(port);
        logger.info("[SUCCESS] Microservice arrêté");
    }


    // ========== LISTE ==========
    private void afficherMicroservices() {
        logger.info("[LIST] === Microservices actifs ===");
        services.forEach((p, name) ->
            logger.info("[LIST] Port : " + p + " | Nom : " + name)
        );
        if (services.isEmpty()) logger.info("[LIST] Aucun microservice actif.");
    }


    // ========== TYPES DE MESSAGES ==========
    public record StartRequest(String path, int port, String name) {}
    public record StopRequest(int port) {}
    public record ListRequest() {}
}

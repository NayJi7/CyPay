package com.example.transactions;

import com.example.transactions.agent.TransactionHttpActeur;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TransactionApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionApplication.class, args);
    }

    @Bean
    public CommandLineRunner startActorSystem(
            TransactionHttpActeur transactionHttpActeur,
            @Value("${server.port:8081}") int port
    ) {
        return args -> {
            System.out.println("╔════════════════════════════════════════════════╗");
            System.out.println("║   🚀 DÉMARRAGE DU MICROSERVICE TRANSACTIONS   ║");
            System.out.println("╚════════════════════════════════════════════════╝");
            System.out.println();

            System.out.println("📋 Démarrage de l'acteur HTTP Transactions sur le port " + port);
            transactionHttpActeur.demarrer();
            transactionHttpActeur.startHttpServer(port);
            System.out.println("✅ Acteur HTTP Transactions démarré et prêt à recevoir des requêtes");
        };
    }
}

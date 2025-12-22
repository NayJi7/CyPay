package com.example.wallet;

import com.example.wallet.acteur.WalletHttpActeur;
import com.example.wallet.service.WalletService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class WalletServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalletServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner startActorSystem(
            WalletService walletService,
            @Value("${server.port:8083}") int port,
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.expiration}") long jwtExpiration
    ) {
        return args -> {
            System.out.println("╔════════════════════════════════════════════════╗");
            System.out.println("║   🚀 DÉMARRAGE DU MICROSERVICE WALLET         ║");
            System.out.println("╚════════════════════════════════════════════════╝");
            System.out.println();

            System.out.println("📋 Démarrage de l'acteur HTTP Wallet sur le port " + port);
            WalletHttpActeur walletHttpActeur = new WalletHttpActeur(walletService, jwtSecret, jwtExpiration);
            walletHttpActeur.demarrer();
            walletHttpActeur.startHttpServer(port);
            System.out.println("✅ Acteur HTTP Wallet démarré et prêt à recevoir des requêtes");
        };
    }
}

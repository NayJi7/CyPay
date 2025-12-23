package com.example.user;

import com.example.user.acteur.UserHttpActeur;
import com.example.user.acteur.SuperviseurActeur;
import com.example.user.acteur.MonitoringActeur;
import com.example.user.repository.UserRepository;
import com.example.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * ✅ Application utilisant le framework d'acteurs
 * Plus de controller Spring, tout passe par UserHttpActeur
 */
@SpringBootApplication
public class UserApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }

    /**
     * ✅ Initialise et démarre l'acteur HTTP au démarrage de l'application
     */
    @Bean
    public CommandLineRunner startActorSystem(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${actor.port:8082}") int port,
            @Value("${monitoring.port:9090}") int monitoringPort,
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.expiration}") long jwtExpiration,
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.datasource.username}") String dbUser,
            @Value("${spring.datasource.password}") String dbPassword
    ) {
        return args -> {
            System.out.println("╔════════════════════════════════════════════════╗");
            System.out.println("║   🚀 DÉMARRAGE DU SYSTÈME D'ACTEURS           ║");
            System.out.println("╚════════════════════════════════════════════════╝");
            System.out.println();

            // ✅ 1. Créer et démarrer le superviseur
            System.out.println("📋 Étape 1/5 : Initialisation du superviseur");
            SuperviseurActeur superviseur = new SuperviseurActeur();
            superviseur.demarrer();
            System.out.println("✅ Superviseur démarré avec vérifications automatiques");
            System.out.println();

            // ✅ 2. Créer le service métier
            System.out.println("📋 Étape 2/5 : Initialisation des services métier");
            UserService userService = new UserService(userRepository, passwordEncoder);
            System.out.println("✅ Services métier initialisés");
            System.out.println();

            // ✅ 3. Créer et enregistrer l'acteur HTTP
            System.out.println("📋 Étape 3/5 : Création de l'acteur HTTP principal");
            UserHttpActeur httpActeur = new UserHttpActeur(userService, jwtSecret, jwtExpiration, jdbcUrl, dbUser, dbPassword);
            superviseur.enregistrerActeur("UserHttpActeur", httpActeur);
            System.out.println();

            // ✅ 4. Créer et enregistrer l'acteur de monitoring
            System.out.println("📋 Étape 4/5 : Création de l'acteur de monitoring");
            MonitoringActeur monitoringActeur = new MonitoringActeur(superviseur);
            superviseur.enregistrerActeur("MonitoringActeur", monitoringActeur);
            System.out.println();

            // ✅ 5. Le superviseur démarre les acteurs un par un
            System.out.println("📋 Étape 5/5 : Démarrage des acteurs supervisés");
            System.out.println("───────────────────────────────────────────────");

            superviseur.demarrerActeur("UserHttpActeur");
            httpActeur.startHttpServer(port);

            superviseur.demarrerActeur("MonitoringActeur");
            monitoringActeur.startMonitoring(monitoringPort);

            System.out.println("───────────────────────────────────────────────");
            System.out.println();

            // ✅ Affichage du résumé
            System.out.println("╔════════════════════════════════════════════════╗");
            System.out.println("║   ✅ SYSTÈME OPÉRATIONNEL                     ║");
            System.out.println("╚════════════════════════════════════════════════╝");
            System.out.println();
            System.out.println("🌐 API Utilisateurs : http://localhost:" + port);
            System.out.println("   POST   /users/register     - Inscription");
            System.out.println("   POST   /users/login        - Connexion");
            System.out.println("   GET    /users/me           - Mon profil");
            System.out.println("   PUT    /users/me           - Modifier profil");
            System.out.println("   DELETE /users/me           - Supprimer compte");
            System.out.println("   GET    /users              - Liste utilisateurs");
            System.out.println("   GET    /users/{id}         - Profil par ID");
            System.out.println();
            System.out.println("🔍 API Monitoring : http://localhost:" + monitoringPort);
            System.out.println("   GET    /health             - Vérification santé");
            System.out.println("   GET    /stats              - Statistiques");
            System.out.println("   POST   /restart?actor=X    - Redémarrer acteur");
            System.out.println("   POST   /shutdown           - Arrêt système");
            System.out.println();
            System.out.println("🏥 Supervision active :");
            System.out.println("   ✓ Vérifications automatiques toutes les 10s");
            System.out.println("   ✓ Redémarrage automatique en cas d'erreur");
            System.out.println("   ✓ Max 3 tentatives par minute");
            System.out.println();

            // ✅ 6. Health check initial après 2 secondes
            Thread.sleep(2000);
            superviseur.envoyerObjet(new SuperviseurActeur.HealthCheckRequest());

            // ✅ 7. Hook d'arrêt propre
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println();
                System.out.println("╔════════════════════════════════════════════════╗");
                System.out.println("║   🛑 ARRÊT DU SYSTÈME                         ║");
                System.out.println("╚════════════════════════════════════════════════╝");
                monitoringActeur.stopMonitoring();
                httpActeur.stopHttpServer();
                superviseur.envoyerObjet(new SuperviseurActeur.ShutdownRequest());
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("✅ Système arrêté proprement");
            }));
        };
    }
}
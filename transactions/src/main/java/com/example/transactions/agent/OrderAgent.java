package com.example.transactions.agent;
// Fichier supprimé : OrderAgent.java (fonctionnalité non utilisée)
import com.cypay.framework.acteur.Acteur;
import com.cypay.framework.acteur.Message;
import com.example.transactions.message.OrderMessage;
import com.example.transactions.message.CreateBlockchainMessage;
import com.example.transactions.model.TransactionType;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OrderAgent extends Acteur<OrderMessage> {

    @Autowired
    private CreateBlockchainAgent createBlockchainAgent;

    @Autowired
    public OrderAgent(
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.datasource.username}") String dbUser,
            @Value("${spring.datasource.password}") String dbPassword) {
        super("OrderAgent", true, jdbcUrl, dbUser, dbPassword);
    }

    @PostConstruct
    public void init() {
        demarrer();
        logger.info("[INIT] OrderAgent démarré");
    }

    public void send(OrderMessage message) {
        logger.info("[SEND] Reçu une demande d'ordre de " + message.getUserId() + " (origine: SupervisorAgent)");
        envoyer(new Message<>("SupervisorAgent", message));
    }

    @Override
    protected void traiterMessage(OrderMessage message) {
        logger.info("[PROCESS] Traitement d'un ordre utilisateur: " + message);
        try {
            // TODO: Logique complète à implémenter (prix, déclenchement, etc.)
            logger.info("[CHECK] Enregistrement de l'ordre dans la blockchain (appel CreateBlockchainAgent)");
            CreateBlockchainMessage blockchainMessage = new CreateBlockchainMessage(
                    TransactionType.ORDER,
                    message.getUserId(),
                    null,
                    message.getAmount(),
                    message.getCryptoUnit()
            );
            createBlockchainAgent.send(blockchainMessage);
            logger.info("[SUCCESS] Ordre programmé et enregistré (prix cible: " + message.getTargetPrice() + ")");
        } catch (Exception e) {
            logger.erreur("[ERROR] Erreur lors du traitement de l'ordre", e);
        }
    }
}

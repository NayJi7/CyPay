package com.example.transactions.agent;

import com.cypay.framework.acteur.ActeurLogger;
import com.example.transactions.message.OrderMessage;
import com.example.transactions.message.CreateBlockchainMessage;
import com.example.transactions.model.TransactionType;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class OrderAgent implements Runnable {

    @Autowired
    private CreateBlockchainAgent createBlockchainAgent;

    @Value("${spring.datasource.url}")
    private String jdbcUrl;
    @Value("${spring.datasource.username}")
    private String dbUser;
    @Value("${spring.datasource.password}")
    private String dbPassword;

    private ActeurLogger logger;
    private final BlockingQueue<OrderMessage> mailbox = new LinkedBlockingQueue<>();
    private Thread thread;

    @PostConstruct
    public void init() {
        this.logger = new ActeurLogger("OrderAgent", true, jdbcUrl, dbUser, dbPassword);
        thread = new Thread(this, "OrderAgent");
        thread.start();
    }

    public void send(OrderMessage message) {
        try {
            mailbox.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (logger != null) logger.erreur("Erreur lors de l'envoi du message", e);
        }
    }

    @Override
    public void run() {
        logger.info("OrderAgent démarré");

        while (!Thread.currentThread().isInterrupted()) {
            try {
                OrderMessage message = mailbox.take();
                processMessage(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.erreur("OrderAgent interrompu", e);
            }
        }
    }

    private void processMessage(OrderMessage message) {
        logger.info("Ordre programmé: " + message);

        try {
            // TODO: Implémenter la logique d'ordre programmé
            // 1. Vérifier le prix actuel de la crypto (API externe ou service de prix)
            // 2. Comparer avec le prix cible (message.getTargetPrice())
            // 3. Si le prix cible est atteint:
            //    - Déclencher l'achat ou la vente selon message.getOrderType()
            //    - Appeler BuyAgent ou SellAgent
            // 4. Sinon, programmer un rappel périodique pour vérifier le prix

            // Pour l'instant, on enregistre juste l'ordre dans la blockchain
            CreateBlockchainMessage blockchainMessage = new CreateBlockchainMessage(
                    TransactionType.ORDER,
                    message.getUserId(),
                    null,
                    message.getAmount(),
                    message.getCryptoUnit()
            );

            createBlockchainAgent.send(blockchainMessage);

            logger.info("Ordre programmé enregistré avec succès (prix cible: " + message.getTargetPrice() + ")");

        } catch (Exception e) {
            logger.erreur("Erreur lors du traitement de l'ordre", e);
        }
    }
}

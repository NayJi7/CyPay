package com.example.transactions.agent;

import com.cypay.framework.acteur.ActeurLogger;
import com.example.transactions.message.CreateBlockchainMessage;
import com.example.transactions.model.Transaction;
import com.example.transactions.model.TransactionStatus;
import com.example.transactions.service.DatabaseService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class CreateBlockchainAgent implements Runnable {

    @Autowired
    private DatabaseService databaseService;

    @Value("${spring.datasource.url}")
    private String jdbcUrl;
    @Value("${spring.datasource.username}")
    private String dbUser;
    @Value("${spring.datasource.password}")
    private String dbPassword;

    private ActeurLogger logger;
    private final BlockingQueue<CreateBlockchainMessage> mailbox = new LinkedBlockingQueue<>();
    private Thread thread;

    @PostConstruct
    public void init() {
        this.logger = new ActeurLogger("CreateBlockchainAgent", true, jdbcUrl, dbUser, dbPassword);
        thread = new Thread(this, "CreateBlockchainAgent");
        thread.start();
    }

    public void send(CreateBlockchainMessage message) {
        try {
            mailbox.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (logger != null) logger.erreur("Erreur lors de l'envoi du message", e);
        }
    }

    @Override
    public void run() {
        logger.info("CreateBlockchainAgent démarré");

        while (!Thread.currentThread().isInterrupted()) {
            try {
                CreateBlockchainMessage message = mailbox.take();
                processMessage(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.erreur("CreateBlockchainAgent interrompu", e);
            }
        }
    }

    private void processMessage(CreateBlockchainMessage message) {
        logger.info("Création d'une transaction blockchain: " + message);

        try {
            // Créer la transaction
            Transaction transaction = new Transaction();
            transaction.setType(message.getType());
            transaction.setActor1(message.getActor1());
            transaction.setActor2(message.getActor2());
            transaction.setAmount(message.getAmount());
            transaction.setUnit(message.getUnit());
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setMessage("Transaction enregistrée dans la blockchain");

            // Sauvegarder dans la blockchain
            Transaction saved = databaseService.saveTransaction(transaction);

            logger.info("Transaction blockchain créée avec succès: ID=" + saved.getId());

        } catch (Exception e) {
            logger.erreur("Erreur lors de la création de la transaction blockchain", e);
        }
    }
}

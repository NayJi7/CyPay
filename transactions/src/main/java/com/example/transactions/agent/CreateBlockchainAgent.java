package com.example.transactions.agent;

import com.cypay.framework.acteur.Acteur;
import com.cypay.framework.acteur.Message;
import com.example.transactions.message.CreateBlockchainMessage;
import com.example.transactions.model.Transaction;
import com.example.transactions.model.TransactionStatus;
import com.example.transactions.service.DatabaseService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CreateBlockchainAgent extends Acteur<CreateBlockchainMessage> {

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    public CreateBlockchainAgent(
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.datasource.username}") String dbUser,
            @Value("${spring.datasource.password}") String dbPassword) {
        super("CreateBlockchainAgent", true, jdbcUrl, dbUser, dbPassword);
    }

    @PostConstruct
    public void init() {
        demarrer();
        logger.info("[INIT] CreateBlockchainAgent démarré");
    }

    /**
     * Entry point for SupervisorAgent
     */
    public void send(CreateBlockchainMessage message) {
        logger.info("[SEND] Reçu une demande d'enregistrement blockchain (origine: SupervisorAgent)");
        envoyer(new Message<>("SupervisorAgent", message));
    }

    @Override
    protected void traiterMessage(CreateBlockchainMessage message) {
        logger.info("[PROCESS] Création d'une transaction blockchain: " + message);
        try {
            Transaction transaction = new Transaction();
            transaction.setType(message.getType());
            transaction.setActor1(message.getActor1());
            transaction.setActor2(message.getActor2());
            transaction.setAmount(message.getAmount());
            transaction.setUnit(message.getUnit());
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setMessage("Transaction enregistrée dans la blockchain");
            Transaction saved = databaseService.saveTransaction(transaction);
            logger.info("[SUCCESS] Transaction blockchain créée: ID=" + saved.getId());
        } catch (Exception e) {
            logger.erreur("[ERROR] Erreur lors de la création de la transaction blockchain", e);
        }
    }
}

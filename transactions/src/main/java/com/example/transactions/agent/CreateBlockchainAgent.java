package com.example.transactions.agent;

import com.example.transactions.message.CreateBlockchainMessage;
import com.example.transactions.model.Transaction;
import com.example.transactions.model.TransactionStatus;
import com.example.transactions.service.DatabaseService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class CreateBlockchainAgent implements Runnable {

    @Autowired
    private DatabaseService databaseService;

    private final BlockingQueue<CreateBlockchainMessage> mailbox = new LinkedBlockingQueue<>();
    private Thread thread;

    @PostConstruct
    public void init() {
        thread = new Thread(this, "CreateBlockchainAgent");
        thread.start();
    }

    public void send(CreateBlockchainMessage message) {
        try {
            mailbox.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Erreur lors de l'envoi du message: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        System.out.println("CreateBlockchainAgent démarré");

        while (!Thread.currentThread().isInterrupted()) {
            try {
                CreateBlockchainMessage message = mailbox.take();
                processMessage(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("CreateBlockchainAgent interrompu");
            }
        }
    }

    private void processMessage(CreateBlockchainMessage message) {
        System.out.println("Création d'une transaction blockchain: " + message);

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

            System.out.println("Transaction blockchain créée avec succès: ID=" + saved.getId());

        } catch (Exception e) {
            System.err.println("Erreur lors de la création de la transaction blockchain: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

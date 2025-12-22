package com.example.transactions.agent;

import com.example.transactions.message.TransferMessage;
import com.example.transactions.message.CreateBlockchainMessage;
import com.example.transactions.model.TransactionType;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class TransferAgent implements Runnable {

    @Autowired
    private CreateBlockchainAgent createBlockchainAgent;

    private final BlockingQueue<TransferMessage> mailbox = new LinkedBlockingQueue<>();
    private Thread thread;

    @PostConstruct
    public void init() {
        thread = new Thread(this, "TransferAgent");
        thread.start();
    }

    public void send(TransferMessage message) {
        try {
            mailbox.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Erreur lors de l'envoi du message: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        System.out.println("TransferAgent démarré");

        while (!Thread.currentThread().isInterrupted()) {
            try {
                TransferMessage message = mailbox.take();
                processMessage(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("TransferAgent interrompu");
            }
        }
    }

    private void processMessage(TransferMessage message) {
        System.out.println("Virement de crypto: " + message);

        try {
            // Validation de base
            if (message.getFromUserId().equals(message.getToUserId())) {
                System.err.println("Impossible de faire un virement vers soi-même");
                return;
            }

            // TODO: Quand Wallet sera créé, décommenter:
            // 1. Vérifier que l'expéditeur a assez de crypto
            // GET http://localhost:8082/wallets/{fromUserId}/balance/{cryptoUnit}
            // boolean hasBalance = walletClient.hasBalance(message.getFromUserId(), message.getCryptoUnit(), message.getAmount());
            // if (!hasBalance) {
            //     System.err.println("Solde crypto insuffisant pour le virement");
            //     return;
            // }

            // 2. Effectuer le virement (débit + crédit atomique)
            // POST http://localhost:8082/wallets/transfer
            // Body: { "fromUserId": ..., "toUserId": ..., "unit": "BTC", "amount": ... }
            // boolean success = walletClient.transfer(message.getFromUserId(), message.getToUserId(), message.getCryptoUnit(), message.getAmount());
            // if (!success) {
            //     System.err.println("Échec du virement");
            //     return;
            // }

            // Pour l'instant, on enregistre juste la transaction
            CreateBlockchainMessage blockchainMessage = new CreateBlockchainMessage(
                    TransactionType.TRANSFER,
                    message.getFromUserId(),
                    message.getToUserId(),
                    message.getAmount(),
                    message.getCryptoUnit()
            );

            createBlockchainAgent.send(blockchainMessage);

            System.out.println("Virement de crypto enregistré avec succès");

        } catch (Exception e) {
            System.err.println("Erreur lors du virement de crypto: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

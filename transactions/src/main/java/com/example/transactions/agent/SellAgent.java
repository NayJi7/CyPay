package com.example.transactions.agent;

import com.example.transactions.message.SellMessage;
import com.example.transactions.message.CreateBlockchainMessage;
import com.example.transactions.model.TransactionType;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class SellAgent implements Runnable {

    @Autowired
    private CreateBlockchainAgent createBlockchainAgent;

    private final BlockingQueue<SellMessage> mailbox = new LinkedBlockingQueue<>();
    private Thread thread;

    @PostConstruct
    public void init() {
        thread = new Thread(this, "SellAgent");
        thread.start();
    }

    public void send(SellMessage message) {
        try {
            mailbox.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Erreur lors de l'envoi du message: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        System.out.println("SellAgent démarré");

        while (!Thread.currentThread().isInterrupted()) {
            try {
                SellMessage message = mailbox.take();
                processMessage(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("SellAgent interrompu");
            }
        }
    }

    private void processMessage(SellMessage message) {
        System.out.println("Vente de crypto: " + message);

        try {
            // TODO: Quand Wallet sera créé, décommenter:
            // 1. Vérifier que l'utilisateur a assez de crypto
            // GET http://localhost:8082/wallets/{userId}/balance/{cryptoUnit}
            // boolean hasBalance = walletClient.hasBalance(message.getUserId(), message.getCryptoUnit(), message.getAmount());
            // if (!hasBalance) {
            //     System.err.println("Solde crypto insuffisant pour la vente");
            //     return;
            // }

            // 2. Débiter le compte en crypto
            // POST http://localhost:8082/wallets/{userId}/debit
            // Body: { "unit": "BTC", "amount": message.getAmount() }
            // walletClient.debitAccount(message.getUserId(), message.getCryptoUnit(), message.getAmount());

            // 3. Créditer le compte en EUR/USD
            // POST http://localhost:8082/wallets/{userId}/credit
            // Body: { "unit": "EUR", "amount": calculatedAmount }
            // walletClient.creditAccount(message.getUserId(), message.getTargetUnit(), calculatedAmount);

            // Pour l'instant, on enregistre juste la transaction
            CreateBlockchainMessage blockchainMessage = new CreateBlockchainMessage(
                    TransactionType.SELL,
                    message.getUserId(),
                    null, // Pas d'acteur 2 pour une vente
                    message.getAmount(),
                    message.getCryptoUnit()
            );

            createBlockchainAgent.send(blockchainMessage);

            System.out.println("Vente de crypto enregistrée avec succès");

        } catch (Exception e) {
            System.err.println("Erreur lors de la vente de crypto: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

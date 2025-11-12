package com.example.transactions.agent;

import com.example.transactions.message.BuyMessage;
import com.example.transactions.message.CreateBlockchainMessage;
import com.example.transactions.model.TransactionType;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class BuyAgent implements Runnable {

    @Autowired
    private CreateBlockchainAgent createBlockchainAgent;

    private final BlockingQueue<BuyMessage> mailbox = new LinkedBlockingQueue<>();
    private Thread thread;

    @PostConstruct
    public void init() {
        thread = new Thread(this, "BuyAgent");
        thread.start();
    }

    public void send(BuyMessage message) {
        try {
            mailbox.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Erreur lors de l'envoi du message: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        System.out.println("BuyAgent démarré");

        while (!Thread.currentThread().isInterrupted()) {
            try {
                BuyMessage message = mailbox.take();
                processMessage(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("BuyAgent interrompu");
            }
        }
    }

    private void processMessage(BuyMessage message) {
        System.out.println("Achat de crypto: " + message);

        try {
            // TODO: Quand Wallet sera créé, décommenter:
            // 1. Vérifier que l'utilisateur a assez de fonds en EUR/USD
            // GET http://localhost:8082/wallets/{userId}/balance/{paymentUnit}
            // boolean hasBalance = walletClient.hasBalance(message.getUserId(), message.getPaymentUnit(), calculatedAmount);
            // if (!hasBalance) {
            //     System.err.println("Solde insuffisant pour l'achat");
            //     return;
            // }

            // 2. Débiter le compte en EUR/USD
            // POST http://localhost:8082/wallets/{userId}/debit
            // Body: { "unit": "EUR", "amount": calculatedAmount }
            // walletClient.debitAccount(message.getUserId(), message.getPaymentUnit(), calculatedAmount);

            // 3. Créditer le compte en crypto
            // POST http://localhost:8082/wallets/{userId}/credit
            // Body: { "unit": "BTC", "amount": message.getAmount() }
            // walletClient.creditAccount(message.getUserId(), message.getCryptoUnit(), message.getAmount());

            // Pour l'instant, on enregistre juste la transaction
            CreateBlockchainMessage blockchainMessage = new CreateBlockchainMessage(
                    TransactionType.BUY,
                    message.getUserId(),
                    null, // Pas d'acteur 2 pour un achat
                    message.getAmount(),
                    message.getCryptoUnit()
            );

            createBlockchainAgent.send(blockchainMessage);

            System.out.println("Achat de crypto enregistré avec succès");

        } catch (Exception e) {
            System.err.println("Erreur lors de l'achat de crypto: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

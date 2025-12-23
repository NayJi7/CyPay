package com.example.transactions.agent;

import com.cypay.framework.acteur.Acteur;
import com.cypay.framework.http.HttpResponse;
import com.example.transactions.message.TransferMessage;
import com.example.transactions.message.CreateBlockchainMessage;
import com.example.transactions.model.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class TransferAgent extends Acteur<TransferMessage> {

    @Autowired
    private CreateBlockchainAgent createBlockchainAgent;

    @Value("${wallet.service.url}")
    private String walletServiceUrl;

    @Autowired
    public TransferAgent(
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.datasource.username}") String dbUser,
            @Value("${spring.datasource.password}") String dbPassword) {
        super("TransferAgent", true, jdbcUrl, dbUser, dbPassword);
    }

    @PostConstruct
    public void init() {
        demarrer();
        log("Agent initialisé avec Wallet URL: " + walletServiceUrl);
    }

    /**
     * Méthode publique pour envoyer un message à cet agent (compatible avec SupervisorAgent)
     */
    public void send(TransferMessage message) {
        envoyer(new com.cypay.framework.acteur.Message<>("SupervisorAgent", message));
    }

    @Override
    protected void traiterMessage(TransferMessage message) {
        log("Traitement virement crypto: " + message.getAmount() + " " + message.getCryptoUnit() + 
            " de " + message.getFromUserId() + " vers " + message.getToUserId());

        try {
            // Validation de base
            if (message.getFromUserId().equals(message.getToUserId())) {
                log("Erreur: Impossible de faire un virement vers soi-même");
                return;
            }

            // 1. Effectuer le virement via le Wallet Service
            String transferUrl = walletServiceUrl + "/api/wallets/transfer";
            
            String transferBody = String.format(
                    "{\"fromUserId\":%d,\"toUserId\":%d,\"currency\":\"%s\",\"amount\":%.8f}",
                    message.getFromUserId(),
                    message.getToUserId(),
                    message.getCryptoUnit().name(),
                    message.getAmount()
            );

            HttpResponse response = post(transferUrl, transferBody);

            if (response.getStatusCode() != 200) {
                log("Erreur lors du virement: " + response.getBody());
                return;
            }

            log("Virement effectué avec succès dans le Wallet Service");

            // 2. Enregistrer dans la blockchain
            CreateBlockchainMessage blockchainMessage = new CreateBlockchainMessage(
                    TransactionType.TRANSFER,
                    message.getFromUserId(),
                    message.getToUserId(),
                    message.getAmount(),
                    message.getCryptoUnit()
            );

            createBlockchainAgent.send(blockchainMessage);

            log("✓ Transaction enregistrée dans la blockchain");

        } catch (Exception e) {
            logErreur("Erreur lors du virement de crypto", e);
        }
    }
}

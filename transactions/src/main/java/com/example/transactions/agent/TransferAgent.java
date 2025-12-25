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


import java.util.Locale;

public class TransferAgent extends Acteur<TransferMessage> {


    private CreateBlockchainAgentPool createBlockchainAgentPool;
    private String walletServiceUrl;


    public TransferAgent(String jdbcUrl, String dbUser, String dbPassword) {
        super("TransferAgent", true, jdbcUrl, dbUser, dbPassword);
    }

    public void setCreateBlockchainAgentPool(CreateBlockchainAgentPool pool) {
        this.createBlockchainAgentPool = pool;
    }
    public void setWalletServiceUrl(String url) {
        this.walletServiceUrl = url;
    }

    public void init() {
        demarrer();
        logger.info("[INIT] TransferAgent démarré avec Wallet URL: " + walletServiceUrl);
    }

    /**
     * Méthode publique pour envoyer un message à cet agent (compatible avec SupervisorAgent)
     */
    public void send(TransferMessage message) {
        logger.info("[SEND] Reçu une demande de transfert de " + message.getAmount() + " " + message.getCryptoUnit() + " de " + message.getFromUserId() + " vers " + message.getToUserId() + " (origine: SupervisorAgent)");
        envoyer(new com.cypay.framework.acteur.Message<>("SupervisorAgent", message));
    }

    @Override
    protected void traiterMessage(TransferMessage message) {
        logger.info("[PROCESS] Transfert de " + message.getAmount() + " " + message.getCryptoUnit() + " de " + message.getFromUserId() + " vers " + message.getToUserId());
        try {
            if (message.getFromUserId().equals(message.getToUserId())) {
                logger.erreur("[ERROR] Auto-transfert interdit", null);
                return;
            }
            String transferUrl = walletServiceUrl + "/api/wallets/transfer";
            String transferBody = String.format(Locale.US,
                    "{\"fromUserId\":%d,\"toUserId\":%d,\"currency\":\"%s\",\"amount\":%.8f}",
                    message.getFromUserId(),
                    message.getToUserId(),
                    message.getCryptoUnit().name(),
                    message.getAmount()
            );
            HttpResponse response = post(transferUrl, transferBody);
            if (response.getStatusCode() != 200) {
                logger.erreur("[ERROR] Echec du transfert: " + response.getBody(), null);
                return;
            }
            logger.info("[SUCCESS] Transfert Wallet Service effectué");
            logger.info("[BLOCKCHAIN] Enregistrement du transfert dans la blockchain (appel CreateBlockchainAgent)");
            CreateBlockchainMessage blockchainMessage = new CreateBlockchainMessage(
                    TransactionType.TRANSFER,
                    message.getFromUserId(),
                    message.getToUserId(),
                    message.getAmount(),
                    message.getCryptoUnit()
            );
            createBlockchainAgentPool.send(blockchainMessage);
            logger.info("[SUCCESS] Transaction blockchain enregistrée");
        } catch (Exception e) {
            logger.erreur("[ERROR] Erreur lors du transfert", e);
        }
    }
}

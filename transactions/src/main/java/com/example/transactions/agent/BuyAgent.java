package com.example.transactions.agent;

import com.cypay.framework.acteur.Acteur;
import com.cypay.framework.http.HttpResponse;
import com.example.transactions.message.BuyMessage;
import com.example.transactions.message.CreateBlockchainMessage;
import com.example.transactions.model.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import com.example.transactions.service.CryptoPriceService;
import java.util.Locale;


public class BuyAgent extends Acteur<BuyMessage> {


    private CreateBlockchainAgentPool createBlockchainAgentPool;
    private CryptoPriceService cryptoPriceService;
    private String walletServiceUrl;


    public BuyAgent(String jdbcUrl, String dbUser, String dbPassword) {
        super("BuyAgent", true, jdbcUrl, dbUser, dbPassword);
    }

    public void setCreateBlockchainAgentPool(CreateBlockchainAgentPool pool) {
        this.createBlockchainAgentPool = pool;
    }
    public void setCryptoPriceService(CryptoPriceService service) {
        this.cryptoPriceService = service;
    }
    public void setWalletServiceUrl(String url) {
        this.walletServiceUrl = url;
    }

    public void init() {
        demarrer();
        logger.info("[INIT] BuyAgent démarré avec Wallet URL: " + walletServiceUrl);
    }

    /**
     * Méthode publique pour envoyer un message à cet agent (compatible avec SupervisorAgent)
     */
    public void send(BuyMessage message) {
        logger.info("[SEND] Reçu une demande d'achat de " + message.getAmount() + " " + message.getCryptoUnit() + " pour l'utilisateur " + message.getUserId() + " (origine: SupervisorAgent)");
        envoyer(new com.cypay.framework.acteur.Message<>("SupervisorAgent", message));
    }

    @Override
    protected void traiterMessage(BuyMessage message) {
        logger.info("[PROCESS] Achat de crypto: " + message.getCryptoUnit() + " pour l'utilisateur " + message.getUserId());
        try {
            double prixUnitaire = cryptoPriceService.getPrice(message.getCryptoUnit().name(), message.getPaymentUnit().name());
            double montantAPayer = message.getAmount() * prixUnitaire;
            logger.info("[CHECK] Prix unitaire " + message.getCryptoUnit() + ": " + prixUnitaire + " " + message.getPaymentUnit());
            logger.info("[CHECK] Total à payer: " + montantAPayer + " " + message.getPaymentUnit());
            String balanceUrl = String.format("%s/api/wallets/%d/%s", walletServiceUrl, message.getUserId(), message.getPaymentUnit().name());
            HttpResponse balanceResponse = get(balanceUrl);
            if (balanceResponse.getStatusCode() != 200) {
                logger.erreur("[ERROR] Portefeuille " + message.getPaymentUnit() + " introuvable pour l'utilisateur " + message.getUserId(), null);
                return;
            }
            String balanceBody = balanceResponse.getBody();
            double balanceActuelle = parseBalance(balanceBody);
            logger.info("[CHECK] Solde actuel " + message.getPaymentUnit() + ": " + balanceActuelle);
            if (balanceActuelle < montantAPayer) {
                logger.erreur("[ERROR] Fonds insuffisants. Requis: " + montantAPayer + ", Disponible: " + balanceActuelle, null);
                return;
            }
            String debitUrl = String.format("%s/api/wallets/%d/debit", walletServiceUrl, message.getUserId());
            String debitBody = String.format(Locale.US, "{\"currency\":\"%s\",\"amount\":%.8f}", message.getPaymentUnit().name(), montantAPayer);
            HttpResponse debitResponse = post(debitUrl, debitBody);
            if (debitResponse.getStatusCode() != 200) {
                logger.erreur("[ERROR] Echec du débit pour " + message.getPaymentUnit() + ": " + debitResponse.getBody(), null);
                return;
            }
            logger.info("[SUCCESS] Débit de " + montantAPayer + " " + message.getPaymentUnit() + " effectué");
            String creditUrl = String.format("%s/api/wallets/%d/credit", walletServiceUrl, message.getUserId());
            String creditBody = String.format(Locale.US, "{\"currency\":\"%s\",\"amount\":%.8f}", message.getCryptoUnit().name(), message.getAmount());
            HttpResponse creditResponse = post(creditUrl, creditBody);
            if (creditResponse.getStatusCode() != 200) {
                logger.erreur("[CRITICAL] Crédit crypto échoué après débit! " + creditResponse.getBody(), null);
                logger.info("[TODO] Implémenter rollback - re-créditer " + montantAPayer + " " + message.getPaymentUnit());
                return;
            }
            logger.info("[SUCCESS] Crédit de " + message.getAmount() + " " + message.getCryptoUnit() + " effectué");
            logger.info("[BLOCKCHAIN] Enregistrement de l'achat dans la blockchain (appel CreateBlockchainAgent)");
            CreateBlockchainMessage blockchainMessage = new CreateBlockchainMessage(
                    TransactionType.BUY,
                    message.getUserId(),
                    null,
                    message.getAmount(),
                    message.getCryptoUnit()
            );
            createBlockchainAgentPool.send(blockchainMessage);
            logger.info("[SUCCESS] Transaction d'achat terminée");
        } catch (Exception e) {
            logger.erreur("[ERROR] Erreur lors de la transaction d'achat", e);
        }
    }

    /**
     * Parse la balance depuis la réponse JSON
     * Format attendu: {"id":1,"userId":1,"balance":1000.0,"currency":"EUR"}
     */
    private double parseBalance(String jsonBody) {
        try {
            String balanceStr = jsonBody.split("\"balance\":")[1].split(",")[0];
            return Double.parseDouble(balanceStr);
        } catch (Exception e) {
            logger.erreur("[ERROR] Erreur lors du parsing du solde", e);
            return 0.0;
        }
    }
}

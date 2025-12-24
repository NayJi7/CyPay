package com.example.transactions.agent;

import com.cypay.framework.acteur.Acteur;
import com.cypay.framework.http.HttpResponse;
import com.example.transactions.message.SellMessage;
import com.example.transactions.message.CreateBlockchainMessage;
import com.example.transactions.model.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import com.example.transactions.service.CryptoPriceService;

@Component
public class SellAgent extends Acteur<SellMessage> {

    @Autowired
    private CreateBlockchainAgent createBlockchainAgent;

    @Autowired
    private CryptoPriceService cryptoPriceService;

    @Value("${wallet.service.url}")
    private String walletServiceUrl;

    @Autowired
    public SellAgent(
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.datasource.username}") String dbUser,
            @Value("${spring.datasource.password}") String dbPassword) {
        super("SellAgent", true, jdbcUrl, dbUser, dbPassword);
    }

    @PostConstruct
    public void init() {
        demarrer();
        logger.info("[INIT] SellAgent démarré avec Wallet URL: " + walletServiceUrl);
    }

    /**
     * Méthode publique pour envoyer un message à cet agent (compatible avec SupervisorAgent)
     */
    public void send(SellMessage message) {
        logger.info("[SEND] Reçu une demande de vente de " + message.getAmount() + " " + message.getCryptoUnit() + " pour l'utilisateur " + message.getUserId() + " (origine: SupervisorAgent)");
        envoyer(new com.cypay.framework.acteur.Message<>("SupervisorAgent", message));
    }

    @Override
    protected void traiterMessage(SellMessage message) {
        logger.info("[PROCESS] Vente de crypto: " + message.getCryptoUnit() + " pour l'utilisateur " + message.getUserId());
        try {
            double prixUnitaire = cryptoPriceService.getPrice(message.getCryptoUnit().name(), message.getTargetUnit().name());
            double montantARecevoir = message.getAmount() * prixUnitaire;
            logger.info("[CHECK] Prix unitaire " + message.getCryptoUnit() + ": " + prixUnitaire + " " + message.getTargetUnit());
            logger.info("[CHECK] Total à recevoir: " + montantARecevoir + " " + message.getTargetUnit());
            String balanceUrl = String.format("%s/api/wallets/%d/%s", walletServiceUrl, message.getUserId(), message.getCryptoUnit().name());
            HttpResponse balanceResponse = get(balanceUrl);
            if (balanceResponse.getStatusCode() != 200) {
                logger.erreur("[ERROR] Portefeuille " + message.getCryptoUnit() + " introuvable pour l'utilisateur " + message.getUserId(), null);
                return;
            }
            String balanceBody = balanceResponse.getBody();
            double balanceActuelle = parseBalance(balanceBody);
            logger.info("[CHECK] Solde actuel " + message.getCryptoUnit() + ": " + balanceActuelle);
            if (balanceActuelle < message.getAmount()) {
                logger.erreur("[ERROR] Fonds crypto insuffisants. Requis: " + message.getAmount() + ", Disponible: " + balanceActuelle, null);
                return;
            }
            String debitUrl = String.format("%s/api/wallets/%d/debit", walletServiceUrl, message.getUserId());
            String debitBody = String.format("{\"currency\":\"%s\",\"amount\":%.8f}", message.getCryptoUnit().name(), message.getAmount());
            HttpResponse debitResponse = post(debitUrl, debitBody);
            if (debitResponse.getStatusCode() != 200) {
                logger.erreur("[ERROR] Echec du débit pour " + message.getCryptoUnit() + ": " + debitResponse.getBody(), null);
                return;
            }
            logger.info("[SUCCESS] Débit de " + message.getAmount() + " " + message.getCryptoUnit() + " effectué");
            String creditUrl = String.format("%s/api/wallets/%d/credit", walletServiceUrl, message.getUserId());
            String creditBody = String.format("{\"currency\":\"%s\",\"amount\":%.8f}", message.getTargetUnit().name(), montantARecevoir);
            HttpResponse creditResponse = post(creditUrl, creditBody);
            if (creditResponse.getStatusCode() != 200) {
                logger.erreur("[CRITICAL] Crédit fiat échoué après débit crypto! " + creditResponse.getBody(), null);
                logger.info("[TODO] Implémenter rollback - re-créditer " + message.getAmount() + " " + message.getCryptoUnit());
                return;
            }
            logger.info("[SUCCESS] Crédit de " + montantARecevoir + " " + message.getTargetUnit() + " effectué");
            logger.info("[BLOCKCHAIN] Enregistrement de la vente dans la blockchain (appel CreateBlockchainAgent)");
            CreateBlockchainMessage blockchainMessage = new CreateBlockchainMessage(
                    TransactionType.SELL,
                    message.getUserId(),
                    null,
                    message.getAmount(),
                    message.getCryptoUnit()
            );
            createBlockchainAgent.send(blockchainMessage);
            logger.info("[SUCCESS] Transaction de vente terminée");
        } catch (Exception e) {
            logger.erreur("[ERROR] Erreur lors de la transaction de vente", e);
        }
    }

    /**
     * Parse la balance depuis la réponse JSON
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

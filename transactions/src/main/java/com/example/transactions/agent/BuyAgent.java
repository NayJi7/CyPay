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

@Component
public class BuyAgent extends Acteur<BuyMessage> {

    @Autowired
    private CreateBlockchainAgent createBlockchainAgent;

    @Autowired
    private CryptoPriceService cryptoPriceService;

    @Value("${wallet.service.url}")
    private String walletServiceUrl;

    @Autowired
    public BuyAgent(
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.datasource.username}") String dbUser,
            @Value("${spring.datasource.password}") String dbPassword) {
        super("BuyAgent", true, jdbcUrl, dbUser, dbPassword);
    }

    @PostConstruct
    public void init() {
        demarrer();
        log("Agent initialisé avec Wallet URL: " + walletServiceUrl);
    }

    /**
     * Méthode publique pour envoyer un message à cet agent (compatible avec SupervisorAgent)
     */
    public void send(BuyMessage message) {
        envoyer(new com.cypay.framework.acteur.Message<>("SupervisorAgent", message));
    }

    @Override
    protected void traiterMessage(BuyMessage message) {
        log("Traitement achat crypto: " + message.getCryptoUnit() + " pour utilisateur " + message.getUserId());

        try {
            // Utilisation du service de prix réel
            double prixUnitaire = cryptoPriceService.getPrice(message.getCryptoUnit().name(), message.getPaymentUnit().name());
            double montantAPayer = message.getAmount() * prixUnitaire;

            log("Prix unitaire " + message.getCryptoUnit() + ": " + prixUnitaire + " " + message.getPaymentUnit());
            log("Montant à payer: " + montantAPayer + " " + message.getPaymentUnit());

            // 1. Vérifier le solde en EUR/USD
            String balanceUrl = String.format("%s/api/wallets/%d/%s",
                    walletServiceUrl,
                    message.getUserId(),
                    message.getPaymentUnit().name());

            HttpResponse balanceResponse = get(balanceUrl);

            if (balanceResponse.getStatusCode() != 200) {
                log("Erreur: Wallet " + message.getPaymentUnit() + " introuvable pour utilisateur " + message.getUserId());
                log("Veuillez créer un wallet " + message.getPaymentUnit() + " d'abord");
                return;
            }

            // Parser la balance (format JSON simple: {"balance": 1000.0})
            String balanceBody = balanceResponse.getBody();
            double balanceActuelle = parseBalance(balanceBody);

            log("Solde actuel " + message.getPaymentUnit() + ": " + balanceActuelle);

            if (balanceActuelle < montantAPayer) {
                log("Erreur: Solde insuffisant. Requis: " + montantAPayer + ", Disponible: " + balanceActuelle);
                return;
            }

            // 2. Débiter le compte en EUR/USD
            String debitUrl = String.format("%s/api/wallets/%d/debit",
                    walletServiceUrl,
                    message.getUserId());

            String debitBody = String.format(
                    "{\"currency\":\"%s\",\"amount\":%.8f}",
                    message.getPaymentUnit().name(),
                    montantAPayer
            );

            HttpResponse debitResponse = post(debitUrl, debitBody);

            if (debitResponse.getStatusCode() != 200) {
                log("Erreur lors du débit de " + message.getPaymentUnit() + ": " + debitResponse.getBody());
                return;
            }

            log("Débit de " + montantAPayer + " " + message.getPaymentUnit() + " réussi");

            // 3. Créditer le compte en crypto
            String creditUrl = String.format("%s/api/wallets/%d/credit",
                    walletServiceUrl,
                    message.getUserId());

            String creditBody = String.format(
                    "{\"currency\":\"%s\",\"amount\":%.8f}",
                    message.getCryptoUnit().name(),
                    message.getAmount()
            );

            HttpResponse creditResponse = post(creditUrl, creditBody);

            if (creditResponse.getStatusCode() != 200) {
                log("ERREUR CRITIQUE: Crédit crypto échoué après débit! " + creditResponse.getBody());
                log("TODO: Implémenter rollback - re-créditer " + montantAPayer + " " + message.getPaymentUnit());
                return;
            }

            log("Crédit de " + message.getAmount() + " " + message.getCryptoUnit() + " réussi");

            // 4. Enregistrer dans la blockchain
            CreateBlockchainMessage blockchainMessage = new CreateBlockchainMessage(
                    TransactionType.BUY,
                    message.getUserId(),
                    null, // Pas d'acteur 2 pour un achat
                    message.getAmount(),
                    message.getCryptoUnit()
            );

            createBlockchainAgent.send(blockchainMessage);

            log("✓ Achat complété avec succès!");

        } catch (Exception e) {
            logErreur("Erreur lors de l'achat de crypto", e);
        }
    }

    /**
     * Parse la balance depuis la réponse JSON
     * Format attendu: {"id":1,"userId":1,"balance":1000.0,"currency":"EUR"}
     */
    private double parseBalance(String jsonBody) {
        try {
            // Simple parsing pour extraire "balance":valeur
            String balanceStr = jsonBody.split("\"balance\":")[1].split(",")[0];
            return Double.parseDouble(balanceStr);
        } catch (Exception e) {
            logErreur("Erreur parsing balance", e);
            return 0.0;
        }
    }
}

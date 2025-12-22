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

@Component
public class SellAgent extends Acteur<SellMessage> {

    @Autowired
    private CreateBlockchainAgent createBlockchainAgent;

    @Value("${wallet.service.url}")
    private String walletServiceUrl;

    public SellAgent() {
        super("SellAgent");
    }

    @PostConstruct
    public void init() {
        demarrer();
        log("Agent initialisé avec Wallet URL: " + walletServiceUrl);
    }

    /**
     * Méthode publique pour envoyer un message à cet agent (compatible avec SupervisorAgent)
     */
    public void send(SellMessage message) {
        envoyer(new com.cypay.framework.acteur.Message<>("SupervisorAgent", message));
    }

    @Override
    protected void traiterMessage(SellMessage message) {
        log("Traitement vente crypto: " + message.getCryptoUnit() + " pour utilisateur " + message.getUserId());

        try {
            // Prix simulé pour la démo
            double prixUnitaire = getPrixCrypto(message.getCryptoUnit().name());
            double montantARecevoir = message.getAmount() * prixUnitaire;

            log("Prix unitaire " + message.getCryptoUnit() + ": " + prixUnitaire + " " + message.getTargetUnit());
            log("Montant à recevoir: " + montantARecevoir + " " + message.getTargetUnit());

            // 1. Vérifier le solde en crypto
            String balanceUrl = String.format("%s/api/wallets/%d/%s",
                    walletServiceUrl,
                    message.getUserId(),
                    message.getCryptoUnit().name());

            HttpResponse balanceResponse = get(balanceUrl);

            if (balanceResponse.getStatusCode() != 200) {
                log("Erreur: Wallet " + message.getCryptoUnit() + " introuvable pour utilisateur " + message.getUserId());
                log("Veuillez créer un wallet " + message.getCryptoUnit() + " d'abord");
                return;
            }

            String balanceBody = balanceResponse.getBody();
            double balanceActuelle = parseBalance(balanceBody);

            log("Solde actuel " + message.getCryptoUnit() + ": " + balanceActuelle);

            if (balanceActuelle < message.getAmount()) {
                log("Erreur: Solde crypto insuffisant. Requis: " + message.getAmount() + ", Disponible: " + balanceActuelle);
                return;
            }

            // 2. Débiter le compte en crypto
            String debitUrl = String.format("%s/api/wallets/%d/debit",
                    walletServiceUrl,
                    message.getUserId());

            String debitBody = String.format(
                    "{\"currency\":\"%s\",\"amount\":%.8f}",
                    message.getCryptoUnit().name(),
                    message.getAmount()
            );

            HttpResponse debitResponse = post(debitUrl, debitBody);

            if (debitResponse.getStatusCode() != 200) {
                log("Erreur lors du débit de " + message.getCryptoUnit() + ": " + debitResponse.getBody());
                return;
            }

            log("Débit de " + message.getAmount() + " " + message.getCryptoUnit() + " réussi");

            // 3. Créditer le compte en EUR/USD
            String creditUrl = String.format("%s/api/wallets/%d/credit",
                    walletServiceUrl,
                    message.getUserId());

            String creditBody = String.format(
                    "{\"currency\":\"%s\",\"amount\":%.8f}",
                    message.getTargetUnit().name(),
                    montantARecevoir
            );

            HttpResponse creditResponse = post(creditUrl, creditBody);

            if (creditResponse.getStatusCode() != 200) {
                log("ERREUR CRITIQUE: Crédit fiat échoué après débit crypto! " + creditResponse.getBody());
                log("TODO: Implémenter rollback - re-créditer " + message.getAmount() + " " + message.getCryptoUnit());
                return;
            }

            log("Crédit de " + montantARecevoir + " " + message.getTargetUnit() + " réussi");

            // 4. Enregistrer dans la blockchain
            CreateBlockchainMessage blockchainMessage = new CreateBlockchainMessage(
                    TransactionType.SELL,
                    message.getUserId(),
                    null, // Pas d'acteur 2 pour une vente
                    message.getAmount(),
                    message.getCryptoUnit()
            );

            createBlockchainAgent.send(blockchainMessage);

            log("✓ Vente complétée avec succès!");

        } catch (Exception e) {
            logErreur("Erreur lors de la vente de crypto", e);
        }
    }

    /**
     * Prix simulés pour la démo (en production, utiliser une API de prix réelle)
     */
    private double getPrixCrypto(String cryptoUnit) {
        return switch (cryptoUnit) {
            case "BTC" -> 40000.0;  // 1 BTC = 40000 EUR/USD
            case "ETH" -> 2500.0;   // 1 ETH = 2500 EUR/USD
            default -> 1.0;
        };
    }

    /**
     * Parse la balance depuis la réponse JSON
     */
    private double parseBalance(String jsonBody) {
        try {
            String balanceStr = jsonBody.split("\"balance\":")[1].split(",")[0];
            return Double.parseDouble(balanceStr);
        } catch (Exception e) {
            logErreur("Erreur parsing balance", e);
            return 0.0;
        }
    }
}

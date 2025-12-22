package com.example.transactions.controller;

import com.example.transactions.agent.SupervisorAgent;
import com.example.transactions.message.*;
import com.example.transactions.model.CryptoUnit;
import com.example.transactions.model.Transaction;
import com.example.transactions.model.TransactionType;
import com.example.transactions.service.DatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    @Autowired
    private SupervisorAgent supervisorAgent;

    @Autowired
    private DatabaseService databaseService;

    /**
     * Acheter de la crypto
     * POST /transactions/buy?userId=1&cryptoUnit=BTC&amount=0.5&paymentUnit=EUR
     */
    @PostMapping("/buy")
    public ResponseEntity<String> buy(
            @RequestParam Long userId,
            @RequestParam CryptoUnit cryptoUnit,
            @RequestParam Double amount,
            @RequestParam CryptoUnit paymentUnit) {

        BuyMessage message = new BuyMessage(userId, cryptoUnit, amount, paymentUnit);
        supervisorAgent.dispatch(message);

        return ResponseEntity.ok("Achat de " + amount + " " + cryptoUnit + " pour l'utilisateur " + userId + " en cours...");
    }

    /**
     * Vendre de la crypto
     * POST /transactions/sell?userId=1&cryptoUnit=BTC&amount=0.5&targetUnit=EUR
     */
    @PostMapping("/sell")
    public ResponseEntity<String> sell(
            @RequestParam Long userId,
            @RequestParam CryptoUnit cryptoUnit,
            @RequestParam Double amount,
            @RequestParam CryptoUnit targetUnit) {

        SellMessage message = new SellMessage(userId, cryptoUnit, amount, targetUnit);
        supervisorAgent.dispatch(message);

        return ResponseEntity.ok("Vente de " + amount + " " + cryptoUnit + " pour l'utilisateur " + userId + " en cours...");
    }

    /**
     * Virement de crypto entre utilisateurs
     * POST /transactions/transfer?fromUserId=1&toUserId=2&cryptoUnit=BTC&amount=0.5
     */
    @PostMapping("/transfer")
    public ResponseEntity<String> transfer(
            @RequestParam Long fromUserId,
            @RequestParam Long toUserId,
            @RequestParam CryptoUnit cryptoUnit,
            @RequestParam Double amount) {

        TransferMessage message = new TransferMessage(fromUserId, toUserId, cryptoUnit, amount);
        supervisorAgent.dispatch(message);

        return ResponseEntity.ok("Virement de " + amount + " " + cryptoUnit +
                " de l'utilisateur " + fromUserId + " vers " + toUserId + " en cours...");
    }

    /**
     * Créer un ordre programmé
     * POST /transactions/order?userId=1&orderType=BUY&cryptoUnit=BTC&amount=0.5&targetPrice=50000
     */
    @PostMapping("/order")
    public ResponseEntity<String> order(
            @RequestParam Long userId,
            @RequestParam TransactionType orderType,
            @RequestParam CryptoUnit cryptoUnit,
            @RequestParam Double amount,
            @RequestParam Double targetPrice) {

        OrderMessage message = new OrderMessage(userId, orderType, cryptoUnit, amount, targetPrice);
        supervisorAgent.dispatch(message);

        return ResponseEntity.ok("Ordre programmé créé pour l'utilisateur " + userId +
                " (" + orderType + " " + amount + " " + cryptoUnit + " au prix " + targetPrice + ")");
    }

    /**
     * Récupérer l'historique des transactions d'un utilisateur
     * GET /transactions/history/{userId}
     */
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<Transaction>> getHistory(@PathVariable Long userId) {
        List<Transaction> transactions = databaseService.findUserTransactions(userId);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Récupérer toutes les transactions
     * GET /transactions/all
     */
    @GetMapping("/all")
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        List<Transaction> transactions = databaseService.findAllTransactions();
        return ResponseEntity.ok(transactions);
    }

    /**
     * Récupérer les transactions par type
     * GET /transactions/type/{type}
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<Transaction>> getTransactionsByType(@PathVariable TransactionType type) {
        List<Transaction> transactions = databaseService.findTransactionsByType(type);
        return ResponseEntity.ok(transactions);
    }
}

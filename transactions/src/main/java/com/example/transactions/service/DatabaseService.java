package com.example.transactions.service;

import com.example.transactions.model.Transaction;
import com.example.transactions.model.TransactionType;
import com.example.transactions.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DatabaseService {

    @Autowired
    private TransactionRepository transactionRepository;

    // Sauvegarder une transaction
    public Transaction saveTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    // Récupérer une transaction par ID
    public Optional<Transaction> findById(Long id) {
        return transactionRepository.findById(id);
    }

    // Récupérer toutes les transactions d'un utilisateur
    public List<Transaction> findUserTransactions(Long userId) {
        return transactionRepository.findByActor1OrActor2OrderByTimestampDesc(userId, userId);
    }

    // Récupérer toutes les transactions
    public List<Transaction> findAllTransactions() {
        return transactionRepository.findAll();
    }

    // Récupérer les transactions par type
    public List<Transaction> findTransactionsByType(TransactionType type) {
        return transactionRepository.findByTypeOrderByTimestampDesc(type);
    }

    // Récupérer les transactions d'un utilisateur par type
    public List<Transaction> findUserTransactionsByType(Long userId, TransactionType type) {
        return transactionRepository.findByActor1AndTypeOrderByTimestampDesc(userId, type);
    }
}

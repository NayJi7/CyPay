package com.example.transactions.model;

public enum TransactionStatus {
    PENDING,    // En attente de traitement
    SUCCESS,    // Transaction réussie
    FAILED,     // Transaction échouée
    CANCELLED   // Transaction annulée
}

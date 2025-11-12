package com.example.transactions.repository;

import com.example.transactions.model.Transaction;
import com.example.transactions.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Trouver toutes les transactions d'un utilisateur
    List<Transaction> findByActor1OrderByTimestampDesc(Long userId);

    // Trouver toutes les transactions où l'utilisateur est acteur 1 ou 2
    List<Transaction> findByActor1OrActor2OrderByTimestampDesc(Long actor1, Long actor2);

    // Trouver les transactions par type
    List<Transaction> findByTypeOrderByTimestampDesc(TransactionType type);

    // Trouver les transactions d'un utilisateur par type
    List<Transaction> findByActor1AndTypeOrderByTimestampDesc(Long userId, TransactionType type);
}

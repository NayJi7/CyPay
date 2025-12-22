package com.example.transactions.message;

import com.example.transactions.model.CryptoUnit;
import com.example.transactions.model.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderMessage {
    private Long userId;
    private TransactionType orderType; // BUY ou SELL
    private CryptoUnit cryptoUnit;
    private Double amount;
    private Double targetPrice; // Prix cible pour déclencher l'ordre
}

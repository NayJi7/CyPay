package com.example.transactions.message;

import com.example.transactions.model.CryptoUnit;
import com.example.transactions.model.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBlockchainMessage {
    private TransactionType type;
    private Long actor1;
    private Long actor2;
    private Double amount;
    private CryptoUnit unit;
}

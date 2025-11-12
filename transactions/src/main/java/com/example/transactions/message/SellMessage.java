package com.example.transactions.message;

import com.example.transactions.model.CryptoUnit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellMessage {
    private Long userId;
    private CryptoUnit cryptoUnit;
    private Double amount;
    private CryptoUnit targetUnit; // EUR ou USD
}

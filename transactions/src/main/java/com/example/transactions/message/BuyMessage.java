package com.example.transactions.message;

import com.example.transactions.model.CryptoUnit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuyMessage {
    private Long userId;
    private CryptoUnit cryptoUnit;
    private Double amount;
    private CryptoUnit paymentUnit; // EUR ou USD
}

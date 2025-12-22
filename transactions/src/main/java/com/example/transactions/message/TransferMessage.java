package com.example.transactions.message;

import com.example.transactions.model.CryptoUnit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferMessage {
    private Long fromUserId;
    private Long toUserId;
    private CryptoUnit cryptoUnit;
    private Double amount;
}

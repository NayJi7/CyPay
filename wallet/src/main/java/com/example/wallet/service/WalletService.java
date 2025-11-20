package com.example.wallet.service;

import com.example.wallet.client.UserServiceClient;
import com.example.wallet.entity.Wallet;
import com.example.wallet.exception.UserNotFoundException;
import com.example.wallet.repository.WalletRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserServiceClient userServiceClient;

    public WalletService(WalletRepository walletRepository, UserServiceClient userServiceClient) {
        this.walletRepository = walletRepository;
        this.userServiceClient = userServiceClient;
    }

    public Wallet createWallet(Long userId, String currency) {
        // ✅ VALIDATION : Vérifier que l'utilisateur existe
        if (!userServiceClient.userExists(userId)) {
            throw new UserNotFoundException(userId);
        }

        return walletRepository.findByUserIdAndCurrency(userId, currency)
                .orElseGet(() -> walletRepository.save(new Wallet(userId, currency)));
    }

    public Wallet getWallet(Long userId, String currency) {
        return walletRepository.findByUserIdAndCurrency(userId, currency)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Wallet not found for user " + userId + " and currency " + currency));
    }

    public List<Wallet> getWalletsByUser(Long userId) {
        return walletRepository.findByUserId(userId);
    }

    @Transactional
    public Wallet credit(Long userId, String currency, BigDecimal amount) {
        Wallet wallet = getWallet(userId, currency);
        wallet.setBalance(wallet.getBalance().add(amount));
        return walletRepository.save(wallet);
    }

    @Transactional
    public Wallet debit(Long userId, String currency, BigDecimal amount) {
        Wallet wallet = getWallet(userId, currency);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Solde insuffisant");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        return walletRepository.save(wallet);
    }
}

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
                .orElseGet(() -> {
                    Wallet newWallet = new Wallet(userId, currency);
                    
                    // 🎁 BONUS : 10 000 €/$ offerts UNIQUEMENT à la création du PREMIER wallet !
                    List<Wallet> userWallets = walletRepository.findByUserId(userId);
                    if (userWallets.isEmpty() && ("EUR".equalsIgnoreCase(currency) || "USD".equalsIgnoreCase(currency))) {
                        newWallet.setBalance(new BigDecimal("10000.00"));
                    }
                    
                    return walletRepository.save(newWallet);
                });
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
        // Récupère le wallet existant ou le crée s'il n'existe pas (ex: réception de crypto)
        Wallet wallet = createWallet(userId, currency);

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

    @Transactional
    public void transfer(Long fromUserId, Long toUserId, String currency, BigDecimal amount) {
        debit(fromUserId, currency, amount);
        credit(toUserId, currency, amount);
    }

    public void deleteWallet(Long walletId) {
        if (!walletRepository.existsById(walletId)) {
            throw new EntityNotFoundException("Wallet not found with ID: " + walletId);
        }
        walletRepository.deleteById(walletId);
    }
}

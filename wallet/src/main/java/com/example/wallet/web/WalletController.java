package com.example.wallet.web;

import com.example.wallet.entity.Wallet;
import com.example.wallet.service.WalletService;
import com.example.wallet.web.dto.CreateWalletRequest;  // ⬅⬅⬅
import com.example.wallet.web.dto.OperationRequest;     // ⬅⬅⬅
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    public Wallet createWallet(@Valid @RequestBody CreateWalletRequest request) {
        return walletService.createWallet(request.getUserId(), request.getCurrency());
    }

    @GetMapping("/{userId}")
    public List<Wallet> getWalletsByUser(@PathVariable Long userId) {
        return walletService.getWalletsByUser(userId);
    }

    @GetMapping("/{userId}/{currency}")
    public Wallet getWallet(@PathVariable Long userId,
                            @PathVariable String currency) {
        return walletService.getWallet(userId, currency);
    }

    @PostMapping("/{userId}/credit")
    public Wallet credit(@PathVariable Long userId,
                         @Valid @RequestBody OperationRequest request) {
        return walletService.credit(userId, request.getCurrency(), request.getAmount());
    }

    @PostMapping("/{userId}/debit")
    public Wallet debit(@PathVariable Long userId,
                        @Valid @RequestBody OperationRequest request) {
        return walletService.debit(userId, request.getCurrency(), request.getAmount());
    }
}

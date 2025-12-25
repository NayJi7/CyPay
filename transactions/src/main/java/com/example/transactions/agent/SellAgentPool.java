package com.example.transactions.agent;

import com.cypay.framework.acteur.DynamicActorPool;
import com.example.transactions.message.SellMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Pool dynamique pour SellAgent : ajuste le nombre d'instances selon la charge
 */
@Component
public class SellAgentPool {
    private DynamicActorPool<SellMessage> pool;

    @Autowired
    private CreateBlockchainAgentPool createBlockchainAgentPool;
    @Autowired
    private com.example.transactions.service.CryptoPriceService cryptoPriceService;
    @Value("${wallet.service.url}")
    private String walletServiceUrl;
    @Value("${spring.datasource.url}")
    private String jdbcUrl;
    @Value("${spring.datasource.username}")
    private String dbUser;
    @Value("${spring.datasource.password}")
    private String dbPassword;

    @PostConstruct
    public void init() {
        pool = new DynamicActorPool<>(
                1, // min
                5, // max
                10, // highWatermark
                2, // lowWatermark
                () -> {
                    SellAgent agent = new SellAgent(jdbcUrl, dbUser, dbPassword);
                    agent.setCreateBlockchainAgentPool(createBlockchainAgentPool);
                    agent.setCryptoPriceService(cryptoPriceService);
                    agent.setWalletServiceUrl(walletServiceUrl);
                    return agent;
                }
        );
    }

    public void send(SellMessage message) {
        pool.envoyer(new com.cypay.framework.acteur.Message<>("SupervisorAgent", message));
    }
}

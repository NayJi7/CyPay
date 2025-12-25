package com.example.transactions.agent;

import com.cypay.framework.acteur.DynamicActorPool;
import com.example.transactions.message.TransferMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Pool dynamique pour TransferAgent : ajuste le nombre d'instances selon la charge
 */
@Component
public class TransferAgentPool {
    private DynamicActorPool<TransferMessage> pool;

    @Autowired
    private CreateBlockchainAgentPool createBlockchainAgentPool;
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
                    TransferAgent agent = new TransferAgent(jdbcUrl, dbUser, dbPassword);
                    agent.setCreateBlockchainAgentPool(createBlockchainAgentPool);
                    agent.setWalletServiceUrl(walletServiceUrl);
                    return agent;
                }
        );
    }

    public void send(TransferMessage message) {
        pool.envoyer(new com.cypay.framework.acteur.Message<>("SupervisorAgent", message));
    }
}

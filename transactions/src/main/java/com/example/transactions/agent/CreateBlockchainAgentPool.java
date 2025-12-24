package com.example.transactions.agent;

import com.cypay.framework.acteur.DynamicActorPool;
import com.example.transactions.message.CreateBlockchainMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Pool dynamique pour CreateBlockchainAgent : ajuste le nombre d'instances selon la charge
 */
@Component
public class CreateBlockchainAgentPool {
    private DynamicActorPool<CreateBlockchainMessage> pool;

    @Autowired
    private com.example.transactions.service.DatabaseService databaseService;
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
                    CreateBlockchainAgent agent = new CreateBlockchainAgent(jdbcUrl, dbUser, dbPassword);
                    agent.setDatabaseService(databaseService);
                    return agent;
                }
        );
    }

    public void send(CreateBlockchainMessage message) {
        pool.envoyer(new com.cypay.framework.acteur.Message<>("SupervisorAgent", message));
    }
}

package com.example.transactions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TransactionApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionApplication.class, args);
        System.out.println("\n===========================================");
        System.out.println("  Microservice Transactions démarré!");
        System.out.println("  Port: 8081");
        System.out.println("  H2 Console: http://localhost:8081/h2-console");
        System.out.println("===========================================\n");
    }
}

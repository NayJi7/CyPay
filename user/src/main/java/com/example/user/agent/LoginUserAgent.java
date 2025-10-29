package com.example.user.agent;

import com.example.user.message.LoginUserMessage;
import com.example.user.model.User;
import com.example.user.service.DatabaseService;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.web.bind.annotation.*;

@Component
public class LoginUserAgent implements Runnable {
    private final BlockingQueue<LoginUserMessage> mailbox = new LinkedBlockingQueue<>();
    private final DatabaseService dbService;
    private volatile boolean running = true;

    public LoginUserAgent(DatabaseService dbService) {
        this.dbService = dbService;
        new Thread(this, "LoginUserAgent-Thread").start();
    }

    public void send(LoginUserMessage message) {
        mailbox.add(message);
    }

    @Override
    public void run() {
        while (running) {
            try {
                LoginUserMessage msg = mailbox.take();
                System.out.println("[LoginUserAgent] try to login with email : " + msg.getEmail());

                Optional<User> userOpt = dbService.findByEmail(msg.getEmail());

                if (userOpt.isPresent() && userOpt.get().getPassword().equals(msg.getPassword())) {
                    System.out.println("[LoginUserAgent] Login Suceed ");
                } else {
                    System.out.println("[LoginUserAgent] Login Failed .");
                }

            } catch (Exception e) {
                System.out.println("[LoginUserAgent] Erreur : " + e.getMessage());
            }
        }
    }
}

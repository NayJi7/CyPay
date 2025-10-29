package com.example.user.agent;

import com.example.user.message.CreateUserMessage;
import com.example.user.model.User;
import com.example.user.service.DatabaseService;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;



@Component
public class CreateUserAgent implements Runnable {
    private final BlockingQueue<CreateUserMessage> mailbox = new LinkedBlockingQueue<>();
    private final DatabaseService dbService;
    private volatile boolean running = true;

    public CreateUserAgent(DatabaseService dbService) {
        this.dbService = dbService;
        new Thread(this, "CreateUserAgent-Thread").start();
    }

    public void send(CreateUserMessage message) {
        mailbox.add(message);
    }

    @Override
    public void run() {
        while (running) {
            try {
                CreateUserMessage msg = mailbox.take();
                System.out.println("[CreateUserAgent] Receive : " + msg.getPseudo());
                Optional<User> userOpt = dbService.findByEmail(msg.getEmail());
                if (userOpt.isPresent())
                {
                    System.out.println("the email are already used by an other User");
                }else{
                    User user = User.builder()
                            .pseudo(msg.getPseudo())
                            .email(msg.getEmail())
                            .password(msg.getPassword())
                            .build();


                    dbService.saveUser(user);
                    System.out.println("[CreateUserAgent] User Create : " +  msg.getPseudo());
                }
            } catch (Exception e) {
                System.out.println("[CreateUserAgent] Error : " +  e.getMessage());
            }
        }
    }
}

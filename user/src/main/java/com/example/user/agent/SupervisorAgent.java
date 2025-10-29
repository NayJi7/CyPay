package com.example.user.agent;

import com.example.user.message.CreateUserMessage;
import com.example.user.message.LoginUserMessage;
import org.springframework.stereotype.Component;

import org.springframework.web.bind.annotation.*;

@Component
public class SupervisorAgent {
    private final CreateUserAgent createUserAgent;
    private final LoginUserAgent loginUserAgent;

    public SupervisorAgent(CreateUserAgent createUserAgent, LoginUserAgent loginUserAgent) {
        this.createUserAgent = createUserAgent;
        this.loginUserAgent = loginUserAgent;
    }

    public void dispatch(Object message) {
        if (message instanceof CreateUserMessage m) {
            System.out.println("[Supervisor] → CreateUserMessage : " +  m.getEmail());
            createUserAgent.send(m);
        } else if (message instanceof LoginUserMessage m) {
            System.out.println("[Supervisor] → LoginUserMessage : " +  m.getEmail());
            loginUserAgent.send(m);
        } else {
            System.out.println("[Supervisor] Type de message inconnu : " + message.getClass().getSimpleName());
        }
    }
}

package com.example.user.controller;

import com.example.user.agent.SupervisorAgent;
import com.example.user.message.CreateUserMessage;
import com.example.user.message.LoginUserMessage;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/users")
public class UserController {

    private final SupervisorAgent supervisor;

    public UserController(SupervisorAgent supervisor) {
        this.supervisor = supervisor;
    }

    @PostMapping("/create")
    public void createUser(@RequestParam String pseudo,
                             @RequestParam String email,
                             @RequestParam String password) {
        System.out.println("[Controller] Request Create User for " + email);

        try {
            supervisor.dispatch(new CreateUserMessage(pseudo, email, password));
        } catch (Exception e) {
            System.out.println("[Controller] Error: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public void login(@RequestParam String email,
                        @RequestParam String password) {
        System.out.println("[Controller] Request for login : " + email);

        try {
            supervisor.dispatch(new LoginUserMessage(email, password));
        } catch (Exception e) {
            System.out.println("[Controller] Error: " + e.getMessage());
        }
    }
}
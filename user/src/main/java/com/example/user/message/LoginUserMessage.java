package com.example.user.message;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginUserMessage {
    private String email;
    private String password;
    public String getEmail() { return email; }
    public String getPassword() { return password; }
}
package com.example.user.dto;

public class LoginResponse {
    private String token;
    private Long userId;
    private String pseudo;
    private String email;
    private int expiresIn;

    public LoginResponse(String token, Long userId, String pseudo, String email, int expiresIn) {
        this.token = token;
        this.userId = userId;
        this.pseudo = pseudo;
        this.email = email;
        this.expiresIn = expiresIn;
    }

    public String getToken() { return token; }
    public Long getUserId() { return userId; }
    public String getPseudo() { return pseudo; }
    public String getEmail() { return email; }
    public int getExpiresIn() { return expiresIn; }
}

package com.example.user.dto;

public class UserResponse {
    private Long id;
    private String pseudo;
    private String email;
    private String message;

    public UserResponse(Long id, String pseudo, String email, String message) {
        this.id = id;
        this.pseudo = pseudo;
        this.email = email;
        this.message = message;
    }

    public Long getId() { return id; }
    public String getPseudo() { return pseudo; }
    public String getEmail() { return email; }
    public String getMessage() { return message; }
}
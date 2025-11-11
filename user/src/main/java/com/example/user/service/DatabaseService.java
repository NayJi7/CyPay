package com.example.user.service;

import com.example.user.model.User;
import com.example.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DatabaseService {
    private final UserRepository repo;

    public DatabaseService(UserRepository repo) {
        this.repo = repo;
    }

    public User saveUser(User user) {
        return repo.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return repo.findByEmail(email);
    }

    public Optional<User> findById(Long id) { return repo.findById(id); }
}

package com.example.user.service;

import com.example.user.exception.EmailAlreadyExistsException;
import com.example.user.exception.InvalidCredentialsException;
import com.example.user.exception.UserNotFoundException;
import com.example.user.model.User;
import com.example.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createUser(String pseudo, String email, String password) {
        System.out.println("[UserService] Creating user: " + email);

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email already exists: " + email);
        }

        User user = new User();
        user.setPseudo(pseudo);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password)); // Hash le mot de passe avec BCrypt

        User savedUser = userRepository.save(user);
        System.out.println("[UserService] User created with ID: " + savedUser.getId());

        return savedUser;
    }

    public User authenticate(String email, String password) {
        System.out.println("[UserService] Authenticating user: " + email);

        // Trouver l'utilisateur par email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        // Vérifier le mot de passe
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        System.out.println("[UserService] Authentication successful for: " + email);
        return user;
    }

    @Transactional
    public User updateUser(Long id, String pseudo, String email, String password) {
        System.out.println("[UserService] Updating user ID: " + id);

        // Trouver l'utilisateur
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));

        // Mettre à jour les champs non-null
        if (pseudo != null && !pseudo.isBlank()) {
            user.setPseudo(pseudo);
        }

        if (email != null && !email.isBlank()) {
            // Vérifier si le nouvel email existe déjà (pour un autre utilisateur)
            Optional<User> existingUser = userRepository.findByEmail(email);
            if (existingUser.isPresent() && !existingUser.get().getId().equals(id)) {
                throw new EmailAlreadyExistsException("Email already exists: " + email);
            }
            user.setEmail(email);
        }

        if (password != null && !password.isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
        }

        User updatedUser = userRepository.save(user);
        System.out.println("[UserService] User updated: " + updatedUser.getId());

        return updatedUser;
    }


    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }

    @Transactional
    public void deleteUser(Long id) {
        System.out.println("[UserService] Deleting user ID: " + id);

        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found with ID: " + id);
        }

        userRepository.deleteById(id);
        System.out.println("[UserService] User deleted: " + id);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));
    }
}
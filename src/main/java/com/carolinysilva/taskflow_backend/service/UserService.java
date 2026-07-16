package com.carolinysilva.taskflow_backend.service;

import com.carolinysilva.taskflow_backend.entity.User;
import com.carolinysilva.taskflow_backend.exception.BusinessRuleException;
import com.carolinysilva.taskflow_backend.exception.ResourceNotFoundException;
import com.carolinysilva.taskflow_backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(String name, String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new BusinessRuleException("E-mail já está em uso: " + normalizedEmail);
        }

        User user = new User(name.trim(), normalizedEmail, passwordEncoder.encode(rawPassword));
        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado para o e-mail: " + normalizedEmail));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}

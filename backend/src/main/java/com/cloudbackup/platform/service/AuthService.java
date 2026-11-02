package com.cloudbackup.platform.service;

import com.cloudbackup.platform.config.JwtTokenProvider;
import com.cloudbackup.platform.dto.request.LoginRequest;
import com.cloudbackup.platform.dto.request.RegisterRequest;
import com.cloudbackup.platform.dto.response.AuthResponse;
import com.cloudbackup.platform.exception.DuplicateFileException;
import com.cloudbackup.platform.exception.ResourceNotFoundException;
import com.cloudbackup.platform.exception.UnauthorizedAccessException;
import com.cloudbackup.platform.model.Role;
import com.cloudbackup.platform.model.User;
import com.cloudbackup.platform.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("[Auth] Registering user '{}' ({})", request.getUsername(), request.getEmail());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateFileException("Username '" + request.getUsername() + "' is already taken.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateFileException("Email '" + request.getEmail() + "' is already in use.");
        }

        User user = new User(
                request.getUsername().trim(),
                request.getEmail().trim().toLowerCase(),
                passwordEncoder.encode(request.getPassword()),
                Role.ROLE_USER
        );

        User savedUser = userRepository.save(user);
        String token = tokenProvider.generateToken(savedUser.getUsername(), savedUser.getId(), savedUser.getRole().name());

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole().name(),
                savedUser.getStorageQuotaBytes(),
                savedUser.getUsedStorageBytes()
        );
    }

    public AuthResponse login(LoginRequest request) {
        String identifier = request.getUsernameOrEmail().trim();
        log.info("[Auth] Login attempt for identifier '{}'", identifier);

        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier.toLowerCase()))
                .orElseThrow(() -> new UnauthorizedAccessException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedAccessException("Invalid username or password");
        }

        String token = tokenProvider.generateToken(user.getUsername(), user.getId(), user.getRole().name());

        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.getStorageQuotaBytes(),
                user.getUsedStorageBytes()
        );
    }

    public User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            throw new UnauthorizedAccessException("No authenticated user found in security context");
        }

        if (auth.getPrincipal() instanceof User user) {
            return userRepository.findById(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found in database"));
        }

        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User '" + username + "' not found"));
    }
}

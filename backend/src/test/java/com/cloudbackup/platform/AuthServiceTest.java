package com.cloudbackup.platform;

import com.cloudbackup.platform.config.JwtTokenProvider;
import com.cloudbackup.platform.dto.request.LoginRequest;
import com.cloudbackup.platform.dto.request.RegisterRequest;
import com.cloudbackup.platform.dto.response.AuthResponse;
import com.cloudbackup.platform.exception.DuplicateFileException;
import com.cloudbackup.platform.exception.UnauthorizedAccessException;
import com.cloudbackup.platform.model.Role;
import com.cloudbackup.platform.model.User;
import com.cloudbackup.platform.repository.UserRepository;
import com.cloudbackup.platform.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit & Security Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User("cloudarchitect", "architect@cloud.internal", "hashed_password", Role.ROLE_USER);
        sampleUser.setId(101L);
    }

    @Test
    @DisplayName("Should successfully register new user with hashed password and JWT")
    void testRegister_Success() {
        RegisterRequest request = new RegisterRequest("cloudarchitect", "architect@cloud.internal", "SuperSecret123!");

        when(userRepository.existsByUsername("cloudarchitect")).thenReturn(false);
        when(userRepository.existsByEmail("architect@cloud.internal")).thenReturn(false);
        when(passwordEncoder.encode("SuperSecret123!")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(tokenProvider.generateToken(anyString(), anyLong(), anyString())).thenReturn("mock.jwt.token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("cloudarchitect", response.getUsername());
        assertEquals("architect@cloud.internal", response.getEmail());
        assertEquals("mock.jwt.token", response.getToken());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject registration if username is already taken")
    void testRegister_DuplicateUsername() {
        RegisterRequest request = new RegisterRequest("cloudarchitect", "other@cloud.internal", "Password123!");
        when(userRepository.existsByUsername("cloudarchitect")).thenReturn(true);

        assertThrows(DuplicateFileException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully login with valid credentials")
    void testLogin_Success() {
        LoginRequest request = new LoginRequest("cloudarchitect", "SuperSecret123!");

        when(userRepository.findByUsername("cloudarchitect")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("SuperSecret123!", "hashed_password")).thenReturn(true);
        when(tokenProvider.generateToken(eq("cloudarchitect"), eq(101L), eq("ROLE_USER"))).thenReturn("valid.jwt.token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("valid.jwt.token", response.getToken());
        assertEquals(101L, response.getId());
    }

    @Test
    @DisplayName("Should reject login with invalid password")
    void testLogin_InvalidPassword() {
        LoginRequest request = new LoginRequest("cloudarchitect", "WrongPassword!");

        when(userRepository.findByUsername("cloudarchitect")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("WrongPassword!", "hashed_password")).thenReturn(false);

        assertThrows(UnauthorizedAccessException.class, () -> authService.login(request));
    }
}

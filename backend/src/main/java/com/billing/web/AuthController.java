package com.billing.web;

import com.billing.domain.AppUser;
import com.billing.dto.Dtos.*;
import com.billing.repo.UserRepository;
import com.billing.security.JwtService;
import com.billing.service.ApiException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final int MAX_FAILS = 5;
    private static final long WINDOW_SEC = 600;
    private static final String DUMMY_HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5BqtRrQkYQ3Gm6S2x2oZ7s0VZ6V9i";

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final Map<String, Deque<Instant>> fails = new ConcurrentHashMap<>();

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest r) {
        String key = r.username().toLowerCase();
        Deque<Instant> q = fails.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            Instant cutoff = Instant.now().minusSeconds(WINDOW_SEC);
            while (!q.isEmpty() && q.peekFirst().isBefore(cutoff)) q.pollFirst();
            if (q.size() >= MAX_FAILS) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed attempts. Try again in a few minutes.");
            }
        }
        AppUser u = users.findByUsernameIgnoreCase(r.username()).orElse(null);
        boolean ok = encoder.matches(r.password(), u != null ? u.passwordHash : DUMMY_HASH) && u != null && u.active;
        if (!ok) {
            synchronized (q) { q.addLast(Instant.now()); }
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        fails.remove(key);
        return new LoginResponse(jwt.issue(u), UserDto.of(u));
    }

    @GetMapping("/me")
    public UserDto me(Authentication a) {
        return UserDto.of(users.findByUsernameIgnoreCase(a.getName()).orElseThrow());
    }

    @PostMapping("/change-password")
    public void change(Authentication a, @Valid @RequestBody PasswordChange r) {
        AppUser u = users.findByUsernameIgnoreCase(a.getName()).orElseThrow();
        if (!encoder.matches(r.currentPassword(), u.passwordHash)) {
            throw ApiException.badRequest("Current password is incorrect");
        }
        u.passwordHash = encoder.encode(r.newPassword());
        users.save(u);
    }
}

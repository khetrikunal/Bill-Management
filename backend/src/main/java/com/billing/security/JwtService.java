package com.billing.security;

import com.billing.config.AppProps;
import com.billing.domain.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expiryMinutes;

    public JwtService(AppProps props) {
        String secret = props.jwtSecret();
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be set and at least 32 characters long");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiryMinutes = props.jwtExpiryMinutes();
    }

    public String issue(AppUser u) {
        Instant now = Instant.now();
        return Jwts.builder().subject(u.username).claim("role", u.role.name())
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(expiryMinutes * 60)))
                .signWith(key).compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}

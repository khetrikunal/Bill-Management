package com.billing.security;

import com.billing.domain.AppUser;
import com.billing.repo.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

/** Validates the bearer token and loads the user's CURRENT role/active flag from the database on every request. */
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserRepository users;

    public JwtAuthFilter(JwtService jwt, UserRepository users) {
        this.jwt = jwt;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            try {
                Claims c = jwt.parse(h.substring(7));
                AppUser u = users.findByUsernameIgnoreCase(c.getSubject()).orElse(null);
                if (u != null && u.active) {
                    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                            u.username, null, List.of(new SimpleGrantedAuthority("ROLE_" + u.role.name()))));
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                // invalid or expired token: request stays unauthenticated and gets a 401
            }
        }
        chain.doFilter(req, res);
    }
}

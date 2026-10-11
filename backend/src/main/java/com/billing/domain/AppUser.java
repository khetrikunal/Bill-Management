package com.billing.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "app_user")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String username;
    public String passwordHash;
    public String fullName;
    @Enumerated(EnumType.STRING) public Role role;
    public boolean active = true;
    public Instant createdAt = Instant.now();
}

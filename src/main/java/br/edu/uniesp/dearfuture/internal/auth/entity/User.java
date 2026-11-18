package br.edu.uniesp.dearfuture.internal.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name = "app_users")
public class User {
    @Id private UUID id;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, length = 254, unique = true) private String email;
    @Column(name = "password_hash", nullable = false, length = 255) private String passwordHash;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected User() {}
    public User(UUID id, String name, String email, String passwordHash, Instant createdAt) {
        this.id = java.util.Objects.requireNonNull(id);
        this.name = java.util.Objects.requireNonNull(name);
        this.email = java.util.Objects.requireNonNull(email);
        this.passwordHash = java.util.Objects.requireNonNull(passwordHash);
        this.createdAt = java.util.Objects.requireNonNull(createdAt);
    }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Instant getCreatedAt() { return createdAt; }
}

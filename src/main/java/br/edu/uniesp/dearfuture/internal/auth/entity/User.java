package br.edu.uniesp.dearfuture.internal.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import java.util.Locale;
import java.util.regex.Pattern;
@Entity
@Table(name = "app_users")
public class User {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    @Id private UUID id;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, length = 254, unique = true) private String email;
    @Column(name = "password_hash", nullable = false, length = 255) private String passwordHash;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected User() {}
    public User(UUID id, String name, String email, String passwordHash, Instant createdAt) {
        this.id = java.util.Objects.requireNonNull(id);
        changeDisplayName(name);
        if (email == null) throw new IllegalArgumentException("E-mail obrigatório.");
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.length() > 254 || !EMAIL.matcher(normalizedEmail).matches()) {
            throw new IllegalArgumentException("E-mail inválido.");
        }
        this.email = normalizedEmail;
        if (passwordHash == null || passwordHash.isBlank() || passwordHash.length() > 255) {
            throw new IllegalArgumentException("Hash da senha obrigatório e limitado a 255 caracteres.");
        }
        this.passwordHash = passwordHash;
        this.createdAt = java.util.Objects.requireNonNull(createdAt);
    }
    public void changeDisplayName(String name) {
        if (name == null || name.isBlank() || name.trim().length() > 120) {
            throw new IllegalArgumentException("Nome obrigatório e limitado a 120 caracteres.");
        }
        this.name = name.trim();
    }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Instant getCreatedAt() { return createdAt; }
}

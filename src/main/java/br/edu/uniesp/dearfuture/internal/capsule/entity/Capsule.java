package br.edu.uniesp.dearfuture.internal.capsule.entity;

import br.edu.uniesp.dearfuture.internal.auth.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name = "capsules")
public class Capsule {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false) private User owner;
    @Column(nullable = false, length = 120) private String title;
    @Column(nullable = false, length = 10000) private String message;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "opens_at", nullable = false) private Instant opensAt;
    protected Capsule() {}
    public Capsule(UUID id, User owner, String title, String message, Instant createdAt, Instant opensAt) {
        this.id = java.util.Objects.requireNonNull(id);
        this.owner = java.util.Objects.requireNonNull(owner);
        this.createdAt = java.util.Objects.requireNonNull(createdAt);
        this.opensAt = java.util.Objects.requireNonNull(opensAt);
        if (title == null || title.isBlank() || title.trim().length() > 120
                || message == null || message.isBlank() || message.trim().length() > 10000
                || !opensAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("Dados inválidos para a cápsula.");
        }
        this.title = title.trim();
        this.message = message.trim();
    }
    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getOpensAt() { return opensAt; }
}

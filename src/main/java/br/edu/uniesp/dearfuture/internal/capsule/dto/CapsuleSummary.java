package br.edu.uniesp.dearfuture.internal.capsule.dto;

import java.time.Instant;
import java.util.UUID;
public record CapsuleSummary(UUID id, String title, Instant createdAt, Instant opensAt, String status) {}

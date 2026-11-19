package br.edu.uniesp.dearfuture.internal.capsule.dto;

import java.util.UUID;
public record CapsuleMessage(UUID id, String message) {}
package br.edu.uniesp.dearfuture.internal.auth.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}

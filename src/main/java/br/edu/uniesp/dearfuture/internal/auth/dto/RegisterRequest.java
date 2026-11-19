package br.edu.uniesp.dearfuture.internal.auth.dto;

import jakarta.validation.constraints.*;
import java.util.Locale;
public record RegisterRequest(@NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull @Size(min = 12, max = 128) String password) {
    public RegisterRequest {
        name = name == null ? null : name.trim();
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}

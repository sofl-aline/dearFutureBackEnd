package br.edu.uniesp.dearfuture.internal.auth.dto;

import jakarta.validation.constraints.*;
import java.util.Locale;
public record LoginRequest(@NotBlank @Email @Size(max = 254) String email,
        @NotNull @Size(min = 12, max = 128) String password) {
    public LoginRequest { email = email == null ? null : email.trim().toLowerCase(Locale.ROOT); }
}

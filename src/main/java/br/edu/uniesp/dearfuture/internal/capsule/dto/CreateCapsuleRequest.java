package br.edu.uniesp.dearfuture.internal.capsule.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
public record CreateCapsuleRequest(@NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 10000) String message, @NotNull Instant opensAt) {
    public CreateCapsuleRequest {
        title = title == null ? null : title.trim();
        message = message == null ? null : message.trim();
    }
}

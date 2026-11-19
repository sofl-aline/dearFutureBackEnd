package br.edu.uniesp.dearfuture.internal.capsule.controller;

import br.edu.uniesp.dearfuture.internal.capsule.dto.CapsuleMessage;
import br.edu.uniesp.dearfuture.internal.capsule.service.CapsuleOpeningService;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/capsules")
public class CapsuleOpeningController {
    private final CapsuleOpeningService service;
    public CapsuleOpeningController(CapsuleOpeningService service) { this.service = service; }
    @GetMapping("/{id}/message")
    public CapsuleMessage read(Authentication authentication, @PathVariable UUID id) {
        return service.read(UUID.fromString(authentication.getName()), id);
    }
}

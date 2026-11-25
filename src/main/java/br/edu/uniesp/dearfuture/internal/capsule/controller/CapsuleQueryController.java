package br.edu.uniesp.dearfuture.internal.capsule.controller;

import br.edu.uniesp.dearfuture.internal.capsule.dto.CapsuleSummary;
import br.edu.uniesp.dearfuture.internal.capsule.service.CapsuleQueryService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/capsules")
public class CapsuleQueryController {
    private final CapsuleQueryService service;
    public CapsuleQueryController(CapsuleQueryService service) { this.service = service; }
    @GetMapping
    public List<CapsuleSummary> list(Authentication authentication) {
        return service.list(UUID.fromString(authentication.getName()));
    }
    @GetMapping("/{id}")
    public CapsuleSummary get(Authentication authentication, @PathVariable UUID id) {
        return service.get(UUID.fromString(authentication.getName()), id);
    }
}

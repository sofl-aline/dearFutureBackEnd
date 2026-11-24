package br.edu.uniesp.dearfuture.internal.capsule.controller;

import br.edu.uniesp.dearfuture.internal.capsule.dto.*;
import br.edu.uniesp.dearfuture.internal.capsule.service.CapsuleCreationService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/capsules")
public class CapsuleCreationController {
    private final CapsuleCreationService service;
    public CapsuleCreationController(CapsuleCreationService service) { this.service = service; }
    @PostMapping
    public ResponseEntity<CapsuleSummary> create(Authentication authentication, @Valid @RequestBody CreateCapsuleRequest request) {
        var result = service.create(UUID.fromString(authentication.getName()), request);
        return ResponseEntity.created(URI.create("/api/v1/capsules/" + result.id())).body(result);
    }
}

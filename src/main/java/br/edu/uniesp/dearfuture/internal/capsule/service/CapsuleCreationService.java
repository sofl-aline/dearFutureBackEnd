package br.edu.uniesp.dearfuture.internal.capsule.service;

import br.edu.uniesp.dearfuture.internal.auth.repository.UserRepository;
import br.edu.uniesp.dearfuture.internal.capsule.dto.*;
import br.edu.uniesp.dearfuture.internal.capsule.entity.Capsule;
import br.edu.uniesp.dearfuture.internal.capsule.mapper.CapsuleSummaryMapper;
import br.edu.uniesp.dearfuture.internal.capsule.repository.CapsuleRepository;
import br.edu.uniesp.dearfuture.shared.exception.ApiException;
import static br.edu.uniesp.dearfuture.shared.exception.ApiException.Code.*;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class CapsuleCreationService {
    private final CapsuleRepository capsules;
    private final UserRepository users;
    private final Clock clock;
    public CapsuleCreationService(CapsuleRepository capsules, UserRepository users, Clock clock) {
        this.capsules = capsules; this.users = users; this.clock = clock;
    }
    @Transactional
    public CapsuleSummary create(UUID userId, CreateCapsuleRequest input) {
        var now = clock.instant();
        Map<String, String> errors = new LinkedHashMap<>();
        if (input.title() == null || input.title().isBlank() || input.title().length() > 120)
            errors.put("title", "Informe um título de até 120 caracteres.");
        if (input.message() == null || input.message().isBlank() || input.message().length() > 10000)
            errors.put("message", "Informe uma carta de até 10000 caracteres.");
        if (input.opensAt() == null || !input.opensAt().isAfter(now))
            errors.put("opensAt", "Escolha uma data e hora futuras.");
        if (!errors.isEmpty()) throw new ApiException(VALIDATION_ERROR, "Revise os campos informados.", errors);
        var owner = users.findById(userId).orElseThrow(() -> new ApiException(UNAUTHENTICATED, "Usuário não encontrado."));
        var capsule = new Capsule(UUID.randomUUID(), owner, input.title(), input.message(), now, input.opensAt());
        return CapsuleSummaryMapper.toSummary(capsules.save(capsule), now);
    }
}

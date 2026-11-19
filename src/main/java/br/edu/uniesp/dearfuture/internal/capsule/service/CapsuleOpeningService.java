package br.edu.uniesp.dearfuture.internal.capsule.service;

import br.edu.uniesp.dearfuture.internal.capsule.dto.CapsuleMessage;
import br.edu.uniesp.dearfuture.internal.capsule.policy.CapsuleAvailability;
import br.edu.uniesp.dearfuture.internal.capsule.repository.CapsuleRepository;
import br.edu.uniesp.dearfuture.shared.exception.ApiException;
import static br.edu.uniesp.dearfuture.shared.exception.ApiException.Code.*;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class CapsuleOpeningService {
    private final CapsuleRepository capsules;
    private final Clock clock;
    public CapsuleOpeningService(CapsuleRepository capsules, Clock clock) { this.capsules = capsules; this.clock = clock; }
    @Transactional(readOnly = true)
    public CapsuleMessage read(UUID userId, UUID id) {
        var capsule = capsules.findByIdAndOwner_Id(id, userId)
                .orElseThrow(() -> new ApiException(CAPSULE_NOT_FOUND, "Cápsula não encontrada."));
        if (!CapsuleAvailability.isAvailable(capsule.getOpensAt(), clock.instant()))
            throw new ApiException(CAPSULE_SEALED, "A cápsula ainda está selada.");
        return new CapsuleMessage(capsule.getId(), capsule.getMessage());
    }
}

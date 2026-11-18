package br.edu.uniesp.dearfuture.internal.capsule.mapper;

import br.edu.uniesp.dearfuture.internal.capsule.dto.CapsuleSummary;
import br.edu.uniesp.dearfuture.internal.capsule.entity.Capsule;
import br.edu.uniesp.dearfuture.internal.capsule.policy.CapsuleAvailability;
import java.time.Instant;
public final class CapsuleSummaryMapper {
    private CapsuleSummaryMapper() {}
    public static CapsuleSummary toSummary(Capsule capsule, Instant now) {
        return new CapsuleSummary(capsule.getId(), capsule.getTitle(), capsule.getCreatedAt(),
                capsule.getOpensAt(), CapsuleAvailability.isAvailable(capsule.getOpensAt(), now)
                ? "AVAILABLE" : "SEALED");
    }
}

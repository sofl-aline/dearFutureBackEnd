package br.edu.uniesp.dearfuture.internal.capsule.service;

import br.edu.uniesp.dearfuture.internal.capsule.dto.CapsuleSummary;
import br.edu.uniesp.dearfuture.internal.capsule.mapper.CapsuleSummaryMapper;
import br.edu.uniesp.dearfuture.internal.capsule.repository.CapsuleRepository;
import br.edu.uniesp.dearfuture.shared.exception.ApiException;
import static br.edu.uniesp.dearfuture.shared.exception.ApiException.Code.CAPSULE_NOT_FOUND;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional(readOnly = true)
public class CapsuleQueryService {
    private final CapsuleRepository capsules;
    private final Clock clock;
    public CapsuleQueryService(CapsuleRepository capsules, Clock clock) { this.capsules = capsules; this.clock = clock; }
    public List<CapsuleSummary> list(UUID userId) {
        var now = clock.instant();
        return capsules.findAllByOwner_IdOrderByCreatedAtDescIdAsc(userId).stream()
                .map(capsule -> CapsuleSummaryMapper.toSummary(capsule, now)).toList();
    }
    public CapsuleSummary get(UUID userId, UUID id) {
        var capsule = capsules.findByIdAndOwner_Id(id, userId)
                .orElseThrow(() -> new ApiException(CAPSULE_NOT_FOUND, "Cápsula não encontrada."));
        return CapsuleSummaryMapper.toSummary(capsule, clock.instant());
    }
}

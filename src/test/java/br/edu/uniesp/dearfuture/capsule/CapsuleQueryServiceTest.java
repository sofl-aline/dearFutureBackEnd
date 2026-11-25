package br.edu.uniesp.dearfuture.capsule;

import br.edu.uniesp.dearfuture.internal.auth.entity.User;
import br.edu.uniesp.dearfuture.internal.capsule.entity.Capsule;
import br.edu.uniesp.dearfuture.internal.capsule.repository.CapsuleRepository;
import br.edu.uniesp.dearfuture.internal.capsule.service.CapsuleQueryService;
import br.edu.uniesp.dearfuture.shared.exception.ApiException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class CapsuleQueryServiceTest {
    final CapsuleRepository capsules = mock(CapsuleRepository.class);
    final Instant now = Instant.parse("2030-01-01T12:00:00Z");
    final UUID owner = UUID.randomUUID();
    final CapsuleQueryService service = new CapsuleQueryService(capsules, Clock.fixed(now, ZoneOffset.UTC));
    @Test void listsOnlyByAuthenticatedOwnerAndCalculatesAvailability() {
        var user = new User(owner, "A", "a@example.com", "hash", now.minusSeconds(60));
        var capsule = new Capsule(UUID.randomUUID(), user, "T", "Carta", now.minusSeconds(60), now);
        when(capsules.findAllByOwner_IdOrderByCreatedAtDescIdAsc(owner)).thenReturn(List.of(capsule));
        assertThat(service.list(owner)).singleElement().satisfies(c -> assertThat(c.status()).isEqualTo("AVAILABLE"));
        verify(capsules).findAllByOwner_IdOrderByCreatedAtDescIdAsc(owner);
    }
    @Test void missingOrOtherOwnersCapsuleReturnsSameException() {
        UUID id = UUID.randomUUID();
        when(capsules.findByIdAndOwner_Id(id, owner)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(owner, id)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getCode()).isEqualTo(ApiException.Code.CAPSULE_NOT_FOUND));
        verify(capsules, never()).findById(any());
    }
    @Test void emptyListIsNotError() {
        when(capsules.findAllByOwner_IdOrderByCreatedAtDescIdAsc(owner)).thenReturn(List.of());
        assertThat(service.list(owner)).isEmpty();
    }
}

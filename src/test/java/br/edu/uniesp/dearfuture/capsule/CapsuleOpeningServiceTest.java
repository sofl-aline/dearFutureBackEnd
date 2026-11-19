package br.edu.uniesp.dearfuture.capsule;

import br.edu.uniesp.dearfuture.internal.auth.entity.User;
import br.edu.uniesp.dearfuture.internal.capsule.entity.Capsule;
import br.edu.uniesp.dearfuture.internal.capsule.repository.CapsuleRepository;
import br.edu.uniesp.dearfuture.internal.capsule.service.CapsuleOpeningService;
import br.edu.uniesp.dearfuture.shared.exception.ApiException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class CapsuleOpeningServiceTest {
    final CapsuleRepository capsules = mock(CapsuleRepository.class);
    final Instant opens = Instant.parse("2030-01-01T12:00:00Z");
    final UUID owner = UUID.randomUUID(), id = UUID.randomUUID();
    final User user = new User(owner, "A", "a@example.com", "hash", opens.minusSeconds(60));
    final Capsule capsule = new Capsule(id, user, "T", "Carta secreta", opens.minusSeconds(60), opens);
    CapsuleOpeningService at(Instant now) { return new CapsuleOpeningService(capsules, Clock.fixed(now, ZoneOffset.UTC)); }
    @Test void blocksOneNanosecondBeforeOpening() {
        when(capsules.findByIdAndOwner_Id(id, owner)).thenReturn(Optional.of(capsule));
        assertThatThrownBy(() -> at(opens.minusNanos(1)).read(owner, id)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getCode()).isEqualTo(ApiException.Code.CAPSULE_SEALED));
    }
    @Test void releasesExactlyAtOpeningAndAfter() {
        when(capsules.findByIdAndOwner_Id(id, owner)).thenReturn(Optional.of(capsule));
        assertThat(at(opens).read(owner, id).message()).isEqualTo("Carta secreta");
        assertThat(at(opens.plusSeconds(1)).read(owner, id).message()).isEqualTo("Carta secreta");
    }
    @Test void otherOwnerNeverGetsContentOrTimingInformation() {
        UUID other = UUID.randomUUID();
        when(capsules.findByIdAndOwner_Id(id, other)).thenReturn(Optional.empty());
        for (Instant now : List.of(opens.minusSeconds(1), opens.plusSeconds(1))) {
            assertThatThrownBy(() -> at(now).read(other, id)).isInstanceOfSatisfying(ApiException.class,
                    e -> assertThat(e.getCode()).isEqualTo(ApiException.Code.CAPSULE_NOT_FOUND));
        }
        verify(capsules, never()).findById(any());
    }
}

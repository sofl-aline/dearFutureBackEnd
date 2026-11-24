package br.edu.uniesp.dearfuture.capsule;

import br.edu.uniesp.dearfuture.internal.auth.entity.User;
import br.edu.uniesp.dearfuture.internal.auth.repository.UserRepository;
import br.edu.uniesp.dearfuture.internal.capsule.dto.CreateCapsuleRequest;
import br.edu.uniesp.dearfuture.internal.capsule.repository.CapsuleRepository;
import br.edu.uniesp.dearfuture.internal.capsule.service.CapsuleCreationService;
import br.edu.uniesp.dearfuture.shared.exception.ApiException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class CapsuleCreationServiceTest {
    final CapsuleRepository capsules = mock(CapsuleRepository.class);
    final UserRepository users = mock(UserRepository.class);
    final Instant now = Instant.parse("2030-01-01T12:00:00Z");
    final UUID owner = UUID.randomUUID();
    final CapsuleCreationService service = new CapsuleCreationService(capsules, users, Clock.fixed(now, ZoneOffset.UTC));
    @Test void createsUsingAuthenticatedOwnerAndServerTime() {
        when(users.findById(owner)).thenReturn(Optional.of(new User(owner, "A", "a@example.com", "hash", now)));
        when(capsules.save(any())).thenAnswer(call -> call.getArgument(0));
        var result = service.create(owner, new CreateCapsuleRequest(" Título ", " Carta ", now.plusSeconds(60)));
        assertThat(result.title()).isEqualTo("Título");
        assertThat(result.createdAt()).isEqualTo(now);
        assertThat(result.status()).isEqualTo("SEALED");
        verify(users).findById(owner);
    }
    @Test void rejectsPastAndExactlyNow() {
        for (Instant date : List.of(now.minusSeconds(1), now)) {
            assertThatThrownBy(() -> service.create(owner, new CreateCapsuleRequest("T", "Carta", date)))
                    .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrors()).containsKey("opensAt"));
        }
        verifyNoInteractions(capsules);
    }
    @Test void rejectsBlankAndOversizedContent() {
        for (String title : List.of(" ", "a".repeat(121))) {
            assertThatThrownBy(() -> service.create(owner, new CreateCapsuleRequest(title, "Carta", now.plusSeconds(1))))
                    .isInstanceOf(ApiException.class);
        }
        assertThatThrownBy(() -> service.create(owner, new CreateCapsuleRequest("T", "a".repeat(10001), now.plusSeconds(1))))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(capsules);
    }
}

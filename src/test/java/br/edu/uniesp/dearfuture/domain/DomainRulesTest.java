package br.edu.uniesp.dearfuture.domain;

import br.edu.uniesp.dearfuture.internal.auth.entity.User;
import br.edu.uniesp.dearfuture.internal.capsule.entity.Capsule;
import br.edu.uniesp.dearfuture.internal.capsule.mapper.CapsuleSummaryMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class DomainRulesTest {
    private final Instant now = Instant.parse("2030-01-01T12:00:00Z");

    private User user() {
        return new User(UUID.randomUUID(), " Ana ", " ANA@EXAMPLE.COM ", "hash-fixture", now);
    }

    @Test void normalizesAccountAndChangesOnlyDisplayName() {
        var user = user();
        var id = user.getId();
        assertThat(user.getName()).isEqualTo("Ana");
        assertThat(user.getEmail()).isEqualTo("ana@example.com");
        user.changeDisplayName(" Maria ");
        assertThat(user.getName()).isEqualTo("Maria");
        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getEmail()).isEqualTo("ana@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("hash-fixture");
        assertThat(user.getCreatedAt()).isEqualTo(now);
        for (String invalid : new String[]{null, " ", "a".repeat(121)}) {
            assertThatIllegalArgumentException().isThrownBy(() -> user.changeDisplayName(invalid));
            assertThat(user.getName()).isEqualTo("Maria");
        }
    }

    @Test void rejectsInvalidEmailAndEmptyHash() {
        for (String email : new String[]{null, "", "invalid", "a@@example.com", "a b@example.com", "a".repeat(250) + "@x.com"}) {
            assertThatIllegalArgumentException().isThrownBy(() ->
                    new User(UUID.randomUUID(), "Ana", email, "hash", now));
        }
        assertThatIllegalArgumentException().isThrownBy(() ->
                new User(UUID.randomUUID(), "Ana", "a@example.com", " ", now));
    }

    @Test void rejectsInvalidCapsulesAndHonorsExactReleaseInstant() {
        for (Instant date : new Instant[]{now.minusSeconds(1), now}) {
            assertThatIllegalArgumentException().isThrownBy(() ->
                    new Capsule(UUID.randomUUID(), user(), "Título", "Carta", now, date));
        }
        assertThatIllegalArgumentException().isThrownBy(() ->
                new Capsule(UUID.randomUUID(), user(), " ", "Carta", now, now.plusSeconds(60)));
        assertThatIllegalArgumentException().isThrownBy(() ->
                new Capsule(UUID.randomUUID(), user(), "Título", "a".repeat(10001), now, now.plusSeconds(60)));
        var capsule = new Capsule(UUID.randomUUID(), user(), " Título ", " Carta ", now, now.plusSeconds(60));
        assertThat(capsule.getTitle()).isEqualTo("Título");
        assertThat(capsule.getMessage()).isEqualTo("Carta");
        assertThat(CapsuleSummaryMapper.toSummary(capsule, now.plusSeconds(59)).status()).isEqualTo("SEALED");
        assertThat(CapsuleSummaryMapper.toSummary(capsule, now.plusSeconds(60)).status()).isEqualTo("AVAILABLE");
        assertThat(CapsuleSummaryMapper.toSummary(capsule, now.plusSeconds(61)).status()).isEqualTo("AVAILABLE");
    }
}

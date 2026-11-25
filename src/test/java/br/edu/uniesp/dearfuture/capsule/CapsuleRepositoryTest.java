package br.edu.uniesp.dearfuture.capsule;

import br.edu.uniesp.dearfuture.internal.auth.entity.User;
import br.edu.uniesp.dearfuture.internal.auth.repository.UserRepository;
import br.edu.uniesp.dearfuture.internal.capsule.entity.Capsule;
import br.edu.uniesp.dearfuture.internal.capsule.repository.CapsuleRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CapsuleRepositoryTest {
    @Autowired UserRepository users;
    @Autowired CapsuleRepository capsules;
    @Test void newestFirstAndOtherAccountExcluded() {
        var now = Instant.parse("2030-01-01T12:00:00Z");
        var a = users.saveAndFlush(new User(UUID.randomUUID(), "A", UUID.randomUUID()+"@example.com", "hash", now));
        var b = users.saveAndFlush(new User(UUID.randomUUID(), "B", UUID.randomUUID()+"@example.com", "hash", now));
        var older = capsules.saveAndFlush(new Capsule(UUID.randomUUID(), a, "Antiga", "Carta", now, now.plusSeconds(600)));
        var newer = capsules.saveAndFlush(new Capsule(UUID.randomUUID(), a, "Nova", "Carta", now.plusSeconds(1), now.plusSeconds(600)));
        capsules.saveAndFlush(new Capsule(UUID.randomUUID(), b, "De B", "Carta", now.plusSeconds(2), now.plusSeconds(600)));
        assertThat(capsules.findAllByOwner_IdOrderByCreatedAtDescIdAsc(a.getId()))
                .extracting(Capsule::getId).containsExactly(newer.getId(), older.getId());
        assertThat(capsules.findByIdAndOwner_Id(newer.getId(), b.getId())).isEmpty();
    }
}

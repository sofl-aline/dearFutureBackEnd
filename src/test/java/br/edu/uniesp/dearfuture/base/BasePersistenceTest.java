package br.edu.uniesp.dearfuture.base;

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
class BasePersistenceTest {
    @Autowired UserRepository users;
    @Autowired CapsuleRepository capsules;
    @Test void migrationAndOwnershipWork() {
        var now = Instant.parse("2026-11-18T12:00:00Z");
        var a = users.saveAndFlush(new User(UUID.randomUUID(), "A", "a-base@example.com", "hash-fixture", now));
        var b = users.saveAndFlush(new User(UUID.randomUUID(), "B", "b-base@example.com", "hash-fixture", now));
        var capsule = capsules.saveAndFlush(new Capsule(UUID.randomUUID(), a, "Título", "Carta", now, now.plusSeconds(60)));
        assertThat(capsules.findByIdAndOwner_Id(capsule.getId(), a.getId())).isPresent();
        assertThat(capsules.findByIdAndOwner_Id(capsule.getId(), b.getId())).isEmpty();
        assertThat(capsules.findAllByOwner_IdOrderByCreatedAtDescIdAsc(b.getId())).isEmpty();
    }
}

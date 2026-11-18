package br.edu.uniesp.dearfuture.internal.capsule.repository;

import br.edu.uniesp.dearfuture.internal.capsule.entity.Capsule;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CapsuleRepository extends JpaRepository<Capsule, UUID> {
    List<Capsule> findAllByOwner_IdOrderByCreatedAtDescIdAsc(UUID ownerId);
    Optional<Capsule> findByIdAndOwner_Id(UUID id, UUID ownerId);
}

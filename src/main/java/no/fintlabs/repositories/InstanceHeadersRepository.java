package no.fintlabs.repositories;

import no.fintlabs.model.InstanceHeadersEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstanceHeadersRepository extends JpaRepository<InstanceHeadersEntity, String> {
}

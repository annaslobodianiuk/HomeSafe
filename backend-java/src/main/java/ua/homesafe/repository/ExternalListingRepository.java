package ua.homesafe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.homesafe.model.ExternalListingEntity;

import java.util.List;
import java.util.Optional;

public interface ExternalListingRepository extends JpaRepository<ExternalListingEntity, String> {
    Optional<ExternalListingEntity> findByProviderCodeAndExternalId(String providerCode, String externalId);
    List<ExternalListingEntity> findByFingerprint(String fingerprint);
}

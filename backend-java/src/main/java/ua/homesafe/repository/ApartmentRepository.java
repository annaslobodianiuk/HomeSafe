package ua.homesafe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ua.homesafe.model.ApartmentEntity;

import java.util.Optional;

public interface ApartmentRepository extends JpaRepository<ApartmentEntity, String>, JpaSpecificationExecutor<ApartmentEntity> {
    Optional<ApartmentEntity> findByProviderCodeAndExternalId(String providerCode, String externalId);
}

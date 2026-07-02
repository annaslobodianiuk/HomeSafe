package ua.homesafe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.homesafe.model.InfrastructureEntity;

public interface InfrastructureRepository extends JpaRepository<InfrastructureEntity, String> {
}

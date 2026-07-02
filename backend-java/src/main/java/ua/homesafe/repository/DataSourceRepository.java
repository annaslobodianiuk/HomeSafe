package ua.homesafe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.homesafe.model.DataSourceEntity;

import java.util.Optional;

public interface DataSourceRepository extends JpaRepository<DataSourceEntity, String> {
    Optional<DataSourceEntity> findByCode(String code);
}

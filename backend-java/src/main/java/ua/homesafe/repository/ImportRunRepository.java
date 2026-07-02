package ua.homesafe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.homesafe.model.ImportRunEntity;

import java.util.List;

public interface ImportRunRepository extends JpaRepository<ImportRunEntity, String> {
    List<ImportRunEntity> findTop50ByOrderByStartedAtDesc();
    List<ImportRunEntity> findTop50ByDataSourceCodeOrderByStartedAtDesc(String code);
}

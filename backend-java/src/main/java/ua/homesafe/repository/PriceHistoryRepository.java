package ua.homesafe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.homesafe.model.PriceHistoryEntity;

public interface PriceHistoryRepository extends JpaRepository<PriceHistoryEntity, String> {
}

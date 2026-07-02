package ua.homesafe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.homesafe.model.FavoriteEntity;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<FavoriteEntity, String> {
    List<FavoriteEntity> findByUser_IdOrderByCreatedAtDesc(String userId);
    Optional<FavoriteEntity> findByUser_IdAndApartment_Id(String userId, String apartmentId);
    void deleteByUser_IdAndApartment_Id(String userId, String apartmentId);
}

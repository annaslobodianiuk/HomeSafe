package ua.homesafe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.homesafe.model.UserEntity;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByEmailIgnoreCase(String email);
    List<UserEntity> findByRoleOrderByStatusAscCreatedAtDesc(String role);
}

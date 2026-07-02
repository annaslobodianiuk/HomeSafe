package ua.homesafe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.homesafe.model.SessionEntity;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<SessionEntity, String> {
    Optional<SessionEntity> findByTokenHashAndExpiresAtAfter(String tokenHash, OffsetDateTime now);
}

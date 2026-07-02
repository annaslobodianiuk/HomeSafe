package ua.homesafe.service;

import org.bouncycastle.crypto.generators.SCrypt;
import org.springframework.stereotype.Service;
import ua.homesafe.config.HomesafeProperties;
import ua.homesafe.dto.AuthDtos;
import ua.homesafe.dto.UserDto;
import ua.homesafe.exception.ConflictException;
import ua.homesafe.exception.ForbiddenException;
import ua.homesafe.exception.UnauthorizedException;
import ua.homesafe.model.SessionEntity;
import ua.homesafe.model.UserEntity;
import ua.homesafe.repository.SessionRepository;
import ua.homesafe.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class SessionAuthService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final HomesafeProperties properties;

    public SessionAuthService(UserRepository userRepository, SessionRepository sessionRepository, HomesafeProperties properties) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.properties = properties;
    }

    public AuthDtos.AuthPayload login(AuthDtos.LoginRequest request) {
        UserEntity user = userRepository.findByEmailIgnoreCase(request.email())
            .filter(candidate -> verifyPassword(request.password(), candidate.getPasswordSalt(), candidate.getPasswordHash()))
            .orElseThrow(() -> new UnauthorizedException("Неправильна електронна пошта або пароль"));

        if ("REJECTED".equals(user.getStatus())) {
            throw new ForbiddenException("Реєстрацію відхилено адміністратором");
        }

        String token = UUID.randomUUID() + UUID.randomUUID().toString().replace("-", "");
        SessionEntity session = new SessionEntity();
        session.setId(UUID.randomUUID().toString());
        session.setTokenHash(sha256(token));
        session.setUser(user);
        session.setExpiresAt(OffsetDateTime.now().plusDays(properties.sessionDays()));
        sessionRepository.save(session);

        return new AuthDtos.AuthPayload(token, UserDto.from(user));
    }

    public UserDto register(AuthDtos.RegisterRequest request) {
        if (userRepository.findByEmailIgnoreCase(request.email()).isPresent()) {
            throw new ConflictException("Користувач із такою електронною поштою вже існує");
        }

        PasswordData password = hashPassword(request.password());

        UserEntity user = new UserEntity();
        OffsetDateTime now = OffsetDateTime.now();
        user.setId(UUID.randomUUID().toString());
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPasswordSalt(password.saltHex());
        user.setPasswordHash(password.hashHex());
        user.setRole("USER");
        user.setStatus("PENDING");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        return UserDto.from(userRepository.save(user));
    }

    public void ensureSeedAdmin(String name, String email, String password) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            UserEntity created = new UserEntity();
            created.setId(UUID.randomUUID().toString());
            created.setEmail(email);
            OffsetDateTime now = OffsetDateTime.now();
            created.setCreatedAt(now);
            created.setUpdatedAt(now);
            return created;
        });

        PasswordData passwordData = hashPassword(password);
        OffsetDateTime now = OffsetDateTime.now();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordSalt(passwordData.saltHex());
        user.setPasswordHash(passwordData.hashHex());
        user.setRole("ADMIN");
        user.setStatus("APPROVED");
        user.setUpdatedAt(now);

        userRepository.save(user);
    }

    public Optional<UserEntity> findUserByToken(String token) {
        return sessionRepository.findByTokenHashAndExpiresAtAfter(sha256(token), OffsetDateTime.now()).map(SessionEntity::getUser);
    }

    public void logout(String token) {
        sessionRepository.findByTokenHashAndExpiresAtAfter(sha256(token), OffsetDateTime.now())
            .ifPresent(sessionRepository::delete);
    }

    private PasswordData hashPassword(String password) {
        String salt = HexFormat.of().formatHex(UUID.randomUUID().toString().replace("-", "").getBytes(StandardCharsets.UTF_8)).substring(0, 32);
        byte[] hash = SCrypt.generate(password.getBytes(StandardCharsets.UTF_8), HexFormat.of().parseHex(salt), 16384, 8, 1, 64);
        return new PasswordData(salt, HexFormat.of().formatHex(hash));
    }

    private boolean verifyPassword(String password, String saltHex, String hashHex) {
        byte[] calculated = SCrypt.generate(password.getBytes(StandardCharsets.UTF_8), HexFormat.of().parseHex(saltHex), 16384, 8, 1, 64);
        return MessageDigest.isEqual(calculated, HexFormat.of().parseHex(hashHex));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record PasswordData(String saltHex, String hashHex) {
    }
}

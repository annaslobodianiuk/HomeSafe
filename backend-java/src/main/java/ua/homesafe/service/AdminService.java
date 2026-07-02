package ua.homesafe.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import ua.homesafe.dto.AdminDtos;
import ua.homesafe.dto.UserDto;
import ua.homesafe.exception.NotFoundException;
import ua.homesafe.integration.OlxAccessTokenService;
import ua.homesafe.integration.OlxProviderClient;
import ua.homesafe.integration.ProviderConfigParser;
import ua.homesafe.model.DataSourceEntity;
import ua.homesafe.repository.DataSourceRepository;
import ua.homesafe.repository.ImportRunRepository;
import ua.homesafe.repository.UserRepository;

import java.util.Comparator;
import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final DataSourceRepository dataSourceRepository;
    private final ImportRunRepository importRunRepository;
    private final SourceSyncService sourceSyncService;
    private final ProviderConfigParser providerConfigParser;
    private final OlxAccessTokenService olxAccessTokenService;
    private final OlxProviderClient olxProviderClient;

    public AdminService(
        UserRepository userRepository,
        DataSourceRepository dataSourceRepository,
        ImportRunRepository importRunRepository,
        SourceSyncService sourceSyncService,
        ProviderConfigParser providerConfigParser,
        OlxAccessTokenService olxAccessTokenService,
        OlxProviderClient olxProviderClient
    ) {
        this.userRepository = userRepository;
        this.dataSourceRepository = dataSourceRepository;
        this.importRunRepository = importRunRepository;
        this.sourceSyncService = sourceSyncService;
        this.providerConfigParser = providerConfigParser;
        this.olxAccessTokenService = olxAccessTokenService;
        this.olxProviderClient = olxProviderClient;
    }

    @Transactional
    public List<UserDto> users() {
        return userRepository.findAll().stream()
            .filter(user -> "USER".equals(user.getRole()))
            .sorted(
                Comparator.comparing((ua.homesafe.model.UserEntity user) -> String.valueOf(user.getStatus()))
                    .thenComparing(user -> user.getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder()))
            )
            .map(UserDto::from)
            .toList();
    }

    @Transactional
    public UserDto updateUserStatus(String userId, String status) {
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("Користувача не знайдено"));
        user.setStatus(status);
        user.setUpdatedAt(java.time.OffsetDateTime.now());
        return UserDto.from(userRepository.save(user));
    }

    @Transactional
    public List<AdminDtos.SourceDto> sources() {
        return dataSourceRepository.findAll().stream()
            .sorted(Comparator.comparing(DataSourceEntity::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
            .map(source -> AdminDtos.SourceDto.from(
                source,
                providerConfigParser.parse(source),
                hasSourceCredentials(source)
            ))
            .toList();
    }

    @Transactional
    public AdminDtos.SourceDto updateSource(String code, Boolean enabled, Integer reliability, String config) {
        var source = findSourceByCode(code);

        if (enabled != null) {
            source.setEnabled(enabled);
        }
        if (reliability != null) {
            source.setReliability(reliability);
        }
        if (config != null) {
            source.setConfig(config);
        }

        return AdminDtos.SourceDto.from(
            dataSourceRepository.save(source),
            providerConfigParser.parse(source),
            hasSourceCredentials(source)
        );
    }

    public AdminDtos.ImportRunDto syncSource(String code, AdminDtos.SyncRequest request) {
        return sourceSyncService.syncSource(code, request);
    }

    public AdminDtos.OlxAuthUrlDto olxAuthUrl(String redirectUri, String state) {
        return new AdminDtos.OlxAuthUrlDto(
            olxAccessTokenService.buildAuthorizationUrl(findSourceByCode("OLX"), redirectUri, state)
        );
    }

    public AdminDtos.OlxTokenDto exchangeOlxCode(AdminDtos.OlxCodeExchangeRequest request) {
        return olxAccessTokenService.exchangeAuthorizationCode(
            findSourceByCode("OLX"),
            request.code(),
            request.redirectUri(),
            request.clientId(),
            request.clientSecret()
        );
    }

    public AdminDtos.OlxDebugDto olxDebug() {
        return olxProviderClient.debugSource(findSourceByCode("OLX"));
    }

    public String completeOlxCallback(String code, String callbackUri, String returnTo) {
        olxAccessTokenService.exchangeAuthorizationCode(
            findSourceByCode("OLX"),
            code,
            callbackUri,
            null,
            null
        );

        String target = (returnTo == null || returnTo.isBlank())
            ? "http://localhost:5173"
            : returnTo;

        return UriComponentsBuilder.fromUriString(target)
            .replaceQueryParam("olx")
            .replaceQueryParam("olx_error")
            .queryParam("olx", "connected")
            .build()
            .toUriString();
    }

    public String olxErrorRedirect(String returnTo, String error) {
        String target = (returnTo == null || returnTo.isBlank())
            ? "http://localhost:5173"
            : returnTo;

        return UriComponentsBuilder.fromUriString(target)
            .replaceQueryParam("olx")
            .replaceQueryParam("olx_error")
            .queryParam("olx_error", error == null || error.isBlank() ? "unknown_error" : error)
            .build()
            .toUriString();
    }

    @Transactional
    public List<AdminDtos.ImportRunDto> imports(String code) {
        return importRunRepository.findAll().stream()
            .filter(run -> code == null || code.isBlank() || (run.getDataSource() != null && code.equalsIgnoreCase(run.getDataSource().getCode())))
            .sorted(Comparator.comparing(ua.homesafe.model.ImportRunEntity::getStartedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(50)
            .map(AdminDtos.ImportRunDto::from)
            .toList();
    }

    private DataSourceEntity findSourceByCode(String code) {
        return dataSourceRepository.findAll().stream()
            .filter(source -> source.getCode() != null && source.getCode().equalsIgnoreCase(code))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Джерело не знайдено"));
    }

    private boolean hasSourceCredentials(DataSourceEntity source) {
        if ("OLX".equalsIgnoreCase(source.getCode())) {
            return true;
        }

        if (source.getApiKeyEnv() != null) {
            String accessToken = System.getenv(source.getApiKeyEnv());
            if (accessToken != null && !accessToken.isBlank()) {
                return true;
            }
        }

        return false;
    }

    private String envFromConfig(java.util.Map<String, Object> config, String key) {
        String envName = providerConfigParser.string(config, key, null);
        if (envName == null || envName.isBlank()) {
            return null;
        }
        String value = System.getenv(envName);
        return value == null || value.isBlank() ? null : value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}

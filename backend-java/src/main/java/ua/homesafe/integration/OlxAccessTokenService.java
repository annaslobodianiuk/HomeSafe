package ua.homesafe.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ua.homesafe.dto.AdminDtos;
import ua.homesafe.model.DataSourceEntity;
import ua.homesafe.repository.DataSourceRepository;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class OlxAccessTokenService {

    private static final Logger log = LoggerFactory.getLogger(OlxAccessTokenService.class);

    private final ProviderHttpClient httpClient;
    private final ProviderConfigParser configParser;
    private final ObjectMapper objectMapper;
    private final DataSourceRepository dataSourceRepository;

    public OlxAccessTokenService(
        ProviderHttpClient httpClient,
        ProviderConfigParser configParser,
        ObjectMapper objectMapper,
        DataSourceRepository dataSourceRepository
    ) {
        this.httpClient = httpClient;
        this.configParser = configParser;
        this.objectMapper = objectMapper;
        this.dataSourceRepository = dataSourceRepository;
    }

    public String resolveAccessToken(DataSourceEntity source) {
        Map<String, Object> config = configParser.parse(source);
        Map<String, Object> oauth = configParser.map(config, "oauth");

        String clientId = firstNonBlank(
            env("OLX_CLIENT_ID"),
            env(configParser.string(oauth, "clientIdEnv", null)),
            configParser.string(oauth, "clientId", null)
        );
        String clientSecret = firstNonBlank(
            env("OLX_CLIENT_SECRET"),
            env(configParser.string(oauth, "clientSecretEnv", null)),
            configParser.string(oauth, "clientSecret", null)
        );
        String refreshToken = firstNonBlank(
            env("OLX_REFRESH_TOKEN"),
            env(configParser.string(oauth, "refreshTokenEnv", null)),
            configParser.string(oauth, "refreshToken", null)
        );

        log.info(
            "OLX token resolution source={} hasClientId={} hasClientSecret={} hasRefreshToken={} hasAccessTokenEnv={}",
            source.getCode(),
            !isBlank(clientId),
            !isBlank(clientSecret),
            !isBlank(refreshToken),
            !isBlank(env(source.getApiKeyEnv()))
        );

        if (!isBlank(clientId) && !isBlank(clientSecret) && !isBlank(refreshToken)) {
            log.info("OLX token resolution: using refresh_token flow");
            return refreshAccessToken(source, config, clientId, clientSecret, refreshToken);
        }

        String accessToken = env(source.getApiKeyEnv());
        if (!isBlank(accessToken)) {
            log.info("OLX token resolution: using access token from env {}", source.getApiKeyEnv());
            return accessToken;
        }

        log.warn("OLX token resolution failed: no usable credentials found");
        throw new IllegalStateException(
            "Для OLX потрібен чинний access token у OLX_API_KEY або набір OLX_CLIENT_ID, OLX_CLIENT_SECRET, OLX_REFRESH_TOKEN"
        );
    }

    public String buildAuthorizationUrl(DataSourceEntity source, String redirectUri, String state) {
        Map<String, Object> config = configParser.parse(source);
        Map<String, Object> oauth = configParser.map(config, "oauth");
        String clientId = firstNonBlank(
            env("OLX_CLIENT_ID"),
            env(configParser.string(oauth, "clientIdEnv", null)),
            configParser.string(oauth, "clientId", null)
        );

        if (isBlank(clientId)) {
            throw new IllegalStateException("Для OLX авторизації потрібен OLX_CLIENT_ID");
        }
        if (isBlank(redirectUri)) {
            throw new IllegalArgumentException("Потрібно передати redirectUri для OLX авторизації");
        }

        log.info("Building OLX authorization URL for redirectUri={}", redirectUri);
        String base = trimSlash(source.getBaseUrl()) + "/oauth/authorize";
        String url = base
            + "?client_id=" + encode(clientId)
            + "&response_type=code"
            + "&scope=" + encode("read write v2")
            + "&redirect_uri=" + encode(redirectUri);
        if (!isBlank(state)) {
            url += "&state=" + encode(state);
        }
        return url;
    }

    public AdminDtos.OlxTokenDto exchangeAuthorizationCode(
        DataSourceEntity source,
        String code,
        String redirectUri,
        String clientIdInput,
        String clientSecretInput
    ) {
        log.info("Exchanging OLX authorization code redirectUri={} codePresent={} clientIdProvided={} clientSecretProvided={}",
            redirectUri,
            !isBlank(code),
            !isBlank(clientIdInput),
            !isBlank(clientSecretInput)
        );
        if (isBlank(code)) {
            throw new IllegalArgumentException("Потрібно передати authorization code від OLX");
        }
        if (isBlank(redirectUri)) {
            throw new IllegalArgumentException("Потрібно передати redirectUri, який використовувався в OLX");
        }

        Map<String, Object> config = configParser.parse(source);
        Map<String, Object> oauth = configParser.map(config, "oauth");
        String clientId = firstNonBlank(
            clientIdInput,
            env("OLX_CLIENT_ID"),
            env(configParser.string(oauth, "clientIdEnv", null)),
            configParser.string(oauth, "clientId", null)
        );
        String clientSecret = firstNonBlank(
            clientSecretInput,
            env("OLX_CLIENT_SECRET"),
            env(configParser.string(oauth, "clientSecretEnv", null)),
            configParser.string(oauth, "clientSecret", null)
        );

        if (isBlank(clientId) || isBlank(clientSecret)) {
            throw new IllegalStateException("Для обміну OLX code потрібні client_id і client_secret");
        }

        String tokenPath = configParser.string(config, "tokenPath", "/api/open/oauth/token");
        URI uri = URI.create(trimSlash(source.getBaseUrl()) + tokenPath);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("grant_type", "authorization_code");
        payload.put("client_id", clientId);
        payload.put("client_secret", clientSecret);
        payload.put("code", code);
        payload.put("scope", "v2 read write");
        payload.put("redirect_uri", redirectUri);

        JsonNode response = httpClient.postJson(uri, Map.of(), writeJson(payload));
        String accessToken = textOrNull(response.path("access_token"));
        String refreshToken = textOrNull(response.path("refresh_token"));
        Integer expiresIn = response.path("expires_in").isNumber() ? response.path("expires_in").intValue() : null;
        String scope = textOrNull(response.path("scope"));
        String tokenType = textOrNull(response.path("token_type"));

        log.info("OLX code exchange response hasAccessToken={} hasRefreshToken={} expiresIn={} scope={}",
            !isBlank(accessToken),
            !isBlank(refreshToken),
            expiresIn,
            scope
        );
        if (isBlank(refreshToken)) {
            throw new IllegalStateException("OLX не повернув refresh_token після обміну code");
        }

        saveOauthConfig(source, config, clientId, clientSecret, refreshToken, redirectUri);
        return new AdminDtos.OlxTokenDto(accessToken, refreshToken, expiresIn, scope, tokenType);
    }

    private String refreshAccessToken(
        DataSourceEntity source,
        Map<String, Object> config,
        String clientId,
        String clientSecret,
        String refreshToken
    ) {
        String tokenPath = configParser.string(config, "tokenPath", "/api/open/oauth/token");
        log.info("Refreshing OLX access token for source={} tokenPath={}", source.getCode(), tokenPath);
        URI uri = URI.create(trimSlash(source.getBaseUrl()) + tokenPath);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("grant_type", "refresh_token");
        payload.put("client_id", clientId);
        payload.put("client_secret", clientSecret);
        payload.put("refresh_token", refreshToken);

        JsonNode response = httpClient.postJson(uri, Map.of(), writeJson(payload));
        String accessToken = textOrNull(response.path("access_token"));
        String nextRefreshToken = textOrNull(response.path("refresh_token"));
        log.info("OLX refresh token response hasAccessToken={}", !isBlank(accessToken));
        if (isBlank(accessToken)) {
            throw new IllegalStateException("OLX не повернув access_token після оновлення токена");
        }
        if (!isBlank(nextRefreshToken) && !nextRefreshToken.equals(refreshToken)) {
            saveOauthConfig(source, config, clientId, clientSecret, nextRefreshToken, configParser.string(configParser.map(config, "oauth"), "redirectUri", null));
        }
        return accessToken;
    }

    private String writeJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Не вдалося підготувати запит для OLX OAuth", exception);
        }
    }

    private void saveOauthConfig(
        DataSourceEntity source,
        Map<String, Object> config,
        String clientId,
        String clientSecret,
        String refreshToken,
        String redirectUri
    ) {
        Map<String, Object> merged = new LinkedHashMap<>(config);
        Map<String, Object> oauth = new LinkedHashMap<>(configParser.map(config, "oauth"));
        oauth.put("clientId", clientId);
        oauth.put("clientSecret", clientSecret);
        oauth.put("refreshToken", refreshToken);
        if (!isBlank(redirectUri)) {
            oauth.put("redirectUri", redirectUri);
        }
        merged.put("oauth", oauth);

        try {
            source.setConfig(objectMapper.writeValueAsString(merged));
            dataSourceRepository.save(source);
            log.info("Saved OLX OAuth config for source={} hasRefreshToken=true", source.getCode());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Не вдалося зберегти OLX refresh token у конфіг", exception);
        }
    }

    private String env(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return System.getenv(key);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private String textOrNull(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    private String trimSlash(String value) {
        return value == null ? "" : value.replaceAll("/+$", "");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

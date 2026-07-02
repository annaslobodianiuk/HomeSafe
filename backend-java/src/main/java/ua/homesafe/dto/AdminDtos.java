package ua.homesafe.dto;

import ua.homesafe.model.DataSourceEntity;
import ua.homesafe.model.ImportRunEntity;

import java.time.OffsetDateTime;
import java.util.Map;

public final class AdminDtos {
    private AdminDtos() {
    }

    public record StatusUpdateRequest(String status) {
    }

    public record SourceUpdateRequest(Boolean enabled, Integer reliability, String config) {
    }

    public record SyncRequest(
        String city,
        String district,
        String state,
        Integer offset,
        String category_ids,
        Integer cityId,
        Integer stateId,
        Integer limit,
        Integer page
    ) {
    }

    public record OlxAuthUrlDto(String url) {
    }

    public record OlxCodeExchangeRequest(
        String code,
        String redirectUri,
        String clientId,
        String clientSecret
    ) {
    }

    public record OlxTokenDto(
        String accessToken,
        String refreshToken,
        Integer expiresIn,
        String scope,
        String tokenType
    ) {
    }

    public record OlxDebugDto(
        String accountName,
        String accountEmail,
        Integer fetchedCount,
        java.util.List<String> advertIds,
        java.util.List<String> advertTitles,
        String message
    ) {
    }

    public record SourceDto(
        String id,
        String code,
        String name,
        String baseUrl,
        String apiKeyEnv,
        boolean enabled,
        Integer reliability,
        Map<String, Object> config,
        OffsetDateTime lastSyncAt,
        String lastStatus,
        String lastError,
        boolean hasApiKey
    ) {
        public static SourceDto from(DataSourceEntity source, Map<String, Object> config, boolean hasApiKey) {
            return new SourceDto(
                source.getId(),
                source.getCode(),
                source.getName(),
                source.getBaseUrl(),
                source.getApiKeyEnv(),
                Boolean.TRUE.equals(source.getEnabled()),
                source.getReliability(),
                config,
                source.getLastSyncAt(),
                source.getLastStatus(),
                source.getLastError(),
                hasApiKey
            );
        }
    }

    public record ImportRunDto(
        String id,
        String status,
        String requestedCity,
        Integer receivedCount,
        Integer normalizedCount,
        Integer publishedCount,
        Integer reviewCount,
        Integer rejectedCount,
        Integer duplicateCount,
        String errorMessage,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        SourceRef dataSource
    ) {
        public static ImportRunDto from(ImportRunEntity run) {
            return new ImportRunDto(
                run.getId(),
                run.getStatus(),
                run.getRequestedCity(),
                run.getReceivedCount(),
                run.getNormalizedCount(),
                run.getPublishedCount(),
                run.getReviewCount(),
                run.getRejectedCount(),
                run.getDuplicateCount(),
                run.getErrorMessage(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getDataSource() == null ? null : new SourceRef(run.getDataSource().getCode(), run.getDataSource().getName())
            );
        }
    }

    public record SourceRef(String code, String name) {
    }
}

package ua.homesafe.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import ua.homesafe.model.DataSourceEntity;
import ua.homesafe.repository.ExternalListingRepository;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class DimRiaProviderClient implements ProviderClient {
    private static final String RIA_STATIC_BASE = "https://cdn.riastatic.com/photos/";

    private final ProviderHttpClient httpClient;
    private final ProviderConfigParser configParser;
    private final ExternalListingRepository externalListingRepository;
    private final ObjectMapper objectMapper;

    public DimRiaProviderClient(
        ProviderHttpClient httpClient,
        ProviderConfigParser configParser,
        ExternalListingRepository externalListingRepository,
        ObjectMapper objectMapper
    ) {
        this.httpClient = httpClient;
        this.configParser = configParser;
        this.externalListingRepository = externalListingRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String providerCode) {
        return "DIM_RIA".equalsIgnoreCase(providerCode);
    }

    @Override
    public List<NormalizedListing> fetchListings(DataSourceEntity source, ImportQuery query) {
        String apiKey = System.getenv(source.getApiKeyEnv());
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing API key in env " + source.getApiKeyEnv());
        }
        if (query.cityId() == null) {
            throw new IllegalArgumentException("DIM.RIA requires cityId");
        }

        Map<String, Object> config = configParser.parse(source);
        int detailLimit = Math.max(1, Math.min(
            query.safeLimit(),
            configParser.integer(config, "detailsLimit", 5)
        ));
        URI searchUri = UriComponentsBuilder.fromHttpUrl(trimSlash(source.getBaseUrl()) + configParser.string(config, "searchPath", "/dom/search"))
            .queryParam("api_key", apiKey)
            .queryParam("category", configParser.integer(config, "category", 1))
            .queryParam("realty_type", configParser.integer(config, "realtyType", 2))
            .queryParam("operation_type", configParser.integer(config, "operationType", 3))
            .queryParam("city_id", query.cityId())
            .queryParamIfPresent("state_id", java.util.Optional.ofNullable(query.stateId()))
            .queryParam("limit", detailLimit)
            .queryParam("page", query.safePage())
            .build(true)
            .toUri();

        JsonNode searchPayload = httpClient.getJson(searchUri, Map.of());
        JsonNode items = searchPayload.path("items").isArray() ? searchPayload.path("items") : searchPayload.path("data");
        List<String> ids = new ArrayList<>();
        if (items.isArray()) {
            for (JsonNode item : items) {
                String id = item.isObject() ? item.path("id").asText() : item.asText();
                if (!id.isBlank()) {
                    ids.add(id);
                }
                if (ids.size() >= detailLimit) {
                    break;
                }
            }
        }

        List<NormalizedListing> listings = new ArrayList<>();
        for (String id : ids) {
            JsonNode cached = cachedPayload(source.getCode(), id);
            if (cached != null && cached.isObject()) {
                listings.add(normalize(cached));
                continue;
            }

            URI detailsUri = UriComponentsBuilder.fromHttpUrl(trimSlash(source.getBaseUrl()) + configParser.string(config, "infoPath", "/dom/info") + "/" + id)
                .queryParam("api_key", apiKey)
                .build(true)
                .toUri();
            JsonNode raw = httpClient.getJson(detailsUri, Map.of());
            listings.add(normalize(raw));
        }
        return listings;
    }

    private JsonNode cachedPayload(String providerCode, String externalId) {
        return externalListingRepository.findAll().stream()
            .filter(item -> providerCode != null && providerCode.equalsIgnoreCase(item.getProviderCode()))
            .filter(item -> externalId != null && externalId.equalsIgnoreCase(item.getExternalId()))
            .map(item -> item.getRawPayload())
            .filter(payload -> payload != null && !payload.isBlank())
            .findFirst()
            .map(this::readJson)
            .orElse(null);
    }

    private JsonNode readJson(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception exception) {
            return null;
        }
    }

    private NormalizedListing normalize(JsonNode raw) {
        String id = firstText(raw, "id", "realty_id", "advert_id");
        List<String> photos = new ArrayList<>();
        JsonNode photosNode = raw.path("photos");
        if (photosNode.isArray()) {
            for (JsonNode photo : photosNode) {
                String image = imageUrl(photo);
                if (image != null) {
                    photos.add(image);
                }
            }
        } else if (photosNode.isObject()) {
            photosNode.fields().forEachRemaining(entry -> {
                String image = imageUrl(entry.getValue());
                if (image != null && !image.isBlank()) {
                    photos.add(image);
                }
            });
        }
        String mainImage = firstNonBlank(imageUrl(raw.path("main_photo")), imageUrl(raw.path("mainPhoto")), photos.isEmpty() ? null : photos.get(0));

        return new NormalizedListing(
            id,
            firstNonBlank(firstText(raw, "beautiful_name", "title", "name"), "Квартира " + id),
            firstText(raw, "city_name", "city"),
            firstNonBlank(firstText(raw, "district_name", "district"), "Не вказано"),
            firstNonBlank(firstText(raw, "street_name", "street", "address"), "Адресу не вказано"),
            firstInt(raw, "price", "price_total"),
            firstNonBlank(firstText(raw, "currency_type", "currency"), "UAH").toUpperCase(),
            firstInt(raw, "rooms_count", "rooms"),
            firstDouble(raw, "total_square_meters", "total_square", "area"),
            firstInt(raw, "floor"),
            firstInt(raw, "floors_count", "total_floors"),
            firstText(raw, "description", "description_uk"),
            mainImage,
            photos.isEmpty() && mainImage != null ? 1 : photos.size(),
            firstDouble(raw, "latitude", "lat"),
            firstDouble(raw, "longitude", "lng", "lon"),
            firstText(raw, "beautiful_url", "url"),
            raw.path("user_is_owner").asBoolean(false) || raw.path("is_owner").asBoolean(false) || raw.path("owner").asBoolean(false),
            raw.path("user_is_owner").asBoolean(false) || raw.path("is_owner").asBoolean(false) ? "Власник" : "Не підтверджено",
            parseDate(firstText(raw, "created_at")),
            raw
        );
    }

    private static String trimSlash(String value) {
        return value == null ? "" : value.replaceAll("/+$", "");
    }

    private static String imageUrl(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (value.isTextual()) {
            return absoluteRiaUrl(value.asText());
        }
        return absoluteRiaUrl(firstNonBlank(
            textOrNull(value.path("url")),
            textOrNull(value.path("large")),
            textOrNull(value.path("file"))
        ));
    }

    private static String absoluteRiaUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        if (value.startsWith("/")) {
            return RIA_STATIC_BASE + toRiaPhotoVariant(value.substring(1));
        }
        if (value.startsWith("dom/")) {
            return RIA_STATIC_BASE + toRiaPhotoVariant(value);
        }
        return value;
    }

    private static String toRiaPhotoVariant(String path) {
        return path.replaceFirst("(\\.[A-Za-z0-9]+)$", "f$1");
    }

    private static String firstText(JsonNode node, String... keys) {
        for (String key : keys) {
            String value = textOrNull(node.path(key));
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static Integer firstInt(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.path(key);
            if (value.isNumber()) {
                return value.intValue();
            }
            if (value.isTextual()) {
                try {
                    return (int) Math.round(Double.parseDouble(value.asText()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 0;
    }

    private static Double firstDouble(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.path(key);
            if (value.isNumber()) {
                return value.doubleValue();
            }
            if (value.isTextual()) {
                try {
                    return Double.parseDouble(value.asText());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 0d;
    }

    private static OffsetDateTime parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private static String textOrNull(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}

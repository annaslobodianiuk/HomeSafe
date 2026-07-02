package ua.homesafe.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ua.homesafe.model.DataSourceEntity;
import ua.homesafe.repository.DataSourceRepository;
import ua.homesafe.util.Ids;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class SeedSourceConfig {

    @Bean
    ApplicationRunner seedSources(DataSourceRepository dataSourceRepository, ObjectMapper objectMapper) {
        return args -> {
            upsertSource(
                dataSourceRepository,
                objectMapper,
                "DIM_RIA",
                "DIM.RIA",
                "https://developers.ria.com",
                "DIM_RIA_API_KEY",
                78,
                defaultDimRiaConfig()
            );

            upsertSource(
                dataSourceRepository,
                objectMapper,
                "OLX",
                "OLX",
                "https://www.olx.ua",
                "OLX_API_KEY",
                75,
                defaultOlxConfig()
            );
        };
    }

    private void upsertSource(
        DataSourceRepository repository,
        ObjectMapper objectMapper,
        String code,
        String name,
        String baseUrl,
        String apiKeyEnv,
        int reliability,
        Map<String, Object> defaultConfig
    ) {
        DataSourceEntity source = repository.findAll().stream()
            .filter(item -> item.getCode() != null && item.getCode().equalsIgnoreCase(code))
            .findFirst()
            .orElseGet(() -> {
                DataSourceEntity created = new DataSourceEntity();
                created.setId(Ids.newId());
                created.setCode(code);
                return created;
            });

        source.setName(name);
        source.setBaseUrl(baseUrl);
        source.setApiKeyEnv(apiKeyEnv);
        if (source.getEnabled() == null) {
            source.setEnabled(true);
        }
        if (source.getReliability() == null) {
            source.setReliability(reliability);
        }
        source.setConfig(mergeConfig(objectMapper, source.getConfig(), defaultConfig));
        repository.save(source);
    }

    private String mergeConfig(ObjectMapper objectMapper, String existingConfig, Map<String, Object> defaultConfig) {
        Map<String, Object> merged = new LinkedHashMap<>(defaultConfig);
        merged.putAll(parseConfig(objectMapper, existingConfig));

        Object existingSyncDefaults = merged.get("syncDefaults");
        Map<String, Object> syncDefaults = new LinkedHashMap<>();
        Object defaultSyncDefaults = defaultConfig.get("syncDefaults");
        if (defaultSyncDefaults instanceof Map<?, ?> defaultsMap) {
            defaultsMap.forEach((key, value) -> syncDefaults.put(String.valueOf(key), value));
        }
        if (existingSyncDefaults instanceof Map<?, ?> existingMap) {
            existingMap.forEach((key, value) -> syncDefaults.put(String.valueOf(key), value));
        }
        if (!syncDefaults.isEmpty()) {
            merged.put("syncDefaults", syncDefaults);
        }

        try {
            return objectMapper.writeValueAsString(merged);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Не вдалося зберегти конфігурацію джерела " + merged, exception);
        }
    }

    private Map<String, Object> parseConfig(ObjectMapper objectMapper, String config) {
        if (config == null || config.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(config, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            return Map.of();
        }
    }

    private Map<String, Object> defaultDimRiaConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("searchPath", "/dom/search");
        config.put("infoPath", "/dom/info");
        config.put("category", 1);
        config.put("realtyType", 2);
        config.put("operationType", 3);
        config.put("detailsLimit", 3);

        Map<String, Object> syncDefaults = new LinkedHashMap<>();
        syncDefaults.put("cityId", 10);
        syncDefaults.put("stateId", 10);
        syncDefaults.put("limit", 3);
        syncDefaults.put("page", 0);
        config.put("syncDefaults", syncDefaults);
        return config;
    }

    private Map<String, Object> defaultOlxConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("searchPathTemplate", "/uk/nedvizhimost/kvartiry/dolgosrochnaya-arenda-kvartir/{citySlug}/");
        config.put("defaultCity", "Київ");
        config.put("defaultCitySlug", "kiev");
        config.put("note", "OLX імпортується як публічний парсер сторінок оренди квартир. Оголошення після імпорту так само потрапляють у модерацію.");

        Map<String, Object> citySlugMap = new LinkedHashMap<>();
        citySlugMap.put("Київ", "kiev");
        citySlugMap.put("Львів", "lvov");
        citySlugMap.put("Одеса", "odessa");
        citySlugMap.put("Харків", "kharkov");
        citySlugMap.put("Дніпро", "dnepr");
        citySlugMap.put("Запоріжжя", "zaporozhe");
        config.put("citySlugMap", citySlugMap);

        Map<String, Object> syncDefaults = new LinkedHashMap<>();
        syncDefaults.put("city", "Київ");
        syncDefaults.put("limit", 6);
        config.put("syncDefaults", syncDefaults);
        return config;
    }
}

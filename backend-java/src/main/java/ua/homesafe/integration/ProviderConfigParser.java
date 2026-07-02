package ua.homesafe.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import ua.homesafe.model.DataSourceEntity;

import java.util.Collections;
import java.util.Map;

@Component
public class ProviderConfigParser {

    private final ObjectMapper objectMapper;

    public ProviderConfigParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> parse(DataSourceEntity source) {
        if (source.getConfig() == null || source.getConfig().isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(source.getConfig(), new TypeReference<>() { });
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid JSON config for source " + source.getCode(), exception);
        }
    }

    public String string(Map<String, Object> config, String key, String defaultValue) {
        Object value = config.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    public Integer integer(Map<String, Object> config, String key, Integer defaultValue) {
        Object value = config.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> map(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value instanceof Map<?, ?> nested) {
            return (Map<String, Object>) nested;
        }
        return Collections.emptyMap();
    }
}

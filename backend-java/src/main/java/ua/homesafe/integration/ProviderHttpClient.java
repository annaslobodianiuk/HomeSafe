package ua.homesafe.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Component
public class ProviderHttpClient {

    private static final Logger log = LoggerFactory.getLogger(ProviderHttpClient.class);

    private final ObjectMapper objectMapper;
    private final HttpClient client;

    public ProviderHttpClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    }

    public JsonNode getJson(URI uri, Map<String, String> headers) {
        log.info("Provider GET {} host={} headerNames={}", uri.getPath(), uri.getHost(), headers.keySet());
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
            .GET()
            .timeout(Duration.ofSeconds(15))
            .header("Accept", "application/json")
            .header("User-Agent", "HomeSafe-Masters-Thesis/1.0");
        headers.forEach(builder::header);

        try {
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            log.info("Provider GET response {} status={}", uri.getPath(), response.statusCode());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Provider GET failed {} status={} body={}", uri.getPath(), response.statusCode(), shorten(response.body()));
                throw new IllegalStateException("Provider returned " + response.statusCode() + ": " + response.body());
            }
            log.info("Provider GET success {} bodyPreview={}", uri.getPath(), shorten(response.body()));
            if (response.body() == null || response.body().isBlank()) {
                return objectMapper.nullNode();
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.error("Provider GET interrupted {}", uri.getPath(), exception);
            throw new IllegalStateException("Failed to call provider API", exception);
        } catch (IOException exception) {
            log.error("Provider GET I/O failure {}", uri.getPath(), exception);
            throw new IllegalStateException("Failed to call provider API", exception);
        }
    }

    public String getText(URI uri, Map<String, String> headers) {
        log.info("Provider GET(text) {} host={} headerNames={}", uri.getPath(), uri.getHost(), headers.keySet());
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
            .GET()
            .timeout(Duration.ofSeconds(15))
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("User-Agent", "Mozilla/5.0 (compatible; HomeSafeBot/1.0; +https://homesafe.local)");
        headers.forEach(builder::header);

        try {
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            log.info("Provider GET(text) response {} status={}", uri.getPath(), response.statusCode());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Provider GET(text) failed {} status={} body={}", uri.getPath(), response.statusCode(), shorten(response.body()));
                throw new IllegalStateException("Provider returned " + response.statusCode() + ": " + response.body());
            }
            return response.body() == null ? "" : response.body();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.error("Provider GET(text) interrupted {}", uri.getPath(), exception);
            throw new IllegalStateException("Failed to load provider page", exception);
        } catch (IOException exception) {
            log.error("Provider GET(text) I/O failure {}", uri.getPath(), exception);
            throw new IllegalStateException("Failed to load provider page", exception);
        }
    }

    public JsonNode postJson(URI uri, Map<String, String> headers, String body) {
        log.info("Provider POST {} host={} headerNames={} bodyPreview={}", uri.getPath(), uri.getHost(), headers.keySet(), shorten(body));
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
            .POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body))
            .timeout(Duration.ofSeconds(15))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("User-Agent", "HomeSafe-Masters-Thesis/1.0");
        headers.forEach(builder::header);

        try {
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            log.info("Provider POST response {} status={}", uri.getPath(), response.statusCode());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Provider POST failed {} status={} body={}", uri.getPath(), response.statusCode(), shorten(response.body()));
                throw new IllegalStateException("Provider returned " + response.statusCode() + ": " + response.body());
            }
            log.info("Provider POST success {} bodyPreview={}", uri.getPath(), shorten(response.body()));
            if (response.body() == null || response.body().isBlank()) {
                return objectMapper.nullNode();
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.error("Provider POST interrupted {}", uri.getPath(), exception);
            throw new IllegalStateException("Failed to call provider API", exception);
        } catch (IOException exception) {
            log.error("Provider POST I/O failure {}", uri.getPath(), exception);
            throw new IllegalStateException("Failed to call provider API", exception);
        }
    }

    private String shorten(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 300 ? normalized : normalized.substring(0, 300) + "...";
    }
}

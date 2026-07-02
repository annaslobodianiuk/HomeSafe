package ua.homesafe.integration;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.OffsetDateTime;

public record NormalizedListing(
    String externalId,
    String title,
    String city,
    String district,
    String address,
    Integer price,
    String currency,
    Integer rooms,
    Double area,
    Integer floor,
    Integer totalFloors,
    String description,
    String imageUrl,
    int imageCount,
    Double latitude,
    Double longitude,
    String sourceUrl,
    boolean isWithoutBroker,
    String ownerType,
    OffsetDateTime publishedAt,
    JsonNode raw
) {
}

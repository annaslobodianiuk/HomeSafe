package ua.homesafe.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ua.homesafe.model.ApartmentEntity;
import ua.homesafe.model.ExternalListingEntity;
import ua.homesafe.model.InfrastructureEntity;
import ua.homesafe.model.PriceHistoryEntity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record ApartmentDto(
    String id,
    String title,
    String city,
    String district,
    String address,
    Integer price,
    Integer marketAveragePrice,
    Integer rooms,
    Double area,
    Integer floor,
    Integer totalFloors,
    String description,
    String imageUrl,
    List<String> images,
    String currency,
    String ownerType,
    String documents,
    String source,
    boolean isWithoutBroker,
    boolean isVerified,
    Integer trustScore,
    Integer valueScore,
    Integer comfortScore,
    String fraudRisk,
    Integer savingsPercent,
    List<String> recommendationReasons,
    InfrastructureDto infrastructure,
    List<PriceHistoryDto> priceHistory
) {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String FALLBACK_IMAGE = "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85";

    public static ApartmentDto from(ApartmentEntity apartment) {
        List<String> images = extractImages(apartment);
        String primaryImage = resolvePrimaryImage(apartment, images);
        return new ApartmentDto(
            apartment.getId(),
            apartment.getTitle(),
            apartment.getCity(),
            apartment.getDistrict(),
            apartment.getAddress(),
            apartment.getPrice(),
            apartment.getMarketAveragePrice(),
            apartment.getRooms(),
            apartment.getArea(),
            apartment.getFloor(),
            apartment.getTotalFloors(),
            apartment.getDescription(),
            primaryImage,
            images,
            apartment.getCurrency(),
            apartment.getOwnerType(),
            apartment.getDocuments(),
            firstNonBlank(apartment.getProviderCode(), apartment.getSource()),
            Boolean.TRUE.equals(apartment.getWithoutBroker()),
            Boolean.TRUE.equals(apartment.getVerified()),
            apartment.getTrustScore(),
            apartment.getValueScore(),
            apartment.getComfortScore(),
            apartment.getFraudRisk(),
            calculateSavings(apartment.getPrice(), apartment.getMarketAveragePrice()),
            buildRecommendation(apartment),
            InfrastructureDto.from(apartment.getInfrastructure()),
            apartment.getPriceHistory().stream().map(PriceHistoryDto::from).toList()
        );
    }

    private static String resolvePrimaryImage(ApartmentEntity apartment, List<String> images) {
        if (apartment.getImageUrl() != null && !apartment.getImageUrl().isBlank() && !apartment.getImageUrl().contains("images.unsplash.com")) {
            return apartment.getImageUrl();
        }
        if (!images.isEmpty()) {
            return images.get(0);
        }
        return apartment.getImageUrl() == null || apartment.getImageUrl().isBlank() ? FALLBACK_IMAGE : apartment.getImageUrl();
    }

    private static List<String> extractImages(ApartmentEntity apartment) {
        Set<String> images = new LinkedHashSet<>();
        addIfPresent(images, apartment.getImageUrl());

        for (ExternalListingEntity listing : apartment.getExternalListings()) {
            String rawPayload = listing.getRawPayload();
            if (rawPayload == null || rawPayload.isBlank()) {
                continue;
            }
            try {
                JsonNode raw = OBJECT_MAPPER.readTree(rawPayload);
                collectDimRiaImages(images, raw.path("main_photo"), raw.path("photos"));
                collectOlxImages(images, raw.path("imageUrl"), raw.path("photos"));
                collectOlxImages(images, null, raw.path("images"));
                collectOlxImages(images, null, raw.path("gallery"));
            } catch (Exception ignored) {
                // Keep response resilient even when one provider payload is malformed.
            }
        }

        images.removeIf(image -> image == null || image.isBlank() || image.contains("images.unsplash.com"));
        return new ArrayList<>(images);
    }

    private static void collectDimRiaImages(Set<String> images, JsonNode mainPhotoNode, JsonNode photosNode) {
        addIfPresent(images, absoluteRiaUrl(textOrNull(mainPhotoNode)));
        if (photosNode == null || photosNode.isMissingNode() || photosNode.isNull()) {
            return;
        }
        if (photosNode.isArray()) {
            for (JsonNode photo : photosNode) {
                addIfPresent(images, absoluteRiaUrl(firstNonBlank(
                    textOrNull(photo.path("url")),
                    textOrNull(photo.path("large")),
                    textOrNull(photo.path("file")),
                    textOrNull(photo)
                )));
            }
            return;
        }
        if (photosNode.isObject()) {
            photosNode.fields().forEachRemaining(entry -> addIfPresent(images, absoluteRiaUrl(firstNonBlank(
                textOrNull(entry.getValue().path("url")),
                textOrNull(entry.getValue().path("large")),
                textOrNull(entry.getValue().path("file"))
            ))));
        }
    }

    private static void collectOlxImages(Set<String> images, JsonNode directImage, JsonNode node) {
        addIfPresent(images, textOrNull(directImage));
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                addIfPresent(images, firstNonBlank(
                    textOrNull(item.path("url")),
                    textOrNull(item.path("link")),
                    textOrNull(item.path("large")),
                    textOrNull(item.path("file")),
                    textOrNull(item)
                ));
            }
            return;
        }
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> addIfPresent(images, firstNonBlank(
                textOrNull(entry.getValue().path("url")),
                textOrNull(entry.getValue().path("link")),
                textOrNull(entry.getValue().path("large")),
                textOrNull(entry.getValue().path("file"))
            )));
        }
    }

    private static String absoluteRiaUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        if (value.startsWith("/")) {
            return "https://cdn.riastatic.com/photos/" + toRiaPhotoVariant(value.substring(1));
        }
        if (value.startsWith("dom/")) {
            return "https://cdn.riastatic.com/photos/" + toRiaPhotoVariant(value);
        }
        return value;
    }

    private static String toRiaPhotoVariant(String path) {
        return path.replaceFirst("(\\.[A-Za-z0-9]+)$", "f$1");
    }

    private static void addIfPresent(Set<String> images, String value) {
        if (value != null && !value.isBlank()) {
            images.add(value);
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
        return null;
    }

    private static int calculateSavings(Integer price, Integer marketAveragePrice) {
        if (price == null || marketAveragePrice == null || marketAveragePrice == 0) {
            return 0;
        }
        return Math.round(((marketAveragePrice - price) * 100f) / marketAveragePrice);
    }

    private static List<String> buildRecommendation(ApartmentEntity apartment) {
        List<String> reasons = new java.util.ArrayList<>();
        int savings = calculateSavings(apartment.getPrice(), apartment.getMarketAveragePrice());
        if (savings > 0) reasons.add(savings + "% дешевше за медіанну ціну схожих квартир");
        if (apartment.getInfrastructure() != null && safe(apartment.getInfrastructure().getSchoolDistanceMeters()) <= 700) reasons.add("Школа у пішій доступності");
        if (apartment.getInfrastructure() != null && safe(apartment.getInfrastructure().getHospitalDistanceMeters()) <= 1000) reasons.add("Лікарня поруч");
        if (apartment.getInfrastructure() != null && safe(apartment.getInfrastructure().getTransportDistanceMeters()) <= 300) reasons.add("Громадський транспорт дуже близько");
        if ("LOW".equals(apartment.getFraudRisk())) reasons.add("Низький ризик шахрайства");
        if (Boolean.TRUE.equals(apartment.getWithoutBroker())) reasons.add("Оголошення без посередника");
        return reasons;
    }

    private static int safe(Integer value) {
        return value == null ? Integer.MAX_VALUE : value;
    }

    public record InfrastructureDto(
        Integer schoolDistanceMeters,
        Integer hospitalDistanceMeters,
        Integer transportDistanceMeters,
        Integer supermarketDistanceMeters,
        Integer kindergartenDistanceMeters
    ) {
        static InfrastructureDto from(InfrastructureEntity infrastructure) {
            if (infrastructure == null) {
                return null;
            }
            return new InfrastructureDto(
                infrastructure.getSchoolDistanceMeters(),
                infrastructure.getHospitalDistanceMeters(),
                infrastructure.getTransportDistanceMeters(),
                infrastructure.getSupermarketDistanceMeters(),
                infrastructure.getKindergartenDistanceMeters()
            );
        }
    }

    public record PriceHistoryDto(String recordedAt, Integer price) {
        static PriceHistoryDto from(PriceHistoryEntity point) {
            return new PriceHistoryDto(point.getRecordedAt().toString(), point.getPrice());
        }
    }
}

package ua.homesafe.service;

import org.springframework.stereotype.Service;
import ua.homesafe.integration.NormalizedListing;
import ua.homesafe.model.ApartmentEntity;
import ua.homesafe.model.InfrastructureEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ListingAnalysisService {

    public AnalysisResult analyze(NormalizedListing listing, int sourceReliability, Integer marketMedian, boolean duplicateDetected, InfrastructureEntity infrastructure) {
        List<String> rejectionReasons = new ArrayList<>();
        List<String> riskFactors = new ArrayList<>();

        if (listing.externalId() == null || listing.externalId().isBlank()) rejectionReasons.add("Відсутній зовнішній ID");
        if (listing.city() == null || listing.city().isBlank()) rejectionReasons.add("Не вказано місто");
        if (listing.price() == null || listing.price() <= 0) rejectionReasons.add("Некоректна ціна");
        if (listing.area() == null || listing.area() < 10) rejectionReasons.add("Некоректна площа");
        if (listing.imageUrl() == null || listing.imageUrl().isBlank()) rejectionReasons.add("Відсутнє основне фото");

        Double priceRatio = marketMedian == null || marketMedian <= 0 || listing.price() == null ? null : listing.price() / (double) marketMedian;
        int trustScore = sourceReliability;

        if (listing.sourceUrl() != null && !listing.sourceUrl().isBlank()) trustScore += 4;
        if (listing.imageCount() >= 3) trustScore += 6;
        else if (listing.imageCount() == 0) trustScore -= 12;
        if (listing.description() != null && listing.description().length() >= 120) trustScore += 5;
        else if (listing.description() == null || listing.description().isBlank()) trustScore -= 8;
        if (hasCoordinates(listing)) trustScore += 5;
        else riskFactors.add("Немає точних координат");
        if (listing.isWithoutBroker()) trustScore += 8;
        else riskFactors.add("Власника не підтверджено");
        if (duplicateDetected) {
            trustScore -= 12;
            riskFactors.add("Схоже оголошення вже знайдено в іншому джерелі");
        }
        if (priceRatio != null && priceRatio < 0.55) {
            trustScore -= 45;
            riskFactors.add("Ціна більш ніж на 45% нижча за медіану сегмента");
        } else if (priceRatio != null && priceRatio < 0.70) {
            trustScore -= 12;
            riskFactors.add("Ціна суттєво нижча за медіану сегмента");
        }
        trustScore = clamp(trustScore);

        int valueScore = 60;
        if (priceRatio != null) {
            if (priceRatio <= 0.75) valueScore = 95;
            else if (priceRatio <= 0.90) valueScore = 85;
            else if (priceRatio <= 1.00) valueScore = 72;
            else if (priceRatio <= 1.15) valueScore = 55;
            else valueScore = 35;
        }

        int comfortScore = 50;
        if (infrastructure != null) {
            comfortScore = 35;
            if (safe(infrastructure.getTransportDistanceMeters()) <= 500) comfortScore += 20;
            if (safe(infrastructure.getSupermarketDistanceMeters()) <= 800) comfortScore += 15;
            if (safe(infrastructure.getHospitalDistanceMeters()) <= 1500) comfortScore += 15;
            if (safe(infrastructure.getSchoolDistanceMeters()) <= 1000) comfortScore += 15;
        } else if (hasCoordinates(listing)) {
            comfortScore = 55;
        }
        comfortScore = clamp(comfortScore);

        int qualityScore = clamp((int) Math.round(trustScore * 0.5 + valueScore * 0.3 + comfortScore * 0.2));
        String fraudRisk = trustScore >= 75 ? "LOW" : trustScore >= 50 ? "MEDIUM" : "HIGH";

        String catalogStatus = "PUBLISHED";
        if (!rejectionReasons.isEmpty() || trustScore < 40) {
            catalogStatus = "REJECTED";
        } else if (trustScore < 60 || qualityScore < 55 || "HIGH".equals(fraudRisk)) {
            catalogStatus = "REVIEW";
        }

        return new AnalysisResult(
            trustScore,
            valueScore,
            comfortScore,
            qualityScore,
            fraudRisk,
            catalogStatus,
            riskFactors,
            rejectionReasons
        );
    }

    public int calculateSavingsPercent(Integer price, Integer marketAveragePrice) {
        if (price == null || marketAveragePrice == null || marketAveragePrice == 0) {
            return 0;
        }
        return (int) Math.round(((marketAveragePrice - price) * 100d) / marketAveragePrice);
    }

    public Integer median(List<Integer> values) {
        List<Integer> sorted = values.stream()
            .filter(value -> value != null && value > 0)
            .sorted()
            .toList();
        if (sorted.isEmpty()) {
            return null;
        }
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(middle);
        }
        return (sorted.get(middle - 1) + sorted.get(middle)) / 2;
    }

    public List<String> buildRecommendation(ApartmentEntity apartment) {
        List<String> reasons = new ArrayList<>();
        int savings = calculateSavingsPercent(apartment.getPrice(), apartment.getMarketAveragePrice());
        if (savings > 0) reasons.add(savings + "% дешевше за медіанну ціну схожих квартир");
        if (apartment.getInfrastructure() != null && safe(apartment.getInfrastructure().getSchoolDistanceMeters()) <= 700) reasons.add("Школа у пішій доступності");
        if (apartment.getInfrastructure() != null && safe(apartment.getInfrastructure().getHospitalDistanceMeters()) <= 1000) reasons.add("Лікарня розташована поруч");
        if (apartment.getInfrastructure() != null && safe(apartment.getInfrastructure().getTransportDistanceMeters()) <= 300) reasons.add("Громадський транспорт дуже близько");
        if ("LOW".equals(apartment.getFraudRisk())) reasons.add("Низький ризик шахрайства за результатами аналізу");
        if (Boolean.TRUE.equals(apartment.getWithoutBroker())) reasons.add("Оголошення подано без посередника");
        return reasons;
    }

    public List<String> riskFactors(ApartmentEntity apartment) {
        return apartment.getRiskFactors() == null ? List.of() : Arrays.stream(apartment.getRiskFactors()).toList();
    }

    private boolean hasCoordinates(NormalizedListing listing) {
        return listing.latitude() != null && listing.longitude() != null && listing.latitude() != 0d && listing.longitude() != 0d;
    }

    private int safe(Integer value) {
        return value == null ? Integer.MAX_VALUE : value;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    public record AnalysisResult(
        int trustScore,
        int valueScore,
        int comfortScore,
        int qualityScore,
        String fraudRisk,
        String catalogStatus,
        List<String> riskFactors,
        List<String> rejectionReasons
    ) {
    }
}

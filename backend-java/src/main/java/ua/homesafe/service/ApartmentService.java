package ua.homesafe.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import ua.homesafe.dto.ApartmentDto;
import ua.homesafe.exception.NotFoundException;
import ua.homesafe.model.ApartmentEntity;
import ua.homesafe.repository.ApartmentRepository;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class ApartmentService {

    private final ApartmentRepository apartmentRepository;

    public ApartmentService(ApartmentRepository apartmentRepository) {
        this.apartmentRepository = apartmentRepository;
    }

    @Transactional
    public List<ApartmentDto> findPublished(
        String city,
        String district,
        String currency,
        Integer rooms,
        Integer minPrice,
        Integer maxPrice,
        Boolean withoutBroker
    ) {
        return apartmentRepository.findAll().stream()
            .filter(apartment -> "PUBLISHED".equals(apartment.getCatalogStatus()))
            .filter(apartment -> !isLegacySeed(apartment))
            .filter(apartment -> city == null || city.isBlank() || containsIgnoreCase(apartment.getCity(), city))
            .filter(apartment -> district == null || district.isBlank() || containsIgnoreCase(apartment.getDistrict(), district))
            .filter(apartment -> currency == null || currency.isBlank() || currency.equalsIgnoreCase(String.valueOf(apartment.getCurrency())))
            .filter(apartment -> rooms == null || rooms.equals(apartment.getRooms()))
            .filter(apartment -> minPrice == null || (apartment.getPrice() != null && apartment.getPrice() >= minPrice))
            .filter(apartment -> maxPrice == null || (apartment.getPrice() != null && apartment.getPrice() <= maxPrice))
            .filter(apartment -> withoutBroker == null || withoutBroker.equals(apartment.getWithoutBroker()))
            .sorted(Comparator.comparing(ApartmentEntity::getTrustScore, Comparator.nullsLast(Comparator.reverseOrder())))
            .map(ApartmentDto::from)
            .toList();
    }

    @Transactional
    public ApartmentDto findById(String id) {
        return apartmentRepository.findById(id)
            .filter(apartment -> "PUBLISHED".equals(apartment.getCatalogStatus()))
            .filter(apartment -> !isLegacySeed(apartment))
            .map(ApartmentDto::from)
            .orElseThrow(() -> new NotFoundException("Квартиру не знайдено"));
    }

    public long totalListings() {
        return apartmentRepository.findAll().stream()
            .filter(apartment -> "PUBLISHED".equals(apartment.getCatalogStatus()))
            .filter(apartment -> !isLegacySeed(apartment))
            .count();
    }

    public long verifiedListings() {
        return apartmentRepository.findAll().stream()
            .filter(apartment -> "PUBLISHED".equals(apartment.getCatalogStatus()) && Boolean.TRUE.equals(apartment.getVerified()))
            .filter(apartment -> !isLegacySeed(apartment))
            .count();
    }

    public long supportedCities() {
        return apartmentRepository.findAll().stream()
            .filter(apartment -> "PUBLISHED".equals(apartment.getCatalogStatus()))
            .filter(apartment -> !isLegacySeed(apartment))
            .map(ApartmentEntity::getCity)
            .filter(city -> city != null && !city.isBlank())
            .distinct()
            .count();
    }

    public long averageSavings() {
        var apartments = apartmentRepository.findAll().stream()
            .filter(apartment -> "PUBLISHED".equals(apartment.getCatalogStatus()))
            .filter(apartment -> !isLegacySeed(apartment))
            .toList();

        if (apartments.isEmpty()) {
            return 0;
        }

        double average = apartments.stream()
            .filter(apartment -> apartment.getMarketAveragePrice() != null && apartment.getMarketAveragePrice() > 0 && apartment.getPrice() != null)
            .mapToDouble(apartment -> ((apartment.getMarketAveragePrice() - apartment.getPrice()) * 100d) / apartment.getMarketAveragePrice())
            .average()
            .orElse(0);

        return Math.round(average);
    }

    @Transactional
    public List<ApartmentDto> findReviewListings() {
        return apartmentRepository.findAll().stream()
            .filter(apartment -> "REVIEW".equals(apartment.getCatalogStatus()))
            .filter(apartment -> !isLegacySeed(apartment))
            .sorted(Comparator.comparing(ApartmentEntity::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .map(ApartmentDto::from)
            .toList();
    }

    @Transactional
    public ApartmentDto updateCatalogStatus(String apartmentId, String status) {
        ApartmentEntity apartment = apartmentRepository.findById(apartmentId)
            .orElseThrow(() -> new NotFoundException("Квартиру не знайдено"));

        apartment.setCatalogStatus(status);
        apartment.setVerified("PUBLISHED".equals(status));
        apartment.setPublishedAt("PUBLISHED".equals(status) ? OffsetDateTime.now() : null);
        apartment.setUpdatedAt(OffsetDateTime.now());

        return ApartmentDto.from(apartmentRepository.save(apartment));
    }

    private boolean containsIgnoreCase(String source, String expected) {
        return source != null && source.toLowerCase().contains(expected.toLowerCase());
    }

    private boolean isLegacySeed(ApartmentEntity apartment) {
        if (apartment == null) {
            return false;
        }
        return "OPEN_DATA".equalsIgnoreCase(String.valueOf(apartment.getProviderCode()))
            || "OPEN_DATA".equalsIgnoreCase(String.valueOf(apartment.getSource()));
    }
}

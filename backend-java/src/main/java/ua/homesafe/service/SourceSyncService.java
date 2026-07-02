package ua.homesafe.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ua.homesafe.dto.AdminDtos;
import ua.homesafe.exception.NotFoundException;
import ua.homesafe.integration.ImportQuery;
import ua.homesafe.integration.ListingFingerprint;
import ua.homesafe.integration.NormalizedListing;
import ua.homesafe.integration.ProviderClient;
import ua.homesafe.integration.ProviderConfigParser;
import ua.homesafe.model.ApartmentEntity;
import ua.homesafe.model.DataSourceEntity;
import ua.homesafe.model.ExternalListingEntity;
import ua.homesafe.model.ImportRunEntity;
import ua.homesafe.model.PriceHistoryEntity;
import ua.homesafe.repository.ApartmentRepository;
import ua.homesafe.repository.DataSourceRepository;
import ua.homesafe.repository.ExternalListingRepository;
import ua.homesafe.repository.ImportRunRepository;
import ua.homesafe.repository.PriceHistoryRepository;
import ua.homesafe.util.Ids;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
public class SourceSyncService {

    private static final Logger log = LoggerFactory.getLogger(SourceSyncService.class);

    private final DataSourceRepository dataSourceRepository;
    private final ImportRunRepository importRunRepository;
    private final ApartmentRepository apartmentRepository;
    private final ExternalListingRepository externalListingRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final ListingAnalysisService listingAnalysisService;
    private final List<ProviderClient> providerClients;
    private final ObjectMapper objectMapper;
    private final ProviderConfigParser providerConfigParser;

    public SourceSyncService(
        DataSourceRepository dataSourceRepository,
        ImportRunRepository importRunRepository,
        ApartmentRepository apartmentRepository,
        ExternalListingRepository externalListingRepository,
        PriceHistoryRepository priceHistoryRepository,
        ListingAnalysisService listingAnalysisService,
        List<ProviderClient> providerClients,
        ObjectMapper objectMapper,
        ProviderConfigParser providerConfigParser
    ) {
        this.dataSourceRepository = dataSourceRepository;
        this.importRunRepository = importRunRepository;
        this.apartmentRepository = apartmentRepository;
        this.externalListingRepository = externalListingRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.listingAnalysisService = listingAnalysisService;
        this.providerClients = providerClients;
        this.objectMapper = objectMapper;
        this.providerConfigParser = providerConfigParser;
    }

    public AdminDtos.ImportRunDto syncSource(String code, AdminDtos.SyncRequest request) {
        log.info("Starting source sync code={} request={}", code, request);
        DataSourceEntity source = dataSourceRepository.findAll().stream()
            .filter(item -> item.getCode() != null && item.getCode().equalsIgnoreCase(code))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Джерело не знайдено"));

        ImportRunEntity run = new ImportRunEntity();
        run.setId(Ids.newId());
        run.setDataSource(source);
        run.setStatus(Boolean.TRUE.equals(source.getEnabled()) ? "RUNNING" : "SKIPPED");
        run.setRequestedCity(request == null ? null : request.city());
        run.setReceivedCount(0);
        run.setNormalizedCount(0);
        run.setPublishedCount(0);
        run.setReviewCount(0);
        run.setRejectedCount(0);
        run.setDuplicateCount(0);
        run.setStartedAt(OffsetDateTime.now());
        run = importRunRepository.save(run);

        if (!Boolean.TRUE.equals(source.getEnabled())) {
            run.setFinishedAt(OffsetDateTime.now());
            run.setErrorMessage("Джерело вимкнене в адмін-панелі");
            source.setLastStatus("SKIPPED");
            source.setLastError(run.getErrorMessage());
            source.setLastSyncAt(run.getFinishedAt());
            dataSourceRepository.save(source);
            importRunRepository.save(run);
            return toImportRunDto(run, source);
        }

        try {
            ProviderClient providerClient = providerClients.stream()
                .filter(client -> client.supports(code))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Для джерела " + code + " ще не налаштовано провайдер"));

            Map<String, Object> config = providerConfigParser.parse(source);
            Map<String, Object> syncDefaults = providerConfigParser.map(config, "syncDefaults");
            ImportQuery query = buildQuery(request, syncDefaults);
            log.info(
                "Resolved sync query for {} city={} district={} cityId={} stateId={} limit={} page={} offset={} categoryIds={}",
                code,
                query.city(),
                query.district(),
                query.cityId(),
                query.stateId(),
                query.safeLimit(),
                query.safePage(),
                query.safeOffset(),
                query.categoryIds()
            );

            List<NormalizedListing> listings = providerClient.fetchListings(source, query);
            run.setReceivedCount(listings.size());
            log.info("Provider {} returned {} listings", code, listings.size());

            for (NormalizedListing listing : listings) {
                processListing(source, run, listing);
            }

            run.setStatus("SUCCEEDED");
            run.setFinishedAt(OffsetDateTime.now());
            source.setLastStatus("SUCCEEDED");
            source.setLastError(null);
            source.setLastSyncAt(run.getFinishedAt());
            log.info("Source sync succeeded code={} published={} review={} rejected={} duplicates={}",
                code, run.getPublishedCount(), run.getReviewCount(), run.getRejectedCount(), run.getDuplicateCount());
        } catch (Exception exception) {
            run.setStatus("FAILED");
            run.setFinishedAt(OffsetDateTime.now());
            run.setErrorMessage(exception.getMessage());
            source.setLastStatus("FAILED");
            source.setLastError(exception.getMessage());
            source.setLastSyncAt(run.getFinishedAt());
            log.error("Source sync failed code={} message={}", code, exception.getMessage(), exception);
        }

        dataSourceRepository.save(source);
        importRunRepository.save(run);
        return toImportRunDto(run, source);
    }

    private ImportQuery buildQuery(AdminDtos.SyncRequest request, Map<String, Object> syncDefaults) {
        String city = request != null && request.city() != null ? request.city() : providerConfigParser.string(syncDefaults, "city", null);
        String district = request != null && request.district() != null ? request.district() : providerConfigParser.string(syncDefaults, "district", null);
        String state = request != null && request.state() != null ? request.state() : providerConfigParser.string(syncDefaults, "state", null);
        Integer offset = request != null && request.offset() != null ? request.offset() : providerConfigParser.integer(syncDefaults, "offset", null);
        String categoryIds = request != null && request.category_ids() != null ? request.category_ids() : providerConfigParser.string(syncDefaults, "category_ids", null);
        Integer cityId = request != null && request.cityId() != null ? request.cityId() : providerConfigParser.integer(syncDefaults, "cityId", null);
        Integer stateId = request != null && request.stateId() != null ? request.stateId() : providerConfigParser.integer(syncDefaults, "stateId", null);
        Integer limit = request != null && request.limit() != null ? request.limit() : providerConfigParser.integer(syncDefaults, "limit", 20);
        Integer page = request != null && request.page() != null ? request.page() : providerConfigParser.integer(syncDefaults, "page", 0);
        return new ImportQuery(city, district, state, offset, categoryIds, cityId, stateId, limit, page);
    }

    private void processListing(DataSourceEntity source, ImportRunEntity run, NormalizedListing listing) {
        run.setNormalizedCount(run.getNormalizedCount() + 1);
        OffsetDateTime now = OffsetDateTime.now();
        String fingerprint = ListingFingerprint.build(listing);

        List<ExternalListingEntity> duplicates = externalListingRepository.findByFingerprint(fingerprint);
        boolean duplicateDetected = duplicates.stream()
            .anyMatch(existing -> !source.getCode().equalsIgnoreCase(existing.getProviderCode()) || !safeEquals(existing.getExternalId(), listing.externalId()));
        if (duplicateDetected) {
            run.setDuplicateCount(run.getDuplicateCount() + 1);
        }

        Integer marketMedian = segmentMedian(listing);
        ListingAnalysisService.AnalysisResult analysis = listingAnalysisService.analyze(
            listing,
            source.getReliability() == null ? 70 : source.getReliability(),
            marketMedian,
            duplicateDetected,
            null
        );

        ApartmentEntity apartment = apartmentRepository.findAll().stream()
            .filter(item -> safeEquals(item.getProviderCode(), source.getCode()))
            .filter(item -> safeEquals(item.getExternalId(), listing.externalId()))
            .findFirst()
            .orElse(null);
        Integer previousPrice = apartment == null ? null : apartment.getPrice();

        if (apartment == null && !"REJECTED".equals(analysis.catalogStatus())) {
            apartment = new ApartmentEntity();
            apartment.setId(Ids.newId());
            apartment.setCreatedAt(now);
        }

        if (apartment != null) {
            applyApartmentData(apartment, source, listing, analysis, marketMedian, now);
            apartment = apartmentRepository.save(apartment);
            if (previousPrice == null || !previousPrice.equals(apartment.getPrice())) {
                PriceHistoryEntity point = new PriceHistoryEntity();
                point.setId(Ids.newId());
                point.setApartment(apartment);
                point.setPrice(apartment.getPrice());
                point.setRecordedAt(now);
                priceHistoryRepository.save(point);
            }
        }

        ExternalListingEntity external = externalListingRepository.findAll().stream()
            .filter(item -> safeEquals(item.getProviderCode(), source.getCode()))
            .filter(item -> safeEquals(item.getExternalId(), listing.externalId()))
            .findFirst()
            .orElseGet(() -> {
                ExternalListingEntity created = new ExternalListingEntity();
                created.setId(Ids.newId());
                created.setFirstSeenAt(now);
                return created;
            });

        external.setDataSource(source);
        external.setProviderCode(source.getCode());
        external.setExternalId(listing.externalId());
        external.setSourceUrl(listing.sourceUrl());
        external.setFingerprint(fingerprint);
        external.setRawPayload(rawPayload(listing));
        external.setStatus(analysis.catalogStatus());
        external.setRejectionReasons(analysis.rejectionReasons().toArray(String[]::new));
        external.setApartment(apartment);
        external.setLastSeenAt(now);
        externalListingRepository.save(external);

        if ("PUBLISHED".equals(analysis.catalogStatus())) {
            run.setPublishedCount(run.getPublishedCount() + 1);
        } else if ("REVIEW".equals(analysis.catalogStatus())) {
            run.setReviewCount(run.getReviewCount() + 1);
        } else {
            run.setRejectedCount(run.getRejectedCount() + 1);
        }
    }

    private void applyApartmentData(
        ApartmentEntity apartment,
        DataSourceEntity source,
        NormalizedListing listing,
        ListingAnalysisService.AnalysisResult analysis,
        Integer marketMedian,
        OffsetDateTime now
    ) {
        apartment.setTitle(blankToDefault(listing.title(), "Квартира"));
        apartment.setCity(blankToDefault(listing.city(), "Невідоме місто"));
        apartment.setDistrict(blankToDefault(listing.district(), "Не вказано"));
        apartment.setAddress(blankToDefault(listing.address(), "Адресу не вказано"));
        apartment.setPrice(listing.price() == null ? 0 : listing.price());
        apartment.setMarketAveragePrice(marketMedian == null || marketMedian <= 0 ? apartment.getPrice() : marketMedian);
        apartment.setRooms(listing.rooms() == null ? 0 : listing.rooms());
        apartment.setArea(listing.area() == null ? 0d : listing.area());
        apartment.setFloor(listing.floor() == null ? 0 : listing.floor());
        apartment.setTotalFloors(listing.totalFloors() == null ? 0 : listing.totalFloors());
        apartment.setDescription(blankToDefault(listing.description(), "Опис відсутній"));
        apartment.setImageUrl(blankToDefault(listing.imageUrl(), "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85"));
        apartment.setCurrency(blankToDefault(listing.currency(), "UAH"));
        apartment.setOwnerType(blankToDefault(listing.ownerType(), listing.isWithoutBroker() ? "Власник" : "Потребує перевірки"));
        apartment.setDocuments(listing.isWithoutBroker() ? "Власник позначений у джерелі" : "Документи потребують перевірки");
        apartment.setRiskFactors(analysis.riskFactors().toArray(String[]::new));
        apartment.setLatitude(listing.latitude() == null ? 0d : listing.latitude());
        apartment.setLongitude(listing.longitude() == null ? 0d : listing.longitude());
        apartment.setSource("PARTNER_API");
        apartment.setSourceUrl(listing.sourceUrl());
        apartment.setWithoutBroker(listing.isWithoutBroker());
        apartment.setVerified("PUBLISHED".equals(analysis.catalogStatus()));
        apartment.setTrustScore(analysis.trustScore());
        apartment.setValueScore(analysis.valueScore());
        apartment.setComfortScore(analysis.comfortScore());
        apartment.setFraudRisk(analysis.fraudRisk());
        apartment.setQualityScore(analysis.qualityScore());
        apartment.setCatalogStatus(analysis.catalogStatus());
        apartment.setProviderCode(source.getCode());
        apartment.setExternalId(listing.externalId());
        apartment.setDataSource(source);
        apartment.setPublishedAt("PUBLISHED".equals(analysis.catalogStatus()) ? (listing.publishedAt() == null ? now : listing.publishedAt()) : null);
        apartment.setLastSeenAt(now);
        apartment.setUpdatedAt(now);
    }

    private Integer segmentMedian(NormalizedListing listing) {
        return listingAnalysisService.median(
            apartmentRepository.findAll().stream()
                .filter(apartment -> "PUBLISHED".equals(apartment.getCatalogStatus()))
                .filter(apartment -> safeEquals(apartment.getCurrency(), blankToDefault(listing.currency(), "UAH")))
                .filter(apartment -> apartment.getCity() != null && apartment.getCity().equalsIgnoreCase(blankToDefault(listing.city(), "")))
                .filter(apartment -> apartment.getDistrict() != null && apartment.getDistrict().equalsIgnoreCase(blankToDefault(listing.district(), "")))
                .filter(apartment -> apartment.getRooms() != null && apartment.getRooms().equals(listing.rooms() == null ? 0 : listing.rooms()))
                .map(ApartmentEntity::getPrice)
                .toList()
        );
    }

    private String rawPayload(NormalizedListing listing) {
        try {
            return objectMapper.writeValueAsString(listing.raw());
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private boolean safeEquals(String left, String right) {
        return left == null ? right == null : right != null && left.equalsIgnoreCase(right);
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private AdminDtos.ImportRunDto toImportRunDto(ImportRunEntity run, DataSourceEntity source) {
        return new AdminDtos.ImportRunDto(
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
            source == null ? null : new AdminDtos.SourceRef(source.getCode(), source.getName())
        );
    }
}

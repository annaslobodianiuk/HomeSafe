package ua.homesafe.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import ua.homesafe.dto.AdminDtos;
import ua.homesafe.model.DataSourceEntity;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OlxProviderClient implements ProviderClient {

    private static final Logger log = LoggerFactory.getLogger(OlxProviderClient.class);
    private static final Pattern ID_PATTERN = Pattern.compile("-ID([A-Za-z0-9]+)\\.html");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+[\\d\\s.,]*)");
    private static final List<String> PRIVATE_OWNER_MARKERS = List.of(
        "власник",
        "без коміс",
        "без комиссии",
        "от хозяина",
        "від власника",
        "свою квартиру",
        "сдам свою",
        "здам свою"
    );

    private final ProviderHttpClient httpClient;
    private final ProviderConfigParser configParser;
    private final ObjectMapper objectMapper;

    public OlxProviderClient(
        ProviderHttpClient httpClient,
        ProviderConfigParser configParser,
        ObjectMapper objectMapper
    ) {
        this.httpClient = httpClient;
        this.configParser = configParser;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String providerCode) {
        return "OLX".equalsIgnoreCase(providerCode);
    }

    @Override
    public List<NormalizedListing> fetchListings(DataSourceEntity source, ImportQuery query) {
        Map<String, Object> config = configParser.parse(source);
        String city = firstNonBlank(query.city(), configParser.string(config, "defaultCity", "Київ"));
        String citySlug = resolveCitySlug(config, city);
        URI searchUri = buildSearchUri(source, config, citySlug, query);

        log.info(
            "OLX public fetch source={} city={} citySlug={} limit={} page={} url={}",
            source.getCode(),
            city,
            citySlug,
            query.safeLimit(),
            query.safePage(),
            searchUri
        );

        String searchHtml = httpClient.getText(searchUri, Map.of());
        List<String> listingUrls = extractListingUrls(searchUri, searchHtml, query.safeLimit());
        log.info("OLX public search extracted {} listing urls", listingUrls.size());

        List<NormalizedListing> listings = new ArrayList<>();
        for (String listingUrl : listingUrls) {
            try {
                NormalizedListing listing = fetchListingDetails(listingUrl, city);
                if (matchesFilter(listing, query)) {
                    listings.add(listing);
                }
            } catch (Exception exception) {
                log.warn("OLX listing parse failed url={} message={}", listingUrl, exception.getMessage());
            }
        }

        log.info("OLX public fetch normalized {} listings", listings.size());
        return listings;
    }

    public AdminDtos.OlxDebugDto debugSource(DataSourceEntity source) {
        ImportQuery query = new ImportQuery(
            configParser.string(configParser.parse(source), "defaultCity", "Київ"),
            null,
            null,
            0,
            null,
            null,
            null,
            3,
            0
        );

        List<NormalizedListing> listings = fetchListings(source, query);
        List<String> ids = listings.stream().map(NormalizedListing::externalId).toList();
        List<String> titles = listings.stream().map(NormalizedListing::title).toList();
        String message = listings.isEmpty()
            ? "Публічний парсер OLX не знайшов оголошень за поточним шляхом пошуку."
            : "Публічний парсер OLX успішно отримав оголошення.";

        return new AdminDtos.OlxDebugDto(
            null,
            null,
            listings.size(),
            ids,
            titles,
            message
        );
    }

    private URI buildSearchUri(DataSourceEntity source, Map<String, Object> config, String citySlug, ImportQuery query) {
        String pathTemplate = configParser.string(config, "searchPathTemplate", "/uk/nedvizhimost/kvartiry/dolgosrochnaya-arenda-kvartir/{citySlug}/");
        String path = pathTemplate.replace("{citySlug}", citySlug);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(trimSlash(source.getBaseUrl()) + path);
        if (query.safePage() > 0) {
            builder.queryParam("page", query.safePage() + 1);
        }
        return builder.build(true).toUri();
    }

    private List<String> extractListingUrls(URI searchUri, String html, int limit) {
        Document document = Jsoup.parse(html, searchUri.toString());
        Set<String> urls = new LinkedHashSet<>();
        Elements anchors = document.select("a[href*=/d/uk/obyavlenie/], a[href*=/d/obyavlenie/]");

        for (Element anchor : anchors) {
            String href = anchor.absUrl("href");
            String normalized = normalizeListingUrl(href);
            if (normalized == null) {
                continue;
            }
            urls.add(normalized);
            if (urls.size() >= limit) {
                break;
            }
        }

        return new ArrayList<>(urls);
    }

    private NormalizedListing fetchListingDetails(String listingUrl, String fallbackCity) {
        String html = httpClient.getText(URI.create(listingUrl), Map.of());
        Document document = Jsoup.parse(html, listingUrl);
        JsonNode productSchema = extractProductSchema(document);
        Map<String, String> params = extractParameters(document);

        String title = firstNonBlank(
            textOrNull(productSchema.path("name")),
            attrOrNull(document, "meta[property=og:title]", "content"),
            document.title()
        );

        String description = firstNonBlank(
            cleanDescription(textOrNull(productSchema.path("description"))),
            cleanDescription(textOf(document.selectFirst("[data-cy=ad_description]"))),
            cleanDescription(attrOrNull(document, "meta[name=description]", "content")),
            "Опис відсутній"
        );

        List<String> images = extractImages(document, productSchema);
        String externalId = firstNonBlank(
            textOrNull(productSchema.path("sku")),
            extractIdFromUrl(listingUrl)
        );
        String district = firstNonBlank(
            textOrNull(productSchema.path("offers").path("areaServed").path("name")),
            params.get("district"),
            "Не вказано"
        );
        String city = firstNonBlank(
            params.get("city"),
            fallbackCity,
            "Невідоме місто"
        );
        String address = firstNonBlank(
            textOrNull(productSchema.path("offers").path("availableAtOrFrom").path("address").path("streetAddress")),
            extractAddress(description),
            district + ", " + city
        );

        Integer price = firstNumber(
            textOrNull(productSchema.path("offers").path("price")),
            textOf(document.selectFirst("[data-testid=ad-price-container] h3")),
            description
        );
        String currency = normalizeCurrency(firstNonBlank(
            textOrNull(productSchema.path("offers").path("priceCurrency")),
            textOf(document.selectFirst("[data-testid=ad-price-container] h3"))
        ));

        Integer rooms = firstNumber(params.get("rooms"), description);
        Double area = firstDouble(params.get("area"), description);
        Integer floor = firstNumber(params.get("floor"), description);
        Integer totalFloors = firstNumber(params.get("totalFloors"), description);
        boolean privateOwner = detectPrivateOwner(title, description, html);

        ObjectNode raw = objectMapper.createObjectNode();
        raw.put("id", externalId);
        raw.put("title", title);
        raw.put("description", description);
        raw.put("url", listingUrl);
        raw.put("city", city);
        raw.put("district", district);
        raw.put("address", address);
        raw.put("price", price == null ? 0 : price);
        raw.put("currency", currency);
        raw.put("rooms", rooms == null ? 0 : rooms);
        raw.put("area", area == null ? 0d : area);
        raw.put("floor", floor == null ? 0 : floor);
        raw.put("totalFloors", totalFloors == null ? 0 : totalFloors);
        raw.put("ownerType", privateOwner ? "Власник" : "Потребує перевірки");
        raw.put("imageUrl", images.isEmpty() ? "" : images.get(0));
        ArrayNode gallery = raw.putArray("images");
        images.forEach(gallery::add);

        return new NormalizedListing(
            externalId,
            title,
            city,
            district,
            address,
            price == null ? 0 : price,
            currency,
            rooms == null ? 0 : rooms,
            area == null ? 0d : area,
            floor == null ? 0 : floor,
            totalFloors == null ? 0 : totalFloors,
            description,
            images.isEmpty() ? "" : images.get(0),
            images.size(),
            0d,
            0d,
            listingUrl,
            privateOwner,
            privateOwner ? "Власник" : "Потребує перевірки",
            null,
            raw
        );
    }

    private JsonNode extractProductSchema(Document document) {
        for (Element script : document.select("script[type=application/ld+json]")) {
            String payload = script.data();
            if (payload == null || payload.isBlank()) {
                continue;
            }
            try {
                JsonNode node = objectMapper.readTree(payload);
                JsonNode product = findProductNode(node);
                if (product != null) {
                    return product;
                }
            } catch (Exception ignored) {
                // Ignore malformed schema blocks and keep searching.
            }
        }
        return objectMapper.nullNode();
    }

    private JsonNode findProductNode(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                JsonNode product = findProductNode(item);
                if (product != null) {
                    return product;
                }
            }
            return null;
        }
        if (node.isObject() && "Product".equalsIgnoreCase(textOrNull(node.path("@type")))) {
            return node;
        }
        return null;
    }

    private Map<String, String> extractParameters(Document document) {
        Map<String, String> params = new LinkedHashMap<>();
        for (Element paragraph : document.select("p[data-nx-name=P3], p.css-odhutu")) {
            String text = paragraph.text();
            int colonIndex = text.indexOf(':');
            if (colonIndex <= 0) {
                continue;
            }
            String key = normalizeLabel(text.substring(0, colonIndex));
            String value = text.substring(colonIndex + 1).trim();
            if (!key.isBlank() && !value.isBlank()) {
                params.putIfAbsent(key, value);
            }
        }

        String locationText = textOf(document.selectFirst("[data-testid=breadcrumbs-wrapper]"));
        if (locationText != null && !locationText.isBlank()) {
            String[] parts = locationText.split("·|/|,");
            if (parts.length > 0) {
                params.putIfAbsent("city", parts[0].trim());
            }
            if (parts.length > 1) {
                params.putIfAbsent("district", parts[1].trim());
            }
        }

        return Map.of(
            "city", firstNonBlank(params.get("city"), ""),
            "district", firstNonBlank(params.get("district"), ""),
            "rooms", firstNonBlank(params.get("кількістькімнат"), params.get("количествокомнат"), ""),
            "area", firstNonBlank(params.get("загальнаплоща"), params.get("общаяплощадь"), ""),
            "floor", firstNonBlank(params.get("поверх"), ""),
            "totalFloors", firstNonBlank(params.get("поверховість"), params.get("этажность"), "")
        );
    }

    private List<String> extractImages(Document document, JsonNode productSchema) {
        Set<String> images = new LinkedHashSet<>();
        JsonNode schemaImages = productSchema.path("image");
        if (schemaImages.isArray()) {
            for (JsonNode image : schemaImages) {
                addImage(images, textOrNull(image));
            }
        } else {
            addImage(images, textOrNull(schemaImages));
        }

        addImage(images, attrOrNull(document, "meta[property=og:image]", "content"));
        for (Element image : document.select("img[src*='apollo.olxcdn.com'], img[src*='olxcdn.com']")) {
            addImage(images, image.absUrl("src"));
            addImage(images, image.absUrl("data-src"));
        }
        return new ArrayList<>(images);
    }

    private boolean matchesFilter(NormalizedListing listing, ImportQuery query) {
        if (query.city() != null && !query.city().isBlank() && (listing.city() == null || !listing.city().toLowerCase(Locale.ROOT).contains(query.city().toLowerCase(Locale.ROOT)))) {
            return false;
        }
        if (query.district() != null && !query.district().isBlank() && (listing.district() == null || !listing.district().toLowerCase(Locale.ROOT).contains(query.district().toLowerCase(Locale.ROOT)))) {
            return false;
        }
        return true;
    }

    private String resolveCitySlug(Map<String, Object> config, String city) {
        Map<String, Object> citySlugMap = configParser.map(config, "citySlugMap");
        if (citySlugMap.containsKey(city)) {
            return String.valueOf(citySlugMap.get(city));
        }
        if (citySlugMap.containsKey(city.toLowerCase(Locale.ROOT))) {
            return String.valueOf(citySlugMap.get(city.toLowerCase(Locale.ROOT)));
        }
        return firstNonBlank(configParser.string(config, "defaultCitySlug", "kiev"), "kiev");
    }

    private boolean detectPrivateOwner(String title, String description, String html) {
        String haystack = (title + " " + description + " " + html).toLowerCase(Locale.ROOT);
        for (String marker : PRIVATE_OWNER_MARKERS) {
            if (haystack.contains(marker)) {
                return true;
            }
        }
        Matcher privateBusinessMatcher = Pattern.compile("\"privateBusiness\":(true|false)").matcher(html);
        if (privateBusinessMatcher.find()) {
            return "false".equalsIgnoreCase(privateBusinessMatcher.group(1));
        }
        return false;
    }

    private String extractAddress(String description) {
        if (description == null || description.isBlank()) {
            return "";
        }
        Matcher matcher = Pattern.compile("(Адреса|Адрес)\\s*:\\s*([^\\n.]{5,120})", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(description);
        if (matcher.find()) {
            return matcher.group(2).trim();
        }
        return "";
    }

    private static Integer firstNumber(String... values) {
        for (String value : values) {
            Double parsed = parseNumber(value);
            if (parsed != null) {
                return (int) Math.round(parsed);
            }
        }
        return null;
    }

    private static Double firstDouble(String... values) {
        for (String value : values) {
            Double parsed = parseNumber(value);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private static Double parseNumber(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(value.replace('\u00A0', ' '));
        if (!matcher.find()) {
            return null;
        }
        try {
            return Double.parseDouble(matcher.group(1).replace(" ", "").replace(",", "."));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String extractIdFromUrl(String listingUrl) {
        Matcher matcher = ID_PATTERN.matcher(listingUrl);
        return matcher.find() ? matcher.group(1) : listingUrl;
    }

    private static String normalizeListingUrl(String href) {
        if (href == null || href.isBlank()) {
            return null;
        }
        String normalized = href.replace("&amp;", "&");
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        if (!normalized.contains("/obyavlenie/")) {
            return null;
        }
        try {
            return new URI(normalized).toString();
        } catch (URISyntaxException exception) {
            return null;
        }
    }

    private static String normalizeCurrency(String value) {
        String normalized = firstNonBlank(value, "UAH").toUpperCase(Locale.ROOT);
        if (normalized.contains("$")) {
            return "USD";
        }
        if (normalized.contains("€")) {
            return "EUR";
        }
        if (normalized.contains("UAH") || normalized.contains("ГРН")) {
            return "UAH";
        }
        return normalized;
    }

    private static void addImage(Set<String> images, String value) {
        if (value != null && !value.isBlank()) {
            images.add(value.trim());
        }
    }

    private static String attrOrNull(Document document, String selector, String attribute) {
        Element element = document.selectFirst(selector);
        if (element == null) {
            return null;
        }
        String value = element.attr(attribute);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String textOf(Element element) {
        if (element == null) {
            return null;
        }
        String text = element.text();
        return text == null || text.isBlank() ? null : text.trim();
    }

    private static String textOrNull(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    private static String cleanDescription(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value
            .replace('\u00A0', ' ')
            .replaceAll("\\s+", " ")
            .replaceFirst("^Опис\\s*", "")
            .trim();
    }

    private static String normalizeLabel(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", "");
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String trimSlash(String value) {
        return value == null ? "" : value.replaceAll("/+$", "");
    }
}

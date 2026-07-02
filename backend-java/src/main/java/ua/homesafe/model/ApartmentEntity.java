package ua.homesafe.model;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "\"Apartment\"")
public class ApartmentEntity {

    @Id
    @Column(name = "\"id\"")
    private String id;

    @Column(name = "\"title\"")
    private String title;
    @Column(name = "\"city\"")
    private String city;
    @Column(name = "\"district\"")
    private String district;
    @Column(name = "\"address\"")
    private String address;
    @Column(name = "\"price\"")
    private Integer price;
    @Column(name = "\"marketAveragePrice\"")
    private Integer marketAveragePrice;
    @Column(name = "\"rooms\"")
    private Integer rooms;
    @Column(name = "\"area\"")
    private Double area;
    @Column(name = "\"floor\"")
    private Integer floor;
    @Column(name = "\"totalFloors\"")
    private Integer totalFloors;
    @Column(name = "\"description\"")
    private String description;
    @Column(name = "\"imageUrl\"")
    private String imageUrl;
    @Column(name = "\"currency\"")
    private String currency;
    @Column(name = "\"ownerType\"")
    private String ownerType;
    @Column(name = "\"documents\"")
    private String documents;
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "\"riskFactors\"")
    private String[] riskFactors;
    @Column(name = "\"latitude\"")
    private Double latitude;
    @Column(name = "\"longitude\"")
    private Double longitude;
    @ColumnTransformer(write = "?::\"ListingSource\"")
    @Column(name = "\"source\"")
    private String source;
    @Column(name = "\"sourceUrl\"")
    private String sourceUrl;
    @Column(name = "\"isWithoutBroker\"")
    private Boolean withoutBroker;
    @Column(name = "\"isVerified\"")
    private Boolean verified;
    @Column(name = "\"trustScore\"")
    private Integer trustScore;
    @Column(name = "\"valueScore\"")
    private Integer valueScore;
    @Column(name = "\"comfortScore\"")
    private Integer comfortScore;
    @ColumnTransformer(write = "?::\"FraudRisk\"")
    @Column(name = "\"fraudRisk\"")
    private String fraudRisk;
    @Column(name = "\"qualityScore\"")
    private Integer qualityScore;
    @ColumnTransformer(write = "?::\"CatalogStatus\"")
    @Column(name = "\"catalogStatus\"")
    private String catalogStatus;
    @ColumnTransformer(write = "?::\"ProviderCode\"")
    @Column(name = "\"providerCode\"")
    private String providerCode;
    @Column(name = "\"externalId\"")
    private String externalId;
    @Column(name = "\"lastSeenAt\"")
    private OffsetDateTime lastSeenAt;
    @Column(name = "\"createdAt\"")
    private OffsetDateTime createdAt;
    @Column(name = "\"updatedAt\"")
    private OffsetDateTime updatedAt;
    @Column(name = "\"publishedAt\"")
    private OffsetDateTime publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "\"dataSourceId\"")
    private DataSourceEntity dataSource;

    @OneToOne(mappedBy = "apartment", fetch = FetchType.LAZY)
    private InfrastructureEntity infrastructure;

    @OneToMany(mappedBy = "apartment", fetch = FetchType.LAZY)
    @OrderBy("recordedAt ASC")
    private List<PriceHistoryEntity> priceHistory = new ArrayList<>();

    @OneToMany(mappedBy = "apartment", fetch = FetchType.LAZY)
    private List<ExternalListingEntity> externalListings = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }
    public Integer getMarketAveragePrice() { return marketAveragePrice; }
    public void setMarketAveragePrice(Integer marketAveragePrice) { this.marketAveragePrice = marketAveragePrice; }
    public Integer getRooms() { return rooms; }
    public void setRooms(Integer rooms) { this.rooms = rooms; }
    public Double getArea() { return area; }
    public void setArea(Double area) { this.area = area; }
    public Integer getFloor() { return floor; }
    public void setFloor(Integer floor) { this.floor = floor; }
    public Integer getTotalFloors() { return totalFloors; }
    public void setTotalFloors(Integer totalFloors) { this.totalFloors = totalFloors; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }
    public String getDocuments() { return documents; }
    public void setDocuments(String documents) { this.documents = documents; }
    public String[] getRiskFactors() { return riskFactors; }
    public void setRiskFactors(String[] riskFactors) { this.riskFactors = riskFactors; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public Boolean getWithoutBroker() { return withoutBroker; }
    public void setWithoutBroker(Boolean withoutBroker) { this.withoutBroker = withoutBroker; }
    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }
    public Integer getTrustScore() { return trustScore; }
    public void setTrustScore(Integer trustScore) { this.trustScore = trustScore; }
    public Integer getValueScore() { return valueScore; }
    public void setValueScore(Integer valueScore) { this.valueScore = valueScore; }
    public Integer getComfortScore() { return comfortScore; }
    public void setComfortScore(Integer comfortScore) { this.comfortScore = comfortScore; }
    public String getFraudRisk() { return fraudRisk; }
    public void setFraudRisk(String fraudRisk) { this.fraudRisk = fraudRisk; }
    public Integer getQualityScore() { return qualityScore; }
    public void setQualityScore(Integer qualityScore) { this.qualityScore = qualityScore; }
    public String getCatalogStatus() { return catalogStatus; }
    public void setCatalogStatus(String catalogStatus) { this.catalogStatus = catalogStatus; }
    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String providerCode) { this.providerCode = providerCode; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public OffsetDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(OffsetDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public OffsetDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(OffsetDateTime publishedAt) { this.publishedAt = publishedAt; }
    public DataSourceEntity getDataSource() { return dataSource; }
    public void setDataSource(DataSourceEntity dataSource) { this.dataSource = dataSource; }
    public InfrastructureEntity getInfrastructure() { return infrastructure; }
    public List<PriceHistoryEntity> getPriceHistory() { return priceHistory; }
    public List<ExternalListingEntity> getExternalListings() { return externalListings; }
}

package ua.homesafe.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "\"ExternalListing\"")
public class ExternalListingEntity {

    @Id
    @Column(name = "\"id\"")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "\"dataSourceId\"")
    private DataSourceEntity dataSource;

    @Column(name = "\"providerCode\"")
    private String providerCode;
    @Column(name = "\"externalId\"")
    private String externalId;
    @Column(name = "\"sourceUrl\"")
    private String sourceUrl;
    @Column(name = "\"fingerprint\"")
    private String fingerprint;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "\"rawPayload\"")
    private String rawPayload;
    @Column(name = "\"status\"")
    private String status;
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "\"rejectionReasons\"")
    private String[] rejectionReasons;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "\"apartmentId\"")
    private ApartmentEntity apartment;

    @Column(name = "\"firstSeenAt\"")
    private OffsetDateTime firstSeenAt;
    @Column(name = "\"lastSeenAt\"")
    private OffsetDateTime lastSeenAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public DataSourceEntity getDataSource() { return dataSource; }
    public void setDataSource(DataSourceEntity dataSource) { this.dataSource = dataSource; }
    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String providerCode) { this.providerCode = providerCode; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }
    public String getRawPayload() { return rawPayload; }
    public void setRawPayload(String rawPayload) { this.rawPayload = rawPayload; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String[] getRejectionReasons() { return rejectionReasons; }
    public void setRejectionReasons(String[] rejectionReasons) { this.rejectionReasons = rejectionReasons; }
    public ApartmentEntity getApartment() { return apartment; }
    public void setApartment(ApartmentEntity apartment) { this.apartment = apartment; }
    public OffsetDateTime getFirstSeenAt() { return firstSeenAt; }
    public void setFirstSeenAt(OffsetDateTime firstSeenAt) { this.firstSeenAt = firstSeenAt; }
    public OffsetDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(OffsetDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
}

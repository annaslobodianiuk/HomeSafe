package ua.homesafe.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "\"DataSource\"")
public class DataSourceEntity {

    @Id
    @Column(name = "\"id\"")
    private String id;
    @Column(name = "\"code\"", length = 64)
    private String code;
    @Column(name = "\"name\"", length = 120)
    private String name;
    @Column(name = "\"baseUrl\"", length = 512)
    private String baseUrl;
    @Column(name = "\"apiKeyEnv\"", length = 120)
    private String apiKeyEnv;
    @Column(name = "\"enabled\"")
    private Boolean enabled;
    @Column(name = "\"reliability\"")
    private Integer reliability;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "\"config\"", columnDefinition = "jsonb")
    private String config;
    @Column(name = "\"lastSyncAt\"")
    private OffsetDateTime lastSyncAt;
    @Column(name = "\"lastStatus\"", length = 64)
    private String lastStatus;
    @Column(name = "\"lastError\"", columnDefinition = "text")
    private String lastError;

    public String getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getBaseUrl() { return baseUrl; }
    public String getApiKeyEnv() { return apiKeyEnv; }
    public Boolean getEnabled() { return enabled; }
    public Integer getReliability() { return reliability; }
    public String getConfig() { return config; }
    public String getLastStatus() { return lastStatus; }
    public String getLastError() { return lastError; }
    public OffsetDateTime getLastSyncAt() { return lastSyncAt; }
    public void setId(String id) { this.id = id; }
    public void setCode(String code) { this.code = code; }
    public void setName(String name) { this.name = name; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public void setApiKeyEnv(String apiKeyEnv) { this.apiKeyEnv = apiKeyEnv; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public void setReliability(Integer reliability) { this.reliability = reliability; }
    public void setConfig(String config) { this.config = config; }
    public void setLastSyncAt(OffsetDateTime lastSyncAt) { this.lastSyncAt = lastSyncAt; }
    public void setLastStatus(String lastStatus) { this.lastStatus = lastStatus; }
    public void setLastError(String lastError) { this.lastError = lastError; }
}

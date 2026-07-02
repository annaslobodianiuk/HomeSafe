package ua.homesafe.model;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnTransformer;

import java.time.OffsetDateTime;

@Entity
@Table(name = "\"ImportRun\"")
public class ImportRunEntity {

    @Id
    @Column(name = "\"id\"")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "\"dataSourceId\"")
    private DataSourceEntity dataSource;

    @ColumnTransformer(write = "?::\"ImportStatus\"")
    @Column(name = "\"status\"")
    private String status;
    @Column(name = "\"requestedCity\"")
    private String requestedCity;
    @Column(name = "\"receivedCount\"")
    private Integer receivedCount;
    @Column(name = "\"normalizedCount\"")
    private Integer normalizedCount;
    @Column(name = "\"publishedCount\"")
    private Integer publishedCount;
    @Column(name = "\"reviewCount\"")
    private Integer reviewCount;
    @Column(name = "\"rejectedCount\"")
    private Integer rejectedCount;
    @Column(name = "\"duplicateCount\"")
    private Integer duplicateCount;
    @Column(name = "\"errorMessage\"")
    private String errorMessage;
    @Column(name = "\"startedAt\"")
    private OffsetDateTime startedAt;
    @Column(name = "\"finishedAt\"")
    private OffsetDateTime finishedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public DataSourceEntity getDataSource() { return dataSource; }
    public void setDataSource(DataSourceEntity dataSource) { this.dataSource = dataSource; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRequestedCity() { return requestedCity; }
    public void setRequestedCity(String requestedCity) { this.requestedCity = requestedCity; }
    public Integer getReceivedCount() { return receivedCount; }
    public void setReceivedCount(Integer receivedCount) { this.receivedCount = receivedCount; }
    public Integer getNormalizedCount() { return normalizedCount; }
    public void setNormalizedCount(Integer normalizedCount) { this.normalizedCount = normalizedCount; }
    public Integer getPublishedCount() { return publishedCount; }
    public void setPublishedCount(Integer publishedCount) { this.publishedCount = publishedCount; }
    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }
    public Integer getRejectedCount() { return rejectedCount; }
    public void setRejectedCount(Integer rejectedCount) { this.rejectedCount = rejectedCount; }
    public Integer getDuplicateCount() { return duplicateCount; }
    public void setDuplicateCount(Integer duplicateCount) { this.duplicateCount = duplicateCount; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    public OffsetDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(OffsetDateTime finishedAt) { this.finishedAt = finishedAt; }
}

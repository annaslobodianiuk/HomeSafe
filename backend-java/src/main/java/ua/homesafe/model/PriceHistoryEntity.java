package ua.homesafe.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "\"PriceHistory\"")
public class PriceHistoryEntity {

    @Id
    @Column(name = "\"id\"")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "\"apartmentId\"")
    private ApartmentEntity apartment;

    @Column(name = "\"price\"")
    private Integer price;

    @Column(name = "\"recordedAt\"")
    private OffsetDateTime recordedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public ApartmentEntity getApartment() { return apartment; }
    public void setApartment(ApartmentEntity apartment) { this.apartment = apartment; }
    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }
    public OffsetDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(OffsetDateTime recordedAt) { this.recordedAt = recordedAt; }
}

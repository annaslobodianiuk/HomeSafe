package ua.homesafe.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "\"Favorite\"")
public class FavoriteEntity {

    @Id
    @Column(name = "\"id\"")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "\"userId\"")
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "\"apartmentId\"")
    private ApartmentEntity apartment;

    @Column(name = "\"createdAt\"")
    private OffsetDateTime createdAt;

    public String getId() { return id; }
    public ApartmentEntity getApartment() { return apartment; }
    public UserEntity getUser() { return user; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setId(String id) { this.id = id; }
    public void setUser(UserEntity user) { this.user = user; }
    public void setApartment(ApartmentEntity apartment) { this.apartment = apartment; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}

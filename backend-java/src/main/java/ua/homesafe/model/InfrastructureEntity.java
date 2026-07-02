package ua.homesafe.model;

import jakarta.persistence.*;

@Entity
@Table(name = "\"Infrastructure\"")
public class InfrastructureEntity {

    @Id
    @Column(name = "\"id\"")
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "\"apartmentId\"")
    private ApartmentEntity apartment;

    @Column(name = "\"schoolDistanceMeters\"")
    private Integer schoolDistanceMeters;
    @Column(name = "\"hospitalDistanceMeters\"")
    private Integer hospitalDistanceMeters;
    @Column(name = "\"transportDistanceMeters\"")
    private Integer transportDistanceMeters;
    @Column(name = "\"supermarketDistanceMeters\"")
    private Integer supermarketDistanceMeters;
    @Column(name = "\"kindergartenDistanceMeters\"")
    private Integer kindergartenDistanceMeters;

    public Integer getSchoolDistanceMeters() { return schoolDistanceMeters; }
    public Integer getHospitalDistanceMeters() { return hospitalDistanceMeters; }
    public Integer getTransportDistanceMeters() { return transportDistanceMeters; }
    public Integer getSupermarketDistanceMeters() { return supermarketDistanceMeters; }
    public Integer getKindergartenDistanceMeters() { return kindergartenDistanceMeters; }
    public ApartmentEntity getApartment() { return apartment; }
    public void setId(String id) { this.id = id; }
    public void setApartment(ApartmentEntity apartment) { this.apartment = apartment; }
    public void setSchoolDistanceMeters(Integer schoolDistanceMeters) { this.schoolDistanceMeters = schoolDistanceMeters; }
    public void setHospitalDistanceMeters(Integer hospitalDistanceMeters) { this.hospitalDistanceMeters = hospitalDistanceMeters; }
    public void setTransportDistanceMeters(Integer transportDistanceMeters) { this.transportDistanceMeters = transportDistanceMeters; }
    public void setSupermarketDistanceMeters(Integer supermarketDistanceMeters) { this.supermarketDistanceMeters = supermarketDistanceMeters; }
    public void setKindergartenDistanceMeters(Integer kindergartenDistanceMeters) { this.kindergartenDistanceMeters = kindergartenDistanceMeters; }
}

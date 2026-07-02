package ua.homesafe.controller;

import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ua.homesafe.dto.ApiResponse;
import ua.homesafe.dto.ApartmentDto;
import ua.homesafe.exception.ForbiddenException;
import ua.homesafe.exception.NotFoundException;
import ua.homesafe.model.ApartmentEntity;
import ua.homesafe.model.FavoriteEntity;
import ua.homesafe.model.UserEntity;
import ua.homesafe.repository.ApartmentRepository;
import ua.homesafe.repository.FavoriteRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteRepository favoriteRepository;
    private final ApartmentRepository apartmentRepository;

    public FavoriteController(FavoriteRepository favoriteRepository, ApartmentRepository apartmentRepository) {
        this.favoriteRepository = favoriteRepository;
        this.apartmentRepository = apartmentRepository;
    }

    @GetMapping
    @Transactional
    public ApiResponse<List<ApartmentDto>> favorites(Authentication authentication) {
        UserEntity user = (UserEntity) authentication.getPrincipal();
        requireApproved(user);

        return ApiResponse.of(
            favoriteRepository.findByUser_IdOrderByCreatedAtDesc(user.getId()).stream()
                .map(FavoriteEntity::getApartment)
                .map(ApartmentDto::from)
                .toList()
        );
    }

    @PostMapping("/{apartmentId}")
    public ApiResponse<Void> addFavorite(@PathVariable String apartmentId, Authentication authentication) {
        UserEntity user = (UserEntity) authentication.getPrincipal();
        requireApproved(user);

        favoriteRepository.findByUser_IdAndApartment_Id(user.getId(), apartmentId).ifPresent(existing -> {
            throw new ForbiddenException("Квартира вже є в обраному");
        });

        ApartmentEntity apartment = apartmentRepository.findById(apartmentId)
            .filter(item -> "PUBLISHED".equals(item.getCatalogStatus()))
            .orElseThrow(() -> new NotFoundException("Квартиру не знайдено"));

        FavoriteEntity favorite = new FavoriteEntity();
        favorite.setId(UUID.randomUUID().toString());
        favorite.setUser(user);
        favorite.setApartment(apartment);
        favorite.setCreatedAt(OffsetDateTime.now());
        favoriteRepository.save(favorite);

        return ApiResponse.of(null);
    }

    @DeleteMapping("/{apartmentId}")
    public ApiResponse<Void> removeFavorite(@PathVariable String apartmentId, Authentication authentication) {
        UserEntity user = (UserEntity) authentication.getPrincipal();
        requireApproved(user);
        favoriteRepository.deleteByUser_IdAndApartment_Id(user.getId(), apartmentId);
        return ApiResponse.of(null);
    }

    private void requireApproved(UserEntity user) {
        if (!"APPROVED".equals(user.getStatus())) {
            throw new ForbiddenException("PENDING".equals(user.getStatus())
                ? "Обліковий запис очікує схвалення адміністратора"
                : "Обліковий запис відхилено");
        }
    }
}

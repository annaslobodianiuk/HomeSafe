package ua.homesafe.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.homesafe.dto.ApiResponse;
import ua.homesafe.dto.ApartmentDto;
import ua.homesafe.service.ApartmentService;

import java.util.List;

@RestController
@RequestMapping("/api/apartments")
public class ApartmentController {

    private final ApartmentService apartmentService;

    public ApartmentController(ApartmentService apartmentService) {
        this.apartmentService = apartmentService;
    }

    @GetMapping
    public ApiResponse<List<ApartmentDto>> apartments(
        @RequestParam(required = false) String city,
        @RequestParam(required = false) String district,
        @RequestParam(required = false) String currency,
        @RequestParam(required = false) Integer rooms,
        @RequestParam(required = false) Integer minPrice,
        @RequestParam(required = false) Integer maxPrice,
        @RequestParam(required = false) Boolean withoutBroker
    ) {
        return ApiResponse.of(apartmentService.findPublished(city, district, currency, rooms, minPrice, maxPrice, withoutBroker));
    }

    @GetMapping("/{id}")
    public ApiResponse<ApartmentDto> apartment(@PathVariable String id) {
        return ApiResponse.of(apartmentService.findById(id));
    }
}

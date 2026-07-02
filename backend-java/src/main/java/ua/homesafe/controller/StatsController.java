package ua.homesafe.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.homesafe.dto.ApiResponse;
import ua.homesafe.service.ApartmentService;

import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final ApartmentService apartmentService;

    public StatsController(ApartmentService apartmentService) {
        this.apartmentService = apartmentService;
    }

    @GetMapping
    public ApiResponse<Map<String, Long>> stats() {
        return ApiResponse.of(Map.of(
            "totalListings", apartmentService.totalListings(),
            "verifiedListings", apartmentService.verifiedListings(),
            "averageSavings", apartmentService.averageSavings(),
            "supportedCities", apartmentService.supportedCities()
        ));
    }
}

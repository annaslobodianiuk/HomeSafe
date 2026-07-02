package ua.homesafe.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ua.homesafe.dto.AdminDtos;
import ua.homesafe.dto.ApiResponse;
import ua.homesafe.dto.ApartmentDto;
import ua.homesafe.dto.UserDto;
import ua.homesafe.exception.ForbiddenException;
import ua.homesafe.model.UserEntity;
import ua.homesafe.service.AdminService;
import ua.homesafe.service.ApartmentService;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final ApartmentService apartmentService;

    public AdminController(AdminService adminService, ApartmentService apartmentService) {
        this.adminService = adminService;
        this.apartmentService = apartmentService;
    }

    @GetMapping("/users")
    public ApiResponse<List<UserDto>> users(Authentication authentication) {
        requireAdmin(authentication);
        return ApiResponse.of(adminService.users());
    }

    @PatchMapping("/users/{userId}/status")
    public ApiResponse<UserDto> updateUserStatus(@PathVariable String userId, @RequestBody AdminDtos.StatusUpdateRequest request, Authentication authentication) {
        requireAdmin(authentication);
        return ApiResponse.of(adminService.updateUserStatus(userId, request.status()));
    }

    @GetMapping("/sources")
    public ApiResponse<List<AdminDtos.SourceDto>> sources(Authentication authentication) {
        requireAdmin(authentication);
        return ApiResponse.of(adminService.sources());
    }

    @PatchMapping("/sources/{code}")
    public ApiResponse<AdminDtos.SourceDto> updateSource(@PathVariable String code, @RequestBody AdminDtos.SourceUpdateRequest request, Authentication authentication) {
        requireAdmin(authentication);
        return ApiResponse.of(adminService.updateSource(code, request.enabled(), request.reliability(), request.config()));
    }

    @PostMapping("/sources/{code}/sync")
    public ApiResponse<AdminDtos.ImportRunDto> syncSource(@PathVariable String code, @RequestBody(required = false) AdminDtos.SyncRequest request, Authentication authentication) {
        requireAdmin(authentication);
        return ApiResponse.of(adminService.syncSource(code, request));
    }

    @GetMapping("/olx/auth-url")
    public ApiResponse<AdminDtos.OlxAuthUrlDto> olxAuthUrl(
        @RequestParam String redirectUri,
        @RequestParam(required = false) String state,
        Authentication authentication
    ) {
        requireAdmin(authentication);
        return ApiResponse.of(adminService.olxAuthUrl(redirectUri, state));
    }

    @PostMapping("/olx/exchange-code")
    public ApiResponse<AdminDtos.OlxTokenDto> exchangeOlxCode(@RequestBody AdminDtos.OlxCodeExchangeRequest request, Authentication authentication) {
        requireAdmin(authentication);
        return ApiResponse.of(adminService.exchangeOlxCode(request));
    }

    @GetMapping("/olx/debug")
    public ApiResponse<AdminDtos.OlxDebugDto> olxDebug(Authentication authentication) {
        requireAdmin(authentication);
        return ApiResponse.of(adminService.olxDebug());
    }

    @GetMapping("/imports")
    public ApiResponse<List<AdminDtos.ImportRunDto>> imports(@RequestParam(required = false) String code, Authentication authentication) {
        requireAdmin(authentication);
        return ApiResponse.of(adminService.imports(code));
    }

    @GetMapping("/listings/review")
    public ApiResponse<List<ApartmentDto>> review(Authentication authentication) {
        requireAdmin(authentication);
        return ApiResponse.of(apartmentService.findReviewListings());
    }

    @PatchMapping("/listings/{apartmentId}/status")
    public ApiResponse<ApartmentDto> updateListingStatus(@PathVariable String apartmentId, @RequestBody AdminDtos.StatusUpdateRequest request, Authentication authentication) {
        requireAdmin(authentication);
        return ApiResponse.of(apartmentService.updateCatalogStatus(apartmentId, request.status()));
    }

    private void requireAdmin(Authentication authentication) {
        UserEntity user = (UserEntity) authentication.getPrincipal();
        if (!"ADMIN".equals(user.getRole()) || !"APPROVED".equals(user.getStatus())) {
            throw new ForbiddenException("Доступ дозволено лише адміністратору");
        }
    }
}

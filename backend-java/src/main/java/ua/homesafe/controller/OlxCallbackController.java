package ua.homesafe.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.homesafe.service.AdminService;

import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/public/olx")
public class OlxCallbackController {

    private final AdminService adminService;
    private final ObjectMapper objectMapper;

    public OlxCallbackController(AdminService adminService, ObjectMapper objectMapper) {
        this.adminService = adminService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
        @RequestParam(required = false) String code,
        @RequestParam(required = false) String state,
        @RequestParam(required = false) String error,
        HttpServletRequest request
    ) {
        String returnTo = decodeReturnTo(state);

        if (error != null && !error.isBlank()) {
            return redirect(adminService.olxErrorRedirect(returnTo, error));
        }
        if (code == null || code.isBlank()) {
            return redirect(adminService.olxErrorRedirect(returnTo, "missing_code"));
        }

        String callbackUri = resolveExternalCallbackUri(request);
        String target = adminService.completeOlxCallback(code, callbackUri, returnTo);
        return redirect(target);
    }

    private ResponseEntity<Void> redirect(String target) {
        return ResponseEntity.status(302)
            .header(HttpHeaders.LOCATION, target)
            .build();
    }

    private String decodeReturnTo(String state) {
        if (state == null || state.isBlank()) {
            return null;
        }
        try {
            String normalized = state.replace('-', '+').replace('_', '/');
            int remainder = normalized.length() % 4;
            if (remainder > 0) {
                normalized += "=".repeat(4 - remainder);
            }
            byte[] decoded = Base64.getDecoder().decode(normalized);
            Map<String, String> payload = objectMapper.readValue(decoded, new TypeReference<>() {
            });
            String returnTo = payload.get("returnTo");
            return returnTo == null || returnTo.isBlank() ? null : returnTo;
        } catch (Exception exception) {
            return null;
        }
    }

    private String resolveExternalCallbackUri(HttpServletRequest request) {
        String forwardedProto = firstHeader(request, "X-Forwarded-Proto");
        String forwardedHost = firstHeader(request, "X-Forwarded-Host");
        String forwardedPort = firstHeader(request, "X-Forwarded-Port");

        if (forwardedProto != null && forwardedHost != null) {
            StringBuilder uri = new StringBuilder();
            uri.append(forwardedProto).append("://").append(forwardedHost);

            if (forwardedPort != null && !forwardedPort.isBlank()) {
                boolean defaultHttp = "http".equalsIgnoreCase(forwardedProto) && "80".equals(forwardedPort);
                boolean defaultHttps = "https".equalsIgnoreCase(forwardedProto) && "443".equals(forwardedPort);
                if (!defaultHttp && !defaultHttps && !forwardedHost.contains(":")) {
                    uri.append(":").append(forwardedPort);
                }
            }

            uri.append(request.getRequestURI());
            return uri.toString();
        }

        return request.getRequestURL().toString();
    }

    private String firstHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank()) {
            return null;
        }
        int commaIndex = value.indexOf(',');
        return commaIndex >= 0 ? value.substring(0, commaIndex).trim() : value.trim();
    }
}

package ua.homesafe.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ua.homesafe.service.SessionAuthService;

@Configuration
public class SeedAdminConfig {

    public static final String SEED_ADMIN_EMAIL = "admin@homesafe.local";
    public static final String SEED_ADMIN_PASSWORD = "Admin12345!";
    public static final String SEED_ADMIN_NAME = "HomeSafe Admin";

    @Bean
    CommandLineRunner seedAdminRunner(SessionAuthService sessionAuthService) {
        return args -> sessionAuthService.ensureSeedAdmin(
            SEED_ADMIN_NAME,
            SEED_ADMIN_EMAIL,
            SEED_ADMIN_PASSWORD
        );
    }
}

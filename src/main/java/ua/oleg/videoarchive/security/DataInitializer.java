package ua.oleg.videoarchive.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import ua.oleg.videoarchive.model.AppUser;
import ua.oleg.videoarchive.repository.AppUserRepository;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner initializeUsers(
            AppUserRepository repository,
            PasswordEncoder encoder,
            @Value("${app.admin.username}") String username,
            @Value("${app.admin.password}") String password) {
        return args -> {
            var all = repository.findAll();
            boolean adminFound = false;

            for (AppUser user : all) {
                if (user.getRole() == null || user.getRole().isBlank()) {
                    user.setRole(user.getUsername().equals(username) ? "ADMIN" : "USER");
                    // Documents created by the previous version had no enabled field.
                    user.setEnabled(true);
                    repository.save(user);
                }
                if (user.getUsername().equals(username)) {
                    adminFound = true;
                }
            }

            if (!adminFound) {
                AppUser user = new AppUser();
                user.setUsername(username);
                user.setPasswordHash(encoder.encode(password));
                user.setRole("ADMIN");
                user.setEnabled(true);
                user.setTotpEnabled(false);
                repository.save(user);
            }
        };
    }
}

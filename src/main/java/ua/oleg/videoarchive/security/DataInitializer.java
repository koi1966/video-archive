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
    CommandLineRunner initializeAdmin(
            AppUserRepository repository,
            PasswordEncoder encoder,
            @Value("${app.admin.username}") String username,
            @Value("${app.admin.password}") String password) {
        return args -> {
            if (repository.findByUsername(username).isEmpty()) {
                AppUser user = new AppUser();
                user.setUsername(username);
                user.setPasswordHash(encoder.encode(password));
                user.setTotpEnabled(false);
                repository.save(user);
            }
        };
    }
}

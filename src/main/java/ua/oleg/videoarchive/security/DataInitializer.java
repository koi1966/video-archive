package ua.oleg.videoarchive.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import ua.oleg.videoarchive.model.AppUser;
import ua.oleg.videoarchive.model.WorkArea;
import ua.oleg.videoarchive.repository.AppUserRepository;
import ua.oleg.videoarchive.repository.WorkAreaRepository;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initializeUsers(
            AppUserRepository repository,
            WorkAreaRepository workAreaRepository,
            PasswordEncoder encoder,
            @Value("${app.admin.username}") String username,
            @Value("${app.admin.password}") String password,
            @Value("${app.admin.reset-password:false}") boolean resetPassword) {

        return args -> {

            var all = repository.findAll();
            boolean adminFound = false;

            for (AppUser user : all) {

                boolean changed = false;

                /*
                 * Если роль отсутствует, назначаем ADMIN только
                 * пользователю с username администратора.
                 */
                if (user.getRole() == null || user.getRole().isBlank()) {
                    user.setRole(
                            user.getUsername().equals(username)
                                    ? "ADMIN"
                                    : "USER"
                    );
                    changed = true;
                }

                /*
                 * Существующий disabled статус специально
                 * не изменяем.
                 */

                if (user.getUsername().equals(username)) {

                    adminFound = true;

                    /*
                     * Одноразовый сброс пароля.
                     *
                     * ВАЖНО:
                     * Обычный запуск приложения пароль НЕ меняет.
                     */
                    if (resetPassword) {
                        user.setPasswordHash(encoder.encode(password));
                        user.setEnabled(true);

                        // Сбрасываем двухфакторную аутентификацию
                        user.setTotpEnabled(false);
                        user.setTotpSecret(null);

                        repository.save(user);

                        System.out.println(
                                "================================================="
                        );
                        System.out.println(
                                "ADMIN PASSWORD AND 2FA HAVE BEEN RESET"
                        );
                        System.out.println(
                                "Username: " + username
                        );
                        System.out.println(
                                "================================================="
                        );
                    }

                }

                if (changed) {
                    repository.save(user);
                }
            }

            /*
             * Если администратора вообще нет в базе —
             * создаём его.
             */
            if (!adminFound) {

                AppUser user = new AppUser();

                user.setUsername(username);
                user.setPasswordHash(encoder.encode(password));
                user.setRole("ADMIN");
                user.setEnabled(true);
                user.setTotpEnabled(false);

                repository.save(user);

                System.out.println(
                        "================================================="
                );
                System.out.println(
                        "ADMIN USER CREATED"
                );
                System.out.println(
                        "Username: " + username
                );
                System.out.println(
                        "================================================="
                );
            }

            initializeDefaultWorkAreas(workAreaRepository);
        };
    }

    private void initializeDefaultWorkAreas(
            WorkAreaRepository repository) {

        if (repository.count() > 0) {
            return;
        }

        String[] names = {
                "м. Житомир",
                "Житомирський район",
                "м. Бердичів",
                "Бердичівський район",
                "м. Коростень",
                "Коростенський район",
                "м. Звягель",
                "Звягельський район",
                "Барановський район",
                "Брусилівський район"
        };

        for (String name : names) {

            WorkArea area = new WorkArea();

            area.setName(name);
            area.setPatch("");

            repository.save(area);
        }
    }
}

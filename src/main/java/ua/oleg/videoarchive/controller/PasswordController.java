package ua.oleg.videoarchive.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ua.oleg.videoarchive.model.AppUser;
import ua.oleg.videoarchive.repository.AppUserRepository;

@Controller
public class PasswordController {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    /**
     * Створює контролер зміни пароля та отримує зі Spring-контейнера репозиторій користувачів і PasswordEncoder.
     * @param users параметр методу
     * @param passwordEncoder параметр методу
     */
    public PasswordController(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Виконує операцію `GetMapping` у рамках класу `PasswordController`.
     * @param form( параметр методу
     * @return результат роботи методу (@)
     */
    @GetMapping("/change-password")
    public String form() {
        return "change-password";
    }

    /**
     * Виконує операцію `PostMapping` у рамках класу `PasswordController`.
     * @param session поточна HTTP-сесія
     * @return результат роботи методу (@)
     */
    @PostMapping("/change-password")
    public String change(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Model model,
            HttpSession session) {

        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        AppUser user = users.findByUsername(username).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            model.addAttribute("error", "Текущий пароль указан неверно.");
            return "change-password";
        }

        if (newPassword == null || newPassword.length() < 8) {
            model.addAttribute("error", "Новый пароль должен содержать минимум 8 символов.");
            return "change-password";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Новый пароль и подтверждение не совпадают.");
            return "change-password";
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            model.addAttribute("error", "Новый пароль должен отличаться от текущего.");
            return "change-password";
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);

        // Require a fresh login after changing credentials.
        session.invalidate();
        SecurityContextHolder.clearContext();

        return "redirect:/login?passwordChanged";
    }
}

package ua.oleg.videoarchive.controller;

import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ua.oleg.videoarchive.model.AppUser;
import ua.oleg.videoarchive.repository.AppUserRepository;
import ua.oleg.videoarchive.security.TotpService;

import java.util.Optional;

@Controller
@RequestMapping("/users")
@Validated
public class UserController {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totp;

    public UserController(AppUserRepository users, PasswordEncoder passwordEncoder, TotpService totp) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.totp = totp;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", users.findAll());
        return "users";
    }

    @GetMapping("/new")
    public String newUser(Model model) {
        AppUser user = new AppUser();
        user.setRole("USER");
        user.setEnabled(true);
        model.addAttribute("user", user);
        return "user-form";
    }

    @PostMapping
    public String create(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam(defaultValue = "USER") String role,
            @RequestParam(defaultValue = "true") boolean enabled,
            Model model) throws Exception {

        username = username == null ? "" : username.trim();
        if (username.isBlank()) return formError(model, username, role, enabled, "Логин не может быть пустым.");
        if (!username.matches("[A-Za-z0-9._-]{3,50}")) {
            return formError(model, username, role, enabled, "Логин: 3-50 символов, только латинские буквы, цифры, '.', '_' и '-'.");
        }
        if (users.findByUsername(username).isPresent()) {
            return formError(model, username, role, enabled, "Пользователь с таким логином уже существует.");
        }
        if (password == null || password.length() < 8) {
            return formError(model, username, role, enabled, "Пароль должен содержать минимум 8 символов.");
        }
        if (!password.equals(confirmPassword)) {
            return formError(model, username, role, enabled, "Пароли не совпадают.");
        }
        if (!role.equals("USER") && !role.equals("ADMIN")) {
            return formError(model, username, role, enabled, "Недопустимая роль.");
        }

        GoogleAuthenticatorKey key = totp.createKey();
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEnabled(enabled);
        user.setTotpSecret(key.getKey());
        user.setTotpEnabled(false);
        users.save(user);

        return "redirect:/users/" + user.getId() + "/2fa-setup";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable String id, Model model) {
        AppUser user = users.findById(id).orElseThrow();
        model.addAttribute("user", user);
        return "user-form";
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable String id,
            @RequestParam String role,
            @RequestParam(defaultValue = "false") boolean enabled,
            Model model) {
        AppUser user = users.findById(id).orElseThrow();
        if (!role.equals("USER") && !role.equals("ADMIN")) {
            model.addAttribute("error", "Недопустимая роль.");
            model.addAttribute("user", user);
            return "user-form";
        }
        user.setRole(role);
        user.setEnabled(enabled);
        users.save(user);
        return "redirect:/users";
    }

    @PostMapping("/{id}/password")
    public String resetPassword(
            @PathVariable String id,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Model model) {
        AppUser user = users.findById(id).orElseThrow();
        if (newPassword == null || newPassword.length() < 8 || !newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Новый пароль должен содержать минимум 8 символов, а подтверждение должно совпадать.");
            model.addAttribute("user", user);
            return "user-form";
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);
        return "redirect:/users";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable String id) {
        AppUser user = users.findById(id).orElseThrow();
        user.setEnabled(!user.isEnabled());
        users.save(user);
        return "redirect:/users";
    }

    @GetMapping("/{id}/2fa-setup")
    public String setup2fa(@PathVariable String id, Model model) throws Exception {
        AppUser user = users.findById(id).orElseThrow();
        if (user.getTotpSecret() == null || user.getTotpSecret().isBlank()) {
            GoogleAuthenticatorKey key = totp.createKey();
            user.setTotpSecret(key.getKey());
            user.setTotpEnabled(false);
            users.save(user);
        }
        String uri = totp.otpAuthUri(user.getUsername(), user.getTotpSecret());
        model.addAttribute("user", user);
        model.addAttribute("qr", totp.qrBase64(uri));
        model.addAttribute("secret", user.getTotpSecret());
        return "user-2fa-setup";
    }

    @PostMapping("/{id}/reset-2fa")
    public String reset2fa(@PathVariable String id) {
        AppUser user = users.findById(id).orElseThrow();
        GoogleAuthenticatorKey key = totp.createKey();
        user.setTotpSecret(key.getKey());
        user.setTotpEnabled(false);
        users.save(user);
        return "redirect:/users/" + id + "/2fa-setup";
    }

    private String formError(Model model, String username, String role, boolean enabled, String error) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setRole(role);
        user.setEnabled(enabled);
        model.addAttribute("user", user);
        model.addAttribute("error", error);
        return "user-form";
    }
}

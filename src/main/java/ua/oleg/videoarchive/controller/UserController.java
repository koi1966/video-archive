package ua.oleg.videoarchive.controller;

import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ua.oleg.videoarchive.model.AppUser;
import ua.oleg.videoarchive.model.WorkArea;
import ua.oleg.videoarchive.repository.AppUserRepository;
import ua.oleg.videoarchive.repository.WorkAreaRepository;
import ua.oleg.videoarchive.security.TotpService;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/users")
public class UserController {
    private final AppUserRepository users;
    private final WorkAreaRepository workAreas;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totp;

    public UserController(AppUserRepository users, WorkAreaRepository workAreas,
                          PasswordEncoder passwordEncoder, TotpService totp) {
        this.users = users;
        this.workAreas = workAreas;
        this.passwordEncoder = passwordEncoder;
        this.totp = totp;
    }

    @GetMapping
    public String list(Model model) {
        var areas = workAreas.findAll();
        Map<String, WorkArea> workAreaById = new LinkedHashMap<>();
        areas.forEach(a -> workAreaById.put(a.getId(), a));
        model.addAttribute("users", users.findAll());
        model.addAttribute("workAreaById", workAreaById);
        return "users";
    }

    @GetMapping("/new")
    public String newUser(Model model) {
        AppUser user = new AppUser();
        user.setRole("USER");
        user.setEnabled(true);
        return showUserForm(model, user);
    }

    @PostMapping
    public String create(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam(defaultValue = "USER") String role,
            @RequestParam(defaultValue = "true") boolean enabled,
            @RequestParam(defaultValue = "") String surname,
            @RequestParam(defaultValue = "") String firstName,
            @RequestParam(defaultValue = "") String patronymic,
            @RequestParam(required = false) String workAreaId,
            Model model) throws Exception {

        AppUser formUser = buildFormUser(username, role, enabled, surname, firstName, patronymic, workAreaId);
        username = clean(username);
        if (username.isBlank()) return formError(model, formUser, "Логин не может быть пустым.");
        if (!username.matches("[A-Za-z0-9._-]{3,50}")) {
            return formError(model, formUser, "Логин: 3-50 символов, только латинские буквы, цифры, '.', '_' и '-'.");
        }
        if (users.findByUsername(username).isPresent()) {
            return formError(model, formUser, "Пользователь с таким логином уже существует.");
        }
        if (password == null || password.length() < 8) {
            return formError(model, formUser, "Пароль должен содержать минимум 8 символов.");
        }
        if (!password.equals(confirmPassword)) {
            return formError(model, formUser, "Пароли не совпадают.");
        }
        if (!validRole(role)) return formError(model, formUser, "Недопустимая роль.");
        if (!validWorkArea(workAreaId)) return formError(model, formUser, "Выберите существующий район работы.");

        GoogleAuthenticatorKey key = totp.createKey();
        AppUser user = buildFormUser(username, role, enabled, surname, firstName, patronymic, workAreaId);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setTotpSecret(key.getKey());
        user.setTotpEnabled(false);
        users.save(user);

        return "redirect:/users/" + user.getId() + "/2fa-setup";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable String id, Model model) {
        return showUserForm(model, users.findById(id).orElseThrow());
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable String id,
            @RequestParam String role,
            @RequestParam(defaultValue = "false") boolean enabled,
            @RequestParam(defaultValue = "") String surname,
            @RequestParam(defaultValue = "") String firstName,
            @RequestParam(defaultValue = "") String patronymic,
            @RequestParam(required = false) String workAreaId,
            Model model) {
        AppUser user = users.findById(id).orElseThrow();
        user.setSurname(clean(surname));
        user.setFirstName(clean(firstName));
        user.setPatronymic(clean(patronymic));
        user.setWorkAreaId(workAreaId);
        user.setRole(role);
        user.setEnabled(enabled);

        if (!validRole(role)) return formError(model, user, "Недопустимая роль.");
        if (!validWorkArea(workAreaId)) return formError(model, user, "Выберите существующий район работы.");

        users.save(user);
        return "redirect:/users";
    }

    @PostMapping("/{id}/password")
    public String resetPassword(@PathVariable String id,
                                @RequestParam String newPassword,
                                @RequestParam String confirmPassword,
                                Model model) {
        AppUser user = users.findById(id).orElseThrow();
        if (newPassword == null || newPassword.length() < 8 || !newPassword.equals(confirmPassword)) {
            return formError(model, user, "Новый пароль должен содержать минимум 8 символов, а подтверждение должно совпадать.");
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

    private String showUserForm(Model model, AppUser user) {
        model.addAttribute("user", user);
        model.addAttribute("workAreas", workAreas.findAll());
        return "user-form";
    }

    private String formError(Model model, AppUser user, String error) {
        model.addAttribute("user", user);
        model.addAttribute("workAreas", workAreas.findAll());
        model.addAttribute("error", error);
        return "user-form";
    }

    private AppUser buildFormUser(String username, String role, boolean enabled,
                                  String surname, String firstName, String patronymic, String workAreaId) {
        AppUser user = new AppUser();
        user.setUsername(clean(username));
        user.setRole(role);
        user.setEnabled(enabled);
        user.setSurname(clean(surname));
        user.setFirstName(clean(firstName));
        user.setPatronymic(clean(patronymic));
        user.setWorkAreaId(workAreaId);
        return user;
    }

    private boolean validRole(String role) {
        return "USER".equals(role) || "ADMIN".equals(role);
    }

    private boolean validWorkArea(String id) {
        return id != null && !id.isBlank() && workAreas.existsById(id);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}

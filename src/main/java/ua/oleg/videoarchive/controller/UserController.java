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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/users")
public class UserController {
    private final AppUserRepository users;
    private final WorkAreaRepository workAreas;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totp;

    /**
     * Створює контролер управления пользователями і отримує репозитории пользователей/робочих зон, PasswordEncoder і TOTP-сервіс.
     * @param users параметр методу
     * @param workAreas параметр методу
     * @param passwordEncoder параметр методу
     * @param totp параметр методу
     */
    public UserController(AppUserRepository users,
                          WorkAreaRepository workAreas,
                          PasswordEncoder passwordEncoder,
                          TotpService totp) {
        this.users = users;
        this.workAreas = workAreas;
        this.passwordEncoder = passwordEncoder;
        this.totp = totp;
    }

    /**
     * Загружает пользователей і робочі зони, сопоставляет workAreaId користувача з об’єктом WorkArea і формує рядка UserRow для таблиці.
     * @param model Model для передачи даних у Thymeleaf-шаблон
     * @return результат роботи методу (String)
     */
    @GetMapping
    public String list(Model model) {
        List<WorkArea> areas = workAreas.findAll();
        Map<String, WorkArea> areaById = new HashMap<>();
        for (WorkArea area : areas) {
            areaById.put(area.getId(), area);
        }

        List<UserRow> rows = new ArrayList<>();
        for (AppUser user : users.findAll()) {
            WorkArea area = user.getWorkAreaId() == null ? null : areaById.get(user.getWorkAreaId());
            rows.add(new UserRow(user, area));
        }

        model.addAttribute("users", rows);
        return "users";
    }

    /**
     * Виконує операцію `GetMapping` у рамках класу `UserController`.
     * @param model Model для передачи даних у Thymeleaf-шаблон
     * @return результат роботи методу (@)
     */
    @GetMapping("/new")
    public String newUser(Model model) {
        AppUser user = new AppUser();
        user.setRole("USER");
        user.setEnabled(true);
        addFormData(model, user);
        return "user-form";
    }

    /**
     * Перевіряє логін, уникальность, пароль, роль і робочу зону; хэширует пароль BCrypt, створює TOTP-секрет і зберігає нового користувача у MongoDB. Після цього переводит администратора на настройку 2FA користувача.
     * @param username логін користувача
     * @param password новый пароль під час создании користувача
     * @param confirmPassword підтвердження нового пароля
     * @param role роль користувача USER або ADMIN
     * @param enabled ознака активного облікового запису
     * @param surname прізвище користувача
     * @param firstName ім’я користувача
     * @param patronymic за батькові користувача
     * @param workAreaId ідентифікатор робочої зони користувача
     * @param model Model для передачи даних у Thymeleaf-шаблон
     * @return результат роботи методу (String)
     * @throws Exception якщо операція налаштування не може быть выполнена
     */
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
            @RequestParam(defaultValue = "") String workAreaId,
            Model model) throws Exception {

        username = username == null ? "" : username.trim();
        surname = surname == null ? "" : surname.trim();
        firstName = firstName == null ? "" : firstName.trim();
        patronymic = patronymic == null ? "" : patronymic.trim();
        workAreaId = workAreaId == null ? "" : workAreaId.trim();

        if (username.isBlank()) return formError(model, username, role, enabled, surname, firstName, patronymic, workAreaId, "Логин не может быть пустым.");
        if (!username.matches("[A-Za-z0-9._-]{3,50}")) {
            return formError(model, username, role, enabled, surname, firstName, patronymic, workAreaId, "Логин: 3-50 символов, только латинские буквы, цифры, '.', '_' и '-'.");
        }
        if (users.findByUsername(username).isPresent()) {
            return formError(model, username, role, enabled, surname, firstName, patronymic, workAreaId, "Пользователь с таким логином уже существует.");
        }
        if (password == null || password.length() < 8) {
            return formError(model, username, role, enabled, surname, firstName, patronymic, workAreaId, "Пароль должен содержать минимум 8 символов.");
        }
        if (!password.equals(confirmPassword)) {
            return formError(model, username, role, enabled, surname, firstName, patronymic, workAreaId, "Пароли не совпадают.");
        }
        if (!role.equals("USER") && !role.equals("ADMIN")) {
            return formError(model, username, role, enabled, surname, firstName, patronymic, workAreaId, "Недопустимая роль.");
        }
        if (!workAreaId.isBlank() && !workAreas.existsById(workAreaId)) {
            return formError(model, username, role, enabled, surname, firstName, patronymic, workAreaId, "Выбранный район работы не существует.");
        }

        GoogleAuthenticatorKey key = totp.createKey();
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEnabled(enabled);
        user.setSurname(surname);
        user.setFirstName(firstName);
        user.setPatronymic(patronymic);
        user.setWorkAreaId(workAreaId.isBlank() ? null : workAreaId);
        user.setTotpSecret(key.getKey());
        user.setTotpEnabled(false);
        users.save(user);

        return "redirect:/users/" + user.getId() + "/2fa-setup";
    }

    /**
     * Загружает существующего користувача і связанные дані робочих зон для формы редактирования.
     * @param id ідентифікатор об’єкта
     * @param model Model для передачи даних у Thymeleaf-шаблон
     * @return результат роботи методу (String)
     */
    @GetMapping("/{id}/edit")
    public String edit(@PathVariable String id, Model model) {
        AppUser user = users.findById(id).orElseThrow();
        addFormData(model, user);
        return "user-form";
    }

    /**
     * Изменяет профильные поля користувача: роль, enabled, фамилию, ім’я, за батькові і робочу зону. Пароль і TOTP-секрет під час обычном редактировании не змінюються.
     * @param id ідентифікатор об’єкта
     * @param role роль користувача USER або ADMIN
     * @param enabled ознака активного облікового запису
     * @param surname прізвище користувача
     * @param firstName ім’я користувача
     * @param patronymic за батькові користувача
     * @param workAreaId ідентифікатор робочої зони користувача
     * @param model Model для передачи даних у Thymeleaf-шаблон
     * @return результат роботи методу (String)
     */
    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable String id,
            @RequestParam String role,
            @RequestParam(defaultValue = "false") boolean enabled,
            @RequestParam(defaultValue = "") String surname,
            @RequestParam(defaultValue = "") String firstName,
            @RequestParam(defaultValue = "") String patronymic,
            @RequestParam(defaultValue = "") String workAreaId,
            Model model) {
        AppUser user = users.findById(id).orElseThrow();
        surname = surname == null ? "" : surname.trim();
        firstName = firstName == null ? "" : firstName.trim();
        patronymic = patronymic == null ? "" : patronymic.trim();
        workAreaId = workAreaId == null ? "" : workAreaId.trim();

        if (!role.equals("USER") && !role.equals("ADMIN")) {
            model.addAttribute("error", "Недопустимая роль.");
            addFormData(model, user);
            return "user-form";
        }
        if (!workAreaId.isBlank() && !workAreas.existsById(workAreaId)) {
            model.addAttribute("error", "Выбранный район работы не существует.");
            addFormData(model, user);
            return "user-form";
        }

        user.setRole(role);
        user.setEnabled(enabled);
        user.setSurname(surname);
        user.setFirstName(firstName);
        user.setPatronymic(patronymic);
        user.setWorkAreaId(workAreaId.isBlank() ? null : workAreaId);
        users.save(user);
        return "redirect:/users";
    }

    /**
     * Встановлює пользователю новый пароль після перевірки длины і подтверждения, сохраняя лише его BCrypt-хеш.
     * @param id ідентифікатор об’єкта
     * @param newPassword новый пароль користувача
     * @param confirmPassword підтвердження нового пароля
     * @param model Model для передачи даних у Thymeleaf-шаблон
     * @return результат роботи методу (String)
     */
    @PostMapping("/{id}/password")
    public String resetPassword(
            @PathVariable String id,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Model model) {
        AppUser user = users.findById(id).orElseThrow();
        if (newPassword == null || newPassword.length() < 8 || !newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Новый пароль должен содержать минимум 8 символов, а подтверждение должно совпадать.");
            addFormData(model, user);
            return "user-form";
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);
        return "redirect:/users";
    }

    /**
     * Перемикає стан enabled користувача між true і false.
     * @param id ідентифікатор об’єкта
     * @return результат роботи методу (String)
     */
    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable String id) {
        AppUser user = users.findById(id).orElseThrow();
        user.setEnabled(!user.isEnabled());
        users.save(user);
        return "redirect:/users";
    }

    /**
     * Показує QR-код і секрет для первинного налаштування Google Authenticator вибраного користувача.
     * @param id ідентифікатор об’єкта
     * @param model Model для передачи даних у Thymeleaf-шаблон
     * @throws Exception якщо операція налаштування не може быть выполнена
     */
    @GetMapping("/{id}/2fa-setup")
    public String setup2fa(@PathVariable String id, Model model) throws Exception {
        AppUser user = users.findById(id).orElseThrow();
        boolean changed = false;

        if (user.getTotpSecret() == null || user.getTotpSecret().isBlank()) {
            GoogleAuthenticatorKey key = totp.createKey();
            user.setTotpSecret(key.getKey());
            user.setTotpEnabled(false);
            changed = true;
        }

        if (changed) {
            users.save(user);
        }

        String uri = totp.otpAuthUri(user.getUsername(), user.getTotpSecret());
        model.addAttribute("user", user);
        model.addAttribute("qr", totp.qrBase64(uri));
        model.addAttribute("secret", user.getTotpSecret());
        return "user-2fa-setup";
    }

    /**
     * Генерує новий TOTP-секрет, вимикає старий TOTP та зберігає новий секрет для користувача.
     * @param id ідентифікатор об’єкта
     * @return результат роботи методу (String)
     */
    @PostMapping("/{id}/reset-2fa")
    public String reset2fa(@PathVariable String id) {
        AppUser user = users.findById(id).orElseThrow();
        GoogleAuthenticatorKey key = totp.createKey();
        user.setTotpSecret(key.getKey());
        user.setTotpEnabled(false);
        user.setFailedTwoFactorAttempts(0);
        user.setTwoFactorWindowStart(null);
        user.setTwoFactorBlockUntil(null);
        users.save(user);
        return "redirect:/users/" + id + "/2fa-setup";
    }

    /**
     * Заповнює Model даними, необхідними для user-form.html: користувача і список робочих зон.
     * @param model Model для передачи даних у Thymeleaf-шаблон
     * @param user параметр методу
     */
    private void addFormData(Model model, AppUser user) {
        model.addAttribute("user", user);
        model.addAttribute("workAreas", workAreas.findAll());
    }

    /**
     * Повертає форму користувача з введеними значеннями та повідомленням про помилку, щоб адміністратору не довелося вводити дані повторно.
     * @param model Model для передачи даних у Thymeleaf-шаблон
     * @param username логін користувача
     * @param role роль користувача USER або ADMIN
     * @param enabled ознака активного облікового запису
     * @param surname прізвище користувача
     * @param firstName ім’я користувача
     * @param patronymic за батькові користувача
     * @param workAreaId ідентифікатор робочої зони користувача
     * @param error параметр методу
     * @return результат роботи методу (String)
     */
    private String formError(Model model, String username, String role, boolean enabled,
                             String surname, String firstName, String patronymic,
                             String workAreaId, String error) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setRole(role);
        user.setEnabled(enabled);
        user.setSurname(surname);
        user.setFirstName(firstName);
        user.setPatronymic(patronymic);
        user.setWorkAreaId(workAreaId.isBlank() ? null : workAreaId);
        addFormData(model, user);
        model.addAttribute("error", error);
        return "user-form";
    }

    public static class UserRow {
        private final AppUser user;
        private final WorkArea workArea;

    /**
     * Створює представлення користувача для таблиці users та пов’язує користувача з об’єктом WorkArea.
     * @param user параметр методу
     * @param workArea робоча зона, у каталог яку записується файл
     */
        public UserRow(AppUser user, WorkArea workArea) {
            this.user = user;
            this.workArea = workArea;
        }

    /**
     * Повертає ідентифікатор об’єкта.
     * @param user.getId( параметр методу
     * @return результат роботи методу (String)
     */
        public String getId() { return user.getId(); }
    /**
     * Повертає логін користувача.
     * @param user.getUsername( параметр методу
     * @return результат роботи методу (String)
     */
        public String getUsername() { return user.getUsername(); }
    /**
     * Повертає фамилию користувача.
     * @param user.getSurname( параметр методу
     * @return результат роботи методу (String)
     */
        public String getSurname() { return user.getSurname(); }
    /**
     * Повертає ім’я користувача.
     * @param user.getFirstName( параметр методу
     * @return результат роботи методу (String)
     */
        public String getFirstName() { return user.getFirstName(); }
    /**
     * Повертає за батькові користувача.
     * @param user.getPatronymic( параметр методу
     * @return результат роботи методу (String)
     */
        public String getPatronymic() { return user.getPatronymic(); }
    /**
     * Повертає роль користувача.
     * @param user.getRole( параметр методу
     * @return результат роботи методу (String)
     */
        public String getRole() { return user.getRole(); }
    /**
     * Повертає ознака завершеного налаштування TOTP.
     * @param user.isTotpEnabled( параметр методу
     * @return результат роботи методу (boolean)
     */
        public boolean isTotpEnabled() { return user.isTotpEnabled(); }
    /**
     * Повертає ознака активного облікового запису.
     * @param user.isEnabled( параметр методу
     * @return результат роботи методу (boolean)
     */
        public boolean isEnabled() { return user.isEnabled(); }
    /**
     * Повертає назва робочої зони користувача або текст «Не выбран», якщо зона отсутствует.
     * @param workArea.getName( параметр методу
     * @return результат роботи методу (String)
     */
        public String getWorkAreaName() { return workArea == null ? "Не выбран" : workArea.getName(); }
    /**
     * Повертає patch робочої зони або пустую рядок, якщо робоча зона або patch не заданы.
     * @param workArea.getPatch( параметр методу
     * @return результат роботи методу (String)
     */
        public String getPatch() { return workArea == null || workArea.getPatch() == null ? "" : workArea.getPatch(); }
    }
}

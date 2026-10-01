package ua.oleg.videoarchive.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("users")
public class AppUser {
    @Id
    private String id;
    private String username;
    private String passwordHash;
    private String role = "USER";
    private boolean enabled = true;
    private String surname;
    private String firstName;
    private String patronymic;
    private String workAreaId;
    private String totpSecret;
    private boolean totpEnabled;


    /** Persistent 2FA brute-force protection. */
    private int failedTwoFactorAttempts;
    private Instant twoFactorWindowStart;
    private Instant twoFactorBlockUntil;

    /**
     * Повертає ідентифікатор об’єкта.
     * @return результат роботи методу (String)
     */
    public String getId() { return id; }
    /**
     * Встановлює ідентифікатор об’єкта.
     * @param id ідентифікатор об’єкта
     */
    public void setId(String id) { this.id = id; }
    /**
     * Повертає логін користувача.
     * @return результат роботи методу (String)
     */
    public String getUsername() { return username; }
    /**
     * Встановлює логін користувача.
     * @param username логін користувача
     */
    public void setUsername(String username) { this.username = username; }
    /**
     * Повертає BCrypt-хеш пароля.
     * @return результат роботи методу (String)
     */
    public String getPasswordHash() { return passwordHash; }
    /**
     * Встановлює BCrypt-хеш пароля.
     * @param passwordHash параметр методу
     */
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    /**
     * Повертає роль користувача.
     * @return результат роботи методу (String)
     */
    public String getRole() { return role; }
    /**
     * Встановлює роль користувача.
     * @param role роль користувача USER або ADMIN
     */
    public void setRole(String role) { this.role = role; }
    /**
     * Повертає ознака активного облікового запису.
     * @return результат роботи методу (boolean)
     */
    public boolean isEnabled() { return enabled; }
    /**
     * Встановлює ознака активного облікового запису.
     * @param enabled ознака активного облікового запису
     */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    /**
     * Повертає фамилию користувача.
     * @return результат роботи методу (String)
     */
    public String getSurname() { return surname; }
    /**
     * Встановлює фамилию користувача.
     * @param surname прізвище користувача
     */
    public void setSurname(String surname) { this.surname = surname; }
    /**
     * Повертає ім’я користувача.
     * @return результат роботи методу (String)
     */
    public String getFirstName() { return firstName; }
    /**
     * Встановлює ім’я користувача.
     * @param firstName ім’я користувача
     */
    public void setFirstName(String firstName) { this.firstName = firstName; }
    /**
     * Повертає за батькові користувача.
     * @return результат роботи методу (String)
     */
    public String getPatronymic() { return patronymic; }
    /**
     * Встановлює за батькові користувача.
     * @param patronymic за батькові користувача
     */
    public void setPatronymic(String patronymic) { this.patronymic = patronymic; }
    /**
     * Повертає ідентифікатор робочої зони.
     * @return результат роботи методу (String)
     */
    public String getWorkAreaId() { return workAreaId; }
    /**
     * Встановлює ідентифікатор робочої зони.
     * @param workAreaId ідентифікатор робочої зони користувача
     */
    public void setWorkAreaId(String workAreaId) { this.workAreaId = workAreaId; }
    /**
     * Повертає секрет TOTP користувача.
     * @return результат роботи методу (String)
     */
    public String getTotpSecret() { return totpSecret; }
    /**
     * Встановлює секрет TOTP користувача.
     * @param totpSecret параметр методу
     */
    public void setTotpSecret(String totpSecret) { this.totpSecret = totpSecret; }
    /**
     * Повертає ознака завершеного налаштування TOTP.
     * @return результат роботи методу (boolean)
     */
    public boolean isTotpEnabled() { return totpEnabled; }
    /**
     * Встановлює ознака завершеного налаштування TOTP.
     * @param totpEnabled параметр методу
     */
    public void setTotpEnabled(boolean totpEnabled) { this.totpEnabled = totpEnabled; }


    /**
     * Повертає кількість невдалих спроб TOTP у текущем окне.
     * @return результат роботи методу (int)
     */
    public int getFailedTwoFactorAttempts() { return failedTwoFactorAttempts; }
    /**
     * Встановлює кількість невдалих спроб TOTP.
     * @param failedTwoFactorAttempts параметр методу
     */
    public void setFailedTwoFactorAttempts(int failedTwoFactorAttempts) { this.failedTwoFactorAttempts = failedTwoFactorAttempts; }
    /**
     * Повертає момент начала поточного окна подсчёта помилок 2FA.
     * @return результат роботи методу (Instant)
     */
    public Instant getTwoFactorWindowStart() { return twoFactorWindowStart; }
    /**
     * Встановлює момент начала окна помилок 2FA.
     * @param twoFactorWindowStart параметр методу
     */
    public void setTwoFactorWindowStart(Instant twoFactorWindowStart) { this.twoFactorWindowStart = twoFactorWindowStart; }
    /**
     * Повертає момент окончания блокування TOTP.
     * @return результат роботи методу (Instant)
     */
    public Instant getTwoFactorBlockUntil() { return twoFactorBlockUntil; }
    /**
     * Встановлює момент окончания блокування TOTP.
     * @param twoFactorBlockUntil параметр методу
     */
    public void setTwoFactorBlockUntil(Instant twoFactorBlockUntil) { this.twoFactorBlockUntil = twoFactorBlockUntil; }
}

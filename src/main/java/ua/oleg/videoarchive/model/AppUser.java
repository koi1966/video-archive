package ua.oleg.videoarchive.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

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

    /** BCrypt hashes of one-time 8-digit recovery codes. */
    private Set<String> backupCodes = new HashSet<>();

    /** Persistent 2FA brute-force protection. */
    private int failedTwoFactorAttempts;
    private Instant twoFactorWindowStart;
    private Instant twoFactorBlockUntil;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getPatronymic() { return patronymic; }
    public void setPatronymic(String patronymic) { this.patronymic = patronymic; }
    public String getWorkAreaId() { return workAreaId; }
    public void setWorkAreaId(String workAreaId) { this.workAreaId = workAreaId; }
    public String getTotpSecret() { return totpSecret; }
    public void setTotpSecret(String totpSecret) { this.totpSecret = totpSecret; }
    public boolean isTotpEnabled() { return totpEnabled; }
    public void setTotpEnabled(boolean totpEnabled) { this.totpEnabled = totpEnabled; }

    public Set<String> getBackupCodes() {
        if (backupCodes == null) {
            backupCodes = new HashSet<>();
        }
        return backupCodes;
    }

    public void setBackupCodes(Set<String> backupCodes) {
        this.backupCodes = backupCodes == null ? new HashSet<>() : backupCodes;
    }

    public int getFailedTwoFactorAttempts() { return failedTwoFactorAttempts; }
    public void setFailedTwoFactorAttempts(int failedTwoFactorAttempts) { this.failedTwoFactorAttempts = failedTwoFactorAttempts; }
    public Instant getTwoFactorWindowStart() { return twoFactorWindowStart; }
    public void setTwoFactorWindowStart(Instant twoFactorWindowStart) { this.twoFactorWindowStart = twoFactorWindowStart; }
    public Instant getTwoFactorBlockUntil() { return twoFactorBlockUntil; }
    public void setTwoFactorBlockUntil(Instant twoFactorBlockUntil) { this.twoFactorBlockUntil = twoFactorBlockUntil; }
}
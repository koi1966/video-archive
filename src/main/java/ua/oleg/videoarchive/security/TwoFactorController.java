package ua.oleg.videoarchive.security;

import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ua.oleg.videoarchive.model.AppUser;
import ua.oleg.videoarchive.repository.AppUserRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class TwoFactorController {
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration ATTEMPT_WINDOW = Duration.ofMinutes(10);
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

    private static final String AUTH_USER = "AUTH_USER";
    private static final String TWO_FACTOR_OK = "TWO_FACTOR_OK";

    private final AppUserRepository users;
    private final TotpService totp;
    private final ConcurrentHashMap<String, Object> userLocks = new ConcurrentHashMap<>();

    public TwoFactorController(AppUserRepository users,
                               TotpService totp) {
        this.users = users;
        this.totp = totp;
    }

    @GetMapping("/2fa")
    public String twoFactor(HttpSession session, Model model) {
        String username = resolveUsername(session);
        if (username == null) {
            return "redirect:/login";
        }

        AppUser user = users.findByUsername(username).orElse(null);
        if (user == null || !user.isEnabled()) {
            return "redirect:/login";
        }

        Instant now = Instant.now();
        synchronized (lockFor(username)) {
            if (isBlocked(user, now)) {
                addBlockInfo(model, user, now);
                return "otp";
            }

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
        }

        try {
            String uri = totp.otpAuthUri(username, user.getTotpSecret());
            if (!user.isTotpEnabled()) {
                model.addAttribute("qr", totp.qrBase64(uri));
                model.addAttribute("secret", user.getTotpSecret());
                model.addAttribute("setup", true);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create 2FA QR", e);
        }

        return "otp";
    }

    @PostMapping("/2fa")
    public String verify(@RequestParam String code, HttpSession session) {
        String username = resolveUsername(session);
        if (username == null) return "redirect:/login";

        AppUser user = users.findByUsername(username).orElse(null);
        if (user == null || !user.isEnabled()) return "redirect:/login";

        Instant now = Instant.now();
        synchronized (lockFor(username)) {
            if (isBlocked(user, now)) {
                return "redirect:/2fa?blocked";
            }

            resetExpiredWindow(user, now);

            String normalized = code == null ? "" : code.replaceAll("\\s+", "").trim();
            boolean success = false;

            if (normalized.matches("\\d{6}")) {
                try {
                    success = user.getTotpSecret() != null
                            && totp.verify(user.getTotpSecret(), Integer.parseInt(normalized));
                } catch (NumberFormatException ignored) {
                    success = false;
                }
            }

            if (success) {
                user.setTotpEnabled(true);
                clearTwoFactorFailures(user);
                users.save(user);
                session.setAttribute(TWO_FACTOR_OK, true);
                return "redirect:/";
            }

            int attemptsLeft = registerFailedAttempt(user, now);
            users.save(user);

            if (isBlocked(user, now)) {
                return "redirect:/2fa?blocked";
            }
            return "redirect:/2fa?error&attemptsLeft=" + attemptsLeft;
        }
    }

    private int registerFailedAttempt(AppUser user, Instant now) {
        if (user.getTwoFactorWindowStart() == null
                || now.isAfter(user.getTwoFactorWindowStart().plus(ATTEMPT_WINDOW))) {
            user.setTwoFactorWindowStart(now);
            user.setFailedTwoFactorAttempts(0);
        }

        int count = user.getFailedTwoFactorAttempts() + 1;
        user.setFailedTwoFactorAttempts(count);

        if (count >= MAX_ATTEMPTS) {
            user.setTwoFactorBlockUntil(now.plus(BLOCK_DURATION));
            return 0;
        }
        return MAX_ATTEMPTS - count;
    }

    private void resetExpiredWindow(AppUser user, Instant now) {
        if (user.getTwoFactorWindowStart() != null
                && now.isAfter(user.getTwoFactorWindowStart().plus(ATTEMPT_WINDOW))) {
            user.setFailedTwoFactorAttempts(0);
            user.setTwoFactorWindowStart(null);
        }

        if (user.getTwoFactorBlockUntil() != null
                && !now.isBefore(user.getTwoFactorBlockUntil())) {
            user.setTwoFactorBlockUntil(null);
            user.setFailedTwoFactorAttempts(0);
            user.setTwoFactorWindowStart(null);
        }
    }

    private boolean isBlocked(AppUser user, Instant now) {
        return user.getTwoFactorBlockUntil() != null
                && now.isBefore(user.getTwoFactorBlockUntil());
    }

    private void clearTwoFactorFailures(AppUser user) {
        user.setFailedTwoFactorAttempts(0);
        user.setTwoFactorWindowStart(null);
        user.setTwoFactorBlockUntil(null);
    }

    private void addBlockInfo(Model model, AppUser user, Instant now) {
        model.addAttribute("blocked", true);
        model.addAttribute("secondsLeft",
                Math.max(1, Duration.between(now, user.getTwoFactorBlockUntil()).toSeconds()));
    }

    private String resolveUsername(HttpSession session) {
        String username = (String) session.getAttribute(AUTH_USER);
        if (username != null) return username;

        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return null;

        username = authentication.getName();
        session.setAttribute(AUTH_USER, username);
        return username;
    }

    private Object lockFor(String username) {
        return userLocks.computeIfAbsent(username, ignored -> new Object());
    }
}

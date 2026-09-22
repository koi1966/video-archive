package ua.oleg.videoarchive.security;

import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import ua.oleg.videoarchive.model.AppUser;
import ua.oleg.videoarchive.repository.AppUserRepository;

import java.time.Duration;
import java.time.Instant;

@Controller
public class TwoFactorController {
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration ATTEMPT_WINDOW = Duration.ofMinutes(10);
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

    private static final String AUTH_USER = "AUTH_USER";
    private static final String TWO_FACTOR_OK = "TWO_FACTOR_OK";
    private static final String FAIL_COUNT = "TOTP_FAIL_COUNT";
    private static final String WINDOW_START = "TOTP_WINDOW_START";
    private static final String BLOCK_UNTIL = "TOTP_BLOCK_UNTIL";

    private final AppUserRepository users;
    private final TotpService totp;

    public TwoFactorController(AppUserRepository users, TotpService totp) {
        this.users = users;
        this.totp = totp;
    }

    @GetMapping("/2fa")
    public String twoFactor(HttpSession session, Model model) {
        String username = (String) session.getAttribute(AUTH_USER);

        if (username == null) {
            var authentication = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return "redirect:/login";
            }
            username = authentication.getName();
            session.setAttribute(AUTH_USER, username);
        }

        AppUser user = users.findByUsername(username).orElse(null);
        if (user == null) return "redirect:/login";

        Instant blockUntil = getInstant(session, BLOCK_UNTIL);
        if (blockUntil != null && Instant.now().isBefore(blockUntil)) {
            model.addAttribute("blocked", true);
            model.addAttribute("secondsLeft",
                    Math.max(1, Duration.between(Instant.now(), blockUntil).toSeconds()));
            return "otp";
        }

        if (!user.isTotpEnabled() && (user.getTotpSecret() == null || user.getTotpSecret().isBlank())) {
            try {
                GoogleAuthenticatorKey key = totp.createKey();
                user.setTotpSecret(key.getKey());
                users.save(user);

                String uri = totp.otpAuthUri(username, key.getKey());
                model.addAttribute("qr", totp.qrBase64(uri));
                model.addAttribute("secret", key.getKey());
                model.addAttribute("setup", true);
            } catch (Exception e) {
                throw new IllegalStateException("Cannot create 2FA QR", e);
            }
        }

        return "otp";
    }

    @PostMapping("/2fa")
    public String verify(@RequestParam String code, HttpSession session, Model model) {
        String username = (String) session.getAttribute(AUTH_USER);
        if (username == null) return "redirect:/login";

        Instant now = Instant.now();
        Instant blockUntil = getInstant(session, BLOCK_UNTIL);

        if (blockUntil != null && now.isBefore(blockUntil)) {
            return "redirect:/2fa?blocked";
        }

        AppUser user = users.findByUsername(username).orElse(null);
        if (user == null) return "redirect:/login";

        resetExpiredWindow(session, now);

        try {
            if (code == null || !code.matches("\\d{6}")) {
                return failedAttempt(session, now);
            }

            int value = Integer.parseInt(code);

            if (totp.verify(user.getTotpSecret(), value)) {
                user.setTotpEnabled(true);
                users.save(user);

                session.removeAttribute(FAIL_COUNT);
                session.removeAttribute(WINDOW_START);
                session.removeAttribute(BLOCK_UNTIL);
                session.setAttribute(TWO_FACTOR_OK, true);

                return "redirect:/";
            }
        } catch (NumberFormatException ignored) {
        }

        return failedAttempt(session, now);
    }

    private String failedAttempt(HttpSession session, Instant now) {
        int count = getInt(session, FAIL_COUNT) + 1;
        session.setAttribute(FAIL_COUNT, count);

        if (session.getAttribute(WINDOW_START) == null) {
            session.setAttribute(WINDOW_START, now.toString());
        }

        if (count >= MAX_ATTEMPTS) {
            Instant until = now.plus(BLOCK_DURATION);
            session.setAttribute(BLOCK_UNTIL, until.toString());
            session.removeAttribute(FAIL_COUNT);
            session.removeAttribute(WINDOW_START);
            return "redirect:/2fa?blocked";
        }

        return "redirect:/2fa?error&attemptsLeft=" + (MAX_ATTEMPTS - count);
    }

    private void resetExpiredWindow(HttpSession session, Instant now) {
        Instant start = getInstant(session, WINDOW_START);
        if (start != null && now.isAfter(start.plus(ATTEMPT_WINDOW))) {
            session.removeAttribute(FAIL_COUNT);
            session.removeAttribute(WINDOW_START);
        }
    }

    private int getInt(HttpSession session, String name) {
        Object value = session.getAttribute(name);
        return value instanceof Integer i ? i : 0;
    }

    private Instant getInstant(HttpSession session, String name) {
        Object value = session.getAttribute(name);
        if (value instanceof String s) {
            try {
                return Instant.parse(s);
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}

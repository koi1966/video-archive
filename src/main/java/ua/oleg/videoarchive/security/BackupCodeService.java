package ua.oleg.videoarchive.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates and verifies one-time recovery codes for two-factor authentication.
 * Only BCrypt hashes are stored in MongoDB; raw codes are returned once to the caller.
 */
@Service
public class BackupCodeService {
    private static final int CODE_COUNT = 8;
    private static final int CODE_LENGTH = 8;

    private final SecureRandom random = new SecureRandom();
    private final PasswordEncoder passwordEncoder;

    public BackupCodeService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public List<String> generateRawCodes() {
        List<String> codes = new ArrayList<>(CODE_COUNT);
        for (int i = 0; i < CODE_COUNT; i++) {
            codes.add(generateCode());
        }
        return codes;
    }

    public Set<String> hashCodes(List<String> rawCodes) {
        Set<String> hashes = new HashSet<>();
        for (String code : rawCodes) {
            hashes.add(passwordEncoder.encode(code));
        }
        return hashes;
    }

    /**
     * Verifies a recovery code and removes the matching hash when successful.
     */
    public boolean verifyAndConsume(String rawCode, Set<String> storedHashes) {
        if (rawCode == null || storedHashes == null || storedHashes.isEmpty()) {
            return false;
        }

        String normalized = normalize(rawCode);
        if (!normalized.matches("\\d{8}")) {
            return false;
        }

        for (String hash : new ArrayList<>(storedHashes)) {
            if (passwordEncoder.matches(normalized, hash)) {
                storedHashes.remove(hash);
                return true;
            }
        }
        return false;
    }

    public String normalize(String code) {
        if (code == null) {
            return "";
        }
        return code.replace("-", "").replaceAll("\\s+", "").trim();
    }

    public String displayCode(String code) {
        String normalized = normalize(code);
        if (normalized.length() != CODE_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, 4) + "-" + normalized.substring(4);
    }

    private String generateCode() {
        int value = random.nextInt(100_000_000);
        return String.format("%08d", value);
    }
}
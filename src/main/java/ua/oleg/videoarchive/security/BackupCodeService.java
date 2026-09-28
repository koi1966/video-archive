package ua.oleg.videoarchive.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BackupCodeService {
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public BackupCodeService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Генерирует 8 сырых текстовых кодов, состоящих из 8 цифр.
     */
    public List<String> generateRawCodes() {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            int num = random.nextInt(90000000) + 10000000; // Строго 8 цифр
            codes.add(String.valueOf(num));
        }
        return codes;
    }

    /**
     * Хэширует коды с помощью BCrypt для безопасного сохранения в БД.
     */
    public Set<String> hashCodes(List<String> rawCodes) {
        return rawCodes.stream()
                .map(passwordEncoder::encode)
                .collect(Collectors.toSet());
    }

    /**
     * Сверяет введенный пользователем сырой код со списком хэшей в БД.
     * Если код совпал, он удаляется из коллекции (принцип одноразовости).
     */
    public boolean verifyAndConsume(Set<String> hashedCodes, String rawInputCode) {
        for (String hashedCode : hashedCodes) {
            if (passwordEncoder.matches(rawInputCode, hashedCode)) {
                hashedCodes.remove(hashedCode);
                return true;
            }
        }
        return false;
    }
}

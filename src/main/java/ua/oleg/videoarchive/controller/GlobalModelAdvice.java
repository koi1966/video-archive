package ua.oleg.videoarchive.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {
    /**
     * Виконує операцію `ModelAttribute` у рамках класу `GlobalModelAdvice`.
     * @param authentication дані поточної автентифікації
     * @return результат роботи методу (@)
     */
    @ModelAttribute("isAdmin")
    public boolean isAdmin(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}

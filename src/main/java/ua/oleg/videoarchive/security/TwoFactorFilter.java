package ua.oleg.videoarchive.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class TwoFactorFilter extends OncePerRequestFilter {

    private static final String TWO_FACTOR_OK = "TWO_FACTOR_OK";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Если пользователь уже аутентифицирован (прошел форму логина)
        if (authentication != null && authentication.isAuthenticated() &&
                !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {

            HttpSession session = request.getSession(false);
            boolean is2faConfirmed = (session != null && session.getAttribute(TWO_FACTOR_OK) != null);

            // Исключаем из проверки саму страницу 2FA, логаут и статические ресурсы
            boolean isAllowedPage = requestURI.equals("/2fa") ||
                    requestURI.equals("/logout") ||
                    requestURI.startsWith("/css/") ||
                    requestURI.startsWith("/js/");

            // Если 2FA не пройден и страница защищенная — принудительный редирект
            if (!is2faConfirmed && !isAllowedPage) {
                response.sendRedirect("/2fa");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}

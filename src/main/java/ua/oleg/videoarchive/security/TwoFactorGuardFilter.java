package ua.oleg.videoarchive.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TwoFactorGuardFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();

        // Добавлен /logout в исключения для предотвращения вечного редиректа при выходе
        if (path.equals("/login") || path.equals("/2fa") || path.equals("/logout")
                || path.startsWith("/css/") || path.startsWith("/js/")) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        boolean ok = session != null && Boolean.TRUE.equals(session.getAttribute("TWO_FACTOR_OK"));

        if (!ok && request.getUserPrincipal() != null) {
            // Мягкая обработка фоновых REST-запросов (если фронтенд использует JS-асинхронность)
            String accept = request.getHeader("Accept");
            if (accept != null && accept.contains("application/json")) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "2FA Required");
                return;
            }

            response.sendRedirect("/2fa");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
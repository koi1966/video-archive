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
    /**
     * Контролює другий етап автентифікації після успішної перевірки логіна та пароля. Якщо користувач ще не встановив прапорець TWO_FACTOR_OK у HTTP-сесії, звичайні запити перенаправляються на `/2fa`; AJAX-запити отримують HTTP 403. Публічні службові шляхи пропускаються.
     * @param request HTTP-запрос
     * @param response HTTP-ответ
     * @param filterChain ланцюжок Spring Security-фильтров
     * @throws ServletException якщо обробка servlet-фільтра завершилась помилкою
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();

        if (path.equals("/login") || path.equals("/2fa")
                || path.startsWith("/css/") || path.startsWith("/js/")) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        boolean ok = session != null && Boolean.TRUE.equals(session.getAttribute("TWO_FACTOR_OK"));

        if (!ok && request.getUserPrincipal() != null) {
            response.sendRedirect("/2fa");
            return;
        }

        filterChain.doFilter(request, response);
    }
}

package ua.oleg.videoarchive.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final TwoFactorGuardFilter twoFactorGuardFilter;

    public SecurityConfig(TwoFactorGuardFilter twoFactorGuardFilter) {
        this.twoFactorGuardFilter = twoFactorGuardFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/2fa", "/logout", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/users/**", "/work-areas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/records/*/videos/*/delete", "/records/*/delete").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                // Подключаем защитный фильтр сразу после обработки базовой формы логина
                .addFilterAfter(twoFactorGuardFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/2fa", true)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
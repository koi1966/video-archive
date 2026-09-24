package ua.oleg.videoarchive.security;

import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import ua.oleg.videoarchive.repository.AppUserRepository;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final AppUserRepository repository;

    public DatabaseUserDetailsService(AppUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = repository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));

        String role = user.getRole();
        if (role == null || role.isBlank()) {
            role = "USER";
        }
        if (role.startsWith("ROLE_")) {
            role = role.substring(5);
        }

        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles(role)
                .disabled(!user.isEnabled())
                .build();
    }
}

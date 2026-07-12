package com.example.freshfarm3.config;

import com.example.freshfarm3.entity.User;
import com.example.freshfarm3.enums.Role;
import com.example.freshfarm3.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * AdminSeeder — creates the platform's admin account(s) once at startup.
 *
 * There is no public /api/auth/register/admin endpoint (by design), so this
 * is the only place admin credentials get inserted. Runs every time the app
 * starts, but the existsByEmail() check means it only inserts once — safe
 * to leave in permanently.
 *
 * CHANGE THE EMAIL/PASSWORD BELOW before running against anything but a
 * local/dev database.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String ADMIN_NAME     = "Super Admin";
    private static final String ADMIN_EMAIL    = "admin@farmfresh.com";
    private static final String ADMIN_PHONE    = "9999999999";
    private static final String ADMIN_PASSWORD = "git "; // change this

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            log.info("Admin account already exists ({}), skipping seed.", ADMIN_EMAIL);
            return;
        }

        User admin = User.builder()
                .fullName(ADMIN_NAME)
                .email(ADMIN_EMAIL)
                .phone(ADMIN_PHONE)
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .role(Role.ADMIN)
                .active(true)
                .build();

        userRepository.save(admin);
        log.info("Seeded admin account: {}", ADMIN_EMAIL);
    }
}

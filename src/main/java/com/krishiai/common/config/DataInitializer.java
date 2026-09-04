package com.krishiai.common.config;

import com.krishiai.user.entity.User;
import com.krishiai.user.entity.UserRole;
import com.krishiai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.first-name}")
    private String adminFirstName;

    @Value("${app.admin.last-name}")
    private String adminLastName;

    @Override
    @Transactional
    public void run(String... args) {
        seedAdminUser();
    }

    private void seedAdminUser() {
        String normalizedEmail = User.normaliseEmail(adminEmail);
        if (userRepository.existsByEmail(normalizedEmail)) {
            log.info("Admin user already exists with email: {}", normalizedEmail);
            return;
        }

        User admin = User.createAdmin(
                normalizedEmail,
                passwordEncoder.encode(adminPassword),
                adminFirstName,
                adminLastName
        );

        userRepository.save(admin);
        log.info("Successfully seeded default platform admin: {}", normalizedEmail);
    }
}

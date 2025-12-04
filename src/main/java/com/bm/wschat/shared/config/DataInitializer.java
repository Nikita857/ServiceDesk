package com.bm.wschat.shared.config;

import com.bm.wschat.feature.user.model.SenderType;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Initialize default admin user if not exists
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("Admin123!@#"))
                    .fio("Bugakov Nikita Valentinovich")
                    .email("admin@company.com")
                    .specialist(false)
                    .refreshTokenExpiryDate(Instant.now().plus(7, ChronoUnit.DAYS))
                    .roles(Set.of(SenderType.ADMIN.name(), SenderType.DEVELOPER.name()))
                    .active(true)
                    .build();

            userRepository.save(admin);
            log.info("Created default admin user: admin");
        }

        // Initialize default user if not exists
        if (userRepository.findByUsername("user").isEmpty()) {
            User user = User.builder()
                    .username("user")
                    .password(passwordEncoder.encode("User123!@#"))
                    .fio("Regular User")
                    .email("user@company.com")
                    .specialist(false)
                    .roles(Set.of("ROLE_USER"))
                    .active(true)
                    .build();

            userRepository.save(user);
            log.info("Created default user: user");
        }
        
        log.info("Data initialization completed");
    }
}
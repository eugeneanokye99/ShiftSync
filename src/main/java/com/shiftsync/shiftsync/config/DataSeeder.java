package com.shiftsync.shiftsync.config;

import com.shiftsync.shiftsync.auth.entity.User;
import com.shiftsync.shiftsync.auth.repository.UserRepository;
import com.shiftsync.shiftsync.common.enums.UserRole;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * The type Data seeder.
 */
@Configuration
public class DataSeeder {

    /**
     * Seed data command line runner.
     *
     * @param userRepository  the user repository
     * @param passwordEncoder the password encoder
     * @return the command line runner
     */
    @Bean
    public CommandLineRunner seedData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return _ -> {
            if (!userRepository.existsByEmail("admin@shiftsync.com")) {
                User admin = new User();
                admin.setEmail("admin@shiftsync.com");
                admin.setPasswordHash(passwordEncoder.encode("Admin@2025"));
                admin.setFullName("System Administrator");
                admin.setRole(UserRole.HR_ADMIN);

                userRepository.save(admin);
                System.out.println("✓ HR Admin user seeded: admin@shiftsync.com");
            }
        };
    }
}

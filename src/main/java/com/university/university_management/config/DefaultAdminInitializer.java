package com.university.university_management.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.university.university_management.model.UniversityUser;
import com.university.university_management.model.UserRole;
import com.university.university_management.repository.UniversityUserRepository;

@Configuration
public class DefaultAdminInitializer {
	@Bean
	CommandLineRunner createDefaultAdmin(
			UniversityUserRepository repository,
			PasswordEncoder passwordEncoder,
			@Value("${app.admin.name:University Admin}") String name,
			@Value("${app.admin.email}") String email,
			@Value("${app.admin.password}") String password) {
		return args -> {
			if (email.isBlank() || password.isBlank() || password.length() < 8) {
				throw new IllegalStateException("APP_ADMIN_EMAIL and an APP_ADMIN_PASSWORD of at least 8 characters are required");
			}
			repository.findByEmail(email).ifPresentOrElse(account -> {
				if (account.getRole() != UserRole.ADMIN) {
					throw new IllegalStateException("APP_ADMIN_EMAIL is already used by a non-admin account");
				}
			}, () -> repository.save(new UniversityUser(name, email, passwordEncoder.encode(password),
					UserRole.ADMIN, null, null)));
		};
	}
}

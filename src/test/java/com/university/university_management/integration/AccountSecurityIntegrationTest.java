package com.university.university_management.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.university.university_management.model.UniversityUser;
import com.university.university_management.model.UserRole;
import com.university.university_management.repository.UniversityUserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountSecurityIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UniversityUserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	@DisplayName("Integration: Deactivating an account revokes login; reactivating restores login")
	void accountStatusToggleAffectsAuthentication() throws Exception {
		String email = "security-student-" + System.nanoTime() + "@test.local";
		String password = "SecStudentPass123!";

		// Create active student
		UniversityUser student = userRepository.save(new UniversityUser(
				"Security Student", email, passwordEncoder.encode(password),
				UserRole.STUDENT, "Mathematics", "MATH-01", "2025", null
		));
		Long studentId = student.getId();

		// 1. Student logs in successfully while active
		mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.param("username", email)
						.param("password", password))
				.andExpect(status().isOk());

		// 2. Admin disables the student account via PATCH /api/accounts/{id}/status
		mockMvc.perform(patch("/api/accounts/{id}/status", studentId)
						.with(user("admin@test.local").roles("ADMIN"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"active\":false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false));

		// Verify database state is inactive
		assertThat(userRepository.findById(studentId).orElseThrow().isActive()).isFalse();

		// 3. Deactivated user login is rejected with 401 Unauthorized
		mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.param("username", email)
						.param("password", password))
				.andExpect(status().isUnauthorized());

		// 4. Admin reactivates the account
		mockMvc.perform(patch("/api/accounts/{id}/status", studentId)
						.with(user("admin@test.local").roles("ADMIN"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"active\":true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(true));

		// 5. Reactivated student logs in successfully again
		mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.param("username", email)
						.param("password", password))
				.andExpect(status().isOk());

		// Cleanup
		userRepository.deleteById(studentId);
	}

	@Test
	@DisplayName("Integration: Enforces authorization matrix across Student, Teacher, Admin, and Anonymous roles")
	void roleBasedAuthorizationMatrixEnforced() throws Exception {
		// 1. Anonymous requests to protected API endpoints return 401 Unauthorized
		mockMvc.perform(get("/api/dashboard"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/profile"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/teachers"))
				.andExpect(status().isUnauthorized());

		// 2. Student role:
		// Cannot view or manage teachers or admins
		mockMvc.perform(get("/api/teachers").with(user("student@test.local").roles("STUDENT")))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/admins").with(user("student@test.local").roles("STUDENT")))
				.andExpect(status().isForbidden());
		mockMvc.perform(patch("/api/accounts/1/status").with(user("student@test.local").roles("STUDENT"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"active\":false}"))
				.andExpect(status().isForbidden());
		// Can view students
		mockMvc.perform(get("/api/students").with(user("student@test.local").roles("STUDENT")))
				.andExpect(status().isOk());

		// 3. Teacher role:
		// Can view teachers and students
		mockMvc.perform(get("/api/teachers").with(user("teacher@test.local").roles("TEACHER")))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/students").with(user("teacher@test.local").roles("TEACHER")))
				.andExpect(status().isOk());
		// Cannot view or modify admins or modify account active status
		mockMvc.perform(get("/api/admins").with(user("teacher@test.local").roles("TEACHER")))
				.andExpect(status().isForbidden());
		mockMvc.perform(patch("/api/accounts/1/status").with(user("teacher@test.local").roles("TEACHER"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"active\":false}"))
				.andExpect(status().isForbidden());
		// Cannot create students directly via POST /api/students (only Admin or self-signup)
		mockMvc.perform(post("/api/students").with(user("teacher@test.local").roles("TEACHER"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Test\",\"email\":\"t@test.local\",\"password\":\"Pass123!\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("Integration: Safeguards prevent deletion or deactivation of the last active admin")
	void lastAdminProtectionRulesEnforced() throws Exception {
		UniversityUser admin = userRepository.findByEmail("admin@test.local").orElseThrow();
		Long adminId = admin.getId();

		// Cannot delete the only admin
		mockMvc.perform(delete("/api/admins/{id}", adminId)
						.with(user("admin@test.local").roles("ADMIN"))
						.with(csrf()))
				.andExpect(status().isConflict());

		// Cannot deactivate the only active admin
		mockMvc.perform(patch("/api/accounts/{id}/status", adminId)
						.with(user("admin@test.local").roles("ADMIN"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"active\":false}"))
				.andExpect(status().isConflict());

		// Admin remains active in DB
		assertThat(userRepository.findById(adminId).orElseThrow().isActive()).isTrue();
	}
}

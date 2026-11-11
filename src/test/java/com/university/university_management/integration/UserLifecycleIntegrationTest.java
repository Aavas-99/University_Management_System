package com.university.university_management.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.university.university_management.model.UniversityUser;
import com.university.university_management.model.UserRole;
import com.university.university_management.repository.UniversityUserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserLifecycleIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UniversityUserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	@DisplayName("Integration: Complete Student lifecycle flow (Create -> Verify DB & Hash -> Login -> Read Profile -> Update -> Delete)")
	void studentFullLifecycleFlow() throws Exception {
		String email = "lifecycle-student-" + System.nanoTime() + "@test.local";
		String rawPassword = "StudentPass123!";

		// 1. Admin creates student via REST API
		String createPayload = """
				{
					"name": "Integration Student",
					"email": "%s",
					"password": "%s",
					"department": "Computer Science",
					"roll": "CSE-2026-01",
					"batch": "2026"
				}
				""".formatted(email, rawPassword);

		mockMvc.perform(post("/api/students")
						.with(user("admin@test.local").roles("ADMIN"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(createPayload))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.role").value("STUDENT"))
				.andExpect(jsonPath("$.department").value("Computer Science"))
				.andExpect(jsonPath("$.studentNumber").value("CSE-2026-01"))
				.andExpect(jsonPath("$.batch").value("2026"))
				.andExpect(jsonPath("$.active").value(true));

		// 2. Verify student is persisted in DB with hashed password (not plain text)
		UniversityUser persisted = userRepository.findByEmail(email).orElseThrow();
		assertThat(persisted.getName()).isEqualTo("Integration Student");
		assertThat(passwordEncoder.matches(rawPassword, persisted.getPasswordHash())).isTrue();
		assertThat(persisted.getPasswordHash()).isNotEqualTo(rawPassword);
		Long studentId = persisted.getId();

		// 3. Newly created student logs in via form-login /api/auth/login endpoint
		MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.param("username", email)
						.param("password", rawPassword))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Login successful"))
				.andExpect(jsonPath("$.email").value(email))
				.andReturn()
				.getRequest()
				.getSession(false);

		assertThat(session).isNotNull();

		// 4. Authenticated student accesses /api/auth/me and /api/profile
		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.roles[0]").value("ROLE_STUDENT"));

		mockMvc.perform(get("/api/profile").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(studentId))
				.andExpect(jsonPath("$.name").value("Integration Student"))
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.department").value("Computer Science"))
				.andExpect(jsonPath("$.studentNumber").value("CSE-2026-01"));

		// 5. Admin updates student's department and batch via PUT /api/students/{id}
		String updatePayload = """
				{
					"name": "Updated Student Name",
					"email": "%s",
					"password": "",
					"department": "Software Engineering",
					"roll": "SE-2026-01",
					"batch": "2027"
				}
				""".formatted(email);

		mockMvc.perform(put("/api/students/{id}", studentId)
						.with(user("admin@test.local").roles("ADMIN"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(updatePayload))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Updated Student Name"))
				.andExpect(jsonPath("$.department").value("Software Engineering"))
				.andExpect(jsonPath("$.studentNumber").value("SE-2026-01"))
				.andExpect(jsonPath("$.batch").value("2027"));

		// Verify GET /api/students/{id} returns updated data
		mockMvc.perform(get("/api/students/{id}", studentId)
						.with(user("admin@test.local").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Updated Student Name"));

		// 6. Admin deletes the student via DELETE /api/students/{id}
		mockMvc.perform(delete("/api/students/{id}", studentId)
						.with(user("admin@test.local").roles("ADMIN"))
						.with(csrf()))
				.andExpect(status().isNoContent());

		// 7. Verify student is removed from DB and querying returns 404
		assertThat(userRepository.findById(studentId)).isEmpty();
		mockMvc.perform(get("/api/students/{id}", studentId)
						.with(user("admin@test.local").roles("ADMIN")))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Integration: Teacher lifecycle and dashboard access")
	void teacherLifecycleAndDashboard() throws Exception {
		String teacherEmail = "teacher-flow-" + System.nanoTime() + "@test.local";
		String rawPassword = "TeacherPass123!";

		// 1. Admin creates teacher
		String createPayload = """
				{
					"name": "Dr. Alan Turing",
					"email": "%s",
					"password": "%s",
					"department": "Computer Science",
					"designation": "Associate Professor"
				}
				""".formatted(teacherEmail, rawPassword);

		mockMvc.perform(post("/api/teachers")
						.with(user("admin@test.local").roles("ADMIN"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(createPayload))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(teacherEmail))
				.andExpect(jsonPath("$.role").value("TEACHER"))
				.andExpect(jsonPath("$.designation").value("Associate Professor"));

		// 2. Teacher logs in
		MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.param("username", teacherEmail)
						.param("password", rawPassword))
				.andExpect(status().isOk())
				.andReturn()
				.getRequest()
				.getSession(false);

		// 3. Teacher accesses /api/dashboard and sees teachers & students counts (not admins)
		mockMvc.perform(get("/api/dashboard").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("TEACHER"))
				.andExpect(jsonPath("$.recordCounts.teachers").isNumber())
				.andExpect(jsonPath("$.recordCounts.students").isNumber())
				.andExpect(jsonPath("$.recordCounts.admins").doesNotExist());

		// Cleanup
		userRepository.findByEmail(teacherEmail).ifPresent(userRepository::delete);
	}
}

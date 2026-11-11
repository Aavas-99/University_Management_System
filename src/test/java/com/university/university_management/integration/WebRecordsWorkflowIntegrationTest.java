package com.university.university_management.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.university.university_management.model.UniversityUser;
import com.university.university_management.model.UserRole;
import com.university.university_management.repository.UniversityUserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WebRecordsWorkflowIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UniversityUserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	@DisplayName("Integration: Public self-signup creates student account and redirects to login")
	void publicStudentSignupFlow() throws Exception {
		String email = "signup-flow-" + System.nanoTime() + "@test.local";
		String password = "SignupPass123!";

		// 1. Visit signup page for student
		mockMvc.perform(get("/signup").param("role", "STUDENT"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Create a Student account")));

		// 2. Submit signup form
		mockMvc.perform(post("/signup")
						.with(csrf())
						.param("role", "STUDENT")
						.param("name", "Self Registered Student")
						.param("email", email)
						.param("password", password)
						.param("department", "Electrical Engineering")
						.param("roll", "EEE-501")
						.param("batch", "2025"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("/login?registered=*"));

		// 3. User is stored in database with encrypted password
		UniversityUser user = userRepository.findByEmail(email).orElseThrow();
		assertThat(user.getName()).isEqualTo("Self Registered Student");
		assertThat(user.getDepartment()).isEqualTo("Electrical Engineering");
		assertThat(passwordEncoder.matches(password, user.getPasswordHash())).isTrue();

		// 4. User can immediately authenticate
		mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.param("username", email)
						.param("password", password))
				.andExpect(status().isOk());

		// Cleanup
		userRepository.delete(user);
	}

	@Test
	@DisplayName("Integration: Admin views HTML table, navigates to edit form, updates record via form post")
	void adminHtmlRecordsBrowseAndEditFlow() throws Exception {
		String email = "html-edit-" + System.nanoTime() + "@test.local";
		UniversityUser student = userRepository.save(new UniversityUser(
				"HTML Student", email, passwordEncoder.encode("Pass1234!"),
				UserRole.STUDENT, "Civil Engineering", "CE-100", "2025", null
		));
		Long studentId = student.getId();

		// 1. Admin views student records HTML table
		mockMvc.perform(get("/records/students").with(user("admin@test.local").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("<table")))
				.andExpect(content().string(containsString("HTML Student")))
				.andExpect(content().string(containsString(email)))
				.andExpect(content().string(containsString("/records/students/" + studentId + "/edit")));

		// 2. Admin opens edit form
		mockMvc.perform(get("/records/students/" + studentId + "/edit")
						.with(user("admin@test.local").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Edit Student account")))
				.andExpect(content().string(containsString("HTML Student")));

		// 3. Admin submits update form
		mockMvc.perform(post("/records/students/" + studentId + "/edit")
						.with(user("admin@test.local").roles("ADMIN"))
						.with(csrf())
						.param("name", "Renamed Student")
						.param("email", email)
						.param("department", "Mechanical Engineering")
						.param("roll", "ME-200")
						.param("batch", "2026"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/records/students"));

		// 4. Verify database has been updated
		UniversityUser updated = userRepository.findById(studentId).orElseThrow();
		assertThat(updated.getName()).isEqualTo("Renamed Student");
		assertThat(updated.getDepartment()).isEqualTo("Mechanical Engineering");
		assertThat(updated.getStudentNumber()).isEqualTo("ME-200");
		assertThat(updated.getBatch()).isEqualTo("2026");

		// 5. Admin deletes record via form post
		mockMvc.perform(post("/records/students/" + studentId + "/delete")
						.with(user("admin@test.local").roles("ADMIN"))
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/records/students"));

		assertThat(userRepository.findById(studentId)).isEmpty();
	}

	@Test
	@DisplayName("Integration: Access control on web page views prevents unauthorized roles")
	void webPageAccessControl() throws Exception {
		// Non-logged-in user accessing records is redirected to /login
		mockMvc.perform(get("/records/students"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));

		// Student cannot access teacher or admin records
		mockMvc.perform(get("/records/teachers").with(user("student@test.local").roles("STUDENT")))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/records/admins").with(user("student@test.local").roles("STUDENT")))
				.andExpect(status().isForbidden());

		// Teacher can access student and teacher records, but not admin records
		mockMvc.perform(get("/records/students").with(user("teacher@test.local").roles("TEACHER")))
				.andExpect(status().isOk());
		mockMvc.perform(get("/records/teachers").with(user("teacher@test.local").roles("TEACHER")))
				.andExpect(status().isOk());
		mockMvc.perform(get("/records/admins").with(user("teacher@test.local").roles("TEACHER")))
				.andExpect(status().isForbidden());
	}
}

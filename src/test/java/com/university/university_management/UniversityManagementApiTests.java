package com.university.university_management;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.university.university_management.repository.UniversityUserRepository;
import com.university.university_management.model.UniversityUser;
import com.university.university_management.model.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
class UniversityManagementApiTests {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UniversityUserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void apiRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/students"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().string(containsString("Authentication is required")));
	}

	@Test
	void loginPageAndCsrfEndpointAreAvailable() throws Exception {
		mockMvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Sign in with your university account")));

		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Sign up")))
				.andExpect(content().string(org.hamcrest.Matchers.not(containsString("/signup?role=ADMIN"))));
		mockMvc.perform(get("/").with(user("admin@test.local").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Create an Admin account")));

		mockMvc.perform(get("/signup").param("role", "STUDENT"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Create a Student account")));

		mockMvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
				.andExpect(jsonPath("$.token").isNotEmpty());
	}

	@Test
	void configuredBootstrapAdminIsStoredWithAnEncodedPassword() {
		UniversityUser admin = userRepository.findByEmail("admin@test.local").orElseThrow();
		org.junit.jupiter.api.Assertions.assertEquals(UserRole.ADMIN, admin.getRole());
		org.junit.jupiter.api.Assertions.assertTrue(
				passwordEncoder.matches("TestAdmin123!", admin.getPasswordHash()));
		org.junit.jupiter.api.Assertions.assertNotEquals("TestAdmin123!", admin.getPasswordHash());
	}

	@Test
	void browserShowsStudentRecordsInAnHtmlTable() throws Exception {
		String email = "table-" + System.nanoTime() + "@test.local";
		userRepository.save(new UniversityUser("Table Student", email,
				passwordEncoder.encode("StudentPass123!"), UserRole.STUDENT, "Science", "S-202"));

		mockMvc.perform(get("/records/students").with(user("admin@test.local").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("<table")))
				.andExpect(content().string(containsString(">Edit</a>")))
				.andExpect(content().string(containsString(">Delete</button>")))
				.andExpect(content().string(org.hamcrest.Matchers.not(containsString(">Status</th>"))))
				.andExpect(content().string(containsString("Table Student")))
				.andExpect(content().string(containsString(email)))
				.andExpect(content().string(containsString("S-202")))
				.andExpect(content().string(org.hamcrest.Matchers.not(containsString("{\""))));
	}

	@Test
	void browserRecordTablesRespectRoleAccess() throws Exception {
		mockMvc.perform(get("/records/admins").with(user("teacher@test.local").roles("TEACHER")))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/records/teachers").with(user("student@test.local").roles("STUDENT")))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/records/students").with(user("student@test.local").roles("STUDENT")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("<table")));
	}

	@Test
	void browserCanEditAndDeleteAStudentRecord() throws Exception {
		String email = "browser-crud-" + System.nanoTime() + "@test.local";
		UniversityUser student = userRepository.save(new UniversityUser("Before Edit", email,
				passwordEncoder.encode("StudentPass123!"), UserRole.STUDENT, "Engineering", "R-10",
				"2025", null));
		String resourcePath = "/records/students/" + student.getId();

		mockMvc.perform(get(resourcePath + "/edit").with(user("admin@test.local").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Edit Student account")));
		mockMvc.perform(post(resourcePath + "/edit")
						.with(user("admin@test.local").roles("ADMIN"))
						.with(csrf())
						.param("name", "After Edit")
						.param("email", email)
						.param("department", "Science")
						.param("roll", "R-11")
						.param("batch", "2026"))
				.andExpect(status().is3xxRedirection())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
						.redirectedUrl("/records/students"));
		org.junit.jupiter.api.Assertions.assertEquals("After Edit",
				userRepository.findById(student.getId()).orElseThrow().getName());

		mockMvc.perform(post(resourcePath + "/delete")
						.with(user("admin@test.local").roles("ADMIN"))
						.with(csrf()))
				.andExpect(status().is3xxRedirection());
		org.junit.jupiter.api.Assertions.assertFalse(userRepository.existsById(student.getId()));
	}

	@Test
	void eachRoleCanSignUpWithItsRequiredFields() throws Exception {
		for (String role : new String[] {"TEACHER", "STUDENT"}) {
			String email = role.toLowerCase() + "-" + System.nanoTime() + "@test.local";
			var request = post("/signup")
					.with(csrf())
					.param("role", role)
					.param("name", "New " + role)
					.param("email", email)
					.param("password", "SignupPass123!")
					.param("department", "Engineering");
			if (role.equals("STUDENT")) {
				request.param("roll", "R-100").param("batch", "2026");
			}
			else {
				request.param("designation", "Lecturer");
			}
			mockMvc.perform(request)
					.andExpect(status().is3xxRedirection())
					.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
							.redirectedUrlPattern("/login?registered=*"));

			org.junit.jupiter.api.Assertions.assertTrue(userRepository.findByEmail(email).isPresent());
		}

		String adminEmail = "new-admin-" + System.nanoTime() + "@test.local";
		mockMvc.perform(get("/signup").param("role", "ADMIN"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Admin account creation is restricted")));
		mockMvc.perform(get("/signup").param("role", "ADMIN")
						.with(user("admin@test.local").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("name=\"role\" value=\"ADMIN\"")));
		mockMvc.perform(post("/signup")
						.with(csrf())
						.param("role", "ADMIN")
						.param("email", adminEmail)
						.param("password", "SignupPass123!"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/signup")
						.with(user("admin@test.local").roles("ADMIN"))
						.with(csrf())
						.param("role", "ADMIN")
						.param("email", adminEmail)
						.param("password", "SignupPass123!"))
				.andExpect(status().is3xxRedirection());
		org.junit.jupiter.api.Assertions.assertTrue(userRepository.findByEmail(adminEmail).isPresent());
		userRepository.delete(userRepository.findByEmail(adminEmail).orElseThrow());
	}

	@Test
	void apiRejectsMissingRoleSpecificAccountFields() throws Exception {
		mockMvc.perform(post("/api/students")
						.with(user("admin@test.local").roles("ADMIN"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Missing Batch","email":"missing-batch@test.local","password":"StudentPass123!","department":"Engineering","roll":"R-10"}
								"""))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/teachers")
						.with(user("admin@test.local").roles("ADMIN"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Missing Designation","email":"missing-designation@test.local","password":"TeacherPass123!","department":"Engineering"}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void loginCreatesSessionAndLogoutEndsIt() throws Exception {
		MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.param("username", "admin@test.local")
						.param("password", "TestAdmin123!"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Login successful"))
				.andReturn()
				.getRequest()
				.getSession(false);

		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("admin@test.local"))
				.andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"));

		mockMvc.perform(post("/api/auth/logout").session(session).with(csrf()).accept(MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/"));
		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void teachersAndStudentsCanOnlyManageTheirOwnResource() throws Exception {
		mockMvc.perform(get("/api/teachers").with(user("teacher@test.local").roles("TEACHER")))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/students").with(user("teacher@test.local").roles("TEACHER")))
				.andExpect(status().isOk());
		String studentEmail = "teacher-managed-" + System.nanoTime() + "@test.local";
		mockMvc.perform(post("/api/students")
						.with(user("admin@test.local").roles("ADMIN"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Managed Student","email":"%s","password":"StudentPass123!","department":"Engineering","roll":"R-1","batch":"2026"}
								""".formatted(studentEmail)))
				.andExpect(status().isCreated());
		Long studentId = userRepository.findByEmail(studentEmail).orElseThrow().getId();
		mockMvc.perform(put("/api/students/{id}", studentId)
						.with(user("teacher@test.local").roles("TEACHER"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Teacher Updated","email":"%s","password":"StudentPass123!","department":"Science","roll":"R-1","batch":"2026"}
								""".formatted(studentEmail)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Teacher Updated"));
		mockMvc.perform(delete("/api/students/{id}", studentId)
						.with(user("teacher@test.local").roles("TEACHER"))
						.with(csrf()))
				.andExpect(status().isNoContent());
		mockMvc.perform(post("/api/students")
						.with(user("teacher@test.local").roles("TEACHER"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Blocked Student","email":"blocked@test.local","password":"StudentPass123!","department":"Engineering","roll":"R-2","batch":"2026"}
								"""))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/students").with(user("student@test.local").roles("STUDENT")))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/teachers").with(user("student@test.local").roles("STUDENT")))
				.andExpect(status().isForbidden());
	}

	@Test
	void dashboardAndProfileAreScopedToTheAuthenticatedUser() throws Exception {
		mockMvc.perform(get("/api/dashboard").with(user("teacher@test.local").roles("TEACHER")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("TEACHER"))
				.andExpect(jsonPath("$.recordCounts.teachers").exists())
				.andExpect(jsonPath("$.recordCounts.students").exists())
				.andExpect(jsonPath("$.recordCounts.admins").doesNotExist());

		mockMvc.perform(get("/api/profile").with(user("admin@test.local").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("admin@test.local"))
				.andExpect(jsonPath("$.role").value("ADMIN"))
				.andExpect(jsonPath("$.active").value(true))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void adminCanCreateReadUpdateAndDeleteStudentsWithoutExposingPassword() throws Exception {
		String email = "student-" + System.nanoTime() + "@test.local";
		String createBody = """
				{"name":"Sam Student","email":"%s","password":"StudentPass123!","department":"Engineering","roll":"S-100","batch":"2026"}
				""".formatted(email);

		mockMvc.perform(post("/api/students")
						.with(user("admin@test.local").roles("ADMIN"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(createBody))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.role").value("STUDENT"))
				.andExpect(jsonPath("$.password").doesNotExist());

		Long id = userRepository.findByEmail(email).orElseThrow().getId();
		org.junit.jupiter.api.Assertions.assertTrue(
				passwordEncoder.matches("StudentPass123!",
						userRepository.findByEmail(email).orElseThrow().getPasswordHash()));

		mockMvc.perform(get("/api/students/{id}", id).with(user("admin@test.local").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Sam Student"));

		String updateBody = """
				{"name":"Sam Updated","email":"%s","password":"ChangedPass123!","department":"Science","roll":"S-101","batch":"2027"}
				""".formatted(email);
		mockMvc.perform(put("/api/students/{id}", id)
						.with(user("admin@test.local").roles("ADMIN"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(updateBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Sam Updated"));

		mockMvc.perform(delete("/api/students/{id}", id)
						.with(user("admin@test.local").roles("ADMIN"))
						.with(csrf()))
				.andExpect(status().isNoContent());
		org.junit.jupiter.api.Assertions.assertFalse(userRepository.existsById(id));
	}

	@Test
	void invalidInputReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/api/teachers")
						.with(user("admin@test.local").roles("ADMIN"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"","email":"not-an-email","password":"short"}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void lastAdminCannotBeDeleted() throws Exception {
		Long adminId = userRepository.findByEmail("admin@test.local").orElseThrow().getId();
		mockMvc.perform(delete("/api/admins/{id}", adminId)
						.with(user("admin@test.local").roles("ADMIN"))
						.with(csrf()))
				.andExpect(status().isConflict());
	}

	@Test
	void adminsCanChangeAccountStatusButTeachersCannot() throws Exception {
		String email = "status-" + System.nanoTime() + "@test.local";
		String requestBody = """
				{"name":"Status Student","email":"%s","password":"StudentPass123!","department":"Engineering","roll":"R-8","batch":"2026"}
				""".formatted(email);
		mockMvc.perform(post("/api/students")
						.with(user("admin@test.local").roles("ADMIN"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isCreated());
		Long id = userRepository.findByEmail(email).orElseThrow().getId();

		mockMvc.perform(patch("/api/accounts/{id}/status", id)
						.with(user("teacher@test.local").roles("TEACHER"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"active":false}
								"""))
				.andExpect(status().isForbidden());

		mockMvc.perform(patch("/api/accounts/{id}/status", id)
						.with(user("admin@test.local").roles("ADMIN"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"active":false}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false));
		org.junit.jupiter.api.Assertions.assertFalse(userRepository.findById(id).orElseThrow().isActive());
		mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.param("username", email)
						.param("password", "StudentPass123!"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void lastActiveAdminCannotBeDisabled() throws Exception {
		Long adminId = userRepository.findByEmail("admin@test.local").orElseThrow().getId();
		mockMvc.perform(patch("/api/accounts/{id}/status", adminId)
						.with(user("admin@test.local").roles("ADMIN"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"active":false}
								"""))
				.andExpect(status().isConflict());
	}
}

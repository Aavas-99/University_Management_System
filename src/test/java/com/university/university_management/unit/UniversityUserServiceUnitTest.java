package com.university.university_management.unit;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.university.university_management.dto.DashboardResponse;
import com.university.university_management.dto.UserRequest;
import com.university.university_management.dto.UserResponse;
import com.university.university_management.model.UniversityUser;
import com.university.university_management.model.UserRole;
import com.university.university_management.repository.UniversityUserRepository;
import com.university.university_management.service.UniversityUserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniversityUserServiceUnitTest {

	@Mock
	private UniversityUserRepository repository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private UniversityUserService userService;

	@Test
	@DisplayName("Feature: Create valid student account hashes password and stores student record")
	void createStudentSuccessfully() {
		UserRequest request = new UserRequest(
				"John Doe", "john@test.local", "ValidPass123!", "Computer Science",
				"CS-101", "2024", null, null
		);

		when(repository.findByEmail("john@test.local")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("ValidPass123!")).thenReturn("encoded_pass");

		UniversityUser savedUser = new UniversityUser(
				"John Doe", "john@test.local", "encoded_pass", UserRole.STUDENT,
				"Computer Science", "CS-101", "2024", null
		);
		when(repository.save(any(UniversityUser.class))).thenReturn(savedUser);

		UserResponse response = userService.create("students", request);

		assertThat(response).isNotNull();
		assertThat(response.email()).isEqualTo("john@test.local");
		assertThat(response.role()).isEqualTo(UserRole.STUDENT);
		assertThat(response.department()).isEqualTo("Computer Science");
		assertThat(response.studentNumber()).isEqualTo("CS-101");
		assertThat(response.batch()).isEqualTo("2024");

		ArgumentCaptor<UniversityUser> userCaptor = ArgumentCaptor.forClass(UniversityUser.class);
		verify(repository).save(userCaptor.capture());
		UniversityUser captured = userCaptor.getValue();
		assertThat(captured.getPasswordHash()).isEqualTo("encoded_pass");
		assertThat(captured.getName()).isEqualTo("John Doe");
	}

	@Test
	@DisplayName("Feature: Create account fails when password is less than 8 characters")
	void createAccountRejectsShortPassword() {
		UserRequest request = new UserRequest(
				"Short Password User", "short@test.local", "1234567", "Science",
				"S-1", "2024", null, null
		);

		assertThatThrownBy(() -> userService.create("students", request))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> {
					ResponseStatusException rse = (ResponseStatusException) ex;
					assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
					assertThat(rse.getReason()).contains("Password must contain at least 8 characters");
				});

		verify(repository, never()).save(any());
	}

	@Test
	@DisplayName("Feature: Create account fails when email is already registered")
	void createAccountRejectsDuplicateEmail() {
		UserRequest request = new UserRequest(
				"Duplicate User", "duplicate@test.local", "ValidPass123!", "Science",
				"S-2", "2024", null, null
		);

		UniversityUser existingUser = new UniversityUser(
				"Existing", "duplicate@test.local", "hash", UserRole.STUDENT, "Science", "S-1"
		);
		ReflectionTestUtils.setField(existingUser, "id", 5L);
		when(repository.findByEmail("duplicate@test.local")).thenReturn(Optional.of(existingUser));

		assertThatThrownBy(() -> userService.create("students", request))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> {
					ResponseStatusException rse = (ResponseStatusException) ex;
					assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
					assertThat(rse.getReason()).contains("Email is already in use");
				});

		verify(repository, never()).save(any());
	}

	@Test
	@DisplayName("Feature: Student validation requires both roll and batch")
	void createStudentRejectsMissingRollOrBatch() {
		UserRequest missingRoll = new UserRequest(
				"No Roll", "noroll@test.local", "ValidPass123!", "Science",
				null, "2024", null, null
		);

		assertThatThrownBy(() -> userService.create("students", missingRoll))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> {
					ResponseStatusException rse = (ResponseStatusException) ex;
					assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
					assertThat(rse.getReason()).contains("Roll and batch are required for students");
				});

		UserRequest missingBatch = new UserRequest(
				"No Batch", "nobatch@test.local", "ValidPass123!", "Science",
				"S-99", null, null, null
		);

		assertThatThrownBy(() -> userService.create("students", missingBatch))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> {
					ResponseStatusException rse = (ResponseStatusException) ex;
					assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
					assertThat(rse.getReason()).contains("Roll and batch are required for students");
				});

		verify(repository, never()).save(any());
	}

	@Test
	@DisplayName("Feature: Teacher validation requires designation")
	void createTeacherRejectsMissingDesignation() {
		UserRequest missingDesignation = new UserRequest(
				"Dr. Smith", "smith@test.local", "ValidPass123!", "Physics",
				null, null, null, null
		);

		assertThatThrownBy(() -> userService.create("teachers", missingDesignation))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> {
					ResponseStatusException rse = (ResponseStatusException) ex;
					assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
					assertThat(rse.getReason()).contains("Designation is required for teachers");
				});

		verify(repository, never()).save(any());
	}

	@Test
	@DisplayName("Feature: Admin account name automatically defaults to admin email")
	void createAdminDefaultsNameToEmail() {
		UserRequest adminRequest = new UserRequest(
				"Custom Admin Name", "newadmin@test.local", "ValidAdminPass123!",
				null, null, null, null, null
		);

		when(repository.findByEmail("newadmin@test.local")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("ValidAdminPass123!")).thenReturn("encoded_admin_pass");

		UniversityUser savedAdmin = new UniversityUser(
				"newadmin@test.local", "newadmin@test.local", "encoded_admin_pass",
				UserRole.ADMIN, null, null
		);
		when(repository.save(any(UniversityUser.class))).thenReturn(savedAdmin);

		UserResponse response = userService.create("admins", adminRequest);

		assertThat(response.role()).isEqualTo(UserRole.ADMIN);
		ArgumentCaptor<UniversityUser> captor = ArgumentCaptor.forClass(UniversityUser.class);
		verify(repository).save(captor.capture());
		// Admin name is set to request.email()
		assertThat(captor.getValue().getName()).isEqualTo("newadmin@test.local");
	}

	@Test
	@DisplayName("Feature: Last remaining admin cannot be deleted")
	void deleteLastAdminIsPrevented() {
		UniversityUser lastAdmin = new UniversityUser(
				"admin@test.local", "admin@test.local", "hash", UserRole.ADMIN, null, null
		);

		when(repository.findById(1L)).thenReturn(Optional.of(lastAdmin));
		when(repository.countByRole(UserRole.ADMIN)).thenReturn(1L);

		assertThatThrownBy(() -> userService.delete("admins", 1L))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> {
					ResponseStatusException rse = (ResponseStatusException) ex;
					assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
					assertThat(rse.getReason()).contains("The last admin account cannot be deleted");
				});

		verify(repository, never()).delete(any());
	}

	@Test
	@DisplayName("Feature: Last active admin cannot be disabled")
	void disableLastActiveAdminIsPrevented() {
		UniversityUser activeAdmin = new UniversityUser(
				"admin@test.local", "admin@test.local", "hash", UserRole.ADMIN, null, null
		);

		when(repository.findById(1L)).thenReturn(Optional.of(activeAdmin));
		when(repository.countByRoleAndActiveTrue(UserRole.ADMIN)).thenReturn(1L);

		assertThatThrownBy(() -> userService.updateStatus(1L, false))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> {
					ResponseStatusException rse = (ResponseStatusException) ex;
					assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
					assertThat(rse.getReason()).contains("The last active admin cannot be disabled");
				});

		verify(repository, never()).save(any());
	}

	@Test
	@DisplayName("Feature: Role-based dashboard counts are scoped to user role")
	void dashboardScopingByRole() {
		when(repository.countByRole(UserRole.ADMIN)).thenReturn(2L);
		when(repository.countByRole(UserRole.TEACHER)).thenReturn(5L);
		when(repository.countByRole(UserRole.STUDENT)).thenReturn(50L);

		// Admin sees admins, teachers, and students
		DashboardResponse adminDashboard = userService.dashboard(UserRole.ADMIN);
		assertThat(adminDashboard.role()).isEqualTo(UserRole.ADMIN);
		assertThat(adminDashboard.recordCounts()).containsKeys("admins", "teachers", "students");
		assertThat(adminDashboard.recordCounts().get("admins")).isEqualTo(2L);

		// Teacher sees only teachers and students
		DashboardResponse teacherDashboard = userService.dashboard(UserRole.TEACHER);
		assertThat(teacherDashboard.role()).isEqualTo(UserRole.TEACHER);
		assertThat(teacherDashboard.recordCounts()).containsOnlyKeys("teachers", "students");

		// Student sees only students
		DashboardResponse studentDashboard = userService.dashboard(UserRole.STUDENT);
		assertThat(studentDashboard.role()).isEqualTo(UserRole.STUDENT);
		assertThat(studentDashboard.recordCounts()).containsOnlyKeys("students");
	}

	@Test
	@DisplayName("Feature: Update user without password retains original password hash")
	void updateUserBlankPasswordPreservesExistingHash() {
		UniversityUser existingStudent = new UniversityUser(
				"Old Name", "student@test.local", "original_hash", UserRole.STUDENT,
				"CSE", "CS-10", "2024", null
		);
		ReflectionTestUtils.setField(existingStudent, "id", 10L);

		when(repository.findById(10L)).thenReturn(Optional.of(existingStudent));
		when(repository.findByEmail("student@test.local")).thenReturn(Optional.of(existingStudent));
		when(repository.save(any(UniversityUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UserRequest updateRequest = new UserRequest(
				"New Name", "student@test.local", "", "EEE", "CS-10", "2024", null, null
		);

		UserResponse response = userService.update("students", 10L, updateRequest);

		assertThat(response.name()).isEqualTo("New Name");
		assertThat(response.department()).isEqualTo("EEE");
		// Ensure password encoder was never invoked with empty string
		verify(passwordEncoder, never()).encode(anyString());
		assertThat(existingStudent.getPasswordHash()).isEqualTo("original_hash");
	}

	@Test
	@DisplayName("Feature: Query with unknown resource throws 404 Not Found")
	void parseRoleRejectsUnknownResource() {
		assertThatThrownBy(() -> userService.findAll("unknown_resource"))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> {
					ResponseStatusException rse = (ResponseStatusException) ex;
					assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
					assertThat(rse.getReason()).contains("Account resource not found");
				});
	}
}

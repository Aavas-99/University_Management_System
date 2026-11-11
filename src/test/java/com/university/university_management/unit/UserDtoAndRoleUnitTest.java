package com.university.university_management.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.university.university_management.dto.UserRequest;
import com.university.university_management.dto.UserResponse;
import com.university.university_management.model.UniversityUser;
import com.university.university_management.model.UserRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserDtoAndRoleUnitTest {

	@Test
	@DisplayName("Feature: UserRole resource mapping maps names bidirectionally")
	void userRoleResourceMapping() {
		assertThat(UserRole.ADMIN.resourceName()).isEqualTo("admins");
		assertThat(UserRole.TEACHER.resourceName()).isEqualTo("teachers");
		assertThat(UserRole.STUDENT.resourceName()).isEqualTo("students");

		assertThat(UserRole.fromResourceName("admins")).isEqualTo(UserRole.ADMIN);
		assertThat(UserRole.fromResourceName("teachers")).isEqualTo(UserRole.TEACHER);
		assertThat(UserRole.fromResourceName("students")).isEqualTo(UserRole.STUDENT);

		assertThatThrownBy(() -> UserRole.fromResourceName("superusers"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Unknown account resource");
	}

	@Test
	@DisplayName("Feature: UserRequest effectiveRoll resolves roll or studentNumber fallback")
	void userRequestEffectiveRollResolution() {
		UserRequest withRoll = new UserRequest(
				"Test", "test@test.local", "pass", "Dept", "ROLL-1", "2024", null, "STUDENT-1"
		);
		assertThat(withRoll.effectiveRoll()).isEqualTo("ROLL-1");

		UserRequest fallbackToStudentNumber = new UserRequest(
				"Test", "test@test.local", "pass", "Dept", null, "2024", null, "STUDENT-1"
		);
		assertThat(fallbackToStudentNumber.effectiveRoll()).isEqualTo("STUDENT-1");

		UserRequest blankRollFallback = new UserRequest(
				"Test", "test@test.local", "pass", "Dept", "   ", "2024", null, "STUDENT-2"
		);
		assertThat(blankRollFallback.effectiveRoll()).isEqualTo("STUDENT-2");
	}

	@Test
	@DisplayName("Feature: UserResponse maps all entity fields accurately")
	void userResponseMapping() {
		UniversityUser user = new UniversityUser(
				"John Doe", "john@test.local", "secret_hash", UserRole.STUDENT,
				"Computer Science", "CSE-100", "2026", null
		);

		UserResponse response = UserResponse.from(user);

		assertThat(response.name()).isEqualTo("John Doe");
		assertThat(response.email()).isEqualTo("john@test.local");
		assertThat(response.role()).isEqualTo(UserRole.STUDENT);
		assertThat(response.department()).isEqualTo("Computer Science");
		assertThat(response.studentNumber()).isEqualTo("CSE-100");
		assertThat(response.batch()).isEqualTo("2026");
		assertThat(response.active()).isTrue();
	}
}

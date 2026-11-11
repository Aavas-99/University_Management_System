package com.university.university_management.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.university.university_management.model.UniversityUser;
import com.university.university_management.model.UserRole;

import static org.assertj.core.api.Assertions.assertThat;

class UniversityUserEntityUnitTest {

	@Test
	@DisplayName("Feature: UniversityUser creates student entity with active default true")
	void createStudentEntity() {
		UniversityUser user = new UniversityUser(
				"Alice Student", "alice@test.local", "hashed_pass", UserRole.STUDENT,
				"Computer Science", "CSE-01", "2025", null
		);

		assertThat(user.getName()).isEqualTo("Alice Student");
		assertThat(user.getEmail()).isEqualTo("alice@test.local");
		assertThat(user.getPasswordHash()).isEqualTo("hashed_pass");
		assertThat(user.getRole()).isEqualTo(UserRole.STUDENT);
		assertThat(user.getDepartment()).isEqualTo("Computer Science");
		assertThat(user.getStudentNumber()).isEqualTo("CSE-01");
		assertThat(user.getBatch()).isEqualTo("2025");
		assertThat(user.getDesignation()).isNull();
		assertThat(user.isActive()).isTrue();
	}

	@Test
	@DisplayName("Feature: UniversityUser creates teacher entity with designation")
	void createTeacherEntity() {
		UniversityUser user = new UniversityUser(
				"Prof. Robert", "robert@test.local", "hashed_pass", UserRole.TEACHER,
				"Mathematics", null, null, "Professor"
		);

		assertThat(user.getName()).isEqualTo("Prof. Robert");
		assertThat(user.getEmail()).isEqualTo("robert@test.local");
		assertThat(user.getRole()).isEqualTo(UserRole.TEACHER);
		assertThat(user.getDepartment()).isEqualTo("Mathematics");
		assertThat(user.getDesignation()).isEqualTo("Professor");
		assertThat(user.isActive()).isTrue();
	}

	@Test
	@DisplayName("Feature: UniversityUser update modifies mutable profile fields")
	void updateEntityFields() {
		UniversityUser user = new UniversityUser(
				"Initial Name", "initial@test.local", "old_hash", UserRole.STUDENT,
				"Civil", "CE-01", "2023", null
		);

		user.update("Updated Name", "updated@test.local", "new_hash",
				"Mechanical", "ME-01", "2024", "Senior Lecturer");

		assertThat(user.getName()).isEqualTo("Updated Name");
		assertThat(user.getEmail()).isEqualTo("updated@test.local");
		assertThat(user.getPasswordHash()).isEqualTo("new_hash");
		assertThat(user.getDepartment()).isEqualTo("Mechanical");
		assertThat(user.getStudentNumber()).isEqualTo("ME-01");
		assertThat(user.getBatch()).isEqualTo("2024");
		assertThat(user.getDesignation()).isEqualTo("Senior Lecturer");
	}

	@Test
	@DisplayName("Feature: UniversityUser setActive allows toggling account status")
	void toggleActiveState() {
		UniversityUser user = new UniversityUser(
				"User", "user@test.local", "hash", UserRole.STUDENT, "Dept", "01"
		);

		assertThat(user.isActive()).isTrue();
		user.setActive(false);
		assertThat(user.isActive()).isFalse();
		user.setActive(true);
		assertThat(user.isActive()).isTrue();
	}
}

package com.university.university_management.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequest(
		@Size(max = 100) String name,
		@NotBlank @Email @Size(max = 150) String email,
		@Size(max = 100) String password,
		@Size(max = 100) String department,
		@Size(max = 50) String roll,
		@Size(max = 50) String batch,
		@Size(max = 100) String designation,
		@Size(max = 50) String studentNumber) {

	public UserRequest(String name, String email, String password, String department, String studentNumber) {
		this(name, email, password, department, studentNumber, null, null, studentNumber);
	}

	public String effectiveRoll() {
		return roll != null && !roll.isBlank() ? roll : studentNumber;
	}
}

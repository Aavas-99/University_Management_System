package com.university.university_management.dto;

import com.university.university_management.model.UniversityUser;
import com.university.university_management.model.UserRole;

public record UserResponse(Long id, String name, String email, UserRole role,
		String department, String studentNumber, String batch, String designation, boolean active) {
	public static UserResponse from(UniversityUser user) {
		return new UserResponse(user.getId(), user.getName(), user.getEmail(),
				user.getRole(), user.getDepartment(), user.getStudentNumber(), user.getBatch(),
				user.getDesignation(), user.isActive());
	}
}

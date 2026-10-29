package com.university.university_management.model;

import java.util.Arrays;

public enum UserRole {
	ADMIN("admins"),
	TEACHER("teachers"),
	STUDENT("students");

	private final String resourceName;

	UserRole(String resourceName) {
		this.resourceName = resourceName;
	}

	public String resourceName() {
		return resourceName;
	}

	public static UserRole fromResourceName(String resourceName) {
		return Arrays.stream(values())
				.filter(role -> role.resourceName.equals(resourceName))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unknown account resource"));
	}
}

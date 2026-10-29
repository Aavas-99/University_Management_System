package com.university.university_management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "university_users")
public class UniversityUser {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, unique = true, length = 150)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserRole role;

	@Column(length = 100)
	private String department;

	@Column(name = "student_number", length = 50)
	private String studentNumber;

	@Column(length = 50)
	private String batch;

	@Column(length = 100)
	private String designation;

	@Column(nullable = false, columnDefinition = "boolean default true")
	private boolean active = true;

	protected UniversityUser() {
	}

	public UniversityUser(String name, String email, String passwordHash, UserRole role,
			String department, String studentNumber) {
		this(name, email, passwordHash, role, department, studentNumber, null, null);
	}

	public UniversityUser(String name, String email, String passwordHash, UserRole role,
			String department, String studentNumber, String batch, String designation) {
		this.name = name;
		this.email = email;
		this.passwordHash = passwordHash;
		this.role = role;
		this.department = department;
		this.studentNumber = studentNumber;
		this.batch = batch;
		this.designation = designation;
	}

	public void update(String name, String email, String passwordHash, String department,
			String studentNumber, String batch, String designation) {
		this.name = name;
		this.email = email;
		this.passwordHash = passwordHash;
		this.department = department;
		this.studentNumber = studentNumber;
		this.batch = batch;
		this.designation = designation;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public UserRole getRole() {
		return role;
	}

	public String getDepartment() {
		return department;
	}

	public String getStudentNumber() {
		return studentNumber;
	}

	public String getBatch() {
		return batch;
	}

	public String getDesignation() {
		return designation;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}

package com.university.university_management.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.university.university_management.dto.AccountStatusRequest;
import com.university.university_management.dto.UserResponse;
import com.university.university_management.service.UniversityUserService;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
	private final UniversityUserService userService;

	public AccountController(UniversityUserService userService) {
		this.userService = userService;
	}

	@PatchMapping("/{id}/status")
	public ResponseEntity<UserResponse> updateStatus(@PathVariable Long id,
			@Valid @RequestBody AccountStatusRequest request) {
		return ResponseEntity.ok(userService.updateStatus(id, request.active()));
	}
}

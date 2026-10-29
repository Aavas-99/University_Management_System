package com.university.university_management.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.university.university_management.dto.DashboardResponse;
import com.university.university_management.dto.UserResponse;
import com.university.university_management.model.UserRole;
import com.university.university_management.service.UniversityUserService;

@RestController
@RequestMapping("/api")
public class ProfileController {
	private final UniversityUserService userService;

	public ProfileController(UniversityUserService userService) {
		this.userService = userService;
	}

	@GetMapping("/profile")
	public UserResponse profile(Authentication authentication) {
		return userService.findByEmail(authentication.getName());
	}

	@GetMapping("/dashboard")
	public DashboardResponse dashboard(Authentication authentication) {
		String roleName = authentication.getAuthorities().stream()
				.map(authority -> authority.getAuthority())
				.filter(authority -> authority.startsWith("ROLE_"))
				.map(authority -> authority.substring("ROLE_".length()))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Authenticated account has no role"));
		return userService.dashboard(UserRole.valueOf(roleName));
	}
}

package com.university.university_management.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	@GetMapping("/me")
	public AuthenticatedUser currentUser(Authentication authentication) {
		List<String> roles = authentication.getAuthorities().stream()
				.map(authority -> authority.getAuthority())
				.toList();
		return new AuthenticatedUser(authentication.getName(), roles);
	}

	@GetMapping("/csrf")
	public CsrfToken csrfToken(CsrfToken csrfToken) {
		return csrfToken;
	}

	public record AuthenticatedUser(String email, List<String> roles) {
	}
}

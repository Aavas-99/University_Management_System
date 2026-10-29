package com.university.university_management.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.university.university_management.dto.UserRequest;
import com.university.university_management.model.UserRole;
import com.university.university_management.service.UniversityUserService;

@Controller
public class SignupController {
	private final UniversityUserService userService;

	public SignupController(UniversityUserService userService) {
		this.userService = userService;
	}

	@PostMapping("/signup")
	public String signup(@RequestParam UserRole role, @Valid @ModelAttribute UserRequest request,
			BindingResult bindingResult, Authentication authentication, Model model,
			RedirectAttributes redirectAttributes) {
		if (role == UserRole.ADMIN && !isAdmin(authentication)) {
			throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN,
					"An authenticated admin is required to create an admin account");
		}

		if (!bindingResult.hasErrors()) {
			try {
				userService.create(role.resourceName(), request);
				redirectAttributes.addAttribute("registered", true);
				return "redirect:/login";
			}
			catch (org.springframework.web.server.ResponseStatusException exception) {
				if (exception.getStatusCode().value() == HttpStatus.CONFLICT.value()) {
					bindingResult.rejectValue("email", "email.inUse", "This email is already in use.");
				}
				else {
					model.addAttribute("formError", exception.getReason());
				}
			}
		}

		model.addAttribute("role", role);
		model.addAttribute("roleName", role.name().charAt(0) + role.name().substring(1).toLowerCase());
		model.addAttribute("adminSignupAllowed", isAdmin(authentication));
		return "signup";
	}

	private boolean isAdmin(Authentication authentication) {
		return authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
	}
}

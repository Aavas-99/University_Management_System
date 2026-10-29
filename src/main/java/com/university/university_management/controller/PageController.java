package com.university.university_management.controller;

import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.university.university_management.dto.UserRequest;
import com.university.university_management.dto.UserResponse;
import com.university.university_management.model.UserRole;
import com.university.university_management.service.UniversityUserService;

import jakarta.validation.Valid;

@Controller
public class PageController {
	private final UniversityUserService userService;

	public PageController(UniversityUserService userService) {
		this.userService = userService;
	}

	@GetMapping("/")
	public String home() {
		return "index";
	}

	@GetMapping("/records/{resource}")
	public String records(@PathVariable String resource, Model model) {
		UserRole role;
		try {
			role = UserRole.fromResourceName(resource);
		}
		catch (IllegalArgumentException exception) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account resource not found");
		}

		model.addAttribute("records", userService.findAll(resource));
		model.addAttribute("resource", resource);
		model.addAttribute("role", role);
		model.addAttribute("roleName", role.name().charAt(0) + role.name().substring(1).toLowerCase());
		return "records";
	}

	@GetMapping("/records/{resource}/{id}/edit")
	public String editRecord(@PathVariable String resource, @PathVariable Long id, Model model) {
		UserResponse record = userService.findById(resource, id);
		UserRole role = record.role();
		model.addAttribute("role", role);
		model.addAttribute("roleName", role.name().charAt(0) + role.name().substring(1).toLowerCase());
		model.addAttribute("resource", resource);
		model.addAttribute("recordId", id);
		model.addAttribute("userRequest", new UserRequest(record.name(), record.email(), "",
				record.department(), record.studentNumber(), record.batch(), record.designation(), null));
		return "account-form";
	}

	@PostMapping("/records/{resource}/{id}/edit")
	public String updateRecord(@PathVariable String resource, @PathVariable Long id,
			@Valid @ModelAttribute UserRequest userRequest, BindingResult bindingResult, Model model) {
		UserRole role = UserRole.fromResourceName(resource);
		if (!bindingResult.hasErrors()) {
			try {
				userService.update(resource, id, userRequest);
				return "redirect:/records/" + resource;
			}
			catch (ResponseStatusException exception) {
				model.addAttribute("formError", exception.getReason());
			}
		}
		model.addAttribute("role", role);
		model.addAttribute("roleName", role.name().charAt(0) + role.name().substring(1).toLowerCase());
		model.addAttribute("resource", resource);
		model.addAttribute("recordId", id);
		return "account-form";
	}

	@PostMapping("/records/{resource}/{id}/delete")
	public String deleteRecord(@PathVariable String resource, @PathVariable Long id,
			RedirectAttributes redirectAttributes) {
		try {
			userService.delete(resource, id);
			redirectAttributes.addFlashAttribute("successMessage", "Record deleted.");
		}
		catch (ResponseStatusException exception) {
			redirectAttributes.addFlashAttribute("errorMessage", exception.getReason());
		}
		return "redirect:/records/" + resource;
	}

	@GetMapping("/login")
	public String login(@RequestParam(required = false) String error, Model model) {
		model.addAttribute("loginFailed", error != null);
		return "login";
	}

	@GetMapping("/signup")
	public String signup(@RequestParam UserRole role, Authentication authentication, Model model) {
		model.addAttribute("role", role);
		model.addAttribute("roleName", role.name().charAt(0) + role.name().substring(1).toLowerCase());
		model.addAttribute("userRequest", new UserRequest("", "", "", "", "", "", "", ""));
		model.addAttribute("adminSignupAllowed", isAdmin(authentication));
		return "signup";
	}

	private boolean isAdmin(Authentication authentication) {
		return authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
	}
}

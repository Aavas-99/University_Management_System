package com.university.university_management.controller;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.university.university_management.dto.UserRequest;
import com.university.university_management.dto.UserResponse;
import com.university.university_management.service.UniversityUserService;

@RestController
@RequestMapping("/api")
public class UserController {
	private final UniversityUserService userService;

	public UserController(UniversityUserService userService) {
		this.userService = userService;
	}

	@GetMapping("/{resource}")
	public List<UserResponse> findAll(@PathVariable String resource) {
		return userService.findAll(resource);
	}

	@GetMapping("/{resource}/{id}")
	public UserResponse findById(@PathVariable String resource, @PathVariable Long id) {
		return userService.findById(resource, id);
	}

	@PostMapping("/{resource}")
	public ResponseEntity<UserResponse> create(@PathVariable String resource,
			@Valid @RequestBody UserRequest request) {
		UserResponse created = userService.create(resource, request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(created.id())
				.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{resource}/{id}")
	public UserResponse update(@PathVariable String resource, @PathVariable Long id,
			@Valid @RequestBody UserRequest request) {
		return userService.update(resource, id, request);
	}

	@DeleteMapping("/{resource}/{id}")
	public ResponseEntity<Void> delete(@PathVariable String resource, @PathVariable Long id) {
		userService.delete(resource, id);
		return ResponseEntity.noContent().build();
	}
}

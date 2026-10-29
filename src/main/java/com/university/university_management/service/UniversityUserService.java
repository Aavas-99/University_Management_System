package com.university.university_management.service;

import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.university.university_management.dto.DashboardResponse;
import com.university.university_management.dto.UserRequest;
import com.university.university_management.dto.UserResponse;
import com.university.university_management.model.UniversityUser;
import com.university.university_management.model.UserRole;
import com.university.university_management.repository.UniversityUserRepository;

@Service
public class UniversityUserService {
	private final UniversityUserRepository repository;
	private final PasswordEncoder passwordEncoder;

	public UniversityUserService(UniversityUserRepository repository, PasswordEncoder passwordEncoder) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public List<UserResponse> findAll(String resourceName) {
		UserRole role = parseRole(resourceName);
		return repository.findAllByRoleOrderByIdAsc(role).stream()
				.map(UserResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public UserResponse findById(String resourceName, Long id) {
		UserRole role = parseRole(resourceName);
		return UserResponse.from(findAccount(id, role));
	}

	@Transactional(readOnly = true)
	public UserResponse findByEmail(String email) {
		return repository.findByEmail(email)
				.map(UserResponse::from)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
	}

	@Transactional(readOnly = true)
	public DashboardResponse dashboard(UserRole role) {
		var counts = new LinkedHashMap<String, Long>();
		if (role == UserRole.ADMIN) {
			counts.put("admins", repository.countByRole(UserRole.ADMIN));
			counts.put("teachers", repository.countByRole(UserRole.TEACHER));
			counts.put("students", repository.countByRole(UserRole.STUDENT));
		}
		else if (role == UserRole.TEACHER) {
			counts.put("teachers", repository.countByRole(UserRole.TEACHER));
			counts.put("students", repository.countByRole(UserRole.STUDENT));
		}
		else {
			counts.put("students", repository.countByRole(UserRole.STUDENT));
		}
		return new DashboardResponse(role, counts);
	}

	@Transactional
	public UserResponse create(String resourceName, UserRequest request) {
		UserRole role = parseRole(resourceName);
		validateAccount(role, request, true);
		ensureEmailAvailable(request.email(), null);
		String name = role == UserRole.ADMIN ? request.email() : request.name().trim();
		UniversityUser user = new UniversityUser(name, request.email(),
				passwordEncoder.encode(request.password()), role, request.department(),
				request.effectiveRoll(), request.batch(), request.designation());
		return UserResponse.from(repository.save(user));
	}

	@Transactional
	public UserResponse update(String resourceName, Long id, UserRequest request) {
		UserRole role = parseRole(resourceName);
		validateAccount(role, request, false);
		UniversityUser user = findAccount(id, role);
		ensureEmailAvailable(request.email(), id);
		String name = role == UserRole.ADMIN ? request.email() : request.name().trim();
		String passwordHash = request.password() == null || request.password().isBlank()
				? user.getPasswordHash()
				: passwordEncoder.encode(request.password());
		user.update(name, request.email(), passwordHash, request.department(),
				request.effectiveRoll(), request.batch(), request.designation());
		return UserResponse.from(repository.save(user));
	}

	@Transactional
	public void delete(String resourceName, Long id) {
		UserRole role = parseRole(resourceName);
		UniversityUser user = findAccount(id, role);
		if (role == UserRole.ADMIN && repository.countByRole(UserRole.ADMIN) == 1) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "The last admin account cannot be deleted");
		}
		repository.delete(user);
	}

	@Transactional
	public UserResponse updateStatus(Long id, boolean active) {
		UniversityUser user = repository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
		if (!active && user.getRole() == UserRole.ADMIN
				&& repository.countByRoleAndActiveTrue(UserRole.ADMIN) == 1) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "The last active admin cannot be disabled");
		}
		user.setActive(active);
		return UserResponse.from(repository.save(user));
	}

	private UniversityUser findAccount(Long id, UserRole role) {
		return repository.findById(id)
				.filter(user -> user.getRole() == role)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Account not found in " + role.resourceName()));
	}

	private void ensureEmailAvailable(String email, Long currentId) {
		repository.findByEmail(email)
				.filter(existing -> !existing.getId().equals(currentId))
				.ifPresent(existing -> {
					throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use");
				});
	}

	private void validateAccount(UserRole role, UserRequest request, boolean creating) {
		if (creating && (request.password() == null || request.password().length() < 8)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must contain at least 8 characters");
		}
		if (request.password() != null && !request.password().isBlank() && request.password().length() < 8) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must contain at least 8 characters");
		}
		if (role == UserRole.ADMIN) {
			return;
		}
		if (request.name() == null || request.name().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
		}
		if (request.department() == null || request.department().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department is required");
		}
		if (role == UserRole.STUDENT
				&& (request.effectiveRoll() == null || request.effectiveRoll().isBlank()
				|| request.batch() == null || request.batch().isBlank())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Roll and batch are required for students");
		}
		if (role == UserRole.TEACHER
				&& (request.designation() == null || request.designation().isBlank())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Designation is required for teachers");
		}
	}

	private UserRole parseRole(String resourceName) {
		try {
			return UserRole.fromResourceName(resourceName);
		}
		catch (IllegalArgumentException exception) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account resource not found");
		}
	}
}

package com.university.university_management.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.university.university_management.model.UniversityUser;
import com.university.university_management.model.UserRole;

public interface UniversityUserRepository extends JpaRepository<UniversityUser, Long> {
	Optional<UniversityUser> findByEmail(String email);

	boolean existsByEmail(String email);

	List<UniversityUser> findAllByRoleOrderByIdAsc(UserRole role);

	long countByRole(UserRole role);

	long countByRoleAndActiveTrue(UserRole role);
}

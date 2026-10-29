package com.university.university_management.dto;

import java.util.Map;

import com.university.university_management.model.UserRole;

public record DashboardResponse(UserRole role, Map<String, Long> recordCounts) {
}

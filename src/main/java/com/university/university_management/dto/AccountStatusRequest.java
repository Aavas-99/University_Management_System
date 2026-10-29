package com.university.university_management.dto;

import jakarta.validation.constraints.NotNull;

public record AccountStatusRequest(@NotNull Boolean active) {
}

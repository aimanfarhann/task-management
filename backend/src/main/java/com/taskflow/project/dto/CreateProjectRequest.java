package com.taskflow.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body of {@code POST /projects}.
 *
 * @param name the project name, 1 to 120 characters
 * @param description optional description, may be null
 * @param colorTag one of the eight preset color tag keys
 */
public record CreateProjectRequest(
    @NotBlank @Size(max = 120) String name,
    String description,
    @NotNull @Pattern(regexp = ColorTag.PATTERN, message = ColorTag.MESSAGE) String colorTag) {}

package com.taskflow.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body of {@code PUT /projects/{id}} — a full replacement of the mutable fields.
 *
 * @param name the new project name, 1 to 120 characters
 * @param description the new description, may be null
 * @param colorTag one of the eight preset color tag keys
 * @param archived the new archived state
 */
public record UpdateProjectRequest(
    @NotBlank @Size(max = 120) String name,
    String description,
    @NotNull @Pattern(regexp = ColorTag.PATTERN, message = ColorTag.MESSAGE) String colorTag,
    @NotNull Boolean archived) {}

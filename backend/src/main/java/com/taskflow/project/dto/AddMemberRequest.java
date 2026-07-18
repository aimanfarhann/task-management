package com.taskflow.project.dto;

import com.taskflow.project.ProjectRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body of {@code POST /projects/{id}/members}.
 *
 * @param email the email of the user to add
 * @param projectRole the role to grant; defaults to MEMBER when omitted
 */
public record AddMemberRequest(@NotBlank @Email String email, ProjectRole projectRole) {}

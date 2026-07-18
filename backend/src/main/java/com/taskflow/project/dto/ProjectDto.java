package com.taskflow.project.dto;

import com.taskflow.project.ProjectRole;
import java.time.Instant;

/**
 * API representation of a project (API contract {@code ProjectDto}).
 *
 * @param id the project's database id
 * @param name the project name
 * @param description the description, or null
 * @param colorTag one of the eight preset color tag keys
 * @param archived whether the project is archived
 * @param createdAt creation timestamp, serialized as an ISO-8601 string
 * @param myRole the calling user's role in the project; ADMINs who are not members are reported as
 *     OWNER since they hold owner-level abilities
 * @param memberCount the number of project members
 */
public record ProjectDto(
    Long id,
    String name,
    String description,
    String colorTag,
    boolean archived,
    Instant createdAt,
    ProjectRole myRole,
    long memberCount) {}

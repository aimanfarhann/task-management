package com.taskflow.user.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request body of {@code PATCH /admin/users/{userId}/active}. A boxed {@link Boolean} so a missing
 * field fails {@link NotNull} validation rather than silently defaulting to {@code false}.
 *
 * @param active the desired account active flag
 */
public record SetActiveRequest(@NotNull Boolean active) {}

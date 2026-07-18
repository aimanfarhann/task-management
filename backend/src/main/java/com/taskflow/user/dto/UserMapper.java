package com.taskflow.user.dto;

import com.taskflow.user.User;

/** Manual entity-to-DTO mapping for users (ARCHITECTURE.md §3.1 — no MapStruct). */
public final class UserMapper {

  private UserMapper() {}

  /**
   * Maps a user entity to its API representation.
   *
   * @param user the persisted user entity
   * @return the contract-shaped DTO
   */
  public static UserDto toDto(User user) {
    return new UserDto(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole());
  }
}

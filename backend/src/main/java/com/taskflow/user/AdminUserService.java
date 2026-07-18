package com.taskflow.user;

import com.taskflow.common.exception.ConflictException;
import com.taskflow.common.exception.ForbiddenException;
import com.taskflow.common.exception.ResourceNotFoundException;
import com.taskflow.common.security.AuthenticatedUser;
import com.taskflow.user.dto.AdminUserDto;
import com.taskflow.user.dto.UserMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin user-management business logic (PRD §5.5): listing every user and toggling account active
 * state. Every method asserts the caller holds the system {@code ADMIN} role as its first line
 * (SCHEMA.md §4).
 */
@Service
public class AdminUserService {

  private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);

  private final UserRepository userRepository;

  /**
   * Creates the service.
   *
   * @param userRepository data access for users
   */
  public AdminUserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Lists every user for the admin page, oldest first. ADMIN only.
   *
   * @param currentUser the authenticated caller
   * @return all users ordered by creation time ascending, then id ascending
   * @throws ForbiddenException if the caller is not a system ADMIN (403)
   */
  @Transactional(readOnly = true)
  public List<AdminUserDto> listAllUsers(AuthenticatedUser currentUser) {
    assertAdmin(currentUser);
    return userRepository.findAllByOrderByCreatedAtAscIdAsc().stream()
        .map(UserMapper::toAdminDto)
        .toList();
  }

  /**
   * Sets a user's account active flag. ADMIN only. Reactivation is always allowed; an admin may not
   * deactivate their own account, which would lock them out (self-lockout guard).
   *
   * @param userId the target user's id
   * @param active the desired active state
   * @param currentUser the authenticated caller
   * @return the updated user
   * @throws ForbiddenException if the caller is not a system ADMIN (403)
   * @throws ResourceNotFoundException with code {@code USER_NOT_FOUND} if the id is unknown (404)
   * @throws ConflictException with code {@code CANNOT_DEACTIVATE_SELF} if an admin deactivates
   *     their own account, or {@code CANNOT_DEACTIVATE_LAST_ADMIN} if it would leave no active
   *     administrator (409)
   */
  @Transactional
  public AdminUserDto setActive(Long userId, boolean active, AuthenticatedUser currentUser) {
    assertAdmin(currentUser);
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "USER_NOT_FOUND", "User " + userId + " was not found"));
    if (!active && currentUser.id().equals(userId)) {
      throw new ConflictException(
          "CANNOT_DEACTIVATE_SELF", "You cannot deactivate your own account");
    }
    if (!active && user.getRole() == Role.ADMIN && user.isActive()) {
      // Serialize concurrent admin deactivations and refuse if this would remove the last active
      // administrator — otherwise two admins deactivating each other at once could leave zero
      // admins, an API-unrecoverable lockout (analogous to the last-OWNER guard, SCHEMA.md §4).
      boolean anotherAdminRemains =
          userRepository.findActiveByRoleForUpdate(Role.ADMIN).stream()
              .anyMatch(admin -> !admin.getId().equals(userId));
      if (!anotherAdminRemains) {
        throw new ConflictException(
            "CANNOT_DEACTIVATE_LAST_ADMIN", "Cannot deactivate the last active administrator");
      }
    }
    if (active) {
      user.activate();
    } else {
      user.deactivate();
    }
    log.info(
        "Admin {} {} user {}", currentUser.id(), active ? "reactivated" : "deactivated", userId);
    return UserMapper.toAdminDto(user);
  }

  private void assertAdmin(AuthenticatedUser currentUser) {
    if (!currentUser.isAdmin()) {
      throw new ForbiddenException("Only an administrator may perform this action");
    }
  }
}

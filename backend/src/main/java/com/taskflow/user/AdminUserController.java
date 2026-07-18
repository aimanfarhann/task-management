package com.taskflow.user;

import com.taskflow.common.security.AuthenticatedUser;
import com.taskflow.common.security.CurrentUser;
import com.taskflow.user.dto.AdminUserDto;
import com.taskflow.user.dto.SetActiveRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP endpoints for admin user management (PRD §5.5). ADMIN role enforced in the service layer.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

  private final AdminUserService adminUserService;

  /**
   * Creates the controller.
   *
   * @param adminUserService the admin user-management business logic
   */
  public AdminUserController(AdminUserService adminUserService) {
    this.adminUserService = adminUserService;
  }

  /**
   * Lists every user, oldest first. ADMIN only.
   *
   * @param currentUser the authenticated caller
   * @return 200 with the user list
   */
  @GetMapping
  public List<AdminUserDto> listUsers(@CurrentUser AuthenticatedUser currentUser) {
    return adminUserService.listAllUsers(currentUser);
  }

  /**
   * Sets a user's account active flag. ADMIN only.
   *
   * @param userId the target user's id
   * @param request the desired active state
   * @param currentUser the authenticated caller
   * @return 200 with the updated user
   */
  @PatchMapping("/{userId}/active")
  public AdminUserDto setActive(
      @PathVariable Long userId,
      @Valid @RequestBody SetActiveRequest request,
      @CurrentUser AuthenticatedUser currentUser) {
    return adminUserService.setActive(userId, request.active(), currentUser);
  }
}

package com.taskflow.dashboard;

import com.taskflow.common.security.AuthenticatedUser;
import com.taskflow.common.security.CurrentUser;
import com.taskflow.dashboard.dto.DashboardDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP endpoint for the authenticated caller's dashboard. */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

  private final DashboardService dashboardService;

  /**
   * Creates the controller.
   *
   * @param dashboardService the dashboard business logic
   */
  public DashboardController(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  /**
   * Returns the caller's dashboard: their assigned tasks and per-project status rollups. Any
   * authenticated user.
   *
   * @param currentUser the authenticated caller
   * @return 200 with the dashboard
   */
  @GetMapping
  public DashboardDto getDashboard(@CurrentUser AuthenticatedUser currentUser) {
    return dashboardService.getDashboard(currentUser);
  }
}

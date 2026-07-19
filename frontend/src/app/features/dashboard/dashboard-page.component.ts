import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import { ColorDotComponent } from '../../shared/color-dot/color-dot.component';
import { DueDatePipe } from '../../shared/due-date.pipe';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { PriorityBadgeComponent } from '../../shared/priority-badge/priority-badge.component';
import { SkeletonComponent } from '../../shared/skeleton/skeleton.component';
import { StatusChipComponent } from '../../shared/status-chip/status-chip.component';
import { DashboardStore } from './dashboard.store';

/**
 * Landing page (PRD §5.4): the caller's assigned tasks across all projects,
 * grouped by due date and each linking to its project, plus per-project
 * status-count summary cards.
 */
@Component({
  selector: 'tf-dashboard-page',
  imports: [
    RouterLink,
    NgIcon,
    ColorDotComponent,
    DueDatePipe,
    EmptyStateComponent,
    PriorityBadgeComponent,
    SkeletonComponent,
    StatusChipComponent,
  ],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss',
})
export class DashboardPageComponent implements OnInit {
  protected readonly dashboardStore = inject(DashboardStore);
  private readonly router = inject(Router);

  protected readonly skeletonSlots = [0, 1, 2];

  ngOnInit(): void {
    void this.dashboardStore.load();
  }

  protected reload(): void {
    void this.dashboardStore.load();
  }

  protected goToProjects(): void {
    void this.router.navigate(['/projects']);
  }
}

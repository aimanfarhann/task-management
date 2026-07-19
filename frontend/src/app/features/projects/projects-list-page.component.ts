import { Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import { HlmButton } from '@spartan-ng/helm/button';
import { ProjectDto } from '../../core/api/models';
import { NotificationService } from '../../core/ui/notification.service';
import { ColorDotComponent } from '../../shared/color-dot/color-dot.component';
import { DialogService } from '../../shared/dialog/dialog.service';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { SkeletonComponent } from '../../shared/skeleton/skeleton.component';
import { ProjectFormDialogComponent } from './project-form-dialog.component';
import { ProjectStore } from './project.store';

@Component({
  selector: 'tf-projects-list-page',
  imports: [
    DatePipe,
    RouterLink,
    NgIcon,
    HlmButton,
    ColorDotComponent,
    EmptyStateComponent,
    SkeletonComponent,
  ],
  templateUrl: './projects-list-page.component.html',
  styleUrl: './projects-list-page.component.scss',
})
export class ProjectsListPageComponent implements OnInit {
  protected readonly projectStore = inject(ProjectStore);
  private readonly dialogs = inject(DialogService);
  private readonly notificationService = inject(NotificationService);

  protected readonly skeletonSlots = [0, 1, 2, 3, 4, 5];

  ngOnInit(): void {
    void this.projectStore.loadProjects();
  }

  protected reload(): void {
    void this.projectStore.loadProjects();
  }

  protected toggleShowArchived(checked: boolean): void {
    this.projectStore.showArchived.set(checked);
  }

  protected async openCreateDialog(): Promise<void> {
    const created = await this.dialogs.open<ProjectDto>(ProjectFormDialogComponent, null, {
      width: '30rem',
    });
    if (created) {
      this.notificationService.success('Project created');
    }
  }
}

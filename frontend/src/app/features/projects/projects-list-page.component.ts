import { Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { ProjectDto } from '../../core/api/models';
import { NotificationService } from '../../core/ui/notification.service';
import { ColorDotComponent } from '../../shared/color-dot/color-dot.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { SkeletonComponent } from '../../shared/skeleton/skeleton.component';
import { ProjectFormDialogComponent } from './project-form-dialog.component';
import { ProjectStore } from './project.store';

@Component({
  selector: 'tf-projects-list-page',
  imports: [
    DatePipe,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatIconModule,
    MatSlideToggleModule,
    ColorDotComponent,
    EmptyStateComponent,
    SkeletonComponent,
  ],
  templateUrl: './projects-list-page.component.html',
  styleUrl: './projects-list-page.component.scss',
})
export class ProjectsListPageComponent implements OnInit {
  protected readonly projectStore = inject(ProjectStore);
  private readonly dialog = inject(MatDialog);
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

  protected openCreateDialog(): void {
    const dialogRef = this.dialog.open<
      ProjectFormDialogComponent,
      ProjectDto | null,
      ProjectDto | undefined
    >(ProjectFormDialogComponent, { data: null, width: '480px' });
    dialogRef.afterClosed().subscribe((created) => {
      if (created) {
        this.notificationService.success('Project created');
      }
    });
  }
}

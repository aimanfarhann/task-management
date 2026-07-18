import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { firstValueFrom } from 'rxjs';
import { toApiErrorBody } from '../../core/api/api-error';
import { MemberDto, ProjectDto } from '../../core/api/models';
import { AuthStore } from '../../core/auth/auth.store';
import { NotificationService } from '../../core/ui/notification.service';
import { ColorDotComponent } from '../../shared/color-dot/color-dot.component';
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from '../../shared/confirm-dialog/confirm-dialog.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { SkeletonComponent } from '../../shared/skeleton/skeleton.component';
import { AddMemberDialogComponent, AddMemberDialogData } from './add-member-dialog.component';
import { ProjectFormDialogComponent } from './project-form-dialog.component';
import { ProjectStore } from './project.store';

@Component({
  selector: 'tf-project-detail-page',
  imports: [
    DatePipe,
    RouterLink,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    MatTableModule,
    MatTooltipModule,
    ColorDotComponent,
    EmptyStateComponent,
    SkeletonComponent,
  ],
  templateUrl: './project-detail-page.component.html',
  styleUrl: './project-detail-page.component.scss',
})
export class ProjectDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly notificationService = inject(NotificationService);
  protected readonly projectStore = inject(ProjectStore);
  protected readonly authStore = inject(AuthStore);

  /** Inline error for archive/edit actions in the header. */
  protected readonly actionError = signal<string | null>(null);
  /** Inline error for member operations (e.g. LAST_OWNER conflicts). */
  protected readonly memberError = signal<string | null>(null);
  protected readonly archiving = signal(false);

  protected readonly memberColumns = ['displayName', 'email', 'projectRole', 'joinedAt', 'actions'];

  /** OWNER of this project or system ADMIN — may edit, archive, manage members. */
  protected readonly canManage = computed(() => {
    const project = this.projectStore.selectedProject();
    return (
      project !== null &&
      (project.myRole === 'OWNER' || this.authStore.currentUser()?.role === 'ADMIN')
    );
  });

  constructor() {
    this.route.paramMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      this.actionError.set(null);
      this.memberError.set(null);
      const projectId = Number(params.get('id'));
      // Guard against a hand-edited/non-numeric URL that would otherwise request /projects/NaN.
      if (!Number.isInteger(projectId) || projectId <= 0) {
        void this.router.navigate(['/projects']);
        return;
      }
      void this.projectStore.loadProject(projectId);
    });
  }

  protected isSelf(member: MemberDto): boolean {
    return member.userId === this.authStore.currentUser()?.id;
  }

  protected canRemove(member: MemberDto): boolean {
    return this.canManage() || this.isSelf(member);
  }

  protected removeLabel(member: MemberDto): string {
    return this.isSelf(member) ? 'Leave project' : `Remove ${member.displayName}`;
  }

  protected goBack(): void {
    void this.router.navigate(['/projects']);
  }

  protected openEditDialog(): void {
    const project = this.projectStore.selectedProject();
    if (!project) {
      return;
    }
    this.dialog
      .open<ProjectFormDialogComponent, ProjectDto, ProjectDto | undefined>(
        ProjectFormDialogComponent,
        { data: project, width: '480px' },
      )
      .afterClosed()
      .subscribe((updated) => {
        if (updated) {
          this.notificationService.success('Project updated');
        }
      });
  }

  protected async toggleArchived(): Promise<void> {
    const project = this.projectStore.selectedProject();
    if (!project) {
      return;
    }
    this.archiving.set(true);
    this.actionError.set(null);
    try {
      const updated = await this.projectStore.updateProject(project.id, {
        name: project.name,
        description: project.description,
        colorTag: project.colorTag,
        archived: !project.archived,
      });
      this.notificationService.success(
        updated.archived ? 'Project archived' : 'Project unarchived',
      );
    } catch (error) {
      this.actionError.set(toApiErrorBody(error).message);
    } finally {
      this.archiving.set(false);
    }
  }

  protected openAddMemberDialog(): void {
    const project = this.projectStore.selectedProject();
    if (!project) {
      return;
    }
    this.dialog
      .open<AddMemberDialogComponent, AddMemberDialogData, MemberDto | undefined>(
        AddMemberDialogComponent,
        { data: { projectId: project.id }, width: '420px' },
      )
      .afterClosed()
      .subscribe((added) => {
        if (added) {
          this.notificationService.success('Member added');
        }
      });
  }

  protected async removeMember(member: MemberDto): Promise<void> {
    const project = this.projectStore.selectedProject();
    if (!project) {
      return;
    }
    const leaving = this.isSelf(member);
    const confirmed = await firstValueFrom(
      this.dialog
        .open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
          data: {
            title: leaving ? 'Leave project?' : 'Remove member?',
            message: leaving
              ? `You will lose access to “${project.name}”.`
              : `${member.displayName} will lose access to “${project.name}”.`,
            confirmLabel: leaving ? 'Leave' : 'Remove',
            destructive: true,
          },
          width: '360px',
        })
        .afterClosed(),
    );
    if (!confirmed) {
      return;
    }
    this.memberError.set(null);
    try {
      await this.projectStore.removeMember(project.id, member.userId);
      if (leaving) {
        this.notificationService.success('You left the project');
        await this.router.navigate(['/projects']);
      } else {
        this.notificationService.success('Member removed');
      }
    } catch (error) {
      const apiError = toApiErrorBody(error);
      this.memberError.set(
        apiError.code === 'LAST_OWNER'
          ? 'A project needs at least one owner. Add another owner before removing this one.'
          : apiError.message,
      );
    }
  }
}

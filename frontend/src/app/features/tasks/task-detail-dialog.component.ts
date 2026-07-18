import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { firstValueFrom } from 'rxjs';
import { toApiErrorBody } from '../../core/api/api-error';
import { CommentDto, TaskDto } from '../../core/api/models';
import { AuthStore } from '../../core/auth/auth.store';
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from '../../shared/confirm-dialog/confirm-dialog.component';
import { DueDatePipe } from '../../shared/due-date.pipe';
import { isPastDue } from '../../shared/due-date.util';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { PriorityBadgeComponent } from '../../shared/priority-badge/priority-badge.component';
import { SkeletonComponent } from '../../shared/skeleton/skeleton.component';
import { StatusChipComponent } from '../../shared/status-chip/status-chip.component';
import { CommentStore } from './comment.store';

export interface TaskDetailDialogData {
  projectId: number;
  task: TaskDto;
  /** True for a project OWNER or system ADMIN — may delete any comment. */
  canModerateComments: boolean;
}

/**
 * Read-only task detail with its comment thread. Loads comments on open, lets a
 * member add a comment (1..2000 chars) and delete their own (owners/admins may
 * delete any). Closes with 'edit' when the user chooses to edit the task so the
 * container can open the edit dialog.
 */
@Component({
  selector: 'tf-task-detail-dialog',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    DueDatePipe,
    EmptyStateComponent,
    PriorityBadgeComponent,
    SkeletonComponent,
    StatusChipComponent,
  ],
  templateUrl: './task-detail-dialog.component.html',
  styleUrl: './task-detail-dialog.component.scss',
})
export class TaskDetailDialogComponent {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly authStore = inject(AuthStore);
  private readonly dialog = inject(MatDialog);
  private readonly dialogRef =
    inject<MatDialogRef<TaskDetailDialogComponent, 'edit'>>(MatDialogRef);
  protected readonly commentStore = inject(CommentStore);
  protected readonly data = inject<TaskDetailDialogData>(MAT_DIALOG_DATA);

  protected readonly task = this.data.task;
  protected readonly overdue = isPastDue(this.task.dueDate) && this.task.status !== 'DONE';
  protected readonly submitting = signal(false);
  protected readonly commentError = signal<string | null>(null);
  protected readonly skeletonSlots = [0, 1];

  protected readonly form = this.formBuilder.group({
    body: ['', [Validators.required, Validators.maxLength(2000)]],
  });

  protected readonly hasComments = computed(() => this.commentStore.comments().length > 0);

  constructor() {
    void this.commentStore.loadComments(this.data.projectId, this.task.id);
  }

  protected canDelete(comment: CommentDto): boolean {
    return (
      this.data.canModerateComments || comment.author.userId === this.authStore.currentUser()?.id
    );
  }

  protected requestEdit(): void {
    this.dialogRef.close('edit');
  }

  protected async submit(): Promise<void> {
    this.form.controls.body.setValue(this.form.controls.body.value.trim());
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.commentError.set(null);
    try {
      await this.commentStore.addComment(this.data.projectId, this.task.id, {
        body: this.form.controls.body.value,
      });
      this.form.reset();
    } catch (error) {
      this.commentError.set(toApiErrorBody(error).message);
    } finally {
      this.submitting.set(false);
    }
  }

  protected async deleteComment(comment: CommentDto): Promise<void> {
    const confirmed = await firstValueFrom(
      this.dialog
        .open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
          data: {
            title: 'Delete comment?',
            message: 'This comment will be permanently removed.',
            confirmLabel: 'Delete',
            destructive: true,
          },
          width: '360px',
        })
        .afterClosed(),
    );
    if (!confirmed) {
      return;
    }
    this.commentError.set(null);
    try {
      await this.commentStore.deleteComment(this.data.projectId, this.task.id, comment.id);
    } catch (error) {
      this.commentError.set(toApiErrorBody(error).message);
    }
  }
}

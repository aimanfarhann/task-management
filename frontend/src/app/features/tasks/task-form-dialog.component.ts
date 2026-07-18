import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { applyFieldErrors, toApiErrorBody } from '../../core/api/api-error';
import { MemberDto, TaskDto, TaskPriority, TaskStatus } from '../../core/api/models';
import { fromIsoDate, toIsoDate } from '../../shared/due-date.util';
import { TaskStore } from './task.store';

export interface TaskFormDialogData {
  projectId: number;
  members: MemberDto[];
  task: TaskDto | null;
}

/**
 * Create/edit task dialog. Dialog data carries the project, its members (for
 * the assignee select) and the task to edit, or null to create. Closes with
 * the saved TaskDto on success.
 */
@Component({
  selector: 'tf-task-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './task-form-dialog.component.html',
  styleUrl: './task-form-dialog.component.scss',
})
export class TaskFormDialogComponent {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly taskStore = inject(TaskStore);
  private readonly dialogRef = inject<MatDialogRef<TaskFormDialogComponent, TaskDto>>(MatDialogRef);
  private readonly data = inject<TaskFormDialogData>(MAT_DIALOG_DATA);

  protected readonly members = this.data.members;
  protected readonly isEdit = this.data.task !== null;
  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.formBuilder.group({
    title: [this.data.task?.title ?? '', [Validators.required, Validators.maxLength(200)]],
    description: [this.data.task?.description ?? ''],
    status: this.formBuilder.control<TaskStatus>(this.data.task?.status ?? 'TODO'),
    priority: this.formBuilder.control<TaskPriority>(
      this.data.task?.priority ?? 'MEDIUM',
      Validators.required,
    ),
    dueDate: this.formBuilder.control<Date | null>(fromIsoDate(this.data.task?.dueDate ?? null)),
    assigneeId: this.formBuilder.control<number | null>(this.data.task?.assignee?.userId ?? null),
  });

  protected async submit(): Promise<void> {
    this.form.controls.title.setValue(this.form.controls.title.value.trim());
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    const description = raw.description.trim() === '' ? null : raw.description.trim();
    const dueDate = raw.dueDate ? toIsoDate(raw.dueDate) : null;
    try {
      const saved = this.data.task
        ? await this.taskStore.updateTask(this.data.projectId, this.data.task.id, {
            title: raw.title,
            description,
            status: raw.status,
            priority: raw.priority,
            dueDate,
            assigneeId: raw.assigneeId,
          })
        : await this.taskStore.createTask(this.data.projectId, {
            title: raw.title,
            description,
            priority: raw.priority,
            dueDate,
            assigneeId: raw.assigneeId,
          });
      this.dialogRef.close(saved);
    } catch (error) {
      const apiError = toApiErrorBody(error);
      if (apiError.code === 'ASSIGNEE_NOT_MEMBER') {
        this.form.controls.assigneeId.setErrors({
          server: 'That person is not a member of this project.',
        });
        this.form.controls.assigneeId.markAsTouched();
      } else if (!applyFieldErrors(this.form, apiError)) {
        this.errorMessage.set(apiError.message);
      }
    } finally {
      this.submitting.set(false);
    }
  }
}

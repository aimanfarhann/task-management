import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { HlmButton } from '@spartan-ng/helm/button';
import { HlmInput } from '@spartan-ng/helm/input';
import { HlmLabel } from '@spartan-ng/helm/label';
import { HlmTextarea } from '@spartan-ng/helm/textarea';
import { applyFieldErrors, toApiErrorBody } from '../../core/api/api-error';
import { MemberDto, TaskDto, TaskPriority, TaskStatus } from '../../core/api/models';
import { TaskStore } from './task.store';

export interface TaskFormDialogData {
  projectId: number;
  members: MemberDto[];
  task: TaskDto | null;
}

/**
 * Create/edit task dialog. Dialog data carries the project, its members (for
 * the assignee select) and the task to edit, or null to create. Closes with
 * the saved TaskDto on success. The due date is a native date input, so its
 * control value is already an ISO `yyyy-MM-dd` string (or '' when unset).
 */
@Component({
  selector: 'tf-task-form-dialog',
  imports: [ReactiveFormsModule, HlmButton, HlmInput, HlmLabel, HlmTextarea],
  templateUrl: './task-form-dialog.component.html',
})
export class TaskFormDialogComponent {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly taskStore = inject(TaskStore);
  private readonly dialogRef = inject<DialogRef<TaskDto>>(DialogRef);
  private readonly data = inject<TaskFormDialogData>(DIALOG_DATA);

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
    dueDate: [this.data.task?.dueDate ?? ''],
    assigneeId: this.formBuilder.control<number | null>(this.data.task?.assignee?.userId ?? null),
  });

  protected cancel(): void {
    this.dialogRef.close();
  }

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
    const dueDate = raw.dueDate === '' ? null : raw.dueDate;
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

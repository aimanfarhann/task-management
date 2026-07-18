import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { applyFieldErrors, toApiErrorBody } from '../../core/api/api-error';
import { MemberDto, ProjectRole } from '../../core/api/models';
import { ProjectStore } from './project.store';

export interface AddMemberDialogData {
  projectId: number;
}

/** Add-member-by-email dialog; closes with the created MemberDto on success. */
@Component({
  selector: 'tf-add-member-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './add-member-dialog.component.html',
  styleUrl: './add-member-dialog.component.scss',
})
export class AddMemberDialogComponent {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly projectStore = inject(ProjectStore);
  private readonly dialogRef =
    inject<MatDialogRef<AddMemberDialogComponent, MemberDto>>(MatDialogRef);
  private readonly data = inject<AddMemberDialogData>(MAT_DIALOG_DATA);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.formBuilder.group({
    email: ['', [Validators.required, Validators.email]],
    projectRole: this.formBuilder.control<ProjectRole>('MEMBER', Validators.required),
  });

  protected async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    try {
      const member = await this.projectStore.addMember(
        this.data.projectId,
        this.form.getRawValue(),
      );
      this.dialogRef.close(member);
    } catch (error) {
      const apiError = toApiErrorBody(error);
      if (apiError.code === 'USER_NOT_FOUND') {
        this.setEmailError('No TaskFlow account uses this email.');
      } else if (apiError.code === 'ALREADY_MEMBER') {
        this.setEmailError('This user is already a member of the project.');
      } else if (!applyFieldErrors(this.form, apiError)) {
        this.errorMessage.set(apiError.message);
      }
    } finally {
      this.submitting.set(false);
    }
  }

  private setEmailError(message: string): void {
    this.form.controls.email.setErrors({ server: message });
    this.form.controls.email.markAsTouched();
  }
}

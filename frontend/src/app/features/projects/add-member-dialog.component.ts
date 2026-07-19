import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { HlmButton } from '@spartan-ng/helm/button';
import { HlmInput } from '@spartan-ng/helm/input';
import { HlmLabel } from '@spartan-ng/helm/label';
import { applyFieldErrors, toApiErrorBody } from '../../core/api/api-error';
import { MemberDto, ProjectRole } from '../../core/api/models';
import { ProjectStore } from './project.store';

export interface AddMemberDialogData {
  projectId: number;
}

/** Add-member-by-email dialog; closes with the created MemberDto on success. */
@Component({
  selector: 'tf-add-member-dialog',
  imports: [ReactiveFormsModule, HlmButton, HlmInput, HlmLabel],
  templateUrl: './add-member-dialog.component.html',
})
export class AddMemberDialogComponent {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly projectStore = inject(ProjectStore);
  private readonly dialogRef = inject<DialogRef<MemberDto>>(DialogRef);
  private readonly data = inject<AddMemberDialogData>(DIALOG_DATA);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.formBuilder.group({
    email: ['', [Validators.required, Validators.email]],
    projectRole: this.formBuilder.control<ProjectRole>('MEMBER', Validators.required),
  });

  protected cancel(): void {
    this.dialogRef.close();
  }

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

import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { applyFieldErrors, toApiErrorBody } from '../../core/api/api-error';
import { COLOR_TAGS, ColorTag, ProjectDto } from '../../core/api/models';
import { ColorDotComponent } from '../../shared/color-dot/color-dot.component';
import { ProjectStore } from './project.store';

/**
 * Create/edit project dialog. Dialog data is the project to edit, or null to
 * create; closes with the saved ProjectDto on success.
 */
@Component({
  selector: 'tf-project-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatRadioModule,
    ColorDotComponent,
  ],
  templateUrl: './project-form-dialog.component.html',
  styleUrl: './project-form-dialog.component.scss',
})
export class ProjectFormDialogComponent {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly projectStore = inject(ProjectStore);
  private readonly dialogRef =
    inject<MatDialogRef<ProjectFormDialogComponent, ProjectDto>>(MatDialogRef);

  protected readonly project = inject<ProjectDto | null>(MAT_DIALOG_DATA);
  protected readonly colorTags = COLOR_TAGS;
  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.formBuilder.group({
    name: [this.project?.name ?? '', [Validators.required, Validators.maxLength(120)]],
    description: [this.project?.description ?? ''],
    colorTag: this.formBuilder.control<ColorTag>(
      this.project?.colorTag ?? 'blue',
      Validators.required,
    ),
  });

  protected async submit(): Promise<void> {
    // Normalize the name before validating so a whitespace-only value fails `required` client-side
    // and trailing/leading spaces are not stored or counted against the 120-char cap.
    this.form.controls.name.setValue(this.form.controls.name.value.trim());
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const { name, description, colorTag } = this.form.getRawValue();
    const normalizedDescription = description.trim() === '' ? null : description;
    try {
      const saved = this.project
        ? await this.projectStore.updateProject(this.project.id, {
            name,
            description: normalizedDescription,
            colorTag,
            archived: this.project.archived,
          })
        : await this.projectStore.createProject({
            name,
            description: normalizedDescription,
            colorTag,
          });
      this.dialogRef.close(saved);
    } catch (error) {
      const apiError = toApiErrorBody(error);
      if (!applyFieldErrors(this.form, apiError)) {
        this.errorMessage.set(apiError.message);
      }
    } finally {
      this.submitting.set(false);
    }
  }
}

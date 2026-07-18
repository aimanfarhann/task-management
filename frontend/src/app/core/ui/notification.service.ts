import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

/**
 * Success feedback per DESIGN.md §7: snackbar, 3 s, bottom center, one at a
 * time (MatSnackBar dismisses any open snackbar before showing the next).
 * Failures are rendered inline next to their source, never as snackbars.
 */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly snackBar = inject(MatSnackBar);

  success(message: string): void {
    this.snackBar.open(message, undefined, { duration: 3000 });
  }
}

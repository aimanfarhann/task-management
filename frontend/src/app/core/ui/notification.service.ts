import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

/**
 * Success feedback per DESIGN.md §7: snackbar, 3 s, bottom center, one at a
 * time (MatSnackBar dismisses any open snackbar before showing the next).
 * Form validation failures stay inline next to their source; only transient
 * whole-action failures with no inline home — a failed optimistic board
 * drag-drop that has already rolled back — surface as an error snackbar.
 */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly snackBar = inject(MatSnackBar);

  success(message: string): void {
    // Non-interrupting live-region announcement (role=status) for success (DESIGN §8).
    this.snackBar.open(message, undefined, { duration: 3000, politeness: 'polite' });
  }

  error(message: string): void {
    // Interrupting live-region announcement (role=alert) so failures are heard (DESIGN §8).
    this.snackBar.open(message, 'Dismiss', { duration: 5000, politeness: 'assertive' });
  }
}

import { Injectable } from '@angular/core';
import { toast } from 'ngx-sonner';

/**
 * Success feedback per DESIGN.md §7: a transient toast, one at a time. Form
 * validation failures stay inline next to their source; only transient
 * whole-action failures with no inline home — a failed optimistic board
 * drag-drop that has already rolled back — surface as an error toast.
 * Rendered by the single <hlm-toaster> in the app shell (sonner manages its own
 * polite/assertive live region for screen-reader announcements, DESIGN §8).
 */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  success(message: string): void {
    toast.success(message, { duration: 3000 });
  }

  error(message: string): void {
    toast.error(message, { duration: 5000 });
  }
}

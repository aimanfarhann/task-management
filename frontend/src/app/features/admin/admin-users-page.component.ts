import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { NgIcon } from '@ng-icons/core';
import { HlmButton } from '@spartan-ng/helm/button';
import { toApiErrorBody } from '../../core/api/api-error';
import { AdminUserDto, ApiErrorBody } from '../../core/api/models';
import { AuthStore } from '../../core/auth/auth.store';
import { NotificationService } from '../../core/ui/notification.service';
import { ConfirmationService } from '../../shared/confirm-dialog/confirmation.service';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { SkeletonComponent } from '../../shared/skeleton/skeleton.component';
import { AdminStore } from './admin.store';

/** Explains why an admin cannot deactivate their own account (self-lockout guard). */
const CANNOT_DEACTIVATE_SELF_MESSAGE = 'You cannot deactivate your own account.';

/**
 * Admin users page (PRD §5.5): a table of every user with an activate /
 * deactivate action. Deactivation is confirmed via a destructive dialog and
 * blocked on the signed-in admin's own row to prevent self-lockout; a
 * CANNOT_DEACTIVATE_SELF conflict is surfaced inline as a server-side fallback.
 */
@Component({
  selector: 'tf-admin-users-page',
  imports: [DatePipe, NgIcon, HlmButton, EmptyStateComponent, SkeletonComponent],
  templateUrl: './admin-users-page.component.html',
  styleUrl: './admin-users-page.component.scss',
})
export class AdminUsersPageComponent implements OnInit {
  protected readonly adminStore = inject(AdminStore);
  private readonly authStore = inject(AuthStore);
  private readonly confirmation = inject(ConfirmationService);
  private readonly notificationService = inject(NotificationService);

  protected readonly skeletonSlots = [0, 1, 2, 3, 4];

  /** Inline error for activate/deactivate failures (e.g. CANNOT_DEACTIVATE_SELF). */
  protected readonly actionError = signal<string | null>(null);
  /** Id of the user whose active state is changing, so its row action disables during the request. */
  protected readonly pendingUserId = signal<number | null>(null);

  ngOnInit(): void {
    this.actionError.set(null);
    void this.adminStore.loadUsers();
  }

  protected reload(): void {
    this.actionError.set(null);
    void this.adminStore.loadUsers();
  }

  protected isSelf(user: AdminUserDto): boolean {
    return user.id === this.authStore.currentUser()?.id;
  }

  protected roleLabel(user: AdminUserDto): string {
    return user.role === 'ADMIN' ? 'Admin' : 'User';
  }

  protected actionLabel(user: AdminUserDto): string {
    return `${user.active ? 'Deactivate' : 'Reactivate'} ${user.displayName}`;
  }

  protected async toggleActive(user: AdminUserDto): Promise<void> {
    if (user.active) {
      const confirmed = await this.confirmation.confirm({
        title: 'Deactivate user?',
        message: `${user.displayName} will be signed out and blocked from signing in until reactivated.`,
        confirmLabel: 'Deactivate',
        destructive: true,
      });
      if (!confirmed) {
        return;
      }
    }
    await this.applyActive(user, !user.active);
  }

  private async applyActive(user: AdminUserDto, active: boolean): Promise<void> {
    this.actionError.set(null);
    this.pendingUserId.set(user.id);
    try {
      await this.adminStore.setActive(user.id, active);
      this.notificationService.success(active ? 'User reactivated' : 'User deactivated');
    } catch (error) {
      this.actionError.set(this.actionErrorMessage(toApiErrorBody(error)));
    } finally {
      this.pendingUserId.set(null);
    }
  }

  private actionErrorMessage(apiError: ApiErrorBody): string {
    switch (apiError.code) {
      case 'CANNOT_DEACTIVATE_SELF':
        return CANNOT_DEACTIVATE_SELF_MESSAGE;
      case 'CANNOT_DEACTIVATE_LAST_ADMIN':
        return 'You cannot deactivate the last active administrator.';
      case 'USER_NOT_FOUND':
        return 'That user no longer exists. Refresh the list.';
      default:
        return apiError.message;
    }
  }
}

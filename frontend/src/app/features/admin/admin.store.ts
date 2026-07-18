import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { toApiErrorBody } from '../../core/api/api-error';
import { AdminApiClient } from '../../core/api/admin-api.client';
import { AdminUserDto } from '../../core/api/models';

/**
 * Signal store for the admin users feature: every user plus loading and error
 * state. `loadUsers` swallows errors into the error signal for page-level
 * rendering; `setActive` reconciles the returned server copy into the list and
 * rethrows on failure so the page can surface conflicts inline (e.g. a
 * CANNOT_DEACTIVATE_SELF self-lockout), matching the M2 mutation pattern.
 */
@Injectable({ providedIn: 'root' })
export class AdminStore {
  private readonly adminApi = inject(AdminApiClient);

  readonly users = signal<AdminUserDto[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  async loadUsers(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.users.set(await firstValueFrom(this.adminApi.list()));
    } catch (error) {
      this.error.set(toApiErrorBody(error).message);
    } finally {
      this.loading.set(false);
    }
  }

  async setActive(userId: number, active: boolean): Promise<AdminUserDto> {
    const updated = await firstValueFrom(this.adminApi.setActive(userId, { active }));
    this.users.update((users) => users.map((user) => (user.id === updated.id ? updated : user)));
    return updated;
  }
}

import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { toApiErrorBody } from '../../core/api/api-error';
import { DashboardDto, DashboardTaskDto } from '../../core/api/models';
import { DashboardApiClient } from '../../core/api/dashboard-api.client';
import { DueBucket, dueBucket } from '../../shared/due-date.util';

/** A due-date bucket with its display label and the tasks that fall in it. */
export interface DueDateGroup {
  key: DueBucket;
  label: string;
  tasks: DashboardTaskDto[];
}

const BUCKET_ORDER: { key: DueBucket; label: string }[] = [
  { key: 'OVERDUE', label: 'Overdue' },
  { key: 'TODAY', label: 'Today' },
  { key: 'UPCOMING', label: 'Upcoming' },
  { key: 'NO_DUE_DATE', label: 'No due date' },
];

/**
 * Signal store for the dashboard: the caller's assigned tasks across every
 * project plus per-project status roll-ups. `dueDateGroups` splits my tasks
 * into the Overdue / Today / Upcoming / No due date buckets (PRD §5.4),
 * dropping empty buckets so the page only renders groups that have tasks.
 */
@Injectable({ providedIn: 'root' })
export class DashboardStore {
  private readonly dashboardApi = inject(DashboardApiClient);

  readonly dashboard = signal<DashboardDto | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly myTasks = computed(() => this.dashboard()?.myTasks ?? []);
  readonly projectSummaries = computed(() => this.dashboard()?.projectSummaries ?? []);

  readonly dueDateGroups = computed<DueDateGroup[]>(() => {
    const tasks = this.myTasks();
    return BUCKET_ORDER.map((bucket) => ({
      ...bucket,
      tasks: tasks.filter((task) => dueBucket(task.dueDate) === bucket.key),
    })).filter((group) => group.tasks.length > 0);
  });

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.dashboard.set(await firstValueFrom(this.dashboardApi.get()));
    } catch (error) {
      this.error.set(toApiErrorBody(error).message);
    } finally {
      this.loading.set(false);
    }
  }
}

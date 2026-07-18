import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { buildDashboard, buildDashboardTask, buildProjectSummary } from '../../testing/fixtures';
import { toIsoDate } from '../../shared/due-date.util';
import { DashboardStore } from './dashboard.store';

function relativeIso(offsetDays: number): string {
  const now = new Date();
  return toIsoDate(new Date(now.getFullYear(), now.getMonth(), now.getDate() + offsetDays));
}

describe('DashboardStore', () => {
  let httpMock: HttpTestingController;
  let store: DashboardStore;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
    store = TestBed.inject(DashboardStore);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('load_success_storesDashboardAndExposesSummaries', async () => {
    const loadPromise = store.load();
    expect(store.loading()).toBe(true);
    httpMock
      .expectOne('/api/v1/dashboard')
      .flush(buildDashboard({ projectSummaries: [buildProjectSummary({ projectId: 9 })] }));
    await loadPromise;

    expect(store.loading()).toBe(false);
    expect(store.projectSummaries().map((summary) => summary.projectId)).toEqual([9]);
    expect(store.error()).toBeNull();
  });

  it('dueDateGroups_splitsMyTasksIntoDateBuckets', () => {
    store.dashboard.set(
      buildDashboard({
        myTasks: [
          buildDashboardTask({ id: 1, dueDate: relativeIso(-1) }),
          buildDashboardTask({ id: 2, dueDate: relativeIso(0) }),
          buildDashboardTask({ id: 3, dueDate: relativeIso(1) }),
          buildDashboardTask({ id: 4, dueDate: null }),
        ],
        projectSummaries: [],
      }),
    );

    const byLabel = Object.fromEntries(
      store.dueDateGroups().map((group) => [group.label, group.tasks.map((task) => task.id)]),
    );
    expect(byLabel).toEqual({
      Overdue: [1],
      Today: [2],
      Upcoming: [3],
      'No due date': [4],
    });
  });

  it('dueDateGroups_dropsEmptyBuckets', () => {
    store.dashboard.set(
      buildDashboard({
        myTasks: [buildDashboardTask({ id: 1, dueDate: relativeIso(5) })],
        projectSummaries: [],
      }),
    );

    expect(store.dueDateGroups().map((group) => group.label)).toEqual(['Upcoming']);
  });
});

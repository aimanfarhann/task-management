import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { buildTask } from '../../testing/fixtures';
import { TaskStore } from './task.store';

describe('TaskStore', () => {
  let httpMock: HttpTestingController;
  let store: TaskStore;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
    store = TestBed.inject(TaskStore);
  });

  afterEach(() => {
    httpMock.verify();
  });

  async function seedTasks(...tasks: ReturnType<typeof buildTask>[]): Promise<void> {
    const loadPromise = store.loadTasks(1);
    httpMock.expectOne('/api/v1/projects/1/tasks').flush(tasks);
    await loadPromise;
  }

  it('loadTasks_success_storesTasksAndClearsLoading', async () => {
    const loadPromise = store.loadTasks(1);
    expect(store.loading()).toBe(true);
    httpMock.expectOne('/api/v1/projects/1/tasks').flush([buildTask({ id: 1 })]);
    await loadPromise;

    expect(store.loading()).toBe(false);
    expect(store.tasks().map((task) => task.id)).toEqual([1]);
    expect(store.error()).toBeNull();
  });

  it('loadTasks_failure_storesErrorMessage', async () => {
    const loadPromise = store.loadTasks(1);
    httpMock
      .expectOne('/api/v1/projects/1/tasks')
      .flush(
        { code: 'FORBIDDEN', message: 'Not a member', fieldErrors: [] },
        { status: 403, statusText: 'Forbidden' },
      );
    await loadPromise;

    expect(store.error()).toBe('Not a member');
    expect(store.loading()).toBe(false);
  });

  it('boardColumns_groupTasksByStatusPreservingOrder', async () => {
    await seedTasks(
      buildTask({ id: 1, status: 'TODO' }),
      buildTask({ id: 2, status: 'IN_PROGRESS' }),
      buildTask({ id: 3, status: 'DONE' }),
      buildTask({ id: 4, status: 'TODO' }),
    );

    expect(store.todoTasks().map((task) => task.id)).toEqual([1, 4]);
    expect(store.inProgressTasks().map((task) => task.id)).toEqual([2]);
    expect(store.doneTasks().map((task) => task.id)).toEqual([3]);
  });

  it('changeStatus_success_appliesOptimisticallyThenReconciles', async () => {
    await seedTasks(buildTask({ id: 1, status: 'TODO', title: 'Before' }));

    const changePromise = store.changeStatus(1, 1, 'DONE');
    // Optimistic: moved before the request resolves.
    expect(store.doneTasks().map((task) => task.id)).toEqual([1]);

    const request = httpMock.expectOne('/api/v1/projects/1/tasks/1/status');
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ status: 'DONE' });
    request.flush(buildTask({ id: 1, status: 'DONE', title: 'After' }));
    await changePromise;

    // Reconciled with the server copy.
    expect(store.tasks()[0].title).toBe('After');
    expect(store.doneTasks().length).toBe(1);
  });

  it('changeStatus_failure_rollsBackAndRethrows', async () => {
    await seedTasks(buildTask({ id: 1, status: 'TODO' }));

    const changePromise = store.changeStatus(1, 1, 'DONE');
    expect(store.doneTasks().length).toBe(1);

    httpMock
      .expectOne('/api/v1/projects/1/tasks/1/status')
      .flush(
        { code: 'UNKNOWN_ERROR', message: 'boom', fieldErrors: [] },
        { status: 500, statusText: 'Server Error' },
      );
    await expect(changePromise).rejects.toBeTruthy();

    expect(store.todoTasks().map((task) => task.id)).toEqual([1]);
    expect(store.doneTasks().length).toBe(0);
  });

  it('changeStatus_failureAfterConcurrentCreate_rollsBackOnlyTheMovedTask', async () => {
    await seedTasks(buildTask({ id: 1, status: 'TODO' }));

    const changePromise = store.changeStatus(1, 1, 'DONE');
    const statusRequest = httpMock.expectOne('/api/v1/projects/1/tasks/1/status');

    // A concurrent create lands while the status PATCH is still in flight.
    const createPromise = store.createTask(1, { title: 'Concurrent' });
    httpMock
      .expectOne('/api/v1/projects/1/tasks')
      .flush(buildTask({ id: 2, title: 'Concurrent', status: 'IN_PROGRESS' }));
    await createPromise;

    // The status change then fails: only task 1 rolls back to TODO; the concurrent task 2 survives
    // (a whole-list snapshot rollback would have wiped task 2).
    statusRequest.flush(
      { code: 'UNKNOWN_ERROR', message: 'boom', fieldErrors: [] },
      { status: 500, statusText: 'Server Error' },
    );
    await expect(changePromise).rejects.toBeTruthy();

    expect(store.tasks().map((task) => task.id)).toEqual([1, 2]);
    expect(store.todoTasks().map((task) => task.id)).toEqual([1]);
    expect(store.inProgressTasks().map((task) => task.id)).toEqual([2]);
    expect(store.doneTasks().length).toBe(0);
  });

  it('changeStatus_sameStatus_isNoOp', async () => {
    await seedTasks(buildTask({ id: 1, status: 'TODO' }));

    await store.changeStatus(1, 1, 'TODO');

    // No PATCH request expected; httpMock.verify() in afterEach enforces it.
    expect(store.todoTasks().map((task) => task.id)).toEqual([1]);
  });

  it('createTask_success_appendsToList', async () => {
    await seedTasks(buildTask({ id: 1 }));

    const createPromise = store.createTask(1, { title: 'Fresh' });
    const request = httpMock.expectOne('/api/v1/projects/1/tasks');
    expect(request.request.method).toBe('POST');
    request.flush(buildTask({ id: 2, title: 'Fresh' }));
    await createPromise;

    expect(store.tasks().map((task) => task.id)).toEqual([1, 2]);
  });

  it('updateTask_success_replacesTaskInList', async () => {
    await seedTasks(buildTask({ id: 1, title: 'Old', status: 'TODO' }));

    const updatePromise = store.updateTask(1, 1, {
      title: 'New',
      description: null,
      status: 'IN_PROGRESS',
      priority: 'HIGH',
      dueDate: null,
      assigneeId: null,
    });
    const request = httpMock.expectOne('/api/v1/projects/1/tasks/1');
    expect(request.request.method).toBe('PUT');
    request.flush(buildTask({ id: 1, title: 'New', status: 'IN_PROGRESS' }));
    await updatePromise;

    expect(store.tasks()[0].title).toBe('New');
    expect(store.inProgressTasks().map((task) => task.id)).toEqual([1]);
  });

  it('deleteTask_success_removesFromList', async () => {
    await seedTasks(buildTask({ id: 1 }), buildTask({ id: 2 }));

    const deletePromise = store.deleteTask(1, 1);
    const request = httpMock.expectOne('/api/v1/projects/1/tasks/1');
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });
    await deletePromise;

    expect(store.tasks().map((task) => task.id)).toEqual([2]);
  });
});

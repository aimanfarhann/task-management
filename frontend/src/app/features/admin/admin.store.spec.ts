import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { buildAdminUser } from '../../testing/fixtures';
import { AdminStore } from './admin.store';

describe('AdminStore', () => {
  let httpMock: HttpTestingController;
  let store: AdminStore;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
    store = TestBed.inject(AdminStore);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('loadUsers_success_storesUsersAndClearsLoading', async () => {
    const loadPromise = store.loadUsers();
    expect(store.loading()).toBe(true);
    httpMock
      .expectOne('/api/v1/admin/users')
      .flush([buildAdminUser({ id: 1 }), buildAdminUser({ id: 2 })]);
    await loadPromise;

    expect(store.loading()).toBe(false);
    expect(store.users().map((user) => user.id)).toEqual([1, 2]);
    expect(store.error()).toBeNull();
  });

  it('loadUsers_failure_setsErrorMessage', async () => {
    const loadPromise = store.loadUsers();
    httpMock
      .expectOne('/api/v1/admin/users')
      .flush(
        { code: 'FORBIDDEN', message: 'Admin access required', fieldErrors: [] },
        { status: 403, statusText: 'Forbidden' },
      );
    await loadPromise;

    expect(store.loading()).toBe(false);
    expect(store.error()).toBe('Admin access required');
    expect(store.users()).toEqual([]);
  });

  it('setActive_success_reconcilesUpdatedUserIntoList', async () => {
    store.users.set([
      buildAdminUser({ id: 1, active: true }),
      buildAdminUser({ id: 2, active: true }),
    ]);

    const setActivePromise = store.setActive(2, false);
    const request = httpMock.expectOne('/api/v1/admin/users/2/active');
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ active: false });
    request.flush(buildAdminUser({ id: 2, active: false }));
    await setActivePromise;

    expect(store.users().find((user) => user.id === 2)?.active).toBe(false);
    expect(store.users().find((user) => user.id === 1)?.active).toBe(true);
  });

  it('setActive_conflict_rethrowsAndLeavesListUnchanged', async () => {
    store.users.set([buildAdminUser({ id: 1, active: true })]);

    const setActivePromise = store.setActive(1, false);
    httpMock
      .expectOne('/api/v1/admin/users/1/active')
      .flush(
        { code: 'CANNOT_DEACTIVATE_SELF', message: 'Cannot deactivate self', fieldErrors: [] },
        { status: 409, statusText: 'Conflict' },
      );

    await expect(setActivePromise).rejects.toBeTruthy();
    expect(store.users()[0].active).toBe(true);
  });
});

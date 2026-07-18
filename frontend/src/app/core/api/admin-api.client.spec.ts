import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { buildAdminUser } from '../../testing/fixtures';
import { AdminApiClient } from './admin-api.client';

describe('AdminApiClient', () => {
  let httpMock: HttpTestingController;
  let client: AdminApiClient;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
    client = TestBed.inject(AdminApiClient);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('list_getsAllUsers', () => {
    client.list().subscribe();

    const request = httpMock.expectOne('/api/v1/admin/users');
    expect(request.request.method).toBe('GET');
    request.flush([buildAdminUser()]);
  });

  it('setActive_patchesActiveFlagForUser', () => {
    client.setActive(7, { active: false }).subscribe();

    const request = httpMock.expectOne('/api/v1/admin/users/7/active');
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ active: false });
    request.flush(buildAdminUser({ id: 7, active: false }));
  });
});

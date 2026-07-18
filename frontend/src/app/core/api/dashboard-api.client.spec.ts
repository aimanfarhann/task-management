import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { buildDashboard } from '../../testing/fixtures';
import { DashboardApiClient } from './dashboard-api.client';

describe('DashboardApiClient', () => {
  let httpMock: HttpTestingController;
  let client: DashboardApiClient;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
    client = TestBed.inject(DashboardApiClient);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('get_requestsDashboard', () => {
    client.get().subscribe();

    const request = httpMock.expectOne('/api/v1/dashboard');
    expect(request.request.method).toBe('GET');
    request.flush(buildDashboard());
  });
});

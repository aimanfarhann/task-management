import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { DashboardDto } from './models';

/** Typed wrapper for the /api/v1/dashboard endpoint. */
@Injectable({ providedIn: 'root' })
export class DashboardApiClient {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/dashboard';

  get(): Observable<DashboardDto> {
    return this.http.get<DashboardDto>(this.baseUrl);
  }
}

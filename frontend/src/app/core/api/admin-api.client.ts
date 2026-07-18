import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AdminUserDto, SetUserActiveRequest } from './models';

/** Typed wrapper for the /api/v1/admin/users endpoints (ADMIN only). */
@Injectable({ providedIn: 'root' })
export class AdminApiClient {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/admin/users';

  list(): Observable<AdminUserDto[]> {
    return this.http.get<AdminUserDto[]>(this.baseUrl);
  }

  setActive(userId: number, request: SetUserActiveRequest): Observable<AdminUserDto> {
    return this.http.patch<AdminUserDto>(`${this.baseUrl}/${userId}/active`, request);
  }
}

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AddMemberRequest,
  CreateProjectRequest,
  MemberDto,
  ProjectDto,
  UpdateProjectRequest,
} from './models';

/** Typed wrapper for the /api/v1/projects endpoints. */
@Injectable({ providedIn: 'root' })
export class ProjectsApiClient {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/projects';

  list(): Observable<ProjectDto[]> {
    return this.http.get<ProjectDto[]>(this.baseUrl);
  }

  get(id: number): Observable<ProjectDto> {
    return this.http.get<ProjectDto>(`${this.baseUrl}/${id}`);
  }

  create(request: CreateProjectRequest): Observable<ProjectDto> {
    return this.http.post<ProjectDto>(this.baseUrl, request);
  }

  update(id: number, request: UpdateProjectRequest): Observable<ProjectDto> {
    return this.http.put<ProjectDto>(`${this.baseUrl}/${id}`, request);
  }

  listMembers(id: number): Observable<MemberDto[]> {
    return this.http.get<MemberDto[]>(`${this.baseUrl}/${id}/members`);
  }

  addMember(id: number, request: AddMemberRequest): Observable<MemberDto> {
    return this.http.post<MemberDto>(`${this.baseUrl}/${id}/members`, request);
  }

  removeMember(id: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}/members/${userId}`);
  }
}

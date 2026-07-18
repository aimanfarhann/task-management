import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { toApiErrorBody } from '../../core/api/api-error';
import {
  AddMemberRequest,
  ApiErrorBody,
  CreateProjectRequest,
  MemberDto,
  ProjectDto,
  UpdateProjectRequest,
} from '../../core/api/models';
import { ProjectsApiClient } from '../../core/api/projects-api.client';

/**
 * Signal store for the projects feature: the projects list, the currently
 * opened project and its members. Load methods swallow errors into error
 * signals for page-level rendering; mutation methods rethrow so dialogs can
 * map field errors onto their forms.
 */
@Injectable({ providedIn: 'root' })
export class ProjectStore {
  private readonly projectsApi = inject(ProjectsApiClient);

  readonly projects = signal<ProjectDto[]>([]);
  readonly projectsLoading = signal(false);
  readonly projectsError = signal<string | null>(null);

  /** Archived projects are hidden by default; toggled client-side. */
  readonly showArchived = signal(false);
  readonly visibleProjects = computed(() =>
    this.showArchived() ? this.projects() : this.projects().filter((project) => !project.archived),
  );

  readonly selectedProject = signal<ProjectDto | null>(null);
  readonly selectedProjectLoading = signal(false);
  readonly selectedProjectError = signal<ApiErrorBody | null>(null);
  readonly members = signal<MemberDto[]>([]);

  async loadProjects(): Promise<void> {
    this.projectsLoading.set(true);
    this.projectsError.set(null);
    try {
      this.projects.set(await firstValueFrom(this.projectsApi.list()));
    } catch (error) {
      this.projectsError.set(toApiErrorBody(error).message);
    } finally {
      this.projectsLoading.set(false);
    }
  }

  async createProject(request: CreateProjectRequest): Promise<ProjectDto> {
    const created = await firstValueFrom(this.projectsApi.create(request));
    this.projects.update((projects) => [...projects, created]);
    return created;
  }

  async loadProject(id: number): Promise<void> {
    this.selectedProjectLoading.set(true);
    this.selectedProjectError.set(null);
    this.selectedProject.set(null);
    this.members.set([]);
    try {
      const [project, members] = await Promise.all([
        firstValueFrom(this.projectsApi.get(id)),
        firstValueFrom(this.projectsApi.listMembers(id)),
      ]);
      this.selectedProject.set(project);
      this.members.set(members);
    } catch (error) {
      this.selectedProjectError.set(toApiErrorBody(error));
    } finally {
      this.selectedProjectLoading.set(false);
    }
  }

  async updateProject(id: number, request: UpdateProjectRequest): Promise<ProjectDto> {
    const updated = await firstValueFrom(this.projectsApi.update(id, request));
    if (this.selectedProject()?.id === updated.id) {
      this.selectedProject.set(updated);
    }
    this.projects.update((projects) =>
      projects.map((project) => (project.id === updated.id ? updated : project)),
    );
    return updated;
  }

  async addMember(projectId: number, request: AddMemberRequest): Promise<MemberDto> {
    const member = await firstValueFrom(this.projectsApi.addMember(projectId, request));
    this.members.update((members) => [...members, member]);
    this.adjustMemberCount(projectId, 1);
    return member;
  }

  async removeMember(projectId: number, userId: number): Promise<void> {
    await firstValueFrom(this.projectsApi.removeMember(projectId, userId));
    this.members.update((members) => members.filter((member) => member.userId !== userId));
    this.adjustMemberCount(projectId, -1);
  }

  private adjustMemberCount(projectId: number, delta: number): void {
    const selected = this.selectedProject();
    if (selected?.id === projectId) {
      this.selectedProject.set({ ...selected, memberCount: selected.memberCount + delta });
    }
    this.projects.update((projects) =>
      projects.map((project) =>
        project.id === projectId
          ? { ...project, memberCount: project.memberCount + delta }
          : project,
      ),
    );
  }
}

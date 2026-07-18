import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { buildMember, buildProject } from '../../testing/fixtures';
import { ProjectStore } from './project.store';

describe('ProjectStore', () => {
  let httpMock: HttpTestingController;
  let store: ProjectStore;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
    store = TestBed.inject(ProjectStore);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('loadProjects_success_storesProjectsAndClearsLoading', async () => {
    const loadPromise = store.loadProjects();
    expect(store.projectsLoading()).toBe(true);
    httpMock.expectOne('/api/v1/projects').flush([buildProject()]);
    await loadPromise;

    expect(store.projectsLoading()).toBe(false);
    expect(store.projects()).toEqual([buildProject()]);
    expect(store.projectsError()).toBeNull();
  });

  it('loadProjects_failure_storesErrorMessage', async () => {
    const loadPromise = store.loadProjects();
    httpMock
      .expectOne('/api/v1/projects')
      .flush(
        { code: 'UNKNOWN_ERROR', message: 'Server exploded', fieldErrors: [] },
        { status: 500, statusText: 'Server Error' },
      );
    await loadPromise;

    expect(store.projectsError()).toBe('Server exploded');
    expect(store.projectsLoading()).toBe(false);
  });

  it('visibleProjects_showArchivedToggle_filtersArchivedProjects', async () => {
    const loadPromise = store.loadProjects();
    httpMock
      .expectOne('/api/v1/projects')
      .flush([buildProject({ id: 1 }), buildProject({ id: 2, archived: true })]);
    await loadPromise;

    expect(store.visibleProjects().map((project) => project.id)).toEqual([1]);

    store.showArchived.set(true);
    expect(store.visibleProjects().map((project) => project.id)).toEqual([1, 2]);
  });

  it('createProject_success_appendsToList', async () => {
    const createPromise = store.createProject({
      name: 'Apollo',
      description: null,
      colorTag: 'blue',
    });
    const request = httpMock.expectOne('/api/v1/projects');
    expect(request.request.method).toBe('POST');
    request.flush(buildProject({ id: 5, name: 'Apollo' }));
    const created = await createPromise;

    expect(created.id).toBe(5);
    expect(store.projects().map((project) => project.id)).toEqual([5]);
  });

  it('loadProject_success_storesProjectAndMembers', async () => {
    const loadPromise = store.loadProject(7);
    expect(store.selectedProjectLoading()).toBe(true);
    httpMock.expectOne('/api/v1/projects/7').flush(buildProject({ id: 7 }));
    httpMock.expectOne('/api/v1/projects/7/members').flush([buildMember()]);
    await loadPromise;

    expect(store.selectedProject()?.id).toBe(7);
    expect(store.members()).toEqual([buildMember()]);
    expect(store.selectedProjectLoading()).toBe(false);
  });

  it('loadProject_forbidden_storesErrorBody', async () => {
    const loadPromise = store.loadProject(7);
    httpMock
      .expectOne('/api/v1/projects/7')
      .flush(
        { code: 'FORBIDDEN', message: 'You are not a member of this project', fieldErrors: [] },
        { status: 403, statusText: 'Forbidden' },
      );
    httpMock.expectOne('/api/v1/projects/7/members').flush([]);
    await loadPromise;

    expect(store.selectedProject()).toBeNull();
    expect(store.selectedProjectError()?.code).toBe('FORBIDDEN');
  });

  it('updateProject_success_replacesProjectInListAndSelection', async () => {
    const loadPromise = store.loadProject(1);
    httpMock.expectOne('/api/v1/projects/1').flush(buildProject());
    httpMock.expectOne('/api/v1/projects/1/members').flush([buildMember()]);
    await loadPromise;

    const updatePromise = store.updateProject(1, {
      name: 'Apollo 2',
      description: null,
      colorTag: 'teal',
      archived: true,
    });
    const request = httpMock.expectOne('/api/v1/projects/1');
    expect(request.request.method).toBe('PUT');
    request.flush(buildProject({ name: 'Apollo 2', colorTag: 'teal', archived: true }));
    await updatePromise;

    expect(store.selectedProject()?.name).toBe('Apollo 2');
    expect(store.selectedProject()?.archived).toBe(true);
  });

  it('addMember_success_appendsMemberAndBumpsCount', async () => {
    const loadPromise = store.loadProject(1);
    httpMock.expectOne('/api/v1/projects/1').flush(buildProject({ memberCount: 1 }));
    httpMock.expectOne('/api/v1/projects/1/members').flush([buildMember()]);
    await loadPromise;

    const addPromise = store.addMember(1, { email: 'grace@example.com', projectRole: 'MEMBER' });
    const request = httpMock.expectOne('/api/v1/projects/1/members');
    expect(request.request.method).toBe('POST');
    request.flush(buildMember({ userId: 2, email: 'grace@example.com', projectRole: 'MEMBER' }));
    await addPromise;

    expect(store.members().length).toBe(2);
    expect(store.selectedProject()?.memberCount).toBe(2);
  });

  it('removeMember_success_removesMemberAndDecrementsCount', async () => {
    const loadPromise = store.loadProject(1);
    httpMock.expectOne('/api/v1/projects/1').flush(buildProject({ memberCount: 2 }));
    httpMock
      .expectOne('/api/v1/projects/1/members')
      .flush([buildMember(), buildMember({ userId: 2, projectRole: 'MEMBER' })]);
    await loadPromise;

    const removePromise = store.removeMember(1, 2);
    const request = httpMock.expectOne('/api/v1/projects/1/members/2');
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });
    await removePromise;

    expect(store.members().map((member) => member.userId)).toEqual([1]);
    expect(store.selectedProject()?.memberCount).toBe(1);
  });

  it('removeMember_lastOwnerConflict_rethrowsAndKeepsState', async () => {
    const loadPromise = store.loadProject(1);
    httpMock.expectOne('/api/v1/projects/1').flush(buildProject({ memberCount: 1 }));
    httpMock.expectOne('/api/v1/projects/1/members').flush([buildMember()]);
    await loadPromise;

    const removePromise = store.removeMember(1, 1);
    httpMock
      .expectOne('/api/v1/projects/1/members/1')
      .flush(
        { code: 'LAST_OWNER', message: 'Cannot remove the last owner', fieldErrors: [] },
        { status: 409, statusText: 'Conflict' },
      );
    await expect(removePromise).rejects.toBeTruthy();

    expect(store.members().length).toBe(1);
    expect(store.selectedProject()?.memberCount).toBe(1);
  });
});

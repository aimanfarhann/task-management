import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { buildComment, buildTask } from '../../testing/fixtures';
import { TasksApiClient } from './tasks-api.client';

describe('TasksApiClient', () => {
  let httpMock: HttpTestingController;
  let client: TasksApiClient;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
    client = TestBed.inject(TasksApiClient);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('listTasks_noFilters_getsTasksWithoutQueryParams', () => {
    client.listTasks(7).subscribe();

    const request = httpMock.expectOne((req) => req.url === '/api/v1/projects/7/tasks');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.keys().length).toBe(0);
    request.flush([buildTask()]);
  });

  it('listTasks_withFilters_setsStatusPriorityAssigneeParams', () => {
    client.listTasks(7, { status: 'TODO', priority: 'HIGH', assigneeId: 5 }).subscribe();

    const request = httpMock.expectOne((req) => req.url === '/api/v1/projects/7/tasks');
    expect(request.request.params.get('status')).toBe('TODO');
    expect(request.request.params.get('priority')).toBe('HIGH');
    expect(request.request.params.get('assigneeId')).toBe('5');
    request.flush([]);
  });

  it('getTask_getsSingleTask', () => {
    client.getTask(7, 3).subscribe();

    const request = httpMock.expectOne('/api/v1/projects/7/tasks/3');
    expect(request.request.method).toBe('GET');
    request.flush(buildTask({ id: 3 }));
  });

  it('createTask_postsRequestBody', () => {
    const body = { title: 'New task', priority: 'HIGH' as const, dueDate: '2026-08-01' };
    client.createTask(7, body).subscribe();

    const request = httpMock.expectOne('/api/v1/projects/7/tasks');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    request.flush(buildTask());
  });

  it('updateTask_putsFullBody', () => {
    const body = {
      title: 'Edited',
      description: 'Now with detail',
      status: 'IN_PROGRESS' as const,
      priority: 'LOW' as const,
      dueDate: null,
      assigneeId: 2,
    };
    client.updateTask(7, 3, body).subscribe();

    const request = httpMock.expectOne('/api/v1/projects/7/tasks/3');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(body);
    request.flush(buildTask({ id: 3 }));
  });

  it('updateStatus_patchesStatusBody', () => {
    client.updateStatus(7, 3, { status: 'DONE' }).subscribe();

    const request = httpMock.expectOne('/api/v1/projects/7/tasks/3/status');
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ status: 'DONE' });
    request.flush(buildTask({ id: 3, status: 'DONE' }));
  });

  it('deleteTask_sendsDelete', () => {
    client.deleteTask(7, 3).subscribe();

    const request = httpMock.expectOne('/api/v1/projects/7/tasks/3');
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('listComments_getsCommentsForTask', () => {
    client.listComments(7, 3).subscribe();

    const request = httpMock.expectOne('/api/v1/projects/7/tasks/3/comments');
    expect(request.request.method).toBe('GET');
    request.flush([buildComment()]);
  });

  it('createComment_postsBody', () => {
    client.createComment(7, 3, { body: 'Nice work' }).subscribe();

    const request = httpMock.expectOne('/api/v1/projects/7/tasks/3/comments');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ body: 'Nice work' });
    request.flush(buildComment());
  });

  it('deleteComment_sendsDelete', () => {
    client.deleteComment(7, 3, 9).subscribe();

    const request = httpMock.expectOne('/api/v1/projects/7/tasks/3/comments/9');
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });
  });
});

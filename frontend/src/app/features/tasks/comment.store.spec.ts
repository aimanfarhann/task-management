import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { buildComment } from '../../testing/fixtures';
import { CommentStore } from './comment.store';

describe('CommentStore', () => {
  let httpMock: HttpTestingController;
  let store: CommentStore;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
    store = TestBed.inject(CommentStore);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('loadComments_success_storesCommentsChronologically', async () => {
    const loadPromise = store.loadComments(7, 3);
    expect(store.loading()).toBe(true);
    httpMock
      .expectOne('/api/v1/projects/7/tasks/3/comments')
      .flush([buildComment({ id: 1 }), buildComment({ id: 2 })]);
    await loadPromise;

    expect(store.loading()).toBe(false);
    expect(store.comments().map((comment) => comment.id)).toEqual([1, 2]);
    expect(store.error()).toBeNull();
  });

  it('loadComments_failure_storesErrorMessage', async () => {
    const loadPromise = store.loadComments(7, 3);
    httpMock
      .expectOne('/api/v1/projects/7/tasks/3/comments')
      .flush(
        { code: 'TASK_NOT_FOUND', message: 'No such task', fieldErrors: [] },
        { status: 404, statusText: 'Not Found' },
      );
    await loadPromise;

    expect(store.error()).toBe('No such task');
    expect(store.comments()).toEqual([]);
  });

  it('loadComments_staleResponse_isIgnored', async () => {
    // Open task 3 (slow), then open task 9 before task 3's response lands.
    const firstLoad = store.loadComments(7, 3);
    const firstRequest = httpMock.expectOne('/api/v1/projects/7/tasks/3/comments');

    const secondLoad = store.loadComments(7, 9);
    httpMock.expectOne('/api/v1/projects/7/tasks/9/comments').flush([buildComment({ id: 20 })]);
    await secondLoad;

    // The stale task-3 response arrives last and must NOT overwrite task 9's comments.
    firstRequest.flush([buildComment({ id: 99 })]);
    await firstLoad;

    expect(store.comments().map((comment) => comment.id)).toEqual([20]);
  });

  it('addComment_success_appendsToEnd', async () => {
    const loadPromise = store.loadComments(7, 3);
    httpMock.expectOne('/api/v1/projects/7/tasks/3/comments').flush([buildComment({ id: 1 })]);
    await loadPromise;

    const addPromise = store.addComment(7, 3, { body: 'Second' });
    const request = httpMock.expectOne('/api/v1/projects/7/tasks/3/comments');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ body: 'Second' });
    request.flush(buildComment({ id: 2, body: 'Second' }));
    await addPromise;

    expect(store.comments().map((comment) => comment.id)).toEqual([1, 2]);
  });

  it('deleteComment_success_removesComment', async () => {
    const loadPromise = store.loadComments(7, 3);
    httpMock
      .expectOne('/api/v1/projects/7/tasks/3/comments')
      .flush([buildComment({ id: 1 }), buildComment({ id: 2 })]);
    await loadPromise;

    const deletePromise = store.deleteComment(7, 3, 1);
    const request = httpMock.expectOne('/api/v1/projects/7/tasks/3/comments/1');
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });
    await deletePromise;

    expect(store.comments().map((comment) => comment.id)).toEqual([2]);
  });

  it('deleteComment_forbidden_rethrowsAndKeepsComment', async () => {
    const loadPromise = store.loadComments(7, 3);
    httpMock.expectOne('/api/v1/projects/7/tasks/3/comments').flush([buildComment({ id: 1 })]);
    await loadPromise;

    const deletePromise = store.deleteComment(7, 3, 1);
    httpMock
      .expectOne('/api/v1/projects/7/tasks/3/comments/1')
      .flush(
        { code: 'FORBIDDEN', message: 'Not your comment', fieldErrors: [] },
        { status: 403, statusText: 'Forbidden' },
      );
    await expect(deletePromise).rejects.toBeTruthy();

    expect(store.comments().map((comment) => comment.id)).toEqual([1]);
  });
});

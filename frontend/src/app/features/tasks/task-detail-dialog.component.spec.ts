import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import {
  buildComment,
  buildTask,
  buildUserSummary,
  seedStoredSession,
  settle,
} from '../../testing/fixtures';
import { TaskDetailDialogComponent, TaskDetailDialogData } from './task-detail-dialog.component';

function configure(data: TaskDetailDialogData): void {
  localStorage.clear();
  seedStoredSession(); // current user id 1
  TestBed.configureTestingModule({
    imports: [TaskDetailDialogComponent],
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter([]),
      { provide: MAT_DIALOG_DATA, useValue: data },
      { provide: MatDialogRef, useValue: { close: () => undefined } },
    ],
  });
}

describe('TaskDetailDialogComponent', () => {
  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    localStorage.clear();
  });

  it('loads and renders the comment thread with a delete control for own comments', async () => {
    configure({
      projectId: 7,
      task: buildTask({ id: 3, title: 'Wire the API' }),
      canModerateComments: false,
    });
    const httpMock = TestBed.inject(HttpTestingController);

    const fixture = TestBed.createComponent(TaskDetailDialogComponent);
    httpMock
      .expectOne('/api/v1/projects/7/tasks/3/comments')
      .flush([
        buildComment({ id: 1, body: 'First pass done', author: buildUserSummary({ userId: 1 }) }),
      ]);
    await settle(fixture);

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Wire the API');
    expect(element.textContent).toContain('First pass done');
    expect(element.textContent).toContain('Add a comment');
    expect(element.querySelector('.comment__delete')).not.toBeNull();
  });

  it('shows the empty state when the task has no comments', async () => {
    configure({ projectId: 7, task: buildTask({ id: 3 }), canModerateComments: false });
    const httpMock = TestBed.inject(HttpTestingController);

    const fixture = TestBed.createComponent(TaskDetailDialogComponent);
    httpMock.expectOne('/api/v1/projects/7/tasks/3/comments').flush([]);
    await settle(fixture);

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('No comments yet');
  });
});

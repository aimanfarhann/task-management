import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TaskDto, TaskStatus } from '../../core/api/models';
import { buildTask } from '../../testing/fixtures';
import { TaskBoardComponent } from './task-board.component';
import { TaskStore } from './task.store';

/** Exposes the (protected) move handler for direct assertion in tests. */
interface BoardMoveHandle {
  requestMove(task: TaskDto, status: TaskStatus): void;
}

describe('TaskBoardComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TaskBoardComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
  });

  it('renders the three status columns with cards grouped by status', async () => {
    const store = TestBed.inject(TaskStore);
    store.tasks.set([
      buildTask({ id: 1, title: 'Draft plan', status: 'TODO' }),
      buildTask({ id: 2, title: 'Ship it', status: 'DONE' }),
    ]);

    const fixture = TestBed.createComponent(TaskBoardComponent);
    await fixture.whenStable();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('To do');
    expect(element.textContent).toContain('In progress');
    expect(element.textContent).toContain('Done');
    expect(element.textContent).toContain('Draft plan');
    expect(element.textContent).toContain('Ship it');
  });

  it('emits moveTask with the requested status', () => {
    const store = TestBed.inject(TaskStore);
    const task = buildTask({ id: 1, status: 'TODO' });
    store.tasks.set([task]);

    const fixture = TestBed.createComponent(TaskBoardComponent);
    const moves: { task: TaskDto; status: TaskStatus }[] = [];
    fixture.componentInstance.moveTask.subscribe((change) => moves.push(change));

    (fixture.componentInstance as unknown as BoardMoveHandle).requestMove(task, 'IN_PROGRESS');

    expect(moves).toEqual([{ task, status: 'IN_PROGRESS' }]);
  });
});

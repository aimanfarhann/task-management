import { TestBed } from '@angular/core/testing';
import { buildTask } from '../../testing/fixtures';
import { TaskListComponent } from './task-list.component';

describe('TaskListComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [TaskListComponent] });
  });

  it('renders a row per task with title, status and priority', async () => {
    const fixture = TestBed.createComponent(TaskListComponent);
    fixture.componentRef.setInput('tasks', [
      buildTask({ id: 1, title: 'Draft plan', status: 'TODO', priority: 'HIGH' }),
    ]);
    fixture.componentRef.setInput('members', []);
    await fixture.whenStable();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Draft plan');
    expect(element.textContent).toContain('To do');
    expect(element.textContent).toContain('High');
  });

  it('shows an empty message when there are no tasks', async () => {
    const fixture = TestBed.createComponent(TaskListComponent);
    fixture.componentRef.setInput('tasks', []);
    await fixture.whenStable();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('No tasks match these filters.');
  });
});

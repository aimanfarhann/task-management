import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import {
  buildDashboard,
  buildDashboardTask,
  buildProjectSummary,
  settle,
} from '../../testing/fixtures';
import { DashboardPageComponent } from './dashboard-page.component';

describe('DashboardPageComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [DashboardPageComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('renders my tasks and project summaries', async () => {
    const fixture = TestBed.createComponent(DashboardPageComponent);
    await fixture.whenStable();
    httpMock.expectOne('/api/v1/dashboard').flush(
      buildDashboard({
        myTasks: [buildDashboardTask({ id: 1, title: 'Ship the board', dueDate: null })],
        projectSummaries: [buildProjectSummary({ projectName: 'Apollo' })],
      }),
    );
    await settle(fixture);

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('My tasks');
    expect(element.textContent).toContain('Ship the board');
    expect(element.textContent).toContain('Apollo');
  });

  it('shows the empty state when no tasks are assigned', async () => {
    const fixture = TestBed.createComponent(DashboardPageComponent);
    await fixture.whenStable();
    httpMock
      .expectOne('/api/v1/dashboard')
      .flush(buildDashboard({ myTasks: [], projectSummaries: [] }));
    await settle(fixture);

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('No tasks are assigned to you');
  });
});

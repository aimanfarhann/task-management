import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { buildProject, settle } from '../../testing/fixtures';
import { ProjectStore } from './project.store';
import { ProjectsListPageComponent } from './projects-list-page.component';

describe('ProjectsListPageComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [ProjectsListPageComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('shows the empty state when the user has no projects', async () => {
    const fixture = TestBed.createComponent(ProjectsListPageComponent);
    await fixture.whenStable();
    httpMock.expectOne('/api/v1/projects').flush([]);
    await settle(fixture);

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('No projects yet');
    expect(element.textContent).toContain('Create project');
  });

  it('renders a card per project with name and member count', async () => {
    const fixture = TestBed.createComponent(ProjectsListPageComponent);
    await fixture.whenStable();
    httpMock
      .expectOne('/api/v1/projects')
      .flush([buildProject({ name: 'Apollo', memberCount: 3 })]);
    await settle(fixture);

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Apollo');
    expect(element.textContent).toContain('3 members');
  });

  it('hides archived projects until the toggle is on', async () => {
    const fixture = TestBed.createComponent(ProjectsListPageComponent);
    await fixture.whenStable();
    httpMock
      .expectOne('/api/v1/projects')
      .flush([
        buildProject({ id: 1, name: 'Active One' }),
        buildProject({ id: 2, name: 'Archived One', archived: true }),
      ]);
    await settle(fixture);

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Active One');
    expect(element.textContent).not.toContain('Archived One');

    TestBed.inject(ProjectStore).showArchived.set(true);
    await settle(fixture);
    expect(element.textContent).toContain('Archived One');
  });
});

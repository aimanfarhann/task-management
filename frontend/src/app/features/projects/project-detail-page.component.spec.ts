import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { buildMember, buildProject, seedStoredSession, settle } from '../../testing/fixtures';
import { ProjectDetailPageComponent } from './project-detail-page.component';

describe('ProjectDetailPageComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    seedStoredSession();
    TestBed.configureTestingModule({
      imports: [ProjectDetailPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { paramMap: of(convertToParamMap({ id: '7' })) },
        },
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('renders the project header and members for an OWNER', async () => {
    const fixture = TestBed.createComponent(ProjectDetailPageComponent);
    httpMock.expectOne('/api/v1/projects/7').flush(buildProject({ id: 7, myRole: 'OWNER' }));
    httpMock
      .expectOne('/api/v1/projects/7/members')
      .flush([buildMember(), buildMember({ userId: 2, displayName: 'Grace Hopper' })]);
    await settle(fixture);

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('h1')?.textContent).toContain('Apollo');
    expect(element.textContent).toContain('Members (2)');
    expect(element.textContent).toContain('Grace Hopper');
    expect(element.textContent).toContain('Add member');
    expect(element.textContent).toContain('Edit');
  });

  it('hides owner controls for a plain MEMBER', async () => {
    const fixture = TestBed.createComponent(ProjectDetailPageComponent);
    httpMock.expectOne('/api/v1/projects/7').flush(buildProject({ id: 7, myRole: 'MEMBER' }));
    httpMock
      .expectOne('/api/v1/projects/7/members')
      .flush([buildMember({ userId: 99, displayName: 'Someone Else', projectRole: 'OWNER' })]);
    await settle(fixture);

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).not.toContain('Add member');
    expect(element.querySelector('.detail-header__actions')).toBeNull();
  });

  it('shows the error state when the project is not accessible', async () => {
    const fixture = TestBed.createComponent(ProjectDetailPageComponent);
    httpMock
      .expectOne('/api/v1/projects/7')
      .flush(
        { code: 'FORBIDDEN', message: 'You are not a member of this project', fieldErrors: [] },
        { status: 403, statusText: 'Forbidden' },
      );
    httpMock.expectOne('/api/v1/projects/7/members').flush([]);
    await settle(fixture);

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('You are not a member of this project');
    expect(element.textContent).toContain('Back to projects');
  });
});

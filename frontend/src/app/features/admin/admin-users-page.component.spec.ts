import { signal } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AdminUserDto } from '../../core/api/models';
import { buildAdminUser, seedStoredSession, settle } from '../../testing/fixtures';
import { AdminStore } from './admin.store';
import { AdminUsersPageComponent } from './admin-users-page.component';

describe('AdminUsersPageComponent', () => {
  let storeStub: {
    users: ReturnType<typeof signal<AdminUserDto[]>>;
    loading: ReturnType<typeof signal<boolean>>;
    error: ReturnType<typeof signal<string | null>>;
    loadUsers: ReturnType<typeof vi.fn>;
    setActive: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    localStorage.clear();
    // The signed-in admin is user id 1 → their own row must not offer self-deactivation.
    seedStoredSession({
      user: { id: 1, email: 'admin@example.com', displayName: 'Grace Admin', role: 'ADMIN' },
    });
    storeStub = {
      users: signal<AdminUserDto[]>([
        buildAdminUser({ id: 1, displayName: 'Grace Admin', role: 'ADMIN', active: true }),
        buildAdminUser({ id: 2, displayName: 'Ada Lovelace', role: 'USER', active: true }),
        buildAdminUser({ id: 3, displayName: 'Bob User', role: 'USER', active: false }),
      ]),
      loading: signal(false),
      error: signal<string | null>(null),
      loadUsers: vi.fn().mockResolvedValue(undefined),
      setActive: vi.fn().mockResolvedValue(buildAdminUser()),
    };
    TestBed.configureTestingModule({
      imports: [AdminUsersPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AdminStore, useValue: storeStub },
      ],
    });
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('loads users and renders a row per user with role and status', async () => {
    const fixture = TestBed.createComponent(AdminUsersPageComponent);
    await settle(fixture);

    const element = fixture.nativeElement as HTMLElement;
    expect(storeStub.loadUsers).toHaveBeenCalled();
    expect(element.textContent).toContain('Ada Lovelace');
    expect(element.textContent).toContain('Bob User');
    expect(element.textContent).toContain('Admin');
    // Bob is deactivated → status conveyed by text, not color alone.
    expect(element.textContent).toContain('Deactivated');
  });

  it('replaces the signed-in admin own-row action with a static note and offers actions on others', async () => {
    const fixture = TestBed.createComponent(AdminUsersPageComponent);
    await settle(fixture);

    const element = fixture.nativeElement as HTMLElement;
    const actions = Array.from(element.querySelectorAll<HTMLButtonElement>('.admin-users__action'));
    // The self row (admin id 1) shows a keyboard/SR-reachable text note instead of a disabled
    // button; the other two rows keep enabled action buttons.
    expect(actions.length).toBe(2);
    expect(actions.every((button) => !button.disabled)).toBe(true);
    expect(element.querySelector('.admin-users__self-note')?.textContent).toContain('Your account');
  });

  it('shows the empty state when there are no users', async () => {
    storeStub.users.set([]);
    const fixture = TestBed.createComponent(AdminUsersPageComponent);
    await settle(fixture);

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('No users to show.');
  });
});

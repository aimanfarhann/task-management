import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { LoginPageComponent } from './login-page.component';

describe('LoginPageComponent', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [LoginPageComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
  });

  it('renders the sign-in form', async () => {
    const fixture = TestBed.createComponent(LoginPageComponent);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h1')?.textContent).toContain('Sign in');
    expect(element.querySelectorAll('input').length).toBe(2);
    expect(element.querySelector('button[type="submit"]')?.textContent).toContain('Sign in');
    expect(element.querySelector('a[href="/register"]')).toBeTruthy();
  });
});

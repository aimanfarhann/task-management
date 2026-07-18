import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RegisterPageComponent } from './register-page.component';

describe('RegisterPageComponent', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [RegisterPageComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
  });

  it('renders the registration form', async () => {
    const fixture = TestBed.createComponent(RegisterPageComponent);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h1')?.textContent).toContain('Create account');
    expect(element.querySelectorAll('input').length).toBe(3);
    expect(element.querySelector('a[href="/login"]')).toBeTruthy();
  });
});

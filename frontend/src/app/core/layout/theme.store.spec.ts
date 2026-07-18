import { TestBed } from '@angular/core/testing';
import { ThemeStore } from './theme.store';

const THEME_STORAGE_KEY = 'taskflow.theme';

describe('ThemeStore', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('tf-dark');
    TestBed.configureTestingModule({});
  });

  afterEach(() => {
    document.documentElement.classList.remove('tf-dark');
    localStorage.clear();
  });

  it('constructor_noStoredPreference_followsSystemScheme', () => {
    const store = TestBed.inject(ThemeStore);

    // jsdom media queries never match, so the system scheme resolves to light.
    expect(store.theme()).toBe('light');
    expect(document.documentElement.classList.contains('tf-dark')).toBe(false);
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBeNull();
  });

  it('constructor_storedDarkPreference_appliesDarkClass', () => {
    localStorage.setItem(THEME_STORAGE_KEY, 'dark');

    const store = TestBed.inject(ThemeStore);

    expect(store.theme()).toBe('dark');
    expect(document.documentElement.classList.contains('tf-dark')).toBe(true);
  });

  it('toggleTheme_fromLight_persistsAndAppliesDark', () => {
    const store = TestBed.inject(ThemeStore);

    store.toggleTheme();

    expect(store.theme()).toBe('dark');
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('dark');
    expect(document.documentElement.classList.contains('tf-dark')).toBe(true);

    store.toggleTheme();

    expect(store.theme()).toBe('light');
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('light');
    expect(document.documentElement.classList.contains('tf-dark')).toBe(false);
  });
});

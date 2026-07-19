import { TestBed } from '@angular/core/testing';
import { ThemeStore } from './theme.store';

const THEME_STORAGE_KEY = 'taskflow.theme';

describe('ThemeStore', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('dark');
    TestBed.configureTestingModule({});
  });

  afterEach(() => {
    document.documentElement.classList.remove('dark');
    localStorage.clear();
  });

  it('constructor_noStoredPreference_defaultsToDark', () => {
    const store = TestBed.inject(ThemeStore);

    // Dark-first: nothing stored -> dark, which applies the .dark class.
    expect(store.theme()).toBe('dark');
    expect(document.documentElement.classList.contains('dark')).toBe(true);
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBeNull();
  });

  it('constructor_storedLightPreference_removesDarkClass', () => {
    localStorage.setItem(THEME_STORAGE_KEY, 'light');

    const store = TestBed.inject(ThemeStore);

    // Light is the token default: no .dark class present.
    expect(store.theme()).toBe('light');
    expect(document.documentElement.classList.contains('dark')).toBe(false);
  });

  it('toggleTheme_fromDark_persistsAndAppliesLight', () => {
    const store = TestBed.inject(ThemeStore);

    store.toggleTheme();

    expect(store.theme()).toBe('light');
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('light');
    expect(document.documentElement.classList.contains('dark')).toBe(false);

    store.toggleTheme();

    expect(store.theme()).toBe('dark');
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('dark');
    expect(document.documentElement.classList.contains('dark')).toBe(true);
  });
});

import { TestBed } from '@angular/core/testing';
import { ThemeStore } from './theme.store';

const THEME_STORAGE_KEY = 'taskflow.theme';

describe('ThemeStore', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('tf-light');
    TestBed.configureTestingModule({});
  });

  afterEach(() => {
    document.documentElement.classList.remove('tf-light');
    localStorage.clear();
  });

  it('constructor_noStoredPreference_defaultsToDark', () => {
    const store = TestBed.inject(ThemeStore);

    // Focus is dark-first: nothing stored -> dark, and no opt-in class is added.
    expect(store.theme()).toBe('dark');
    expect(document.documentElement.classList.contains('tf-light')).toBe(false);
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBeNull();
  });

  it('constructor_storedLightPreference_appliesLightClass', () => {
    localStorage.setItem(THEME_STORAGE_KEY, 'light');

    const store = TestBed.inject(ThemeStore);

    expect(store.theme()).toBe('light');
    expect(document.documentElement.classList.contains('tf-light')).toBe(true);
  });

  it('toggleTheme_fromDark_persistsAndAppliesLight', () => {
    const store = TestBed.inject(ThemeStore);

    store.toggleTheme();

    expect(store.theme()).toBe('light');
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('light');
    expect(document.documentElement.classList.contains('tf-light')).toBe(true);

    store.toggleTheme();

    expect(store.theme()).toBe('dark');
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('dark');
    expect(document.documentElement.classList.contains('tf-light')).toBe(false);
  });
});

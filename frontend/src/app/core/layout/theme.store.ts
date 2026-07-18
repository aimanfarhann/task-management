import { DOCUMENT } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';

export type ThemePreference = 'light' | 'dark';

const THEME_STORAGE_KEY = 'taskflow.theme';

/**
 * Signal store for the color scheme (DESIGN.md §2): defaults to
 * prefers-color-scheme, manual toggle persisted in localStorage. The active
 * scheme is applied as the .tf-dark class on <html>, which drives the dark
 * Material color tokens in styles.scss.
 */
@Injectable({ providedIn: 'root' })
export class ThemeStore {
  private readonly document = inject(DOCUMENT);

  readonly theme = signal<ThemePreference>(this.resolveInitialTheme());

  constructor() {
    this.applyTheme(this.theme());
  }

  toggleTheme(): void {
    const nextTheme: ThemePreference = this.theme() === 'dark' ? 'light' : 'dark';
    this.theme.set(nextTheme);
    // Persist only explicit choices so the system preference keeps applying
    // until the user actually toggles.
    localStorage.setItem(THEME_STORAGE_KEY, nextTheme);
    this.applyTheme(nextTheme);
  }

  private resolveInitialTheme(): ThemePreference {
    const stored = localStorage.getItem(THEME_STORAGE_KEY);
    if (stored === 'light' || stored === 'dark') {
      return stored;
    }
    return this.prefersDark() ? 'dark' : 'light';
  }

  private prefersDark(): boolean {
    const window = this.document.defaultView;
    return (
      typeof window?.matchMedia === 'function' &&
      window.matchMedia('(prefers-color-scheme: dark)').matches
    );
  }

  private applyTheme(theme: ThemePreference): void {
    this.document.documentElement.classList.toggle('tf-dark', theme === 'dark');
  }
}

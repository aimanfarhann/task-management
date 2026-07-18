import { DOCUMENT } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';

export type ThemePreference = 'light' | 'dark';

const THEME_STORAGE_KEY = 'taskflow.theme';

/**
 * Signal store for the color scheme (DESIGN.md §2). The "Focus" direction is
 * dark-first, so dark is the default; an explicit choice is persisted in
 * localStorage. The light scheme is applied as the .tf-light class on <html>,
 * which re-emits the light Material color tokens in styles.scss.
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
    localStorage.setItem(THEME_STORAGE_KEY, nextTheme);
    this.applyTheme(nextTheme);
  }

  private resolveInitialTheme(): ThemePreference {
    const stored = localStorage.getItem(THEME_STORAGE_KEY);
    if (stored === 'light' || stored === 'dark') {
      return stored;
    }
    // Dark-first: default to dark unless the user has explicitly chosen light.
    return 'dark';
  }

  private applyTheme(theme: ThemePreference): void {
    // Base scheme is dark; .tf-light opts into the light scheme.
    this.document.documentElement.classList.toggle('tf-light', theme === 'light');
  }
}

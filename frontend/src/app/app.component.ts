import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ThemeStore } from './core/layout/theme.store';

@Component({
  selector: 'tf-root',
  imports: [RouterOutlet],
  template: '<router-outlet />',
})
export class AppComponent {
  // Instantiated at the root so the persisted/system theme is applied to
  // <html> before any route (including the auth pages) renders.
  protected readonly themeStore = inject(ThemeStore);
}

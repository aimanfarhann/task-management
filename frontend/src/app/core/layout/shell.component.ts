import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatToolbarModule } from '@angular/material/toolbar';
import { AuthStore } from '../auth/auth.store';
import { ThemeStore } from './theme.store';

/**
 * App layout shell (DESIGN.md §4): top app bar plus a navigation rail on
 * desktop that becomes a bottom navigation bar under 600px. Only the content
 * area scrolls.
 */
@Component({
  selector: 'tf-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
  ],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  protected readonly authStore = inject(AuthStore);
  protected readonly themeStore = inject(ThemeStore);

  protected signOut(): void {
    void this.authStore.logout();
  }
}

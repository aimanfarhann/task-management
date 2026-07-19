import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { CdkMenu, CdkMenuItem, CdkMenuTrigger } from '@angular/cdk/menu';
import { NgIcon } from '@ng-icons/core';
import { HlmButton } from '@spartan-ng/helm/button';
import { HlmToaster } from '@spartan-ng/helm/sonner';
import { AuthStore } from '../auth/auth.store';
import { ThemeStore } from './theme.store';

/**
 * App layout shell (DESIGN.md §4): top app bar plus a navigation rail on
 * desktop that becomes a bottom navigation bar under 600px. Only the content
 * area scrolls. Hosts the single sonner toaster for the whole app.
 */
@Component({
  selector: 'tf-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    CdkMenuTrigger,
    CdkMenu,
    CdkMenuItem,
    NgIcon,
    HlmButton,
    HlmToaster,
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

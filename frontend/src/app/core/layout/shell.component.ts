import { Component, HostListener, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Dialog } from '@angular/cdk/dialog';
import { CdkMenu, CdkMenuItem, CdkMenuTrigger } from '@angular/cdk/menu';
import { Overlay } from '@angular/cdk/overlay';
import { NgIcon } from '@ng-icons/core';
import { HlmToaster } from '@spartan-ng/helm/sonner';
import { AuthStore } from '../auth/auth.store';
import { CommandPaletteComponent } from './command-palette.component';
import { ThemeStore } from './theme.store';

/**
 * App layout shell (DESIGN.md §4): top app bar plus a navigation rail on
 * desktop that becomes a bottom navigation bar under 600px. Only the content
 * area scrolls. Hosts the single sonner toaster and the ⌘K command palette.
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
    HlmToaster,
  ],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  protected readonly authStore = inject(AuthStore);
  protected readonly themeStore = inject(ThemeStore);
  private readonly dialog = inject(Dialog);
  private readonly overlay = inject(Overlay);

  /** Prevents opening a second palette while one is already up (⌘K spam / click). */
  private readonly paletteOpen = signal(false);

  /** Up-to-two-letter monogram for the topbar avatar chip. */
  protected readonly initials = computed(() => {
    const name = this.authStore.currentUser()?.displayName?.trim() ?? '';
    if (!name) {
      return '';
    }
    const parts = name.split(/\s+/);
    const first = parts[0]?.[0] ?? '';
    const last = parts.length > 1 ? (parts[parts.length - 1][0] ?? '') : '';
    return (first + last).toUpperCase();
  });

  protected openCommandPalette(): void {
    if (this.paletteOpen()) {
      return;
    }
    this.paletteOpen.set(true);
    const positionStrategy = this.overlay.position().global().top('12vh').centerHorizontally();
    const dialogRef = this.dialog.open(CommandPaletteComponent, {
      positionStrategy,
      width: '36rem',
      maxWidth: 'calc(100vw - 2rem)',
      backdropClass: 'tf-overlay-backdrop',
      autoFocus: 'first-tabbable',
      ariaLabel: 'Command palette',
    });
    dialogRef.closed.subscribe(() => this.paletteOpen.set(false));
  }

  @HostListener('document:keydown', ['$event'])
  protected onGlobalKeydown(event: KeyboardEvent): void {
    if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
      event.preventDefault();
      this.openCommandPalette();
    }
  }

  protected signOut(): void {
    void this.authStore.logout();
  }
}

import { Component, computed, inject, signal } from '@angular/core';
import { DialogRef } from '@angular/cdk/dialog';
import { Router } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import { AuthStore } from '../auth/auth.store';
import { ProjectStore } from '../../features/projects/project.store';

interface Command {
  id: string;
  label: string;
  icon: string;
  kind: string;
  path: unknown[];
}

/**
 * ⌘K command palette (opened from the shell). Jump to a page (Dashboard /
 * Projects / Admin) or straight to any project by name. Keyboard-first: type to
 * filter, ↑/↓ to move, Enter to go, Esc to close (handled by the CDK dialog).
 * Rendered in the CDK overlay, so it styles itself with global Tailwind
 * utilities rather than component-scoped styles.
 */
@Component({
  selector: 'tf-command-palette',
  imports: [NgIcon],
  template: `
    <div
      class="w-full overflow-hidden rounded-xl border border-border bg-popover text-popover-foreground shadow-[var(--tf-shadow-2)]"
    >
      <div class="flex items-center gap-2.5 border-b border-border px-4">
        <ng-icon
          name="lucideSearch"
          size="1.05rem"
          class="text-muted-foreground"
          aria-hidden="true"
        />
        <input
          class="h-12 w-full bg-transparent text-sm text-foreground outline-none placeholder:text-muted-foreground"
          [value]="query()"
          (input)="onInput($event)"
          (keydown)="onKeydown($event)"
          placeholder="Search projects and pages…"
          aria-label="Search projects and pages"
          role="combobox"
          aria-expanded="true"
          aria-controls="tf-palette-list"
          [attr.aria-activedescendant]="activeId()"
        />
        <kbd
          class="rounded-[5px] border border-border bg-card px-1.5 py-0.5 font-mono text-[10.5px] text-muted-foreground"
        >
          Esc
        </kbd>
      </div>
      <ul
        id="tf-palette-list"
        class="max-h-[320px] overflow-y-auto p-1.5"
        role="listbox"
        aria-label="Commands"
      >
        @for (cmd of results(); track cmd.id; let i = $index) {
          <!-- Keyboard nav lives on the combobox input (aria-activedescendant); options are
               mouse affordances, so the focus/key-event lint rules don't apply here. -->
          <!-- eslint-disable-next-line @angular-eslint/template/click-events-have-key-events, @angular-eslint/template/interactive-supports-focus -->
          <li
            [id]="'tf-cmd-' + cmd.id"
            role="option"
            [attr.aria-selected]="i === activeIndex()"
            (click)="run(cmd)"
            (mouseenter)="activeIndex.set(i)"
            class="flex cursor-pointer items-center gap-3 rounded-lg px-3 py-2.5 text-sm"
            [class.bg-accent]="i === activeIndex()"
          >
            <ng-icon
              [name]="cmd.icon"
              size="1rem"
              class="text-muted-foreground"
              aria-hidden="true"
            />
            <span class="flex-1 truncate text-foreground">{{ cmd.label }}</span>
            <span class="font-mono text-[11px] uppercase tracking-wider text-muted-foreground">
              {{ cmd.kind }}
            </span>
          </li>
        } @empty {
          <li class="px-3 py-8 text-center text-sm text-muted-foreground">No matches</li>
        }
      </ul>
    </div>
  `,
})
export class CommandPaletteComponent {
  private readonly dialogRef = inject<DialogRef<unknown>>(DialogRef);
  private readonly router = inject(Router);
  private readonly authStore = inject(AuthStore);
  private readonly projectStore = inject(ProjectStore);

  protected readonly query = signal('');
  protected readonly activeIndex = signal(0);

  private readonly navCommands = computed<Command[]>(() => {
    const isAdmin = this.authStore.currentUser()?.role === 'ADMIN';
    return [
      {
        id: 'go-dashboard',
        label: 'Dashboard',
        icon: 'lucideLayoutDashboard',
        kind: 'Go to',
        path: ['/dashboard'],
      },
      {
        id: 'go-projects',
        label: 'Projects',
        icon: 'lucideFolder',
        kind: 'Go to',
        path: ['/projects'],
      },
      ...(isAdmin
        ? [
            {
              id: 'go-admin',
              label: 'Admin',
              icon: 'lucideShieldCheck',
              kind: 'Go to',
              path: ['/admin/users'],
            },
          ]
        : []),
    ];
  });

  private readonly projectCommands = computed<Command[]>(() =>
    this.projectStore.projects().map((project) => ({
      id: `project-${project.id}`,
      label: project.name,
      icon: 'lucideFolder',
      kind: 'Project',
      path: ['/projects', project.id],
    })),
  );

  protected readonly results = computed<Command[]>(() => {
    const query = this.query().trim().toLowerCase();
    const all = [...this.navCommands(), ...this.projectCommands()];
    const matches = query ? all.filter((cmd) => cmd.label.toLowerCase().includes(query)) : all;
    return matches.slice(0, 12);
  });

  /** Id of the highlighted option, for the combobox input's aria-activedescendant. */
  protected readonly activeId = computed(() => {
    const command = this.results()[this.activeIndex()];
    return command ? `tf-cmd-${command.id}` : null;
  });

  constructor() {
    // Make sure the user's projects are searchable even if the list page hasn't been opened.
    if (this.projectStore.projects().length === 0) {
      void this.projectStore.loadProjects();
    }
  }

  protected onInput(event: Event): void {
    this.query.set((event.target as HTMLInputElement).value);
    this.activeIndex.set(0);
  }

  protected onKeydown(event: KeyboardEvent): void {
    const count = this.results().length;
    if (count === 0) {
      return;
    }
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      this.activeIndex.set((this.activeIndex() + 1) % count);
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      this.activeIndex.set((this.activeIndex() - 1 + count) % count);
    } else if (event.key === 'Enter') {
      event.preventDefault();
      this.run(this.results()[this.activeIndex()]);
    }
  }

  protected run(command: Command | undefined): void {
    if (!command) {
      return;
    }
    this.dialogRef.close();
    void this.router.navigate(command.path);
  }
}

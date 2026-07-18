import { Component, input } from '@angular/core';

/**
 * Small round swatch for a project color tag. Decorative only: every usage
 * must pair it with a visible text label (DESIGN.md §8 — color never alone).
 */
@Component({
  selector: 'tf-color-dot',
  template: `
    <span
      class="color-dot"
      [style.background-color]="'var(--tf-tag-' + colorTag() + ')'"
      aria-hidden="true"
    ></span>
  `,
  styles: `
    :host {
      display: inline-flex;
    }

    .color-dot {
      width: 12px;
      height: 12px;
      border-radius: 50%;
      flex-shrink: 0;
    }
  `,
})
export class ColorDotComponent {
  readonly colorTag = input.required<string>();
}

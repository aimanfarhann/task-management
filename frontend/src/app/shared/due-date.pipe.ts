import { Pipe, PipeTransform } from '@angular/core';
import { fromIsoDate } from './due-date.util';

/**
 * Formats a contract `yyyy-MM-dd` due date as a localized medium date.
 * Parses to a local-midnight Date first: Angular's DatePipe reads a date-only
 * ISO string as UTC midnight, which renders a day early in negative offsets.
 */
@Pipe({ name: 'tfDueDate' })
export class DueDatePipe implements PipeTransform {
  private readonly formatter = new Intl.DateTimeFormat(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });

  transform(value: string | null): string {
    const date = fromIsoDate(value);
    return date ? this.formatter.format(date) : '';
  }
}

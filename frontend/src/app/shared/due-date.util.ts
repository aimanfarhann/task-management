/**
 * Date helpers for the task domain. Kept dependency-free (no core/feature
 * imports) so it stays a leaf `shared/` utility. Everything works on the
 * contract's `yyyy-MM-dd` string form to avoid timezone drift from Date math:
 * lexical comparison of ISO date strings is chronological comparison.
 */

/** Formats a Date as a local `yyyy-MM-dd` string (defaults to today). */
export function toIsoDate(reference: Date = new Date()): string {
  const year = reference.getFullYear();
  const month = `${reference.getMonth() + 1}`.padStart(2, '0');
  const day = `${reference.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/** Parses a `yyyy-MM-dd` string into a local-midnight Date, or null. */
export function fromIsoDate(iso: string | null): Date | null {
  if (!iso) {
    return null;
  }
  const [year, month, day] = iso.split('-').map(Number);
  return new Date(year, month - 1, day);
}

/** True when a due date has already passed (strictly before today). */
export function isPastDue(dueDate: string | null, today: string = toIsoDate()): boolean {
  return dueDate !== null && dueDate < today;
}

/** Bucket a due date falls into relative to today, for dashboard grouping. */
export type DueBucket = 'OVERDUE' | 'TODAY' | 'UPCOMING' | 'NO_DUE_DATE';

export function dueBucket(dueDate: string | null, today: string = toIsoDate()): DueBucket {
  if (dueDate === null) {
    return 'NO_DUE_DATE';
  }
  if (dueDate < today) {
    return 'OVERDUE';
  }
  if (dueDate === today) {
    return 'TODAY';
  }
  return 'UPCOMING';
}

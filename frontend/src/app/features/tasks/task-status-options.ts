import { TaskStatus } from '../../core/api/models';

/** A selectable task status with its display label and icon. */
export interface TaskStatusOption {
  status: TaskStatus;
  label: string;
  icon: string;
}

/** The three statuses in board order, shared by the card and list move menus. */
export const TASK_STATUS_OPTIONS: TaskStatusOption[] = [
  { status: 'TODO', label: 'To do', icon: 'radio_button_unchecked' },
  { status: 'IN_PROGRESS', label: 'In progress', icon: 'timelapse' },
  { status: 'DONE', label: 'Done', icon: 'check_circle' },
];

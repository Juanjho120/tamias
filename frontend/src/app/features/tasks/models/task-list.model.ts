export type TaskListStatus = 'OPEN' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'DELETED';

export interface TaskItemRequest {
  taskTemplateId: string | null;
  taskName: string;
  responsiblePerson: string | null;
  completed: boolean | null;
  sortOrder: number | null;
}

export interface TaskItemUpdateRequest extends TaskItemRequest {
}

export interface TaskItemCompletionRequest {
  completed: boolean;
}

export interface TaskItem {
  id: string;
  taskTemplateId: string | null;
  taskTemplateName: string | null;
  taskName: string;
  responsiblePerson: string | null;
  completed: boolean | null;
  completionDate: string | null;
  sortOrder: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface TaskListSummary {
  id: string;
  propertyId: string;
  propertyName: string;
  reservationId: string | null;
  maintenanceRecordId: string | null;
  title: string;
  creationDate: string | null;
  dueDate: string | null;
  status: TaskListStatus;
  totalItems: number;
  completedItems: number;
  createdAt: string;
}

export interface TaskList {
  id: string;
  propertyId: string;
  propertyName: string;
  reservationId: string | null;
  maintenanceRecordId: string | null;
  title: string;
  creationDate: string | null;
  dueDate: string | null;
  status: TaskListStatus;
  items: TaskItem[];
  createdAt: string;
  updatedAt: string;
}

export interface TaskListRequest {
  propertyId: string;
  reservationId: string | null;
  maintenanceRecordId: string | null;
  title: string;
  creationDate: string | null;
  dueDate: string | null;
  status: TaskListStatus;
  items: TaskItemRequest[];
}

export interface TaskListFilters {
  propertyId?: string;
  reservationId?: string;
  maintenanceRecordId?: string;
  status?: TaskListStatus | '';
  page: number;
  size: number;
  sort?: string;
}

export const TASK_LIST_STATUSES: TaskListStatus[] = [
  'OPEN',
  'IN_PROGRESS',
  'COMPLETED',
  'CANCELLED',
  'DELETED'
];

import React from 'react';
import type { Priority, TicketStatus, UrgencyLevel } from '../api/types';
import { ClockAlertIcon } from './Icons';

interface UrgencyBadgeProps {
  level: UrgencyLevel | string;
  score?: number;
}

export const UrgencyBadge: React.FC<UrgencyBadgeProps> = ({ level, score }) => {
  const norm = (level || 'LOW').toUpperCase();

  let tokenClasses = 'bg-urgency-low-bg text-urgency-low-text border-urgency-low-border';
  if (norm === 'MEDIUM') {
    tokenClasses = 'bg-urgency-medium-bg text-urgency-medium-text border-urgency-medium-border';
  } else if (norm === 'HIGH') {
    tokenClasses = 'bg-urgency-high-bg text-urgency-high-text border-urgency-high-border';
  } else if (norm === 'URGENT') {
    tokenClasses = 'bg-urgency-urgent-bg text-urgency-urgent-text border-urgency-urgent-border font-bold';
  }

  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 text-xs font-medium rounded-full border shadow-sm transition-colors duration-fast ${tokenClasses}`}
    >
      Urgency: {norm}
      {typeof score === 'number' && score > 0 ? ` (${score})` : ''}
    </span>
  );
};

export const OverdueBadge: React.FC = () => {
  return (
    <span
      className="relative inline-flex items-center gap-1.5 px-2.5 py-0.5 text-xs font-semibold rounded-full bg-overdue-bg text-overdue-text border border-overdue-border animate-overdue-glow"
      role="status"
      aria-label="Overdue ticket"
    >
      <ClockAlertIcon className="w-3.5 h-3.5 text-overdue-text flex-shrink-0" />
      <span>Overdue</span>
    </span>
  );
};

interface StatusBadgeProps {
  status: TicketStatus | string;
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status }) => {
  const norm = (status || 'OPEN').toUpperCase();

  let tokenClasses = 'bg-status-open-bg text-status-open-text';
  let label = norm;

  if (norm === 'ASSIGNED') {
    tokenClasses = 'bg-status-assigned-bg text-status-assigned-text';
  } else if (norm === 'IN_PROGRESS') {
    tokenClasses = 'bg-status-progress-bg text-status-progress-text';
    label = 'IN PROGRESS';
  } else if (norm === 'RESOLVED') {
    tokenClasses = 'bg-status-resolved-bg text-status-resolved-text';
  } else if (norm === 'CLOSED') {
    tokenClasses = 'bg-status-closed-bg text-status-closed-text';
  }

  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 text-xs font-medium rounded-full transition-colors duration-fast ${tokenClasses}`}
    >
      {label}
    </span>
  );
};

interface RequestedPriorityBadgeProps {
  priority: Priority | string;
}

export const RequestedPriorityBadge: React.FC<RequestedPriorityBadgeProps> = ({ priority }) => {
  return (
    <span className="text-xs font-medium text-text-muted">
      Requested: {priority || 'LOW'}
    </span>
  );
};

export interface CommentTypeBadgeProps {
  type?: 'GENERAL' | 'ISSUE' | 'REASSIGNMENT' | 'CLOSURE' | string;
}

export const CommentTypeBadge: React.FC<CommentTypeBadgeProps> = ({ type }) => {
  const norm = (type || 'GENERAL').toUpperCase();
  if (norm === 'GENERAL') return null;

  let classes = 'bg-secondary text-secondary-text border-border';
  let label = norm;

  if (norm === 'ISSUE') {
    classes =
      'bg-amber-100 text-amber-900 border-amber-300 dark:bg-amber-950/60 dark:text-amber-200 dark:border-amber-600/50 font-bold';
    label = 'ISSUE';
  } else if (norm === 'REASSIGNMENT') {
    classes =
      'bg-indigo-100 text-indigo-900 border-indigo-300 dark:bg-indigo-950/60 dark:text-indigo-200 dark:border-indigo-600/50 font-semibold';
    label = 'REASSIGNMENT';
  } else if (norm === 'CLOSURE') {
    classes =
      'bg-rose-100 text-rose-900 border-rose-300 dark:bg-rose-950/60 dark:text-rose-200 dark:border-rose-600/50 font-semibold';
    label = 'CLOSURE';
  }

  return (
    <span
      className={`inline-flex items-center px-2 py-0.5 text-[10px] uppercase tracking-wider rounded-md border ${classes}`}
    >
      {label}
    </span>
  );
};

export type Role = 'STUDENT' | 'AGENT' | 'ADMIN';

export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export type UrgencyLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export type TicketStatus = 'OPEN' | 'ASSIGNED' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';

export type IncidentSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type CommentType = 'GENERAL' | 'ISSUE' | 'REASSIGNMENT' | 'CLOSURE';

export interface User {
  userId: number;
  name: string;
  email: string;
  role: Role;
  createdAt: string;
}

export interface Category {
  categoryId: number;
  categoryName: string;
}

export interface CommentDetail {
  commentId: number;
  ticketId: number;
  userId: number;
  authorName: string;
  commentText: string;
  commentType?: CommentType;
  createdAt: string;
}

export interface TicketItem {
  ticketId: number;
  ticketNumber: string;
  title: string;
  description: string;
  categoryName: string;
  requestedPriority: Priority | string;
  priority?: Priority | string;
  urgencyLevel: UrgencyLevel;
  urgencyScore: number;
  overdue: boolean;
  dueAt?: string | null;
  reasons?: string[];
  status: TicketStatus;
  createdBy?: number;
  assignedTo?: number | null;
  assignedAgentId?: number | null;
  incidentId?: number | null;
  createdAt: string;
  updatedAt?: string | null;
}

export interface TicketDetail extends TicketItem {
  creatorName: string;
  createdByName: string;
  assignedTo?: number | null;
  assignedAgentId?: number | null;
  assignedAgentName: string | null;
  incidentTitle: string | null;
  participantCount: number;
  comments: CommentDetail[];
}

export interface AssignmentRecord {
  historyId: number;
  ticketId: number;
  fromAgentId: number | null;
  fromAgentName: string | null;
  toAgentId: number;
  toAgentName: string;
  changedById: number;
  changedByName: string;
  assignmentType: 'ASSIGN' | 'REASSIGN';
  reason: string | null;
  changedAt: string;
}

export interface ClosedTicketLogItem {
  ticketNumber: string;
  title: string;
  category: string;
  assignedAgentName: string | null;
  closedBy: string;
  closedAt: string;
  closureNote: string;
  reassignmentCount: number;
}

export interface IncidentItem {
  incidentId: number;
  title: string;
  description: string;
  location: string;
  status: string;
  severity: IncidentSeverity;
  createdAt: string;
  updatedAt?: string | null;
  affectedStudentCount?: number;
}

export interface AdminStats {
  totalTickets: number;
  countsByStatus: Record<string, number>;
  countsByPriority: Record<string, number>;
  usersByRole: Record<string, number>;
  totalIncidents: number;
  overdueTickets: number;
  countsByUrgencyLevel: Record<string, number>;
}

export interface UrgencySummary {
  totalOverdue: number;
  countsByLevel: Record<string, number>;
}

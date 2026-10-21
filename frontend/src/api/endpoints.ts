import { request } from './client';
import type {
  AdminStats,
  AssignmentRecord,
  Category,
  ClosedTicketLogItem,
  CommentDetail,
  IncidentItem,
  TicketDetail,
  TicketItem,
  UrgencySummary,
  User,
} from './types';

export const api = {
  auth: {
    register: (data: { name: string; email: string; password: string }) =>
      request<User>('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify(data),
      }),
    me: () => request<User>('/api/auth/me', { method: 'GET' }),
  },

  tickets: {
    mine: () => request<TicketItem[]>('/api/tickets/mine', { method: 'GET' }),
    getByNumber: (ticketNumber: string) =>
      request<TicketDetail>(`/api/tickets/${encodeURIComponent(ticketNumber)}`, { method: 'GET' }),
    create: (data: {
      categoryId: number;
      title: string;
      description: string;
      priority: string;
    }) =>
      request<TicketItem>('/api/tickets', {
        method: 'POST',
        body: JSON.stringify(data),
      }),
    join: (ticketNumber: string) =>
      request<{ joined: boolean; ticketNumber: string }>(
        `/api/tickets/${encodeURIComponent(ticketNumber)}/join`,
        { method: 'POST' }
      ),
    addComment: (ticketId: number, commentText: string, commentType?: 'GENERAL' | 'ISSUE') =>
      request<CommentDetail>(`/api/tickets/${ticketId}/comments`, {
        method: 'POST',
        body: JSON.stringify({ commentText, ...(commentType ? { commentType } : {}) }),
      }),
    getComments: (ticketId: number) =>
      request<CommentDetail[]>(`/api/tickets/${ticketId}/comments`, { method: 'GET' }),
    updateStatus: (ticketId: number, status: string) =>
      request<TicketItem>(`/api/tickets/${ticketId}/status`, {
        method: 'PUT',
        body: JSON.stringify({ status }),
      }),
  },

  categories: {
    list: () => request<Category[]>('/api/categories', { method: 'GET' }),
  },

  incidents: {
    list: () => request<IncidentItem[]>('/api/incidents', { method: 'GET' }),
    getById: (id: number) => request<IncidentItem>(`/api/incidents/${id}`, { method: 'GET' }),
    create: (data: {
      title: string;
      description: string;
      location: string;
      severity: string;
    }) =>
      request<IncidentItem>('/api/incidents', {
        method: 'POST',
        body: JSON.stringify(data),
      }),
    getTickets: (id: number) =>
      request<{ incident: IncidentItem; tickets: TicketItem[]; affectedStudentCount: number }>(
        `/api/incidents/${id}/tickets`,
        { method: 'GET' }
      ),
  },

  admin: {
    tickets: (sort?: string) => {
      const query = sort ? `?sort=${encodeURIComponent(sort)}` : '';
      return request<TicketItem[]>(`/api/admin/tickets${query}`, { method: 'GET' });
    },
    overdueTickets: () =>
      request<TicketItem[]>('/api/admin/tickets/overdue', { method: 'GET' }),
    unassignedTickets: () =>
      request<TicketItem[]>('/api/admin/tickets/unassigned', { method: 'GET' }),
    urgencySummary: () =>
      request<UrgencySummary>('/api/admin/urgency/summary', { method: 'GET' }),
    assignTicket: (ticketId: number, agentId: number, reason?: string) =>
      request<TicketItem>(`/api/admin/tickets/${ticketId}/assign`, {
        method: 'POST',
        body: JSON.stringify({ agentId, ...(reason ? { reason } : {}) }),
      }),
    getAssignmentHistory: (ticketId: number) =>
      request<AssignmentRecord[]>(`/api/admin/tickets/${ticketId}/assignments`, { method: 'GET' }),
    closeTicket: (ticketId: number, note: string) =>
      request<TicketItem>(`/api/admin/tickets/${ticketId}/close`, {
        method: 'POST',
        body: JSON.stringify({ note }),
      }),
    closedTickets: () =>
      request<ClosedTicketLogItem[]>('/api/admin/tickets/closed', { method: 'GET' }),
    reopenTicket: (ticketId: number) =>
      request<TicketItem>(`/api/admin/tickets/${ticketId}/reopen`, { method: 'PUT' }),
    linkTicket: (incidentId: number, ticketId: number) =>
      request<{ message: string; ticketId: number; incidentId: number }>(
        `/api/admin/incidents/${incidentId}/tickets/${ticketId}`,
        { method: 'PUT' }
      ),
    listAgents: () => request<User[]>('/api/admin/agents', { method: 'GET' }),
    createAgent: (data: { name: string; email: string; password: string }) =>
      request<User>('/api/admin/agents', {
        method: 'POST',
        body: JSON.stringify(data),
      }),
    stats: () => request<AdminStats>('/api/admin/stats', { method: 'GET' }),
  },
};

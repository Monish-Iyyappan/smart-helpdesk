import React, { useEffect, useState, useMemo } from 'react';
import { motion } from 'motion/react';
import type { TicketDetail, TicketItem } from '../api/types';
import { api } from '../api/endpoints';
import { useAuth } from '../auth/AuthContext';
import { Shell } from '../components/Shell';
import { TicketCard } from '../components/TicketCard';
import { TicketDetailModal } from '../components/TicketDetailModal';
import { CountUp } from '../components/CountUp';

export const AgentPage: React.FC = () => {
  const { user, showToast } = useAuth();
  const [tickets, setTickets] = useState<TicketItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // View mode: 'KANBAN' or 'LIST'
  const [viewMode, setViewMode] = useState<'KANBAN' | 'LIST'>('KANBAN');

  // Filters & Sort
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [overdueOnly, setOverdueOnly] = useState(false);
  const [sortBy, setSortBy] = useState<'URGENCY' | 'NEWEST' | 'OLDEST'>('URGENCY');

  // Selected Ticket Modal
  const [selectedTicket, setSelectedTicket] = useState<TicketDetail | null>(null);

  const loadTickets = async () => {
    setIsLoading(true);
    try {
      const list = await api.tickets.mine();
      setTickets(list);
    } catch {
      // handled by global toast
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadTickets();
  }, []);

  const handleSelectTicket = async (ticket: TicketItem) => {
    try {
      const detail = await api.tickets.getByNumber(ticket.ticketNumber);
      setSelectedTicket(detail);
    } catch {
      // handled by toast
    }
  };

  const handleRefreshSelected = async () => {
    if (selectedTicket) {
      try {
        const detail = await api.tickets.getByNumber(selectedTicket.ticketNumber);
        setSelectedTicket(detail);
        const list = await api.tickets.mine();
        setTickets(list);
      } catch {
        // handled
      }
    }
  };

  const handleQuickStatusUpdate = async (e: React.MouseEvent, ticketId: number, nextStatus: string) => {
    e.stopPropagation();
    try {
      await api.tickets.updateStatus(ticketId, nextStatus);
      showToast(`Status updated to ${nextStatus}`, 'success');
      loadTickets();
    } catch {
      // handled by toast
    }
  };

  const filteredTickets = useMemo(() => {
    let result = [...tickets];

    if (statusFilter !== 'ALL') {
      result = result.filter((t) => t.status === statusFilter);
    }

    if (overdueOnly) {
      result = result.filter((t) => t.overdue);
    }

    if (sortBy === 'URGENCY') {
      const urgencyRank: Record<string, number> = { URGENT: 4, HIGH: 3, MEDIUM: 2, LOW: 1 };
      result.sort((a, b) => {
        const diff = (urgencyRank[b.urgencyLevel] || 0) - (urgencyRank[a.urgencyLevel] || 0);
        if (diff !== 0) return diff;
        return (b.urgencyScore || 0) - (a.urgencyScore || 0);
      });
    } else if (sortBy === 'OLDEST') {
      result.sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
    } else {
      result.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    }

    return result;
  }, [tickets, statusFilter, overdueOnly, sortBy]);

  const assignedCount = tickets.filter((t) => t.status === 'ASSIGNED').length;
  const progressCount = tickets.filter((t) => t.status === 'IN_PROGRESS').length;
  const resolvedCount = tickets.filter((t) => t.status === 'RESOLVED').length;
  const overdueCount = tickets.filter((t) => t.overdue).length;

  // Grouping for Kanban columns
  const kanbanColumns = useMemo(() => {
    return [
      {
        id: 'ASSIGNED',
        label: 'Assigned',
        badgeColor: 'bg-primary/10 text-primary border-primary/20',
        tickets: filteredTickets.filter((t) => t.status === 'ASSIGNED'),
        nextAction: (ticket: TicketItem) => (
          <button
            type="button"
            onClick={(e) => handleQuickStatusUpdate(e, ticket.ticketId, 'IN_PROGRESS')}
            className="px-2.5 py-1 text-xs font-semibold rounded-md bg-primary text-primary-text hover:bg-primary-hover transition-colors shadow-xs"
          >
            Start Work
          </button>
        ),
      },
      {
        id: 'IN_PROGRESS',
        label: 'In Progress',
        badgeColor: 'bg-warning/10 text-warning border-warning/20',
        tickets: filteredTickets.filter((t) => t.status === 'IN_PROGRESS'),
        nextAction: (ticket: TicketItem) => (
          <button
            type="button"
            onClick={(e) => handleQuickStatusUpdate(e, ticket.ticketId, 'RESOLVED')}
            className="px-2.5 py-1 text-xs font-semibold rounded-md bg-success text-white hover:opacity-90 transition-colors shadow-xs"
          >
            Resolve
          </button>
        ),
      },
      {
        id: 'RESOLVED',
        label: 'Resolved',
        badgeColor: 'bg-success/10 text-success border-success/20',
        tickets: filteredTickets.filter((t) => t.status === 'RESOLVED'),
        nextAction: () => null,
      },
    ];
  }, [filteredTickets]);

  return (
    <Shell
      title="Agent Workspace"
      headerActions={
        <div className="flex items-center gap-1 bg-surface-muted p-1 rounded-lg border border-border">
          <button
            type="button"
            onClick={() => setViewMode('KANBAN')}
            className={`px-3 py-1 text-xs font-semibold rounded-md transition-colors ${
              viewMode === 'KANBAN'
                ? 'bg-surface text-primary shadow-xs'
                : 'text-text-muted hover:text-text'
            }`}
          >
            Kanban Board
          </button>
          <button
            type="button"
            onClick={() => setViewMode('LIST')}
            className={`px-3 py-1 text-xs font-semibold rounded-md transition-colors ${
              viewMode === 'LIST'
                ? 'bg-surface text-primary shadow-xs'
                : 'text-text-muted hover:text-text'
            }`}
          >
            List View
          </button>
        </div>
      }
    >
      {/* Overview Cards with Count-up */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-3 mb-6">
        <div className="p-4 bg-surface border border-border rounded-xl shadow-xs">
          <div className="text-xs font-semibold text-text-muted">Assigned Queue</div>
          <div className="text-2xl font-bold text-text mt-1">
            <CountUp value={assignedCount} />
          </div>
          <div className="text-xs text-text-dim mt-0.5">Awaiting initial action</div>
        </div>

        <div className="p-4 bg-surface border border-border rounded-xl shadow-xs">
          <div className="text-xs font-semibold text-text-muted">In Progress</div>
          <div className="text-2xl font-bold text-text mt-1">
            <CountUp value={progressCount} />
          </div>
          <div className="text-xs text-text-dim mt-0.5">Currently active work</div>
        </div>

        <div className="p-4 bg-surface border border-border rounded-xl shadow-xs">
          <div className="text-xs font-semibold text-text-muted">Resolved</div>
          <div className="text-2xl font-bold text-success mt-1">
            <CountUp value={resolvedCount} />
          </div>
          <div className="text-xs text-text-dim mt-0.5">Awaiting student closing</div>
        </div>

        <div className="p-4 bg-surface border border-border rounded-xl shadow-xs">
          <div className="text-xs font-semibold text-text-muted">Overdue SLA</div>
          <div className="text-2xl font-bold text-danger mt-1">
            <CountUp value={overdueCount} />
          </div>
          <div className="text-xs text-text-dim mt-0.5">Past urgency deadline</div>
        </div>
      </div>

      {/* Filter and Sort Bar */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-3 mb-6 p-3 bg-surface border border-border rounded-xl shadow-xs">
        <div className="flex items-center gap-2 flex-wrap">
          {viewMode === 'LIST' && (
            <>
              <span className="text-xs font-semibold text-text-muted">Status:</span>
              {(['ALL', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED'] as const).map((st) => (
                <button
                  key={st}
                  type="button"
                  onClick={() => setStatusFilter(st)}
                  className={`px-2.5 py-1 text-xs rounded-md transition-colors ${
                    statusFilter === st
                      ? 'bg-primary text-primary-text font-semibold'
                      : 'bg-surface-muted text-text-muted hover:text-text'
                  }`}
                >
                  {st}
                </button>
              ))}
            </>
          )}

          <label className="inline-flex items-center gap-1.5 text-xs font-medium text-text-muted cursor-pointer">
            <input
              type="checkbox"
              checked={overdueOnly}
              onChange={(e) => setOverdueOnly(e.target.checked)}
              className="rounded border-border text-primary focus:ring-0"
            />
            <span>Overdue Only</span>
          </label>
        </div>

        <div className="flex items-center gap-2">
          <span className="text-xs font-semibold text-text-muted">Sort:</span>
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value as 'URGENCY' | 'NEWEST' | 'OLDEST')}
            className="px-2 py-1 text-xs rounded-md border border-border bg-surface text-text focus:outline-none focus:ring-1 focus:ring-primary"
          >
            <option value="URGENCY">Urgency Level (Highest First)</option>
            <option value="NEWEST">Newest First</option>
            <option value="OLDEST">Oldest First</option>
          </select>
        </div>
      </div>

      {/* Loading State */}
      {isLoading ? (
        <div className="text-center py-16 bg-surface border border-border rounded-xl">
          <div className="inline-block w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin mb-2" />
          <p className="text-xs text-text-muted">Loading assigned tickets...</p>
        </div>
      ) : filteredTickets.length === 0 ? (
        <div className="text-center py-16 bg-surface border border-border rounded-xl p-6">
          <p className="text-sm font-semibold text-text">No tickets found</p>
          <p className="text-xs text-text-muted mt-1">
            {overdueOnly
              ? 'No overdue tickets match your current filters.'
              : 'You have no tickets matching the current criteria.'}
          </p>
        </div>
      ) : viewMode === 'KANBAN' ? (
        /* Agent Kanban Board View (Assigned / In Progress / Resolved) */
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 items-start">
          {kanbanColumns.map((col) => (
            <div
              key={col.id}
              className="bg-surface-muted/60 border border-border rounded-xl p-3 flex flex-col min-h-[480px]"
            >
              <div className="flex items-center justify-between pb-3 mb-3 border-b border-border">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-bold text-text uppercase tracking-wider">
                    {col.label}
                  </span>
                  <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold border ${col.badgeColor}`}>
                    {col.tickets.length}
                  </span>
                </div>
              </div>

              <div className="space-y-3 flex-1 overflow-y-auto">
                {col.tickets.length === 0 ? (
                  <div className="text-center py-12 border border-dashed border-border rounded-lg p-4">
                    <p className="text-xs text-text-dim">No tickets in this column</p>
                  </div>
                ) : (
                  col.tickets.map((ticket) => (
                    <TicketCard
                      key={ticket.ticketId}
                      layoutId={`agent-kanban-card-${ticket.ticketId}`}
                      ticket={ticket}
                      userRole={user?.role}
                      onClick={() => handleSelectTicket(ticket)}
                      actionSlot={col.nextAction(ticket)}
                    />
                  ))
                )}
              </div>
            </div>
          ))}
        </div>
      ) : (
        /* Standard List View with layout animation */
        <motion.div layout className="grid grid-cols-1 gap-3">
          {filteredTickets.map((ticket) => (
            <TicketCard
              key={ticket.ticketId}
              layoutId={`agent-ticket-${ticket.ticketId}`}
              ticket={ticket}
              userRole={user?.role}
              onClick={() => handleSelectTicket(ticket)}
              actionSlot={
                ticket.status === 'ASSIGNED' ? (
                  <button
                    type="button"
                    onClick={(e) => handleQuickStatusUpdate(e, ticket.ticketId, 'IN_PROGRESS')}
                    className="px-2.5 py-1 text-xs font-semibold rounded-md bg-primary text-primary-text hover:bg-primary-hover transition-colors shadow-xs"
                  >
                    Start Work
                  </button>
                ) : ticket.status === 'IN_PROGRESS' ? (
                  <button
                    type="button"
                    onClick={(e) => handleQuickStatusUpdate(e, ticket.ticketId, 'RESOLVED')}
                    className="px-2.5 py-1 text-xs font-semibold rounded-md bg-success text-white hover:opacity-90 transition-colors shadow-xs"
                  >
                    Resolve
                  </button>
                ) : null
              }
            />
          ))}
        </motion.div>
      )}

      {/* Ticket Detail Modal */}
      {selectedTicket && (
        <TicketDetailModal
          ticket={selectedTicket}
          userRole={user?.role}
          onClose={() => setSelectedTicket(null)}
          onRefresh={handleRefreshSelected}
        />
      )}
    </Shell>
  );
};

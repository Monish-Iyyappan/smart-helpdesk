import React, { useEffect, useState, useMemo } from 'react';
import { motion } from 'motion/react';
import type { TicketDetail, TicketItem } from '../api/types';
import { api } from '../api/endpoints';
import { useAuth } from '../auth/AuthContext';
import { Shell } from '../components/Shell';
import { TicketCard } from '../components/TicketCard';
import { TicketDetailModal } from '../components/TicketDetailModal';
import { BentoStatCard } from '../components/BentoStatCard';
import { GreetingHeader } from '../components/GreetingHeader';
import { EmptyState } from '../components/EmptyState';
import { CardSkeleton } from '../components/ui/skeleton';
import { SearchIcon } from '../components/Icons';

export const AgentPage: React.FC = () => {
  const { user, showToast } = useAuth();
  const [tickets, setTickets] = useState<TicketItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // View mode: 'KANBAN' or 'LIST'
  const [viewMode, setViewMode] = useState<'KANBAN' | 'LIST'>('KANBAN');

  // Search & Filters & Sort (Jakob's Law & Hick's Law)
  const [searchQuery, setSearchQuery] = useState('');
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
      result = result.filter((t) => t.overdue && t.status !== 'RESOLVED' && t.status !== 'CLOSED');
    }

    if (searchQuery.trim()) {
      const q = searchQuery.trim().toLowerCase();
      result = result.filter(
        (t) =>
          t.ticketNumber?.toLowerCase().includes(q) ||
          t.title?.toLowerCase().includes(q) ||
          t.description?.toLowerCase().includes(q) ||
          t.categoryName?.toLowerCase().includes(q)
      );
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
  }, [tickets, statusFilter, overdueOnly, searchQuery, sortBy]);

  const assignedCount = tickets.filter((t) => t.status === 'ASSIGNED').length;
  const progressCount = tickets.filter((t) => t.status === 'IN_PROGRESS').length;
  const resolvedCount = tickets.filter((t) => t.status === 'RESOLVED').length;
  const overdueCount = tickets.filter(
    (t) => t.overdue && t.status !== 'RESOLVED' && t.status !== 'CLOSED'
  ).length;

  // Grouping for Kanban columns with 44px+ touch targets
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
            className="px-3.5 py-2 min-h-[38px] text-xs font-bold rounded-xl bg-primary text-primary-text hover:bg-primary-hover transition-all shadow-xs focus-ring active:scale-95"
            aria-label={`Start work on ticket ${ticket.ticketNumber}`}
          >
            Start Work
          </button>
        ),
      },
      {
        id: 'IN_PROGRESS',
        label: 'In Progress',
        badgeColor: 'bg-amber-500/10 text-amber-600 border-amber-500/20',
        tickets: filteredTickets.filter((t) => t.status === 'IN_PROGRESS'),
        nextAction: (ticket: TicketItem) => (
          <button
            type="button"
            onClick={(e) => handleQuickStatusUpdate(e, ticket.ticketId, 'RESOLVED')}
            className="px-3.5 py-2 min-h-[38px] text-xs font-bold rounded-xl bg-success text-white hover:opacity-90 transition-all shadow-xs focus-ring active:scale-95"
            aria-label={`Resolve ticket ${ticket.ticketNumber}`}
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

  const statusTabItems = [
    { id: 'ALL', label: 'All Queue', count: tickets.length },
    { id: 'ASSIGNED', label: 'Assigned', count: assignedCount },
    { id: 'IN_PROGRESS', label: 'In Progress', count: progressCount },
    { id: 'RESOLVED', label: 'Resolved', count: resolvedCount },
  ];

  return (
    <Shell
      title="Agent Workspace"
      headerActions={
        /* View mode toggle in navbar (Occam's Razor & Fitts's Law 44px+ touch target) */
        <div className="flex items-center gap-1 bg-surface-muted p-1 rounded-xl border border-border">
          <button
            type="button"
            onClick={() => setViewMode('KANBAN')}
            className={`px-3.5 py-2 min-h-[38px] text-xs font-bold rounded-lg transition-all focus-ring ${
              viewMode === 'KANBAN'
                ? 'bg-surface text-primary shadow-xs'
                : 'text-text-muted hover:text-text'
            }`}
            aria-pressed={viewMode === 'KANBAN'}
          >
            Kanban Board
          </button>
          <button
            type="button"
            onClick={() => setViewMode('LIST')}
            className={`px-3.5 py-2 min-h-[38px] text-xs font-bold rounded-lg transition-all focus-ring ${
              viewMode === 'LIST'
                ? 'bg-surface text-primary shadow-xs'
                : 'text-text-muted hover:text-text'
            }`}
            aria-pressed={viewMode === 'LIST'}
          >
            List View
          </button>
        </div>
      }
    >
      {/* 1. Bento Greeting Header with Clean Subtitle & Summary */}
      <GreetingHeader
        subtitle="Manage assigned queue, advance ticket workflows, and resolve issues within strict SLA deadlines."
      />

      {/* 2. Overview Bento Stat Tiles (Miller's Law & 8-point Grid) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6 min-w-0">
        <BentoStatCard
          label="Assigned Queue"
          value={assignedCount}
          sparklineData={[assignedCount, assignedCount + 1, assignedCount]}
          description="Awaiting initial work"
        />
        <BentoStatCard
          label="In Progress"
          value={progressCount}
          sparklineData={[progressCount, progressCount + 1, progressCount]}
          sparklineColor="var(--color-accent)"
          description="Currently active work"
        />
        <BentoStatCard
          label="Resolved"
          value={resolvedCount}
          sparklineData={[Math.max(0, resolvedCount - 1), resolvedCount, resolvedCount]}
          sparklineColor="var(--color-success)"
          description="Pending student confirmation"
        />
        <BentoStatCard
          label="Overdue SLA"
          value={overdueCount}
          sparklineData={[overdueCount, overdueCount + 1, overdueCount]}
          isAlert={overdueCount > 0}
          description="Priority SLA breached"
        />
      </div>

      {/* 3. Search, Filter and Sort Bar (Common Region & 44px+ touch targets) */}
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4 mb-6 p-4 bg-surface border border-border rounded-2xl shadow-xs">
        {/* Search Bar */}
        <div className="relative flex-1 max-w-md min-w-0">
          <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-text-dim pointer-events-none">
            <SearchIcon className="w-4 h-4" />
          </span>
          <input
            type="search"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search tickets by title, #HD-1001, or category..."
            className="w-full pl-10 pr-4 py-2.5 min-h-[44px] text-xs sm:text-sm rounded-xl border border-border bg-surface text-text placeholder-text-dim focus-ring transition-colors shadow-xs"
            aria-label="Search assigned tickets"
          />
        </div>

        {/* Filter Controls */}
        <div className="flex items-center gap-3 flex-wrap">
          {viewMode === 'LIST' && (
            <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar py-1">
              {statusTabItems.map((tab) => {
                const isActive = statusFilter === tab.id;
                return (
                  <button
                    key={tab.id}
                    type="button"
                    onClick={() => setStatusFilter(tab.id)}
                    className={`inline-flex items-center gap-1.5 px-3 py-2 min-h-[44px] text-xs rounded-xl font-semibold whitespace-nowrap transition-all focus-ring ${
                      isActive
                        ? 'bg-primary text-primary-text shadow-sm'
                        : 'bg-surface-muted text-text-muted hover:text-text hover:bg-surface-hover'
                    }`}
                    aria-pressed={isActive}
                  >
                    <span>{tab.label}</span>
                    <span
                      className={`px-1.5 py-0.5 rounded-full text-[10px] font-bold ${
                        isActive ? 'bg-primary-text/20 text-primary-text' : 'bg-surface text-text-dim'
                      }`}
                    >
                      {tab.count}
                    </span>
                  </button>
                );
              })}
            </div>
          )}

          {/* Overdue Only Filter (Von Restorff Effect) */}
          <label className="inline-flex items-center gap-2 px-3 py-2 min-h-[44px] rounded-xl border border-border bg-surface-muted/60 text-xs font-semibold text-text cursor-pointer hover:bg-surface-hover transition-colors focus-ring select-none">
            <input
              type="checkbox"
              checked={overdueOnly}
              onChange={(e) => setOverdueOnly(e.target.checked)}
              className="w-4 h-4 rounded border-border text-primary focus:ring-primary focus:ring-offset-1"
            />
            <span className={overdueOnly ? 'text-danger font-bold' : 'text-text-muted'}>
              Overdue Only ({overdueCount})
            </span>
          </label>

          {/* Sort Dropdown */}
          <div className="flex items-center gap-2 shrink-0">
            <label htmlFor="agent-sort-select" className="text-xs font-bold text-text-muted">
              Sort:
            </label>
            <select
              id="agent-sort-select"
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value as 'URGENCY' | 'NEWEST' | 'OLDEST')}
              className="px-3 py-2 min-h-[44px] text-xs font-semibold rounded-xl border border-border bg-surface text-text focus-ring shadow-xs cursor-pointer"
            >
              <option value="URGENCY">Urgency Level (Highest First)</option>
              <option value="NEWEST">Newest First</option>
              <option value="OLDEST">Oldest First</option>
            </select>
          </div>
        </div>
      </div>

      {/* 4. Ticket Views with Skeleton Loading (Doherty Threshold & Common Region) */}
      {isLoading ? (
        <CardSkeleton count={3} />
      ) : filteredTickets.length === 0 ? (
        <EmptyState
          title="No Tickets Found"
          description={
            searchQuery.trim()
              ? `No tickets match "${searchQuery}".`
              : overdueOnly
              ? 'Great work! No tickets are currently breaching SLA deadlines.'
              : 'You have no tickets matching the current criteria.'
          }
          iconType={searchQuery ? 'search' : 'inbox'}
          action={
            (searchQuery || overdueOnly || statusFilter !== 'ALL') && (
              <button
                type="button"
                onClick={() => {
                  setSearchQuery('');
                  setOverdueOnly(false);
                  setStatusFilter('ALL');
                }}
                className="px-4 py-2.5 min-h-[44px] text-xs font-semibold rounded-xl border border-border bg-surface text-text hover:bg-surface-hover transition-colors shadow-xs focus-ring"
              >
                Reset Filters
              </button>
            )
          }
        />
      ) : viewMode === 'KANBAN' ? (
        /* Agent Kanban Board View (Assigned / In Progress / Resolved) */
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 items-start">
          {kanbanColumns.map((col) => (
            <div
              key={col.id}
              className="bg-surface-muted/60 border border-border rounded-2xl p-4 flex flex-col min-h-[520px] shadow-xs"
            >
              <div className="flex items-center justify-between pb-3.5 mb-4 border-b border-border">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-bold text-text uppercase tracking-wider">
                    {col.label}
                  </span>
                  <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold border ${col.badgeColor}`}>
                    {col.tickets.length}
                  </span>
                </div>
              </div>

              <div className="space-y-4 flex-1 overflow-y-auto">
                {col.tickets.length === 0 ? (
                  <div className="text-center py-16 border-2 border-dashed border-border rounded-xl p-4">
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
        <motion.div layout className="grid grid-cols-1 gap-4">
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
                    className="px-3.5 py-2 min-h-[38px] text-xs font-bold rounded-xl bg-primary text-primary-text hover:bg-primary-hover transition-all shadow-xs focus-ring active:scale-95"
                    aria-label={`Start work on ticket ${ticket.ticketNumber}`}
                  >
                    Start Work
                  </button>
                ) : ticket.status === 'IN_PROGRESS' ? (
                  <button
                    type="button"
                    onClick={(e) => handleQuickStatusUpdate(e, ticket.ticketId, 'RESOLVED')}
                    className="px-3.5 py-2 min-h-[38px] text-xs font-bold rounded-xl bg-success text-white hover:opacity-90 transition-all shadow-xs focus-ring active:scale-95"
                    aria-label={`Resolve ticket ${ticket.ticketNumber}`}
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

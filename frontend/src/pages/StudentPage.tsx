import React, { useEffect, useState, useMemo } from 'react';
import { motion } from 'motion/react';
import type { Category, Priority, TicketDetail, TicketItem } from '../api/types';
import { api } from '../api/endpoints';
import { useAuth } from '../auth/AuthContext';
import { Shell } from '../components/Shell';
import { TicketCard } from '../components/TicketCard';
import { TicketDetailModal } from '../components/TicketDetailModal';
import { PlusIcon, LinkIcon, SearchIcon, SparklesIcon, CheckCircleIcon } from '../components/Icons';
import { Modal, ModalBody, ModalContent } from '../components/ui/animated-modal';
import { StatefulButton } from '../components/ui/stateful-button';
import { BentoStatCard } from '../components/BentoStatCard';
import { GreetingHeader } from '../components/GreetingHeader';
import { EmptyState } from '../components/EmptyState';
import { CardSkeleton } from '../components/ui/skeleton';

export const StudentPage: React.FC = () => {
  const { user, showToast } = useAuth();
  const [tickets, setTickets] = useState<TicketItem[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Filter & Search & Sort (Jakob's Law & Hick's Law)
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [sortBy, setSortBy] = useState<'NEWEST' | 'URGENCY' | 'OLDEST'>('NEWEST');

  // Modals
  const [selectedTicket, setSelectedTicket] = useState<TicketDetail | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showJoinModal, setShowJoinModal] = useState(false);

  // Create Form State & Live Validation (Zeigarnik Effect & Goal-Gradient)
  const [newCategoryId, setNewCategoryId] = useState<number | ''>('');
  const [newTitle, setNewTitle] = useState('');
  const [newDescription, setNewDescription] = useState('');
  const [newPriority, setNewPriority] = useState<Priority>('MEDIUM');
  const [isCreating, setIsCreating] = useState(false);
  const [createFormTouched, setCreateFormTouched] = useState<{ title?: boolean; desc?: boolean }>({});

  // Join Form State (Postel's Law: forgiving uppercase & whitespace handling)
  const [joinTicketNumber, setJoinTicketNumber] = useState('');
  const [isJoining, setIsJoining] = useState(false);

  const loadData = async () => {
    setIsLoading(true);
    try {
      const [tList, cList] = await Promise.all([
        api.tickets.mine(),
        api.categories.list(),
      ]);
      setTickets(tList);
      setCategories(cList);
      if (cList.length > 0 && newCategoryId === '') {
        setNewCategoryId(cList[0].categoryId);
      }
    } catch {
      // handled by global toast
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleSelectTicket = async (ticket: TicketItem) => {
    try {
      const detail = await api.tickets.getByNumber(ticket.ticketNumber);
      setSelectedTicket(detail);
    } catch {
      // handled by global toast
    }
  };

  const handleRefreshSelectedTicket = async () => {
    if (selectedTicket) {
      try {
        const detail = await api.tickets.getByNumber(selectedTicket.ticketNumber);
        setSelectedTicket(detail);
        // also refresh tickets list in background
        const tList = await api.tickets.mine();
        setTickets(tList);
      } catch {
        // handled by toast
      }
    }
  };

  const handleCreateTicket = async (e: React.FormEvent) => {
    e.preventDefault();
    setCreateFormTouched({ title: true, desc: true });

    if (!newCategoryId || !newTitle.trim() || !newDescription.trim()) return;

    setIsCreating(true);
    try {
      await api.tickets.create({
        categoryId: Number(newCategoryId),
        title: newTitle.trim(),
        description: newDescription.trim(),
        priority: newPriority,
      });
      showToast('Ticket created successfully!', 'success');
      setShowCreateModal(false);
      setNewTitle('');
      setNewDescription('');
      setNewPriority('MEDIUM');
      setCreateFormTouched({});
      loadData();
    } catch {
      // handled by global toast
    } finally {
      setIsCreating(false);
    }
  };

  const handleJoinTicket = async (e: React.FormEvent) => {
    e.preventDefault();
    const cleanNumber = joinTicketNumber.trim().toUpperCase();
    if (!cleanNumber) return;

    setIsJoining(true);
    try {
      const joined = await api.tickets.join(cleanNumber);
      showToast(`Joined ticket ${joined.ticketNumber}`, 'success');
      setShowJoinModal(false);
      setJoinTicketNumber('');
      loadData();
    } catch {
      // handled by global toast
    } finally {
      setIsJoining(false);
    }
  };

  // Filter, Search, and Sort Tickets (Miller's Law & Cognitive Load)
  const filteredTickets = useMemo(() => {
    let result = [...tickets];

    // Status filtering
    if (statusFilter !== 'ALL') {
      result = result.filter((t) => t.status === statusFilter);
    }

    // Search query matching (Jakob's Law)
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

    // Sorting (Serial Position Effect)
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
      // NEWEST
      result.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    }

    return result;
  }, [tickets, statusFilter, searchQuery, sortBy]);

  // Aggregate Metrics (Bento Chunking & Miller's Law)
  const openCount = tickets.filter(
    (t) => t.status === 'OPEN' || t.status === 'ASSIGNED' || t.status === 'IN_PROGRESS'
  ).length;
  const resolvedCount = tickets.filter(
    (t) => t.status === 'RESOLVED' || t.status === 'CLOSED'
  ).length;
  const overdueCount = tickets.filter(
    (t) => t.overdue && t.status !== 'RESOLVED' && t.status !== 'CLOSED'
  ).length;

  const statusTabItems = [
    { id: 'ALL', label: 'All Tickets', count: tickets.length },
    { id: 'OPEN', label: 'Open', count: tickets.filter((t) => t.status === 'OPEN').length },
    { id: 'ASSIGNED', label: 'Assigned', count: tickets.filter((t) => t.status === 'ASSIGNED').length },
    { id: 'IN_PROGRESS', label: 'In Progress', count: tickets.filter((t) => t.status === 'IN_PROGRESS').length },
    { id: 'RESOLVED', label: 'Resolved', count: tickets.filter((t) => t.status === 'RESOLVED').length },
    { id: 'CLOSED', label: 'Closed', count: tickets.filter((t) => t.status === 'CLOSED').length },
  ];

  return (
    <Shell
      title="My Tickets"
      headerActions={
        /* Desktop Top Navbar: Single Primary Action with 44px+ touch target */
        <button
          type="button"
          onClick={() => setShowCreateModal(true)}
          className="hidden sm:inline-flex items-center gap-2 px-4 py-2.5 min-h-[44px] text-xs font-bold rounded-xl bg-primary text-primary-text hover:bg-primary-hover transition-all shadow-sm focus-ring active:scale-95"
          aria-label="Create New Ticket"
        >
          <PlusIcon className="w-4 h-4" />
          <span>New Ticket</span>
        </button>
      }
    >
      {/* 1. Bento Greeting Header with Clear Hierarchy & Primary Action (Von Restorff Effect) */}
      <GreetingHeader
        subtitle="Track submitted requests, join shared incident tickets, and monitor live resolution times."
        actions={
          <div className="flex items-center gap-3 flex-wrap">
            {/* Secondary Action: Join Ticket */}
            <button
              type="button"
              onClick={() => setShowJoinModal(true)}
              className="inline-flex items-center gap-2 px-3.5 py-2.5 min-h-[44px] text-xs font-semibold rounded-xl border border-border bg-surface text-text hover:bg-surface-hover hover:border-text-dim transition-all focus-ring shadow-xs active:scale-95"
              aria-label="Join an Existing Ticket"
            >
              <LinkIcon className="w-4 h-4 text-text-muted" />
              <span>Join Ticket</span>
            </button>
            {/* Primary Action: Create Ticket */}
            <button
              type="button"
              onClick={() => setShowCreateModal(true)}
              className="inline-flex items-center gap-2 px-4 py-2.5 min-h-[44px] text-xs font-bold rounded-xl bg-primary text-primary-text hover:bg-primary-hover transition-all shadow-md focus-ring active:scale-95"
              aria-label="Create New Ticket"
            >
              <PlusIcon className="w-4 h-4" />
              <span>Create Ticket</span>
            </button>
          </div>
        }
      />

      {/* 2. Bento Stat Tiles (Miller's Law: 4 items, 8pt Grid spacing) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6 min-w-0">
        <BentoStatCard
          label="Total Tickets"
          value={tickets.length}
          sparklineData={[tickets.length, tickets.length + 1, Math.max(1, tickets.length), tickets.length]}
          description="All student submissions"
        />
        <BentoStatCard
          label="Active In Flight"
          value={openCount}
          sparklineData={[openCount, openCount + 1, openCount]}
          sparklineColor="var(--color-accent)"
          description="Open, assigned & working"
        />
        <BentoStatCard
          label="Resolved / Closed"
          value={resolvedCount}
          sparklineData={[Math.max(0, resolvedCount - 1), resolvedCount, resolvedCount]}
          sparklineColor="var(--color-success)"
          description="Successfully completed"
        />
        <BentoStatCard
          label="Overdue Alerts"
          value={overdueCount}
          sparklineData={[overdueCount, overdueCount + 1, overdueCount]}
          isAlert={overdueCount > 0}
          description="SLA breached tickets"
        />
      </div>

      {/* 3. Search & Filter Bar (Common Region, 44px+ touch targets, 8pt Grid) */}
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4 mb-6 p-4 bg-surface border border-border rounded-2xl shadow-xs">
        {/* Search Input (Jakob's Law) */}
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
            aria-label="Search my tickets"
          />
        </div>

        {/* Status Filter Pills (Fitts's Law: min 44px touch targets) */}
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

        {/* Sort Dropdown */}
        <div className="flex items-center gap-2 shrink-0">
          <label htmlFor="sort-select" className="text-xs font-bold text-text-muted">
            Sort:
          </label>
          <select
            id="sort-select"
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value as 'NEWEST' | 'URGENCY' | 'OLDEST')}
            className="px-3 py-2 min-h-[44px] text-xs font-semibold rounded-xl border border-border bg-surface text-text focus-ring shadow-xs cursor-pointer"
          >
            <option value="NEWEST">Newest First</option>
            <option value="URGENCY">Urgency Level</option>
            <option value="OLDEST">Oldest First</option>
          </select>
        </div>
      </div>

      {/* 4. Ticket Stream with Staggered Entrance & Skeleton States (Doherty Threshold) */}
      {isLoading ? (
        <CardSkeleton count={3} />
      ) : filteredTickets.length === 0 ? (
        <EmptyState
          title="No Tickets Found"
          description={
            searchQuery.trim()
              ? `No tickets match "${searchQuery}". Try clearing your search term.`
              : statusFilter !== 'ALL'
              ? `You don't have any tickets currently marked as ${statusFilter}.`
              : 'Create a new ticket or join an existing ticket to begin tracking issues.'
          }
          iconType={searchQuery ? 'search' : 'tickets'}
          action={
            <div className="flex items-center gap-3">
              {searchQuery && (
                <button
                  type="button"
                  onClick={() => setSearchQuery('')}
                  className="px-4 py-2.5 min-h-[44px] text-xs font-semibold rounded-xl border border-border bg-surface text-text hover:bg-surface-hover transition-colors shadow-xs focus-ring"
                >
                  Clear Search
                </button>
              )}
              <button
                type="button"
                onClick={() => setShowCreateModal(true)}
                className="inline-flex items-center gap-2 px-4 py-2.5 min-h-[44px] text-xs font-bold rounded-xl bg-primary text-primary-text hover:bg-primary-hover transition-all shadow-md focus-ring"
              >
                <PlusIcon className="w-4 h-4" />
                <span>Create Ticket</span>
              </button>
            </div>
          }
        />
      ) : (
        <motion.div
          layout
          initial="hidden"
          animate="show"
          variants={{
            hidden: { opacity: 0 },
            show: {
              opacity: 1,
              transition: { staggerChildren: 0.04 },
            },
          }}
          className="grid grid-cols-1 gap-4"
        >
          {filteredTickets.map((ticket) => (
            <motion.div
              key={ticket.ticketId}
              layout
              variants={{
                hidden: { opacity: 0, y: 10 },
                show: { opacity: 1, y: 0, transition: { duration: 0.2 } },
              }}
            >
              <TicketCard
                ticket={ticket}
                userRole={user?.role}
                onClick={() => handleSelectTicket(ticket)}
              />
            </motion.div>
          ))}
        </motion.div>
      )}

      {/* Detail Modal */}
      {selectedTicket && (
        <TicketDetailModal
          ticket={selectedTicket}
          userRole={user?.role}
          onClose={() => setSelectedTicket(null)}
          onRefresh={handleRefreshSelectedTicket}
        />
      )}

      {/* 5. Create Ticket Modal (Goal-Gradient, Zeigarnik Effect, & 44px+ Touch Targets) */}
      <Modal open={showCreateModal} setOpen={setShowCreateModal}>
        <ModalBody className="max-w-xl">
          <ModalContent>
            <div className="border-b border-border pb-4 mb-4">
              <h3 className="text-lg font-bold text-text">Create Support Ticket</h3>
              <p className="text-xs text-text-muted mt-1 leading-relaxed max-w-prose">
                Submit an inquiry or issue to campus technicians. Urgency scores are automatically evaluated based on category and SLA deadlines.
              </p>
            </div>

            <form onSubmit={handleCreateTicket} className="space-y-4" noValidate>
              {/* Category Field */}
              <div>
                <label className="block text-xs font-bold text-text mb-1" htmlFor="ticket-category">
                  Helpdesk Category
                </label>
                <select
                  id="ticket-category"
                  value={newCategoryId}
                  onChange={(e) => setNewCategoryId(Number(e.target.value))}
                  className="w-full px-3.5 py-2.5 min-h-[44px] text-xs sm:text-sm rounded-xl border border-border bg-surface text-text focus-ring shadow-xs cursor-pointer"
                  required
                >
                  {categories.map((c) => (
                    <option key={c.categoryId} value={c.categoryId}>
                      {c.categoryName}
                    </option>
                  ))}
                </select>
              </div>

              {/* Title Field with Live Counter and Checkmark (Goal-Gradient Effect) */}
              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="block text-xs font-bold text-text" htmlFor="ticket-title">
                    Title
                  </label>
                  <div className="flex items-center gap-1.5 text-[11px] text-text-dim">
                    {newTitle.trim().length >= 5 && (
                      <span className="inline-flex items-center gap-1 text-success font-semibold">
                        <CheckCircleIcon className="w-3.5 h-3.5" />
                        Valid
                      </span>
                    )}
                    <span>{newTitle.length}/100</span>
                  </div>
                </div>
                <input
                  id="ticket-title"
                  type="text"
                  maxLength={100}
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  onBlur={() => setCreateFormTouched((prev) => ({ ...prev, title: true }))}
                  placeholder="e.g. Wi-Fi connection drops intermittently in Library 2nd floor"
                  className="w-full px-3.5 py-2.5 min-h-[44px] text-xs sm:text-sm rounded-xl border border-border bg-surface text-text placeholder-text-dim focus-ring shadow-xs"
                  required
                />
                {createFormTouched.title && !newTitle.trim() && (
                  <p className="mt-1 text-[11px] text-danger font-medium">Please enter a concise ticket title.</p>
                )}
              </div>

              {/* Description Field (50-75 char line length guidance) */}
              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="block text-xs font-bold text-text" htmlFor="ticket-desc">
                    Detailed Description
                  </label>
                  <div className="flex items-center gap-1.5 text-[11px] text-text-dim">
                    {newDescription.trim().length >= 10 && (
                      <span className="inline-flex items-center gap-1 text-success font-semibold">
                        <CheckCircleIcon className="w-3.5 h-3.5" />
                        Detailed
                      </span>
                    )}
                    <span>{newDescription.length}/1000</span>
                  </div>
                </div>
                <textarea
                  id="ticket-desc"
                  rows={4}
                  maxLength={1000}
                  value={newDescription}
                  onChange={(e) => setNewDescription(e.target.value)}
                  onBlur={() => setCreateFormTouched((prev) => ({ ...prev, desc: true }))}
                  placeholder="Describe what occurred, any error messages shown, and your device details..."
                  className="w-full px-3.5 py-2.5 text-xs sm:text-sm rounded-xl border border-border bg-surface text-text placeholder-text-dim focus-ring shadow-xs max-w-prose leading-relaxed"
                  required
                />
                {createFormTouched.desc && !newDescription.trim() && (
                  <p className="mt-1 text-[11px] text-danger font-medium">Please describe the problem you are experiencing.</p>
                )}
              </div>

              {/* Requested Priority & Explanatory Tip (Tesler's Law) */}
              <div>
                <label className="block text-xs font-bold text-text mb-1" htmlFor="ticket-priority">
                  Requested Priority
                </label>
                <select
                  id="ticket-priority"
                  value={newPriority}
                  onChange={(e) => setNewPriority(e.target.value as Priority)}
                  className="w-full px-3.5 py-2.5 min-h-[44px] text-xs sm:text-sm rounded-xl border border-border bg-surface text-text focus-ring shadow-xs cursor-pointer"
                >
                  <option value="LOW">LOW — Routine request, no urgent blocker</option>
                  <option value="MEDIUM">MEDIUM — Standard issue, partial impediment</option>
                  <option value="HIGH">HIGH — Significant blocker affecting coursework</option>
                  <option value="URGENT">URGENT — Critical stoppage requiring emergency intervention</option>
                </select>

                <div className="mt-2.5 p-3 rounded-xl bg-surface-muted border border-border/80 flex items-start gap-2.5 text-[11px] text-text-muted leading-relaxed">
                  <SparklesIcon className="w-4 h-4 text-accent shrink-0 mt-0.5" />
                  <span>
                    <strong>Automated Urgency Engine:</strong> Final SLA targets and agent assignment are continuously balanced based on incident groupings, keywords, and priority metrics.
                  </span>
                </div>
              </div>

              {/* Form Action Controls (44px+ touch targets & Doherty Threshold) */}
              <div className="flex items-center justify-end gap-3 pt-4 border-t border-border mt-4">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2.5 min-h-[44px] text-xs font-semibold rounded-xl border border-border text-text-muted hover:text-text hover:bg-surface-hover transition-colors focus-ring"
                >
                  Cancel
                </button>
                <StatefulButton
                  type="submit"
                  disabled={isCreating}
                  loading={isCreating}
                  className="px-5 py-2.5 min-h-[44px] text-xs font-bold rounded-xl shadow-md"
                  loadingText="Creating ticket..."
                  successText="Ticket Created!"
                >
                  Submit Ticket
                </StatefulButton>
              </div>
            </form>
          </ModalContent>
        </ModalBody>
      </Modal>

      {/* 6. Join Ticket Modal (Unified Modal Architecture & Postel's Law) */}
      <Modal open={showJoinModal} setOpen={setShowJoinModal}>
        <ModalBody className="max-w-md">
          <ModalContent>
            <div className="border-b border-border pb-4 mb-4">
              <h3 className="text-lg font-bold text-text">Join Shared Ticket</h3>
              <p className="text-xs text-text-muted mt-1 leading-relaxed">
                If other students already filed a ticket for the same campus issue, enter its ticket number below to subscribe to incident updates.
              </p>
            </div>

            <form onSubmit={handleJoinTicket} className="space-y-4" noValidate>
              <div>
                <label className="block text-xs font-bold text-text mb-1" htmlFor="join-ticket-num">
                  Ticket Number
                </label>
                <div className="relative flex items-center">
                  <span className="absolute left-3.5 text-text-dim pointer-events-none">
                    <LinkIcon className="w-4 h-4" />
                  </span>
                  <input
                    id="join-ticket-num"
                    type="text"
                    value={joinTicketNumber}
                    onChange={(e) => setJoinTicketNumber(e.target.value.toUpperCase())}
                    onBlur={() => setJoinTicketNumber((prev) => prev.trim().toUpperCase())}
                    placeholder="e.g. HD-1001"
                    className="w-full pl-10 pr-3.5 py-2.5 min-h-[44px] font-mono text-xs sm:text-sm rounded-xl border border-border bg-surface text-text placeholder-text-dim focus-ring shadow-xs uppercase"
                    required
                  />
                </div>
                <p className="mt-1 text-[11px] text-text-dim">
                  Enter the standard format like <span className="font-mono font-semibold">HD-1001</span>.
                </p>
              </div>

              <div className="flex items-center justify-end gap-3 pt-4 border-t border-border mt-4">
                <button
                  type="button"
                  onClick={() => setShowJoinModal(false)}
                  className="px-4 py-2.5 min-h-[44px] text-xs font-semibold rounded-xl border border-border text-text-muted hover:text-text hover:bg-surface-hover transition-colors focus-ring"
                >
                  Cancel
                </button>
                <StatefulButton
                  type="submit"
                  disabled={isJoining}
                  loading={isJoining}
                  className="px-5 py-2.5 min-h-[44px] text-xs font-bold rounded-xl shadow-md"
                  loadingText="Joining..."
                  successText="Joined!"
                >
                  Join Ticket
                </StatefulButton>
              </div>
            </form>
          </ModalContent>
        </ModalBody>
      </Modal>
    </Shell>
  );
};

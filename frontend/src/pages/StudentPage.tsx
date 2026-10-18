import React, { useEffect, useState, useMemo } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import type { Category, Priority, TicketDetail, TicketItem } from '../api/types';
import { api } from '../api/endpoints';
import { useAuth } from '../auth/AuthContext';
import { Shell } from '../components/Shell';
import { TicketCard } from '../components/TicketCard';
import { TicketDetailModal } from '../components/TicketDetailModal';
import { PlusIcon, LinkIcon } from '../components/Icons';
import { Modal, ModalBody, ModalContent } from '../components/ui/animated-modal';
import { StatefulButton } from '../components/ui/stateful-button';

export const StudentPage: React.FC = () => {
  const { user, showToast } = useAuth();
  const [tickets, setTickets] = useState<TicketItem[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Filter & Sort
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [sortBy, setSortBy] = useState<'NEWEST' | 'URGENCY' | 'OLDEST'>('NEWEST');

  // Modals
  const [selectedTicket, setSelectedTicket] = useState<TicketDetail | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showJoinModal, setShowJoinModal] = useState(false);

  // Create Form State
  const [newCategoryId, setNewCategoryId] = useState<number | ''>('');
  const [newTitle, setNewTitle] = useState('');
  const [newDescription, setNewDescription] = useState('');
  const [newPriority, setNewPriority] = useState<Priority>('MEDIUM');
  const [isCreating, setIsCreating] = useState(false);

  // Join Form State
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
      loadData();
    } catch {
      // handled by global toast
    } finally {
      setIsCreating(false);
    }
  };

  const handleJoinTicket = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!joinTicketNumber.trim()) return;

    setIsJoining(true);
    try {
      const joined = await api.tickets.join(joinTicketNumber.trim().toUpperCase());
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

  // Filter and Sort Tickets
  const filteredTickets = useMemo(() => {
    let result = [...tickets];
    if (statusFilter !== 'ALL') {
      result = result.filter((t) => t.status === statusFilter);
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
      // NEWEST
      result.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    }

    return result;
  }, [tickets, statusFilter, sortBy]);

  return (
    <Shell
      title="My Tickets"
      headerActions={
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setShowJoinModal(true)}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold rounded-md border border-border bg-surface text-text hover:bg-surface-hover transition-colors"
          >
            <LinkIcon className="w-3.5 h-3.5" />
            <span>Join Ticket</span>
          </button>
          <button
            type="button"
            onClick={() => setShowCreateModal(true)}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold rounded-md bg-primary text-primary-text hover:bg-primary-hover transition-colors shadow-xs"
          >
            <PlusIcon className="w-3.5 h-3.5" />
            <span>Create Ticket</span>
          </button>
        </div>
      }
    >
      {/* Filtering and Sorting Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-6 p-3 bg-surface border border-border rounded-xl shadow-xs">
        <div className="flex items-center gap-2 flex-wrap">
          <span className="text-xs font-semibold text-text-muted">Status:</span>
          {(['ALL', 'OPEN', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'] as const).map((st) => (
            <button
              key={st}
              type="button"
              onClick={() => setStatusFilter(st)}
              className={`px-2.5 py-1 text-xs rounded-md transition-colors ${
                statusFilter === st
                  ? 'bg-primary text-primary-text font-semibold shadow-xs'
                  : 'bg-surface-muted text-text-muted hover:text-text'
              }`}
            >
              {st}
            </button>
          ))}
        </div>

        <div className="flex items-center gap-2">
          <span className="text-xs font-semibold text-text-muted">Sort:</span>
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value as 'NEWEST' | 'URGENCY' | 'OLDEST')}
            className="px-2 py-1 text-xs rounded-md border border-border bg-surface text-text focus:outline-none focus:ring-1 focus:ring-primary"
          >
            <option value="NEWEST">Newest First</option>
            <option value="URGENCY">Urgency Level</option>
            <option value="OLDEST">Oldest First</option>
          </select>
        </div>
      </div>

      {/* Ticket List with Staggered Entrance & Layout Animation */}
      {isLoading ? (
        <div className="text-center py-16 bg-surface border border-border rounded-xl">
          <div className="inline-block w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin mb-2" />
          <p className="text-xs text-text-muted">Loading your tickets...</p>
        </div>
      ) : filteredTickets.length === 0 ? (
        <div className="text-center py-16 bg-surface border border-border rounded-xl p-6">
          <p className="text-sm font-semibold text-text">No tickets found</p>
          <p className="text-xs text-text-muted mt-1">
            {statusFilter !== 'ALL'
              ? `You don't have any tickets with status ${statusFilter}`
              : 'Create a new ticket or join an existing ticket using its ticket number.'}
          </p>
        </div>
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
          className="grid grid-cols-1 gap-3"
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

      {/* Create Ticket Animated Modal (Aceternity UI) */}
      <Modal open={showCreateModal} setOpen={setShowCreateModal}>
        <ModalBody>
          <ModalContent>
            <h3 className="text-base font-bold text-text mb-4">Create New Helpdesk Ticket</h3>
            <form onSubmit={handleCreateTicket} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-text mb-1">
                  Category
                </label>
                <select
                  value={newCategoryId}
                  onChange={(e) => setNewCategoryId(Number(e.target.value))}
                  className="w-full px-3 py-2 text-sm rounded-md border border-border bg-surface text-text focus:outline-none focus:ring-1 focus:ring-primary"
                  required
                >
                  {categories.map((c) => (
                    <option key={c.categoryId} value={c.categoryId}>
                      {c.categoryName}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-text mb-1">
                  Title
                </label>
                <input
                  type="text"
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  placeholder="Brief summary of the issue..."
                  className="w-full px-3 py-2 text-sm rounded-md border border-border bg-surface text-text placeholder-text-dim focus:outline-none focus:ring-1 focus:ring-primary"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-text mb-1">
                  Description
                </label>
                <textarea
                  rows={4}
                  value={newDescription}
                  onChange={(e) => setNewDescription(e.target.value)}
                  placeholder="Provide details about the problem you are experiencing..."
                  className="w-full px-3 py-2 text-sm rounded-md border border-border bg-surface text-text placeholder-text-dim focus:outline-none focus:ring-1 focus:ring-primary"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-text mb-1">
                  Requested Priority
                </label>
                <select
                  value={newPriority}
                  onChange={(e) => setNewPriority(e.target.value as Priority)}
                  className="w-full px-3 py-2 text-sm rounded-md border border-border bg-surface text-text focus:outline-none focus:ring-1 focus:ring-primary"
                >
                  <option value="LOW">LOW</option>
                  <option value="MEDIUM">MEDIUM</option>
                  <option value="HIGH">HIGH</option>
                  <option value="URGENT">URGENT</option>
                </select>
                <span className="block text-xs text-text-dim mt-1">
                  Note: The system automatically computes urgency based on category, keywords, affected students, and deadlines.
                </span>
              </div>

              <div className="flex items-center justify-end gap-2 pt-3 border-t border-border-subtle">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-3 py-1.5 text-xs font-semibold rounded-md border border-border text-text-muted hover:text-text hover:bg-surface-hover transition-colors"
                >
                  Cancel
                </button>
                <StatefulButton
                  type="submit"
                  disabled={isCreating}
                  loadingText="Submitting..."
                  successText="Submitted!"
                >
                  Submit Ticket
                </StatefulButton>
              </div>
            </form>
          </ModalContent>
        </ModalBody>
      </Modal>

      {/* Join Ticket Modal */}
      <AnimatePresence>
        {showJoinModal && (
          <div
            role="dialog"
            aria-modal="true"
            className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-overlay overflow-y-auto"
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              transition={{ duration: 0.2 }}
              className="bg-surface border border-border rounded-xl max-w-sm w-full p-6 shadow-xl"
            >
              <h3 className="text-base font-bold text-text mb-2">Join an Existing Ticket</h3>
              <p className="text-xs text-text-muted mb-4">
                Enter the ticket number (e.g. <span className="font-mono">HD-1001</span>) if you share the same issue.
              </p>
              <form onSubmit={handleJoinTicket} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-text mb-1">
                    Ticket Number
                  </label>
                  <input
                    type="text"
                    value={joinTicketNumber}
                    onChange={(e) => setJoinTicketNumber(e.target.value)}
                    placeholder="HD-1001"
                    className="w-full px-3 py-2 text-sm font-mono rounded-md border border-border bg-surface text-text placeholder-text-dim focus:outline-none focus:ring-1 focus:ring-primary uppercase"
                    required
                  />
                </div>

                <div className="flex items-center justify-end gap-2 pt-3 border-t border-border-subtle">
                  <button
                    type="button"
                    onClick={() => setShowJoinModal(false)}
                    className="px-3 py-1.5 text-xs font-semibold rounded-md border border-border text-text-muted hover:text-text hover:bg-surface-hover transition-colors"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={isJoining}
                    className="px-4 py-1.5 text-xs font-semibold rounded-md bg-primary text-primary-text hover:bg-primary-hover disabled:opacity-50 transition-colors shadow-xs"
                  >
                    {isJoining ? 'Joining...' : 'Join Ticket'}
                  </button>
                </div>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </Shell>
  );
};

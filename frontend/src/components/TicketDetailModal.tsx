import React, { useState } from 'react';
import { motion } from 'motion/react';
import type { Role, TicketDetail, TicketStatus, User } from '../api/types';
import { api } from '../api/endpoints';
import { useAuth } from '../auth/AuthContext';
import { OverdueBadge, RequestedPriorityBadge, StatusBadge, UrgencyBadge } from './Badge';
import { MessageSquareIcon, XCircleIcon, ClockAlertIcon } from './Icons';
import { StatefulButton } from './ui/stateful-button';
import { Timeline } from './ui/timeline';

interface TicketDetailModalProps {
  ticket: TicketDetail;
  userRole?: Role;
  agents?: User[];
  onClose: () => void;
  onRefresh: () => void;
}

const STATUS_STEPS: { status: TicketStatus; label: string }[] = [
  { status: 'OPEN', label: 'Open' },
  { status: 'ASSIGNED', label: 'Assigned' },
  { status: 'IN_PROGRESS', label: 'In Progress' },
  { status: 'RESOLVED', label: 'Resolved' },
  { status: 'CLOSED', label: 'Closed' },
];

export const TicketDetailModal: React.FC<TicketDetailModalProps> = ({
  ticket,
  userRole,
  agents = [],
  onClose,
  onRefresh,
}) => {
  const { showToast } = useAuth();
  const [commentText, setCommentText] = useState('');
  const [isSubmittingComment, setIsSubmittingComment] = useState(false);
  const [isUpdatingStatus, setIsUpdatingStatus] = useState(false);
  const [selectedAgentId, setSelectedAgentId] = useState<number | ''>('');

  const isPrivileged = userRole === 'ADMIN' || userRole === 'AGENT';
  const showReasons = isPrivileged && ticket.reasons && ticket.reasons.length > 0;

  const handleAddComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!commentText.trim()) return;

    setIsSubmittingComment(true);
    try {
      await api.tickets.addComment(ticket.ticketId, commentText.trim());
      setCommentText('');
      showToast('Comment posted successfully', 'success');
      onRefresh();
    } catch {
      // Handled by global toast
    } finally {
      setIsSubmittingComment(false);
    }
  };

  const handleUpdateStatus = async (newStatus: string) => {
    setIsUpdatingStatus(true);
    try {
      await api.tickets.updateStatus(ticket.ticketId, newStatus);
      showToast(`Status updated to ${newStatus}`, 'success');
      onRefresh();
    } catch {
      // Error handled by global toast
    } finally {
      setIsUpdatingStatus(false);
    }
  };

  const handleAssignAgent = async () => {
    if (!selectedAgentId) return;
    setIsUpdatingStatus(true);
    try {
      await api.admin.assignTicket(ticket.ticketId, Number(selectedAgentId));
      showToast('Agent assigned successfully', 'success');
      onRefresh();
    } catch {
      // Error handled by global toast
    } finally {
      setIsUpdatingStatus(false);
    }
  };

  const handleCloseTicket = async () => {
    setIsUpdatingStatus(true);
    try {
      await api.admin.closeTicket(ticket.ticketId);
      showToast('Ticket marked as CLOSED', 'success');
      onRefresh();
    } catch {
      // Error handled by global toast
    } finally {
      setIsUpdatingStatus(false);
    }
  };

  const handleReopenTicket = async () => {
    setIsUpdatingStatus(true);
    try {
      await api.tickets.updateStatus(ticket.ticketId, 'IN_PROGRESS');
      showToast('Ticket reopened to IN_PROGRESS', 'success');
      onRefresh();
    } catch {
      // Error handled by global toast
    } finally {
      setIsUpdatingStatus(false);
    }
  };

  const currentStatusIndex = STATUS_STEPS.findIndex((s) => s.status === ticket.status);

  // Map comments to OriginKit timeline items
  const timelineItems = (ticket.comments || []).map((c) => ({
    id: c.commentId,
    author: c.authorName,
    time: new Date(c.createdAt).toLocaleString(),
    content: c.commentText,
    isStaff: ticket.assignedAgentName ? c.authorName === ticket.assignedAgentName : false,
  }));

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
      {/* Backdrop */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        transition={{ duration: 0.15 }}
        onClick={onClose}
        className="fixed inset-0 bg-black/60 backdrop-blur-xs"
      />

      {/* Modal Dialog (Glass panel blur permitted on modals) */}
      <motion.div
        initial={{ opacity: 0, scale: 0.96, y: 12 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.96, y: 12 }}
        transition={{ type: 'spring', damping: 25, stiffness: 350 }}
        className="relative w-full max-w-2xl max-h-[90vh] glass-panel rounded-2xl shadow-2xl flex flex-col overflow-hidden border border-border"
        role="dialog"
        aria-modal="true"
        aria-label={`Ticket ${ticket.ticketNumber} Details`}
      >
        {/* Header */}
        <div className="flex items-center justify-between p-4 sm:p-5 border-b border-border bg-surface/50">
          <div className="flex items-center gap-2.5 flex-wrap">
            <span className="font-mono text-xs font-bold text-text-muted">
              {ticket.ticketNumber}
            </span>
            <span className="text-xs text-text-dim">•</span>
            <StatusBadge status={ticket.status} />
            {ticket.overdue && <OverdueBadge />}
            <UrgencyBadge level={ticket.urgencyLevel} score={ticket.urgencyScore} />
          </div>

          <button
            type="button"
            onClick={onClose}
            className="text-text-muted hover:text-text p-1.5 rounded-lg hover:bg-surface-hover transition-colors focus-ring"
            aria-label="Close modal"
          >
            <XCircleIcon className="w-5 h-5" />
          </button>
        </div>

        {/* Content body */}
        <div className="p-4 sm:p-6 overflow-y-auto flex-1 space-y-4">
          {/* Status Progression Track with Spring Dot */}
          <div className="bento-card p-4 rounded-xl border border-border bg-surface-muted/40">
            <div className="text-[11px] font-bold text-text-muted mb-3 uppercase tracking-wider">
              Status Progression
            </div>
            <div className="relative flex items-center justify-between px-2">
              <div className="absolute left-6 right-6 top-3.5 -translate-y-1/2 h-1 bg-border rounded-full" />
              {STATUS_STEPS.map((step, idx) => {
                const isCurrent = ticket.status === step.status;
                const isPast = currentStatusIndex >= idx;

                return (
                  <div key={step.status} className="relative z-10 flex flex-col items-center">
                    <div
                      className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold transition-colors duration-fast ${
                        isCurrent
                          ? 'bg-primary text-primary-text ring-4 ring-accent-ring shadow-sm'
                          : isPast
                          ? 'bg-secondary text-secondary-text border border-border'
                          : 'bg-surface border border-border text-text-dim'
                      }`}
                    >
                      {isCurrent ? (
                        <motion.div
                          layoutId="status-track-dot"
                          transition={{ type: 'spring', stiffness: 350, damping: 25 }}
                          className="w-2.5 h-2.5 rounded-full bg-surface"
                        />
                      ) : (
                        <span>{idx + 1}</span>
                      )}
                    </div>
                    <span
                      className={`text-[11px] mt-1.5 font-medium whitespace-nowrap ${
                        isCurrent
                          ? 'text-primary font-bold'
                          : isPast
                          ? 'text-text'
                          : 'text-text-dim'
                      }`}
                    >
                      {step.label}
                    </span>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Ticket Title & Metadata */}
          <div>
            <h2 className="text-lg sm:text-xl font-bold text-text tracking-tight">{ticket.title}</h2>
            <div className="flex items-center gap-3 text-xs text-text-muted mt-2 flex-wrap">
              <span>Category: <strong className="text-text">{ticket.categoryName}</strong></span>
              <span>•</span>
              <RequestedPriorityBadge priority={ticket.requestedPriority || ticket.priority || 'LOW'} />
              <span>•</span>
              <span>Creator: <strong className="text-text">{ticket.createdByName || ticket.creatorName || 'Student'}</strong></span>
              <span>•</span>
              <span>Participants: <strong className="text-text">{ticket.participantCount || 1}</strong></span>
            </div>
            {ticket.assignedAgentName && (
              <div className="text-xs text-text-muted mt-1.5">
                Assigned Agent: <span className="font-semibold text-text">{ticket.assignedAgentName}</span>
              </div>
            )}
            {ticket.incidentTitle && (
              <div className="text-xs text-text-muted mt-1">
                Linked Incident: <span className="font-semibold text-danger">{ticket.incidentTitle}</span>
              </div>
            )}
            {ticket.dueAt && (
              <div className="text-xs text-text-muted mt-1 flex items-center gap-1.5">
                <ClockAlertIcon className="w-3.5 h-3.5 text-text-dim" />
                <span>Due: <strong className="text-text">{new Date(ticket.dueAt).toLocaleString()}</strong></span>
              </div>
            )}
          </div>

          {/* Description Tile */}
          <div className="bento-card p-4 rounded-xl border border-border bg-surface-muted/30">
            <h4 className="text-[11px] font-bold text-text-muted uppercase tracking-wider mb-1.5">
              Description
            </h4>
            <p className="text-xs sm:text-sm text-text whitespace-pre-wrap leading-relaxed">
              {ticket.description}
            </p>
          </div>

          {/* Urgency Reasons Tile (Dedicated Bento Tile for Privileged Roles) */}
          {showReasons && (
            <div className="bento-card p-4 rounded-xl border border-amber-300/40 bg-amber-50/30 dark:bg-amber-950/20">
              <div className="flex items-center justify-between gap-2 mb-2">
                <h4 className="text-xs font-bold text-text uppercase tracking-wider flex items-center gap-1.5">
                  <ClockAlertIcon className="w-4 h-4 text-amber-600" />
                  <span>Urgency Analysis & SLA Factors</span>
                </h4>
                <UrgencyBadge level={ticket.urgencyLevel} score={ticket.urgencyScore} />
              </div>
              <ul className="text-xs text-text-muted space-y-1 list-disc list-inside mt-2">
                {ticket.reasons!.map((reason, idx) => (
                  <li key={idx} className="leading-relaxed">
                    {reason}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {/* Agent: only the valid next status buttons */}
          {userRole === 'AGENT' && (
            <div className="p-4 rounded-xl border border-border bg-surface-muted/50 flex flex-wrap items-center gap-2">
              <span className="text-xs font-bold text-text-muted">Agent Actions:</span>
              {ticket.status === 'ASSIGNED' && (
                <button
                  type="button"
                  disabled={isUpdatingStatus}
                  onClick={() => handleUpdateStatus('IN_PROGRESS')}
                  className="px-3.5 py-1.5 text-xs font-semibold rounded-xl bg-primary text-primary-text hover:bg-primary-hover disabled:opacity-50 transition-colors shadow-xs focus-ring"
                >
                  Start Work (IN_PROGRESS)
                </button>
              )}
              {ticket.status === 'IN_PROGRESS' && (
                <button
                  type="button"
                  disabled={isUpdatingStatus}
                  onClick={() => handleUpdateStatus('RESOLVED')}
                  className="px-3.5 py-1.5 text-xs font-semibold rounded-xl bg-success text-white hover:opacity-90 disabled:opacity-50 transition-colors shadow-xs focus-ring"
                >
                  Mark as RESOLVED
                </button>
              )}
              {ticket.status !== 'ASSIGNED' && ticket.status !== 'IN_PROGRESS' && (
                <span className="text-xs text-text-muted italic">
                  No status transitions available for status {ticket.status}
                </span>
              )}
            </div>
          )}

          {/* Admin actions: Assign agent dropdown, Close/Reopen (for RESOLVED tickets) */}
          {userRole === 'ADMIN' && (
            <div className="p-4 rounded-xl border border-border bg-surface-muted/50 space-y-3">
              <span className="text-xs font-bold text-text-muted block">Admin Actions:</span>

              {/* Assign Agent */}
              <div className="flex items-center gap-2 flex-wrap">
                <select
                  value={selectedAgentId}
                  onChange={(e) => setSelectedAgentId(e.target.value ? Number(e.target.value) : '')}
                  className="px-3 py-1.5 text-xs rounded-xl border border-border bg-surface text-text focus-ring"
                >
                  <option value="">Select Agent to Assign...</option>
                  {agents.map((ag) => (
                    <option key={ag.userId} value={ag.userId}>
                      {ag.name} ({ag.email})
                    </option>
                  ))}
                </select>
                <StatefulButton
                  type="button"
                  disabled={!selectedAgentId || isUpdatingStatus}
                  onClick={handleAssignAgent}
                  loadingText="Assigning..."
                  successText="Assigned!"
                >
                  Assign
                </StatefulButton>
              </div>

              {/* Close/Reopen for RESOLVED tickets */}
              {ticket.status === 'RESOLVED' && (
                <div className="flex items-center gap-2 pt-2 border-t border-border-subtle">
                  <StatefulButton
                    type="button"
                    disabled={isUpdatingStatus}
                    onClick={handleCloseTicket}
                    className="bg-danger hover:bg-danger-hover text-white"
                    loadingText="Closing..."
                    successText="Closed!"
                  >
                    Close Ticket
                  </StatefulButton>
                  <StatefulButton
                    type="button"
                    disabled={isUpdatingStatus}
                    onClick={handleReopenTicket}
                    className="border border-border bg-surface text-text hover:bg-surface-hover"
                    loadingText="Reopening..."
                    successText="Reopened!"
                  >
                    Reopen Ticket (IN_PROGRESS)
                  </StatefulButton>
                </div>
              )}
            </div>
          )}

          {/* OriginKit Comments Timeline */}
          <div className="pt-2">
            <h4 className="text-sm font-bold text-text flex items-center gap-1.5 mb-4">
              <MessageSquareIcon className="w-4 h-4 text-text-muted" />
              <span>Comment Timeline ({ticket.comments?.length || 0})</span>
            </h4>

            <Timeline
              items={timelineItems}
              emptyMessage="No comments posted on this ticket yet."
              className="mb-4"
            />

            {/* Comment Form (Cannot add comments to CLOSED tickets) */}
            {ticket.status !== 'CLOSED' ? (
              <form onSubmit={handleAddComment} className="space-y-2.5 mt-4">
                <textarea
                  rows={2}
                  value={commentText}
                  onChange={(e) => setCommentText(e.target.value)}
                  placeholder="Write a comment or update..."
                  className="w-full px-3.5 py-2.5 text-xs sm:text-sm rounded-xl border border-border bg-surface text-text placeholder-text-dim focus-ring"
                  required
                />
                <div className="flex justify-end">
                  <StatefulButton
                    type="submit"
                    disabled={isSubmittingComment || !commentText.trim()}
                    loadingText="Posting..."
                    successText="Posted!"
                  >
                    Post Comment
                  </StatefulButton>
                </div>
              </form>
            ) : (
              <p className="text-xs text-text-dim italic mt-2">
                This ticket is CLOSED. New comments cannot be added.
              </p>
            )}
          </div>
        </div>
      </motion.div>
    </div>
  );
};

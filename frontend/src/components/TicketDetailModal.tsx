import React, { useState } from 'react';
import { motion } from 'motion/react';
import type { Role, TicketDetail, TicketStatus, User } from '../api/types';
import { api } from '../api/endpoints';
import { useAuth } from '../auth/AuthContext';
import { OverdueBadge, RequestedPriorityBadge, StatusBadge, UrgencyBadge } from './Badge';
import { MessageSquareIcon, XCircleIcon } from './Icons';
import { StatefulButton } from './ui/stateful-button';

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
      await api.admin.reopenTicket(ticket.ticketId);
      showToast('Ticket reopened to IN_PROGRESS', 'success');
      onRefresh();
    } catch {
      // Error handled by global toast
    } finally {
      setIsUpdatingStatus(false);
    }
  };

  const currentStatusIndex = STATUS_STEPS.findIndex((s) => s.status === ticket.status);

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-overlay overflow-y-auto"
    >
      <motion.div
        initial={{ opacity: 0, scale: 0.96 }}
        animate={{ opacity: 1, scale: 1 }}
        exit={{ opacity: 0, scale: 0.96 }}
        transition={{ duration: 0.2, ease: 'easeOut' }}
        className="bg-surface border border-border rounded-xl max-w-2xl w-full max-h-[90vh] flex flex-col shadow-lg overflow-hidden my-8"
      >
        {/* Header */}
        <div className="flex items-center justify-between p-4 sm:p-5 border-b border-border">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="font-mono text-sm font-bold text-text-muted">
              {ticket.ticketNumber}
            </span>
            <StatusBadge status={ticket.status} />
            {ticket.overdue && <OverdueBadge />}
            <UrgencyBadge level={ticket.urgencyLevel} score={ticket.urgencyScore} />
          </div>
          <button
            type="button"
            onClick={onClose}
            className="text-text-muted hover:text-text p-1.5 rounded-lg hover:bg-surface-hover transition-colors"
            aria-label="Close modal"
          >
            <XCircleIcon className="w-5 h-5" />
          </button>
        </div>

        {/* Content body */}
        <div className="p-4 sm:p-5 overflow-y-auto flex-1 space-y-4">
          {/* Status Progression Track with Spring Dot */}
          <div className="bg-surface-muted p-4 rounded-xl border border-border-subtle">
            <div className="text-xs font-semibold text-text-muted mb-3 uppercase tracking-wider">
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

          <div>
            <h2 className="text-lg font-bold text-text">{ticket.title}</h2>
            <div className="flex items-center gap-3 text-xs text-text-muted mt-1.5 flex-wrap">
              <span>Category: {ticket.categoryName}</span>
              <span>•</span>
              <RequestedPriorityBadge priority={ticket.requestedPriority || ticket.priority || 'LOW'} />
              <span>•</span>
              <span>Creator: {ticket.createdByName || ticket.creatorName || 'Student'}</span>
              <span>•</span>
              <span>Students: {ticket.participantCount || 1}</span>
            </div>
            {ticket.assignedAgentName && (
              <div className="text-xs text-text-muted mt-1">
                Assigned Agent: <span className="font-medium text-text">{ticket.assignedAgentName}</span>
              </div>
            )}
            {ticket.incidentTitle && (
              <div className="text-xs text-text-muted mt-1">
                Linked Incident: <span className="font-medium text-text">{ticket.incidentTitle}</span>
              </div>
            )}
            {ticket.dueAt && (
              <div className="text-xs text-text-muted mt-1">
                Due: <span className="font-medium text-text">{new Date(ticket.dueAt).toLocaleString()}</span>
              </div>
            )}
          </div>

          <div className="bg-surface-muted p-4 rounded-xl border border-border-subtle">
            <h4 className="text-xs font-semibold text-text-muted uppercase tracking-wider mb-1">
              Description
            </h4>
            <p className="text-sm text-text whitespace-pre-wrap leading-relaxed">{ticket.description}</p>
          </div>

          {/* Show urgency reasons to ADMIN and AGENT only */}
          {showReasons && (
            <div className="bg-surface-muted p-4 rounded-xl border border-border-subtle">
              <h4 className="text-xs font-semibold text-text-muted uppercase tracking-wider mb-1">
                System Urgency Reasons (Admin/Agent Only)
              </h4>
              <ul className="text-xs text-text-muted space-y-1 list-disc list-inside">
                {ticket.reasons!.map((reason, idx) => (
                  <li key={idx}>{reason}</li>
                ))}
              </ul>
            </div>
          )}

          {/* Role actions */}
          {/* Agent: only the valid next status buttons */}
          {userRole === 'AGENT' && (
            <div className="p-4 rounded-xl border border-border bg-surface-muted flex flex-wrap items-center gap-2">
              <span className="text-xs font-semibold text-text-muted">Agent Actions:</span>
              {ticket.status === 'ASSIGNED' && (
                <button
                  type="button"
                  disabled={isUpdatingStatus}
                  onClick={() => handleUpdateStatus('IN_PROGRESS')}
                  className="px-3.5 py-1.5 text-xs font-semibold rounded-lg bg-primary text-primary-text hover:bg-primary-hover disabled:opacity-50 transition-colors shadow-sm"
                >
                  Start Work (IN_PROGRESS)
                </button>
              )}
              {ticket.status === 'IN_PROGRESS' && (
                <button
                  type="button"
                  disabled={isUpdatingStatus}
                  onClick={() => handleUpdateStatus('RESOLVED')}
                  className="px-3.5 py-1.5 text-xs font-semibold rounded-lg bg-success text-white hover:opacity-90 disabled:opacity-50 transition-colors shadow-sm"
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
            <div className="p-4 rounded-xl border border-border bg-surface-muted space-y-3">
              <span className="text-xs font-semibold text-text-muted block">Admin Actions:</span>

              {/* Assign Agent */}
              <div className="flex items-center gap-2 flex-wrap">
                <select
                  value={selectedAgentId}
                  onChange={(e) => setSelectedAgentId(e.target.value ? Number(e.target.value) : '')}
                  className="px-3 py-1.5 text-xs rounded-lg border border-border bg-surface text-text focus:outline-none focus:ring-1 focus:ring-accent"
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

          {/* Comments Section */}
          <div className="pt-2">
            <h4 className="text-sm font-bold text-text flex items-center gap-1.5 mb-3">
              <MessageSquareIcon className="w-4 h-4 text-text-muted" />
              Comments ({ticket.comments?.length || 0})
            </h4>

            {ticket.comments && ticket.comments.length > 0 ? (
              <div className="space-y-2.5 mb-4">
                {ticket.comments.map((c) => (
                  <div
                    key={c.commentId}
                    className="p-3.5 bg-surface-muted rounded-xl border border-border-subtle"
                  >
                    <div className="flex items-center justify-between text-xs text-text-muted mb-1">
                      <span className="font-semibold text-text">{c.authorName}</span>
                      <span>{new Date(c.createdAt).toLocaleString()}</span>
                    </div>
                    <p className="text-sm text-text whitespace-pre-wrap leading-relaxed">{c.commentText}</p>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-xs text-text-muted mb-4 italic">No comments yet.</p>
            )}

            {/* Comment Form (Cannot add comments to CLOSED tickets) */}
            {ticket.status !== 'CLOSED' ? (
              <form onSubmit={handleAddComment} className="space-y-2.5">
                <textarea
                  rows={2}
                  value={commentText}
                  onChange={(e) => setCommentText(e.target.value)}
                  placeholder="Write a comment..."
                  className="w-full px-3.5 py-2.5 text-sm rounded-lg border border-border bg-surface text-text placeholder-text-dim focus:outline-none focus:ring-2 focus:ring-accent"
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
              <p className="text-xs text-text-dim italic">
                This ticket is CLOSED. New comments cannot be added.
              </p>
            )}
          </div>
        </div>
      </motion.div>
    </div>
  );
};

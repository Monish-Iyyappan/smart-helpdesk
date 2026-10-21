import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import type { AssignmentRecord, Role, TicketDetail, TicketStatus, User } from '../api/types';
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
  const [isIssue, setIsIssue] = useState(false);
  const [isSubmittingComment, setIsSubmittingComment] = useState(false);
  const [isUpdatingStatus, setIsUpdatingStatus] = useState(false);
  const [selectedAgentId, setSelectedAgentId] = useState<number | ''>('');

  // Reassign Dialog State
  const [showReassignModal, setShowReassignModal] = useState(false);
  const [reassignReason, setReassignReason] = useState('');
  const [isSubmittingReassign, setIsSubmittingReassign] = useState(false);

  // Close Dialog State
  const [showCloseModal, setShowCloseModal] = useState(false);
  const [closeNote, setCloseNote] = useState('');
  const [isSubmittingClose, setIsSubmittingClose] = useState(false);

  // Assignment History Panel State
  const [showHistory, setShowHistory] = useState(false);
  const [history, setHistory] = useState<AssignmentRecord[]>([]);
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);

  const isPrivileged = userRole === 'ADMIN' || userRole === 'AGENT';
  const showReasons = isPrivileged && ticket.reasons && ticket.reasons.length > 0;
  const isOverdue = Boolean(ticket.overdue && ticket.status !== 'RESOLVED' && ticket.status !== 'CLOSED');

  const currentAgentName = ticket.assignedAgentName || (ticket.assignedTo ? `Agent ${ticket.assignedTo}` : null);
  const hasCurrentAgent = Boolean(currentAgentName);
  const isResolvedOrClosed = ticket.status === 'RESOLVED' || ticket.status === 'CLOSED';

  const selectedAgent = agents.find((a) => a.userId === Number(selectedAgentId));

  useEffect(() => {
    if (userRole === 'ADMIN') {
      setIsLoadingHistory(true);
      api.admin
        .getAssignmentHistory(ticket.ticketId)
        .then((list) => setHistory(list || []))
        .catch(() => setHistory([]))
        .finally(() => setIsLoadingHistory(false));
    }
  }, [ticket.ticketId, userRole]);

  const handleAddComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!commentText.trim()) return;

    setIsSubmittingComment(true);
    try {
      const typeToSend = userRole === 'STUDENT' && isIssue ? 'ISSUE' : 'GENERAL';
      await api.tickets.addComment(ticket.ticketId, commentText.trim(), typeToSend);
      setCommentText('');
      setIsIssue(false);
      showToast(isIssue ? 'Issue note posted successfully' : 'Comment posted successfully', 'success');
      onRefresh();
    } catch (err: any) {
      showToast(err?.message || 'Failed to post comment', 'error');
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
    } catch (err: any) {
      showToast(err?.message || 'Failed to update status', 'error');
    } finally {
      setIsUpdatingStatus(false);
    }
  };

  const handleAssignClick = () => {
    if (!selectedAgentId) return;
    if (hasCurrentAgent) {
      setReassignReason('');
      setShowReassignModal(true);
    } else {
      handlePlainAssign();
    }
  };

  const handlePlainAssign = async () => {
    if (!selectedAgentId || isUpdatingStatus) return;
    setIsUpdatingStatus(true);
    try {
      await api.admin.assignTicket(ticket.ticketId, Number(selectedAgentId));
      showToast('Agent assigned successfully', 'success');
      setSelectedAgentId('');
      onRefresh();
    } catch (err: any) {
      showToast(err?.message || 'Failed to assign ticket', 'error');
    } finally {
      setIsUpdatingStatus(false);
    }
  };

  const handleConfirmReassign = async () => {
    const trimmed = reassignReason.trim();
    if (!selectedAgentId || trimmed.length < 10 || trimmed.length > 500 || isSubmittingReassign) return;

    setIsSubmittingReassign(true);
    try {
      await api.admin.assignTicket(ticket.ticketId, Number(selectedAgentId), trimmed);
      showToast('Ticket reassigned successfully', 'success');
      setShowReassignModal(false);
      setReassignReason('');
      setSelectedAgentId('');
      onRefresh();
    } catch (err: any) {
      showToast(err?.message || 'Failed to reassign ticket', 'error');
    } finally {
      setIsSubmittingReassign(false);
    }
  };

  const handleConfirmClose = async () => {
    const trimmed = closeNote.trim();
    if (trimmed.length < 10 || trimmed.length > 500 || isSubmittingClose) return;

    setIsSubmittingClose(true);
    try {
      await api.admin.closeTicket(ticket.ticketId, trimmed);
      showToast('Ticket closed successfully', 'success');
      setShowCloseModal(false);
      setCloseNote('');
      onRefresh();
    } catch (err: any) {
      showToast(err?.message || 'Failed to close ticket', 'error');
    } finally {
      setIsSubmittingClose(false);
    }
  };

  const handleReopenTicket = async () => {
    setIsUpdatingStatus(true);
    try {
      await api.tickets.updateStatus(ticket.ticketId, 'IN_PROGRESS');
      showToast('Ticket reopened to IN_PROGRESS', 'success');
      onRefresh();
    } catch (err: any) {
      showToast(err?.message || 'Failed to reopen ticket', 'error');
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
    commentType: c.commentType,
  }));

  const reassignTrimmedLen = reassignReason.trim().length;
  const isReassignValid = reassignTrimmedLen >= 10 && reassignTrimmedLen <= 500;

  const closeTrimmedLen = closeNote.trim().length;
  const isCloseValid = closeTrimmedLen >= 10 && closeTrimmedLen <= 500;

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

      {/* Modal Dialog */}
      <motion.div
        initial={{ opacity: 0, scale: 0.96, y: 12 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.96, y: 12 }}
        transition={{ type: 'spring', damping: 25, stiffness: 350 }}
        className={`relative w-full max-w-2xl max-h-[90vh] glass-panel rounded-2xl shadow-2xl flex flex-col overflow-hidden border ${
          isOverdue
            ? 'ticket-card-overdue border-overdue-border shadow-[0_0_24px_-4px_var(--color-overdue-glow)]'
            : 'border-border'
        }`}
        role="dialog"
        aria-modal="true"
        aria-label={`Ticket ${ticket.ticketNumber} Details`}
      >
        {/* Header */}
        <div
          className={`flex items-center justify-between p-4 sm:p-5 border-b ${
            isOverdue
              ? 'border-overdue-border/50 bg-overdue-bg/40'
              : 'border-border bg-surface/50'
          }`}
        >
          <div className="flex items-center gap-2.5 flex-wrap">
            <span className="font-mono text-xs font-bold text-text-muted">
              {ticket.ticketNumber}
            </span>
            <span className="text-xs text-text-dim">•</span>
            <StatusBadge status={ticket.status} />
            {isOverdue && <OverdueBadge />}
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
          {/* Overdue Alert Banner if overdue */}
          {isOverdue && (
            <div className="p-3.5 rounded-xl border border-overdue-border bg-overdue-bg/60 text-overdue-text flex items-center gap-2.5 text-xs font-semibold">
              <ClockAlertIcon className="w-4 h-4 text-overdue-text shrink-0" />
              <div className="flex-1">
                <span>This ticket is currently OVERDUE for SLA resolution.</span>
                {ticket.dueAt && (
                  <span className="block sm:inline sm:ml-1 font-normal opacity-90">
                    SLA deadline was {new Date(ticket.dueAt).toLocaleString()}.
                  </span>
                )}
              </div>
            </div>
          )}

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
            {/* Current Assignment Indicator */}
            <div className="text-xs text-text-muted mt-2 flex items-center gap-1.5 flex-wrap">
              <span>Currently assigned to:</span>
              <span className="font-semibold text-text">
                {currentAgentName || 'Unassigned'}
              </span>
            </div>
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

          {/* Urgency Reasons Tile */}
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

          {/* Agent: valid next status buttons */}
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

          {/* Admin actions: Assign/Reassign, Close/Reopen */}
          {userRole === 'ADMIN' && (
            <div className="p-4 rounded-xl border border-border bg-surface-muted/50 space-y-3">
              <span className="text-xs font-bold text-text-muted block">Admin Actions:</span>

              {/* Assign/Reassign Section */}
              {isResolvedOrClosed ? (
                <div className="text-xs text-text-muted italic p-2.5 rounded-lg bg-surface/50 border border-border-subtle">
                  Ticket is <span className="font-semibold text-text">{ticket.status}</span>. Reopen first to assign or reassign.
                </div>
              ) : (
                <div className="flex items-center gap-2 flex-wrap">
                  <select
                    value={selectedAgentId}
                    onChange={(e) => setSelectedAgentId(e.target.value ? Number(e.target.value) : '')}
                    className="px-3 py-1.5 text-xs rounded-xl border border-border bg-surface text-text focus-ring"
                  >
                    <option value="">
                      {hasCurrentAgent ? 'Select New Agent to Reassign...' : 'Select Agent to Assign...'}
                    </option>
                    {agents.map((ag) => {
                      const isCurrent =
                        (ticket.assignedTo && ag.userId === ticket.assignedTo) ||
                        (ticket.assignedAgentName && ag.name === ticket.assignedAgentName);
                      return (
                        <option key={ag.userId} value={ag.userId} disabled={Boolean(isCurrent)}>
                          {ag.name} ({ag.email}){isCurrent ? ' - (current)' : ''}
                        </option>
                      );
                    })}
                  </select>

                  <button
                    type="button"
                    disabled={!selectedAgentId || isUpdatingStatus}
                    onClick={handleAssignClick}
                    className="px-3.5 py-1.5 text-xs font-semibold rounded-xl bg-primary text-primary-text hover:bg-primary-hover disabled:opacity-50 transition-colors shadow-xs focus-ring"
                  >
                    {hasCurrentAgent ? 'Reassign' : 'Assign'}
                  </button>
                </div>
              )}

              {/* Close/Reopen for RESOLVED tickets */}
              {ticket.status === 'RESOLVED' && (
                <div className="flex items-center gap-2 pt-2 border-t border-border-subtle">
                  <button
                    type="button"
                    disabled={isUpdatingStatus}
                    onClick={() => {
                      setCloseNote('');
                      setShowCloseModal(true);
                    }}
                    className="px-3.5 py-1.5 text-xs font-semibold rounded-xl bg-danger hover:bg-danger-hover text-white transition-colors shadow-xs focus-ring"
                  >
                    Close Ticket
                  </button>
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

          {/* Admin: Collapsible Assignment History Panel */}
          {userRole === 'ADMIN' && (
            <div className="bento-card p-4 rounded-xl border border-border bg-surface-muted/30">
              <button
                type="button"
                onClick={() => setShowHistory((prev) => !prev)}
                className="w-full flex items-center justify-between text-xs font-bold text-text hover:text-primary transition-colors focus-ring rounded-lg p-1"
                aria-expanded={showHistory}
              >
                <span className="flex items-center gap-2 uppercase tracking-wider">
                  <span>Assignment History</span>
                  <span className="px-2 py-0.5 text-[10px] rounded-full bg-secondary text-secondary-text font-bold">
                    {history.length}
                  </span>
                </span>
                <span className="text-xs text-text-muted">{showHistory ? 'Hide ▲' : 'Show ▼'}</span>
              </button>

              <AnimatePresence>
                {showHistory && (
                  <motion.div
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: 'auto' }}
                    exit={{ opacity: 0, height: 0 }}
                    transition={{ duration: 0.2 }}
                    className="overflow-hidden"
                  >
                    <div className="pt-3 mt-3 border-t border-border-subtle space-y-2.5">
                      {isLoadingHistory ? (
                        <p className="text-xs text-text-muted italic">Loading assignment history...</p>
                      ) : history.length === 0 ? (
                        <p className="text-xs text-text-muted italic">No assignment history recorded yet.</p>
                      ) : (
                        history.map((h) => (
                          <div
                            key={h.historyId}
                            className="p-3 rounded-lg border border-border bg-surface text-xs space-y-1 shadow-2xs"
                          >
                            <div className="flex items-center justify-between gap-2 flex-wrap">
                              <span
                                className={`font-semibold px-2 py-0.5 rounded text-[10px] uppercase tracking-wider ${
                                  h.assignmentType === 'ASSIGN'
                                    ? 'bg-secondary text-secondary-text'
                                    : 'bg-indigo-100 text-indigo-900 border border-indigo-200 dark:bg-indigo-950/40 dark:text-indigo-200 dark:border-indigo-700/50'
                                }`}
                              >
                                {h.assignmentType === 'ASSIGN' ? 'Initial Assignment' : 'Reassignment'}
                              </span>
                              <time className="text-[10px] text-text-dim">
                                {new Date(h.changedAt).toLocaleString()}
                              </time>
                            </div>
                            <div className="text-text-muted mt-1">
                              {h.fromAgentName ? (
                                <>
                                  From <strong className="text-text">{h.fromAgentName}</strong> to{' '}
                                  <strong className="text-text">{h.toAgentName}</strong>
                                </>
                              ) : (
                                <>
                                  Assigned to <strong className="text-text">{h.toAgentName}</strong>
                                </>
                              )}
                              <span className="text-text-dim ml-2 font-normal">by {h.changedByName}</span>
                            </div>
                            {h.reason && (
                              <div className="text-text text-[11px] bg-surface-muted/60 p-2 rounded border border-border-subtle mt-1.5 italic">
                                &ldquo;{h.reason}&rdquo;
                              </div>
                            )}
                          </div>
                        ))
                      )}
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>
            </div>
          )}

          {/* Comments Timeline */}
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

                <div className="flex items-center justify-between gap-3 flex-wrap">
                  {/* Student "Mark as issue" checkbox */}
                  {userRole === 'STUDENT' ? (
                    <label className="inline-flex items-center gap-2 text-xs text-text-muted cursor-pointer hover:text-text select-none">
                      <input
                        type="checkbox"
                        checked={isIssue}
                        onChange={(e) => setIsIssue(e.target.checked)}
                        className="w-4 h-4 rounded border-border text-primary focus-ring"
                      />
                      <span className="font-medium">Mark as issue</span>
                    </label>
                  ) : (
                    <div />
                  )}

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

      {/* Reassign Dialog */}
      <AnimatePresence>
        {showReassignModal && (
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="reassign-modal-title"
            className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs"
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.95, y: 10 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 10 }}
              transition={{ duration: 0.15 }}
              className="glass-panel border border-border rounded-2xl max-w-md w-full p-5 sm:p-6 shadow-2xl bg-surface"
            >
              <h3 id="reassign-modal-title" className="text-base font-bold text-text mb-3">
                Reassign Ticket
              </h3>

              <div className="text-xs text-text-muted space-y-1.5 mb-4 p-3 rounded-xl bg-surface-muted/50 border border-border-subtle">
                <div>
                  Current Agent:{' '}
                  <strong className="text-text">{currentAgentName || 'None'}</strong>
                </div>
                <div>
                  New Assignee:{' '}
                  <strong className="text-primary font-bold">
                    {selectedAgent ? `${selectedAgent.name} (${selectedAgent.email})` : 'Selected Agent'}
                  </strong>
                </div>
              </div>

              <div className="space-y-1.5 mb-4">
                <label htmlFor="reassign-reason" className="block text-xs font-semibold text-text">
                  Reassignment Reason <span className="text-danger">*</span>
                </label>
                <textarea
                  id="reassign-reason"
                  rows={3}
                  value={reassignReason}
                  onChange={(e) => setReassignReason(e.target.value)}
                  placeholder="Explain why this ticket is being reassigned (10-500 characters)..."
                  className="w-full px-3 py-2 text-xs sm:text-sm rounded-xl border border-border bg-surface text-text placeholder-text-dim focus-ring"
                  required
                />
                <div className="flex justify-between items-center text-[11px]">
                  <span
                    className={
                      isReassignValid
                        ? 'text-success font-semibold'
                        : 'text-text-dim font-medium'
                    }
                  >
                    {reassignTrimmedLen} / 500 characters
                    {reassignTrimmedLen < 10
                      ? ` (${10 - reassignTrimmedLen} more needed)`
                      : ''}
                  </span>
                </div>
              </div>

              <div className="flex items-center justify-end gap-2.5 pt-2 border-t border-border-subtle">
                <button
                  type="button"
                  onClick={() => setShowReassignModal(false)}
                  className="px-3.5 py-1.5 text-xs font-semibold rounded-xl border border-border bg-surface text-text hover:bg-surface-hover transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  disabled={!isReassignValid || isSubmittingReassign}
                  onClick={handleConfirmReassign}
                  className="px-4 py-1.5 text-xs font-semibold rounded-xl bg-primary text-primary-text hover:bg-primary-hover disabled:opacity-50 transition-colors shadow-xs focus-ring"
                >
                  {isSubmittingReassign ? 'Reassigning...' : 'Confirm Reassign'}
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Close Ticket Dialog */}
      <AnimatePresence>
        {showCloseModal && (
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="close-modal-title"
            className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs"
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.95, y: 10 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 10 }}
              transition={{ duration: 0.15 }}
              className="glass-panel border border-border rounded-2xl max-w-md w-full p-5 sm:p-6 shadow-2xl bg-surface"
            >
              <h3 id="close-modal-title" className="text-base font-bold text-text mb-2">
                Close Ticket
              </h3>
              <p className="text-xs text-text-muted mb-4">
                Provide a resolution and closure note for ticket <strong className="text-text">{ticket.ticketNumber}</strong>.
              </p>

              <div className="space-y-1.5 mb-4">
                <label htmlFor="closure-note" className="block text-xs font-semibold text-text">
                  Closure Note <span className="text-danger">*</span>
                </label>
                <textarea
                  id="closure-note"
                  rows={3}
                  value={closeNote}
                  onChange={(e) => setCloseNote(e.target.value)}
                  placeholder="Summarize the final resolution (10-500 characters)..."
                  className="w-full px-3 py-2 text-xs sm:text-sm rounded-xl border border-border bg-surface text-text placeholder-text-dim focus-ring"
                  required
                />
                <div className="flex justify-between items-center text-[11px]">
                  <span
                    className={
                      isCloseValid
                        ? 'text-success font-semibold'
                        : 'text-text-dim font-medium'
                    }
                  >
                    {closeTrimmedLen} / 500 characters
                    {closeTrimmedLen < 10 ? ` (${10 - closeTrimmedLen} more needed)` : ''}
                  </span>
                </div>
              </div>

              <div className="flex items-center justify-end gap-2.5 pt-2 border-t border-border-subtle">
                <button
                  type="button"
                  onClick={() => setShowCloseModal(false)}
                  className="px-3.5 py-1.5 text-xs font-semibold rounded-xl border border-border bg-surface text-text hover:bg-surface-hover transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  disabled={!isCloseValid || isSubmittingClose}
                  onClick={handleConfirmClose}
                  className="px-4 py-1.5 text-xs font-semibold rounded-xl bg-danger hover:bg-danger-hover text-white disabled:opacity-50 transition-colors shadow-xs focus-ring"
                >
                  {isSubmittingClose ? 'Closing...' : 'Close Ticket'}
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
};

import React from 'react';
import { motion } from 'motion/react';
import type { Role, TicketItem } from '../api/types';
import { OverdueBadge, RequestedPriorityBadge, StatusBadge, UrgencyBadge } from './Badge';
import { CardSpotlight } from './ui/card-spotlight';

interface TicketCardProps {
  ticket: TicketItem;
  userRole?: Role;
  onClick?: () => void;
  actionSlot?: React.ReactNode;
  layoutId?: string;
}

export const TicketCard: React.FC<TicketCardProps> = ({
  ticket,
  userRole,
  onClick,
  actionSlot,
  layoutId,
}) => {
  const isPrivileged = userRole === 'ADMIN' || userRole === 'AGENT';
  const showReasons = isPrivileged && ticket.reasons && ticket.reasons.length > 0;
  const isOverdue = Boolean(ticket.overdue && ticket.status !== 'RESOLVED' && ticket.status !== 'CLOSED');

  // Left urgency stripe color mapping
  const urgencyStripeColor = {
    URGENT: 'bg-danger',
    HIGH: 'bg-amber-600',
    MEDIUM: 'bg-amber-500',
    LOW: 'bg-slate-400',
  }[ticket.urgencyLevel || 'LOW'];

  return (
    <motion.div
      layout
      layoutId={layoutId}
      transition={{ type: 'spring', stiffness: 380, damping: 30 }}
      onClick={onClick}
      className={`relative rounded-2xl border transition-all duration-normal shadow-xs overflow-hidden ${
        isOverdue
          ? 'ticket-card-overdue animate-overdue-glow'
          : 'border-border hover:border-accent bg-surface'
      } ${onClick ? 'cursor-pointer hover:shadow-md' : ''}`}
    >
      {/* Left Urgency Stripe */}
      <div
        className={`absolute left-0 top-0 bottom-0 w-1.5 z-20 ${
          isOverdue ? 'bg-danger' : urgencyStripeColor
        }`}
        title={isOverdue ? 'Overdue for SLA resolution' : `Urgency: ${ticket.urgencyLevel}`}
      />

      <CardSpotlight
        color={isOverdue ? 'var(--color-overdue-glow)' : undefined}
        className="p-4 sm:p-5 pl-5 sm:pl-6 rounded-2xl"
      >
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 mb-2.5">
          <div className="flex items-center gap-2 flex-wrap min-w-0">
            <span className="font-mono text-xs font-bold text-text-muted shrink-0">
              {ticket.ticketNumber}
            </span>
            <span className="text-xs text-text-dim">•</span>
            <span
              className="text-xs font-semibold text-text-muted truncate max-w-[140px] sm:max-w-xs"
              title={ticket.categoryName}
            >
              {ticket.categoryName}
            </span>
            {/* Status chip with clear text label */}
            <StatusBadge status={ticket.status} />
          </div>

          <div className="flex items-center gap-2 flex-wrap shrink-0">
            {isOverdue && <OverdueBadge />}
            <UrgencyBadge level={ticket.urgencyLevel} score={ticket.urgencyScore} />
          </div>
        </div>

        <div className="mb-2">
          <h3
            className="text-base font-bold text-text line-clamp-1 tracking-tight"
            title={ticket.title}
          >
            {ticket.title}
          </h3>
          <p className="text-xs sm:text-sm text-text-muted line-clamp-2 mt-1 leading-relaxed">
            {ticket.description}
          </p>
        </div>

        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 pt-3 border-t border-border-subtle mt-3">
          <div className="flex items-center gap-3 flex-wrap">
            {/* Student's chosen priority shown strictly as small 'Requested: X' */}
            <RequestedPriorityBadge priority={ticket.requestedPriority || ticket.priority || 'LOW'} />
            {ticket.assignedAgentName && (
              <span className="text-[11px] font-medium text-text-muted">
                Agent: <span className="text-text font-semibold">{ticket.assignedAgentName}</span>
              </span>
            )}
            {ticket.createdAt && (
              <span className="text-[11px] text-text-dim">
                {new Date(ticket.createdAt).toLocaleDateString()}
              </span>
            )}
          </div>

          {actionSlot && <div className="flex items-center gap-2 shrink-0">{actionSlot}</div>}
        </div>

        {/* Show urgency reasons to ADMIN and AGENT only */}
        {showReasons && (
          <div
            className={`mt-3 pt-2 border-t p-2.5 rounded-xl border ${
              isOverdue
                ? 'border-overdue-border/40 bg-overdue-bg/50'
                : 'border-border-subtle bg-surface-muted/60 border-border'
            }`}
          >
            <div className="text-[11px] font-bold text-text-muted mb-1">
              Urgency Factors:
            </div>
            <ul className="text-xs text-text-muted space-y-0.5 list-disc list-inside">
              {ticket.reasons!.map((reason, idx) => (
                <li key={idx} className="truncate" title={reason}>
                  {reason}
                </li>
              ))}
            </ul>
          </div>
        )}
      </CardSpotlight>
    </motion.div>
  );
};

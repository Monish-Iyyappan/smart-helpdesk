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

  return (
    <motion.div
      layout
      layoutId={layoutId}
      transition={{ type: 'spring', stiffness: 380, damping: 30 }}
      onClick={onClick}
      className={`relative rounded-xl border transition-all duration-normal shadow-sm overflow-hidden ${
        ticket.overdue
          ? 'border-overdue-border animate-overdue-glow bg-surface'
          : 'border-border hover:border-accent bg-surface'
      } ${onClick ? 'cursor-pointer hover:bg-surface-hover hover:shadow-md' : ''}`}
    >
      <CardSpotlight className="p-4 rounded-xl">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 mb-2">
        <div className="flex items-center gap-2 flex-wrap">
          <span className="font-mono text-xs font-semibold text-text-muted">
            {ticket.ticketNumber}
          </span>
          <span className="text-xs text-text-dim">•</span>
          <span className="text-xs font-medium text-text-muted">
            {ticket.categoryName}
          </span>
          <StatusBadge status={ticket.status} />
        </div>

        <div className="flex items-center gap-2 flex-wrap">
          {ticket.overdue && <OverdueBadge />}
          <UrgencyBadge level={ticket.urgencyLevel} score={ticket.urgencyScore} />
        </div>
      </div>

      <div className="mb-2">
        <h3 className="text-base font-semibold text-text line-clamp-1">
          {ticket.title}
        </h3>
        <p className="text-sm text-text-muted line-clamp-2 mt-1 leading-relaxed">
          {ticket.description}
        </p>
      </div>

      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 pt-2 border-t border-border-subtle mt-3">
        <div className="flex items-center gap-3">
          {/* Student's chosen priority shown strictly as small 'Requested: X' */}
          <RequestedPriorityBadge priority={ticket.requestedPriority || ticket.priority || 'LOW'} />
          {ticket.createdAt && (
            <span className="text-xs text-text-dim">
              {new Date(ticket.createdAt).toLocaleDateString()}
            </span>
          )}
        </div>

        {actionSlot && <div className="flex items-center gap-2">{actionSlot}</div>}
      </div>

      {/* Show urgency reasons to ADMIN and AGENT only */}
      {showReasons && (
        <div className="mt-3 pt-2 border-t border-border-subtle bg-surface-muted p-2.5 rounded-lg">
          <div className="text-xs font-semibold text-text-muted mb-1">
            Urgency Factors:
          </div>
          <ul className="text-xs text-text-muted space-y-0.5 list-disc list-inside">
            {ticket.reasons!.map((reason, idx) => (
              <li key={idx} className="truncate">
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

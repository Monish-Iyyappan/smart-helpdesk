/**
 * OriginKit Timeline Component
 * Source: https://originkit.dev/components/timeline
 * License: MIT
 * Adapted for Signal Desk tokens and comments.
 */

import React from 'react';
import { CommentTypeBadge } from '../Badge';

export interface TimelineItem {
  id: string | number;
  author: string;
  role?: string;
  time: string;
  content: string;
  isStaff?: boolean;
  commentType?: string;
}

export interface TimelineProps {
  items: TimelineItem[];
  emptyMessage?: string;
  className?: string;
}

export const Timeline: React.FC<TimelineProps> = ({
  items,
  emptyMessage = 'No comments yet.',
  className = '',
}) => {
  if (!items || items.length === 0) {
    return (
      <div className="text-center py-6 text-xs text-text-muted border border-dashed border-border rounded-xl">
        {emptyMessage}
      </div>
    );
  }

  return (
    <div className={`relative pl-4 space-y-6 before:absolute before:left-2 before:top-2 before:bottom-2 before:w-0.5 before:bg-border ${className}`}>
      {items.map((item) => {
        const initials = item.author
          .split(' ')
          .map((n) => n[0])
          .slice(0, 2)
          .join('')
          .toUpperCase();

        const isIssue = item.commentType === 'ISSUE';
        const isReassignment = item.commentType === 'REASSIGNMENT';
        const isClosure = item.commentType === 'CLOSURE';

        const cardBorderBg = isIssue
          ? 'border-amber-400/80 bg-amber-50/60 dark:border-amber-500/40 dark:bg-amber-950/25'
          : isReassignment
          ? 'border-indigo-300/80 bg-indigo-50/60 dark:border-indigo-500/40 dark:bg-indigo-950/25'
          : isClosure
          ? 'border-rose-300/80 bg-rose-50/60 dark:border-rose-500/40 dark:bg-rose-950/25'
          : 'border-border bg-surface';

        return (
          <div key={item.id} className="relative flex items-start gap-3">
            {/* Dot / Avatar on Timeline */}
            <div
              className={`absolute -left-4 w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-bold ring-4 ring-surface ${
                isClosure
                  ? 'bg-rose-600 text-white'
                  : isIssue
                  ? 'bg-amber-600 text-white'
                  : isReassignment
                  ? 'bg-indigo-600 text-white'
                  : item.isStaff
                  ? 'bg-primary text-primary-text'
                  : 'bg-secondary text-secondary-text'
              }`}
            >
              {initials}
            </div>

            {/* Comment Body */}
            <div className={`ml-4 flex-1 p-3.5 rounded-xl border shadow-xs transition-colors ${cardBorderBg}`}>
              <div className="flex items-center justify-between gap-2 mb-1.5 flex-wrap">
                <div className="flex items-center gap-1.5 flex-wrap">
                  <span className="text-xs font-bold text-text">{item.author}</span>
                  {item.role && (
                    <span
                      className={`text-[9px] uppercase font-bold px-1.5 py-0.2 rounded ${
                        item.isStaff
                          ? 'bg-primary/10 text-primary border border-primary/20'
                          : 'bg-surface-muted text-text-dim border border-border'
                      }`}
                    >
                      {item.role}
                    </span>
                  )}
                  {item.commentType && <CommentTypeBadge type={item.commentType} />}
                </div>
                <time className="text-[10px] text-text-dim">{item.time}</time>
              </div>
              <p className="text-xs sm:text-sm text-text leading-relaxed whitespace-pre-wrap">
                {item.content}
              </p>
            </div>
          </div>
        );
      })}
    </div>
  );
};

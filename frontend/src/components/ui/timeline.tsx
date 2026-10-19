/**
 * OriginKit Timeline Component
 * Source: https://originkit.dev/components/timeline
 * License: MIT
 * Adapted for Signal Desk tokens and comments.
 */

import React from 'react';

export interface TimelineItem {
  id: string | number;
  author: string;
  role?: string;
  time: string;
  content: string;
  isStaff?: boolean;
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

        return (
          <div key={item.id} className="relative flex items-start gap-3">
            {/* Dot / Avatar on Timeline */}
            <div
              className={`absolute -left-4 w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-bold ring-4 ring-surface ${
                item.isStaff
                  ? 'bg-primary text-primary-text'
                  : 'bg-secondary text-secondary-text'
              }`}
            >
              {initials}
            </div>

            {/* Comment Body */}
            <div className="ml-4 flex-1 p-3.5 rounded-xl border border-border bg-surface shadow-xs">
              <div className="flex items-center justify-between gap-2 mb-1.5 flex-wrap">
                <div className="flex items-center gap-1.5">
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

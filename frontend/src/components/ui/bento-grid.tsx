/**
 * Aceternity UI Bento Grid Component
 * Source: https://ui.aceternity.com/components/bento-grid
 * License: MIT
 * Adapted for Signal Desk tokens, React 19, and motion/react.
 */

import React from 'react';

export interface BentoGridProps {
  className?: string;
  children?: React.ReactNode;
}

export const BentoGrid: React.FC<BentoGridProps> = ({
  className = '',
  children,
}) => {
  return (
    <div
      className={`grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 auto-rows-auto max-w-7xl mx-auto ${className}`}
    >
      {children}
    </div>
  );
};

export interface BentoGridItemProps {
  className?: string;
  title?: React.ReactNode;
  description?: React.ReactNode;
  header?: React.ReactNode;
  icon?: React.ReactNode;
  footer?: React.ReactNode;
  children?: React.ReactNode;
  colSpan?: 1 | 2 | 3 | 4;
}

export const BentoGridItem: React.FC<BentoGridItemProps> = ({
  className = '',
  title,
  description,
  header,
  icon,
  footer,
  children,
  colSpan = 1,
}) => {
  const colSpanClasses = {
    1: 'col-span-1',
    2: 'col-span-1 md:col-span-2',
    3: 'col-span-1 md:col-span-2 lg:col-span-3',
    4: 'col-span-1 md:col-span-2 lg:col-span-4',
  }[colSpan];

  return (
    <div
      className={`bento-card p-5 flex flex-col justify-between overflow-hidden shadow-xs hover:shadow-md ${colSpanClasses} ${className}`}
    >
      {header && <div className="mb-3">{header}</div>}
      {children ? (
        children
      ) : (
        <div className="flex-1 flex flex-col justify-between">
          <div className="flex items-start justify-between gap-3 mb-2">
            <div>
              {title && <h4 className="text-sm font-bold text-text tracking-tight">{title}</h4>}
              {description && (
                <p className="text-xs text-text-muted mt-0.5 line-clamp-2">{description}</p>
              )}
            </div>
            {icon && <div className="text-primary shrink-0">{icon}</div>}
          </div>
          {footer && <div className="mt-4 pt-3 border-t border-border">{footer}</div>}
        </div>
      )}
    </div>
  );
};

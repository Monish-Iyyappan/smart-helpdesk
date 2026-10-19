/**
 * OriginKit Skeleton Component
 * Source: https://originkit.dev/components/skeleton
 * License: MIT
 * Adapted for Signal Desk design tokens and reduced-motion support.
 */

import React from 'react';

export interface SkeletonProps extends React.HTMLAttributes<HTMLDivElement> {
  className?: string;
}

export const Skeleton: React.FC<SkeletonProps> = ({ className = '', ...props }) => {
  return (
    <div
      className={`animate-pulse rounded-md bg-[var(--color-skeleton)] ${className}`}
      aria-hidden="true"
      {...props}
    />
  );
};

export const CardSkeleton: React.FC<{ count?: number }> = ({ count = 3 }) => {
  return (
    <div className="space-y-3" role="status" aria-label="Loading tickets">
      {Array.from({ length: count }).map((_, idx) => (
        <div
          key={idx}
          className="p-4 rounded-xl border border-border bg-surface space-y-3"
        >
          <div className="flex items-center justify-between">
            <Skeleton className="h-4 w-24" />
            <Skeleton className="h-5 w-16 rounded-full" />
          </div>
          <Skeleton className="h-5 w-3/4" />
          <Skeleton className="h-3 w-1/2" />
          <div className="flex items-center justify-between pt-2">
            <Skeleton className="h-3 w-20" />
            <Skeleton className="h-6 w-24 rounded-md" />
          </div>
        </div>
      ))}
    </div>
  );
};

export const StatTileSkeleton: React.FC<{ count?: number }> = ({ count = 4 }) => {
  return (
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-4" role="status" aria-label="Loading statistics">
      {Array.from({ length: count }).map((_, idx) => (
        <div key={idx} className="p-4 rounded-xl border border-border bg-surface space-y-2">
          <Skeleton className="h-3.5 w-20" />
          <Skeleton className="h-8 w-16" />
          <Skeleton className="h-6 w-full" />
        </div>
      ))}
    </div>
  );
};

import React from 'react';
import { CountUp } from './CountUp';
import { Sparkline } from './Sparkline';

export interface BentoStatCardProps {
  label: string;
  value: number;
  sparklineData?: number[];
  sparklineColor?: string;
  icon?: React.ReactNode;
  description?: string;
  isAlert?: boolean;
  className?: string;
}

export const BentoStatCard: React.FC<BentoStatCardProps> = ({
  label,
  value,
  sparklineData,
  sparklineColor = 'var(--color-primary)',
  icon,
  description,
  isAlert = false,
  className = '',
}) => {
  return (
    <div
      className={`bento-card p-4 sm:p-5 flex flex-col justify-between shadow-xs hover:shadow-md transition-all min-w-0 overflow-hidden ${
        isAlert
          ? 'ticket-card-overdue'
          : 'bg-surface border-border'
      } ${className}`}
    >
      <div className="flex items-center justify-between gap-2 mb-2 min-w-0">
        <span
          className="text-xs font-bold uppercase tracking-wider text-text-muted truncate flex-1 min-w-0"
          title={label}
        >
          {label}
        </span>
        {icon && (
          <div
            className={`p-1.5 rounded-lg shrink-0 ${
              isAlert ? 'text-danger bg-danger-bg' : 'text-primary bg-surface-muted'
            }`}
          >
            {icon}
          </div>
        )}
      </div>

      <div className="flex items-baseline justify-between gap-2 my-1 min-w-0">
        <div className="text-xl sm:text-2xl lg:text-3xl font-extrabold text-text tracking-tight font-sans truncate min-w-0">
          <CountUp value={value} />
        </div>
        {sparklineData && (
          <div className="shrink-0 hidden xs:block">
            <Sparkline
              data={sparklineData}
              color={isAlert ? 'var(--color-danger)' : sparklineColor}
              height={32}
              width={72}
            />
          </div>
        )}
      </div>

      {description && (
        <div
          className="mt-2 text-[11px] text-text-dim truncate min-w-0"
          title={description}
        >
          {description}
        </div>
      )}
    </div>
  );
};

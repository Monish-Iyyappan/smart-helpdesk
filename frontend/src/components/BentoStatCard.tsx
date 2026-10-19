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
      className={`bento-card p-5 flex flex-col justify-between shadow-xs hover:shadow-md transition-all ${
        isAlert
          ? 'border-danger-border bg-danger-bg/20'
          : 'bg-surface border-border'
      } ${className}`}
    >
      <div className="flex items-center justify-between gap-2 mb-2">
        <span className="text-xs font-bold uppercase tracking-wider text-text-muted">
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

      <div className="flex items-baseline justify-between gap-3 my-1">
        <div className="text-2xl sm:text-3xl font-extrabold text-text tracking-tight font-sans">
          <CountUp value={value} />
        </div>
        {sparklineData && (
          <div className="shrink-0">
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
        <div className="mt-2 text-[11px] text-text-dim truncate">
          {description}
        </div>
      )}
    </div>
  );
};

import React from 'react';
import { useAuth } from '../auth/AuthContext';
import { ShinyText } from './ui/shiny-text';

export interface GreetingHeaderProps {
  subtitle?: string;
  actions?: React.ReactNode;
}

export const GreetingHeader: React.FC<GreetingHeaderProps> = ({
  subtitle,
  actions,
}) => {
  const { user } = useAuth();

  const initials = user?.name
    ? user.name
        .split(' ')
        .map((n) => n[0])
        .slice(0, 2)
        .join('')
        .toUpperCase()
    : 'SH';

  const roleLabel = {
    STUDENT: 'Student Portal',
    AGENT: 'Support Agent Desk',
    ADMIN: 'System Administrator',
  }[user?.role || 'STUDENT'];

  return (
    <div className="bento-card p-5 sm:p-6 mb-6 flex flex-col md:flex-row md:items-center justify-between gap-4 bg-surface border-border shadow-xs">
      <div className="flex items-center gap-4">
        {/* Initials Avatar */}
        <div className="w-12 h-12 rounded-2xl bg-primary text-primary-text font-bold text-base flex items-center justify-center shrink-0 shadow-sm ring-2 ring-accent/20">
          {initials}
        </div>

        <div>
          <div className="flex items-center gap-2 flex-wrap">
            <h2 className="text-lg sm:text-xl font-bold tracking-tight text-text">
              Welcome back, <ShinyText text={user?.name || 'User'} />
            </h2>
            <span className="text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded-full bg-secondary text-secondary-text border border-border">
              {roleLabel}
            </span>
          </div>
          <p className="text-xs text-text-muted mt-0.5">
            {subtitle || 'Manage support tickets, track issues, and collaborate across teams.'}
          </p>
        </div>
      </div>

      {actions && <div className="flex items-center gap-2 flex-wrap shrink-0">{actions}</div>}
    </div>
  );
};

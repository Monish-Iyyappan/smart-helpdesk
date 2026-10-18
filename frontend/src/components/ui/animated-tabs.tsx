/**
 * Aceternity UI - Animated Tabs
 * Source: https://ui.aceternity.com/components/tabs
 * License: MIT
 */

import React from 'react';
import { motion } from 'motion/react';
import { cn } from '../../lib/utils';

export interface TabItem {
  id: string;
  label: string;
  count?: number;
}

export interface AnimatedTabsProps {
  tabs: TabItem[];
  activeTab: string;
  onTabChange: (id: string) => void;
  className?: string;
  tabClassName?: string;
  activeTabClassName?: string;
}

export const AnimatedTabs: React.FC<AnimatedTabsProps> = ({
  tabs,
  activeTab,
  onTabChange,
  className,
  tabClassName,
  activeTabClassName,
}) => {
  return (
    <div
      role="tablist"
      className={cn(
        'flex items-center gap-1.5 p-1 bg-surface-muted border border-border rounded-xl overflow-x-auto no-scrollbar',
        className
      )}
    >
      {tabs.map((tab) => {
        const isActive = activeTab === tab.id;
        return (
          <button
            key={tab.id}
            role="tab"
            aria-selected={isActive}
            onClick={() => onTabChange(tab.id)}
            className={cn(
              'relative inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-lg whitespace-nowrap transition-colors z-10 focus:outline-none focus-visible:ring-2 focus-visible:ring-accent',
              isActive
                ? 'text-primary font-semibold'
                : 'text-text-muted hover:text-text',
              tabClassName
            )}
          >
            {isActive && (
              <motion.div
                layoutId="active-animated-tab"
                transition={{ type: 'spring', bounce: 0.2, duration: 0.4 }}
                className={cn(
                  'absolute inset-0 bg-surface rounded-lg shadow-xs -z-10 border border-border/60',
                  activeTabClassName
                )}
              />
            )}
            <span className="relative z-10">{tab.label}</span>
            {typeof tab.count === 'number' && (
              <span
                className={cn(
                  'relative z-10 px-1.5 py-0.2 rounded-full text-[10px] font-bold transition-colors',
                  isActive
                    ? 'bg-primary/10 text-primary'
                    : 'bg-surface text-text-dim'
                )}
              >
                {tab.count}
              </span>
            )}
          </button>
        );
      })}
    </div>
  );
};

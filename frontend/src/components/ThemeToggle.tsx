import React, { useEffect, useState } from 'react';
import { SunIcon, MoonIcon } from './Icons';

export const ThemeToggle: React.FC<{ className?: string; showLabel?: boolean }> = ({
  className = '',
  showLabel = false,
}) => {
  const [isDark, setIsDark] = useState<boolean>(() => {
    if (typeof window === 'undefined') return false;
    const stored = localStorage.getItem('smart_helpdesk_theme');
    if (stored) return stored === 'dark';
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
  });

  useEffect(() => {
    if (isDark) {
      document.documentElement.classList.add('dark');
      localStorage.setItem('smart_helpdesk_theme', 'dark');
    } else {
      document.documentElement.classList.remove('dark');
      localStorage.setItem('smart_helpdesk_theme', 'light');
    }
  }, [isDark]);

  const toggleTheme = () => {
    setIsDark((prev) => !prev);
  };

  return (
    <button
      type="button"
      onClick={toggleTheme}
      className={`inline-flex items-center gap-2 px-2.5 py-1.5 text-xs font-medium rounded-md border border-border bg-surface text-text-muted hover:text-text hover:bg-surface-hover transition-colors duration-fast ${className}`}
      title={isDark ? 'Switch to light mode' : 'Switch to dark mode'}
      aria-label="Toggle theme"
    >
      {isDark ? (
        <>
          <SunIcon className="w-4 h-4 text-warning" />
          {showLabel && <span>Light Mode</span>}
        </>
      ) : (
        <>
          <MoonIcon className="w-4 h-4 text-primary" />
          {showLabel && <span>Dark Mode</span>}
        </>
      )}
    </button>
  );
};

import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { useAuth } from '../auth/AuthContext';
import { LogOutIcon, MenuIcon, XCircleIcon, SearchIcon, CommandIcon } from './Icons';
import { ThemeToggle } from './ThemeToggle';
import { ToastContainer } from './Toast';
import type { User } from '../api/types';
import { Sidebar, SidebarBody, SidebarLink, useSidebar } from './ui/sidebar';
import { CommandPalette, type CommandItem } from './ui/command-palette';

export interface ShellTab {
  id: string;
  label: string;
  count?: number;
}

export interface ShellProps {
  children: React.ReactNode;
  title?: string;
  tabs?: ShellTab[];
  activeTab?: string;
  onTabChange?: (tabId: string) => void;
  headerActions?: React.ReactNode;
}

interface DesktopSidebarContentProps {
  user: User | null;
  initials: string;
  tabs?: ShellTab[];
  activeTab?: string;
  onTabChange?: (tabId: string) => void;
  setIsPaletteOpen: (open: boolean) => void;
  logout: () => void;
}

const DesktopSidebarContent: React.FC<DesktopSidebarContentProps> = ({
  user,
  initials,
  tabs,
  activeTab,
  onTabChange,
  setIsPaletteOpen,
  logout,
}) => {
  const { open } = useSidebar();

  return (
    <SidebarBody className="justify-between gap-4 h-full">
      {/* Top Section (Fixed at top of sidebar) */}
      <div className="shrink-0 flex flex-col">
        {/* Logo / Header */}
        <div
          className={`flex items-center ${
            open ? 'gap-3 px-2' : 'justify-center px-0'
          } py-2 border-b border-border/60 pb-4 mb-4 overflow-hidden`}
        >
          <div
            className="w-9 h-9 rounded-xl bg-primary text-primary-text font-bold text-sm flex items-center justify-center shrink-0 shadow-sm"
            title="Smart Helpdesk"
          >
            SH
          </div>
          {open && (
            <div className="flex flex-col overflow-hidden min-w-0">
              <span
                className="font-bold text-sm text-text tracking-tight truncate"
                title="Smart Helpdesk"
              >
                Smart Helpdesk
              </span>
              {user && (
                <span
                  className="text-[10px] uppercase font-bold tracking-wider text-primary truncate"
                  title={`${user.role} Portal`}
                >
                  {user.role} Portal
                </span>
              )}
            </div>
          )}
        </div>

        {/* Quick Command Trigger Pill */}
        <button
          type="button"
          onClick={() => setIsPaletteOpen(true)}
          className={`flex items-center ${
            open ? 'justify-between px-3' : 'justify-center px-1'
          } py-2 mb-3 rounded-xl border border-border bg-surface-muted/60 hover:bg-surface-hover text-text-muted hover:text-text transition-colors text-xs focus-ring`}
          title="Search commands (Ctrl+K)"
          aria-label="Search commands (Ctrl+K)"
        >
          <div className="flex items-center gap-2 truncate">
            <SearchIcon className="w-3.5 h-3.5 shrink-0" />
            {open && <span className="truncate">Commands</span>}
          </div>
          {open && (
            <kbd className="text-[10px] font-mono px-1 py-0.5 rounded bg-surface border border-border text-text-dim shrink-0">
              ⌘K
            </kbd>
          )}
        </button>
      </div>

      {/* Navigation Links (Scrolls internally if taller than screen) */}
      <div className="flex-1 overflow-y-auto overflow-x-hidden min-w-0 space-y-1 py-1 custom-scrollbar">
        {tabs && tabs.length > 0 ? (
          tabs.map((tab) => {
            const isActive = activeTab === tab.id;
            return (
              <SidebarLink
                key={tab.id}
                label={tab.label}
                active={isActive}
                count={tab.count}
                onClick={() => onTabChange?.(tab.id)}
                icon={
                  <div
                    className={`w-2 h-2 rounded-full ${
                      isActive ? 'bg-primary-text' : 'bg-accent/60'
                    }`}
                  />
                }
              />
            );
          })
        ) : (
          open && (
            <div className="px-2 py-4">
              <div className="p-3 rounded-xl bg-surface-muted border border-border">
                <div className="text-xs font-bold text-text capitalize">
                  {user?.role.toLowerCase() || 'User'}
                </div>
                <div className="text-[11px] text-text-muted mt-0.5">Workspace Active</div>
              </div>
            </div>
          )
        )}
      </div>

      {/* Desktop Sidebar Bottom with Profile & Theme (Fixed at bottom of sidebar) */}
      <div className="border-t border-border pt-4 space-y-3 min-w-0 shrink-0">
        {user && (
          <div
            className={`flex items-center ${
              open ? 'gap-2.5 px-1' : 'justify-center px-0'
            }`}
            title={`${user.name} (${user.email})`}
          >
            <div className="w-8 h-8 rounded-full bg-secondary text-secondary-text font-bold text-xs flex items-center justify-center shrink-0">
              {initials}
            </div>
            {open && (
              <div className="min-w-0 flex-1 overflow-hidden">
                <div className="text-xs font-bold text-text truncate" title={user.name}>
                  {user.name}
                </div>
                <div className="text-[11px] text-text-muted truncate" title={user.email}>
                  {user.email}
                </div>
              </div>
            )}
          </div>
        )}

        <div
          className={`flex items-center ${
            open ? 'justify-between' : 'flex-col gap-2 justify-center'
          } pt-1`}
        >
          <ThemeToggle />
          {user && (
            <button
              type="button"
              onClick={logout}
              className="p-1.5 rounded-lg text-text-muted hover:text-danger hover:bg-surface-hover transition-colors focus-ring"
              title="Logout"
              aria-label="Logout"
            >
              <LogOutIcon className="w-4 h-4" />
            </button>
          )}
        </div>
      </div>
    </SidebarBody>
  );
};

export const Shell: React.FC<ShellProps> = ({
  children,
  title,
  tabs,
  activeTab,
  onTabChange,
  headerActions,
}) => {
  const { user, logout } = useAuth();
  const [isMobileOpen, setIsMobileOpen] = useState(false);
  const [isPaletteOpen, setIsPaletteOpen] = useState(false);

  // Keyboard shortcut Ctrl+K / Cmd+K to open palette
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        setIsPaletteOpen((prev) => !prev);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  // Compute initials avatar
  const initials = user?.name
    ? user.name
        .split(' ')
        .map((n) => n[0])
        .slice(0, 2)
        .join('')
        .toUpperCase()
    : 'SH';

  // Build command palette navigation items based on current context
  const commandItems: CommandItem[] = [
    ...(tabs?.map((t) => ({
      id: `tab-${t.id}`,
      label: `Go to ${t.label}`,
      category: 'Navigation',
      onSelect: () => onTabChange?.(t.id),
    })) || []),
    {
      id: 'action-logout',
      label: 'Log Out of Smart Helpdesk',
      category: 'Account',
      shortcut: 'Alt+Q',
      onSelect: logout,
    },
  ];

  return (
    <div className="min-h-screen md:h-screen md:overflow-hidden bg-bg text-text flex flex-col md:flex-row font-sans w-full">
      {/* Mobile Top Navbar */}
      <header className="md:hidden sticky top-0 z-40 bg-surface/90 backdrop-blur-md border-b border-border shadow-xs px-4 h-14 flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <button
            type="button"
            onClick={() => setIsMobileOpen(true)}
            className="p-1.5 rounded-lg text-text-muted hover:text-text hover:bg-surface-hover focus-ring"
            aria-label="Open menu"
          >
            <MenuIcon className="w-5 h-5" />
          </button>
          <div className="flex items-center gap-2">
            <div className="w-7 h-7 rounded-lg bg-primary text-primary-text font-bold text-xs flex items-center justify-center shadow-xs">
              SH
            </div>
            <span className="font-bold text-sm text-text">Smart Helpdesk</span>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setIsPaletteOpen(true)}
            className="p-1.5 rounded-lg text-text-muted hover:text-text hover:bg-surface-hover focus-ring"
            title="Search commands (Ctrl+K)"
            aria-label="Search commands"
          >
            <SearchIcon className="w-4 h-4" />
          </button>
          <ThemeToggle />
          {user && (
            <button
              type="button"
              onClick={logout}
              className="p-1.5 rounded-lg text-text-muted hover:text-danger hover:bg-surface-hover focus-ring"
              title="Logout"
              aria-label="Logout"
            >
              <LogOutIcon className="w-4 h-4" />
            </button>
          )}
        </div>
      </header>

      {/* Mobile Backdrop & Slide Drawer */}
      <AnimatePresence>
        {isMobileOpen && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.15 }}
              onClick={() => setIsMobileOpen(false)}
              className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs md:hidden"
            />
            <motion.aside
              initial={{ x: '-100%' }}
              animate={{ x: 0 }}
              exit={{ x: '-100%' }}
              transition={{ type: 'spring', damping: 25, stiffness: 300 }}
              className="fixed inset-y-0 left-0 z-50 w-72 bg-surface border-r border-border flex flex-col md:hidden p-4 shadow-2xl"
            >
              <div className="flex items-center justify-between pb-4 border-b border-border">
                <div className="flex items-center gap-2.5">
                  <div className="w-8 h-8 rounded-lg bg-primary text-primary-text font-bold text-sm flex items-center justify-center shadow-xs">
                    SH
                  </div>
                  <div>
                    <div className="font-bold text-sm text-text">Smart Helpdesk</div>
                    {user && (
                      <span className="text-[10px] uppercase font-bold text-primary tracking-wider">
                        {user.role} Portal
                      </span>
                    )}
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => setIsMobileOpen(false)}
                  className="p-1.5 rounded-lg text-text-muted hover:text-text hover:bg-surface-hover focus-ring"
                  aria-label="Close menu"
                >
                  <XCircleIcon className="w-5 h-5" />
                </button>
              </div>

              {/* Navigation items for tabs */}
              {tabs && tabs.length > 0 && (
                <div className="flex-1 py-4 space-y-1 overflow-y-auto custom-scrollbar">
                  <div className="px-2 pb-2 text-[10px] font-bold uppercase tracking-wider text-text-dim">
                    Navigation
                  </div>
                  {tabs.map((tab) => {
                    const isActive = activeTab === tab.id;
                    return (
                      <button
                        key={tab.id}
                        type="button"
                        onClick={() => {
                          onTabChange?.(tab.id);
                          setIsMobileOpen(false);
                        }}
                        className={`w-full flex items-center justify-between px-3 py-2.5 text-xs font-semibold rounded-xl transition-colors duration-fast focus-ring ${
                          isActive
                            ? 'bg-primary text-primary-text shadow-sm'
                            : 'text-text-muted hover:text-text hover:bg-surface-hover'
                        }`}
                      >
                        <span>{tab.label}</span>
                        {typeof tab.count === 'number' && (
                          <span
                            className={`px-1.5 py-0.5 rounded-full text-[10px] font-bold ${
                              isActive
                                ? 'bg-surface text-primary'
                                : 'bg-surface-muted text-text-muted'
                            }`}
                          >
                            {tab.count}
                          </span>
                        )}
                      </button>
                    );
                  })}
                </div>
              )}

              {/* Drawer Footer */}
              <div className="pt-4 border-t border-border mt-auto space-y-3">
                {user && (
                  <div className="flex items-center gap-2.5 px-2">
                    <div className="w-8 h-8 rounded-full bg-secondary text-secondary-text font-bold text-xs flex items-center justify-center shrink-0">
                      {initials}
                    </div>
                    <div className="min-w-0 flex-1">
                      <div className="text-xs font-bold text-text truncate">{user.name}</div>
                      <div className="text-[11px] text-text-muted truncate">{user.email}</div>
                    </div>
                  </div>
                )}
                <div className="flex items-center justify-between gap-2 px-2">
                  <ThemeToggle showLabel />
                  <button
                    type="button"
                    onClick={logout}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-lg border border-border text-danger hover:bg-danger-bg transition-colors focus-ring"
                  >
                    <LogOutIcon className="w-3.5 h-3.5" />
                    <span>Logout</span>
                  </button>
                </div>
              </div>
            </motion.aside>
          </>
        )}
      </AnimatePresence>

      {/* Desktop Aceternity UI Sidebar */}
      <Sidebar>
        <DesktopSidebarContent
          user={user}
          initials={initials}
          tabs={tabs}
          activeTab={activeTab}
          onTabChange={onTabChange}
          setIsPaletteOpen={setIsPaletteOpen}
          logout={logout}
        />
      </Sidebar>

      {/* Main Workspace Area: Only the main content area scrolls on desktop */}
      <div className="flex-1 flex flex-col min-w-0 md:h-screen md:overflow-y-auto md:overflow-x-hidden">
        {/* Sticky Translucent Top Bar */}
        <header className="sticky top-0 z-20 bg-surface/85 backdrop-blur-md border-b border-border px-4 sm:px-8 py-3.5 shadow-xs shrink-0">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
            <div className="flex items-center gap-3">
              {title && (
                <h1 className="text-lg sm:text-xl font-bold tracking-tight text-text">
                  {title}
                </h1>
              )}
              {/* Command Palette Trigger in Top Bar */}
              <button
                type="button"
                onClick={() => setIsPaletteOpen(true)}
                className="hidden lg:inline-flex items-center gap-2 px-2.5 py-1 text-xs rounded-lg border border-border bg-surface-muted/60 hover:bg-surface-hover text-text-muted hover:text-text transition-colors focus-ring"
              >
                <CommandIcon className="w-3 h-3" />
                <span>Search / Press</span>
                <kbd className="font-mono text-[10px] px-1 py-0.5 rounded bg-surface border border-border text-text-dim">
                  Ctrl K
                </kbd>
              </button>
            </div>

            {headerActions && (
              <div className="flex items-center gap-2 flex-wrap">{headerActions}</div>
            )}
          </div>
        </header>

        {/* Animated Page Transition Wrapper */}
        <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-8 py-6 min-w-0">
          <AnimatePresence mode="wait">
            <motion.div
              key={activeTab || title || 'main-page'}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              transition={{ duration: 0.2, ease: 'easeOut' }}
              className="w-full"
            >
              {children}
            </motion.div>
          </AnimatePresence>
        </main>

        {/* Footer */}
        <footer className="mt-auto py-4 px-6 border-t border-border bg-surface-muted/30 text-center text-[11px] text-text-dim shrink-0">
          <span>Smart Helpdesk Portal • Signal Desk SaaS System</span>
        </footer>
      </div>

      {/* Skiper UI Ctrl+K Command Palette */}
      <CommandPalette
        isOpen={isPaletteOpen}
        onClose={() => setIsPaletteOpen(false)}
        items={commandItems}
      />

      {/* Global Toast Feedback */}
      <ToastContainer />
    </div>
  );
};

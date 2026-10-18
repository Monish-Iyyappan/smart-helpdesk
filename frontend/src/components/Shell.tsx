import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { useAuth } from '../auth/AuthContext';
import { LogOutIcon, MenuIcon, ChevronLeftIcon, ChevronRightIcon, XCircleIcon } from './Icons';
import { ThemeToggle } from './ThemeToggle';
import { ToastContainer } from './Toast';

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

export const Shell: React.FC<ShellProps> = ({
  children,
  title,
  tabs,
  activeTab,
  onTabChange,
  headerActions,
}) => {
  const { user, logout } = useAuth();
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [isMobileOpen, setIsMobileOpen] = useState(false);

  return (
    <div className="min-h-screen bg-bg text-text flex flex-col md:flex-row font-sans">
      {/* Mobile Top Navbar */}
      <header className="md:hidden sticky top-0 z-40 bg-surface border-b border-border shadow-sm px-4 h-14 flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <button
            type="button"
            onClick={() => setIsMobileOpen(true)}
            className="p-1.5 rounded-md text-text-muted hover:text-text hover:bg-surface-hover"
            aria-label="Open menu"
          >
            <MenuIcon className="w-5 h-5" />
          </button>
          <div className="flex items-center gap-2">
            <div className="w-7 h-7 rounded-md bg-primary text-primary-text font-bold text-xs flex items-center justify-center">
              SH
            </div>
            <span className="font-bold text-sm text-text">Smart Helpdesk</span>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <ThemeToggle />
          {user && (
            <button
              type="button"
              onClick={logout}
              className="p-1.5 rounded-md text-text-muted hover:text-text hover:bg-surface-hover"
              title="Logout"
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
              className="fixed inset-0 z-50 bg-black/50 md:hidden"
            />
            <motion.aside
              initial={{ x: '-100%' }}
              animate={{ x: 0 }}
              exit={{ x: '-100%' }}
              transition={{ type: 'spring', damping: 25, stiffness: 300 }}
              className="fixed inset-y-0 left-0 z-50 w-72 bg-surface border-r border-border flex flex-col md:hidden p-4 shadow-xl"
            >
              <div className="flex items-center justify-between pb-4 border-b border-border">
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 rounded-md bg-primary text-primary-text font-bold text-sm flex items-center justify-center">
                    SH
                  </div>
                  <div>
                    <div className="font-bold text-sm text-text">Smart Helpdesk</div>
                    {user && (
                      <span className="text-[10px] uppercase font-bold text-text-dim tracking-wider">
                        {user.role} Portal
                      </span>
                    )}
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => setIsMobileOpen(false)}
                  className="p-1.5 rounded-md text-text-muted hover:text-text hover:bg-surface-hover"
                >
                  <XCircleIcon className="w-5 h-5" />
                </button>
              </div>

              {/* Navigation items for tabs */}
              {tabs && tabs.length > 0 && (
                <div className="flex-1 py-4 space-y-1 overflow-y-auto">
                  <div className="px-2 pb-2 text-[11px] font-bold uppercase tracking-wider text-text-dim">
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
                        className={`w-full flex items-center justify-between px-3 py-2 text-xs font-medium rounded-md transition-colors duration-fast ${
                          isActive
                            ? 'bg-primary text-primary-text font-semibold shadow-xs'
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
                  <div className="px-2">
                    <div className="text-xs font-semibold text-text truncate">{user.name}</div>
                    <div className="text-[11px] text-text-muted truncate">{user.email}</div>
                  </div>
                )}
                <div className="flex items-center justify-between gap-2 px-2">
                  <ThemeToggle showLabel />
                  <button
                    type="button"
                    onClick={logout}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md border border-border text-danger hover:bg-danger-bg transition-colors"
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

      {/* Desktop Collapsible Sidebar */}
      <motion.aside
        animate={{ width: isCollapsed ? 72 : 240 }}
        transition={{ type: 'spring', damping: 25, stiffness: 280 }}
        className="hidden md:flex flex-col sticky top-0 h-screen bg-surface border-r border-border z-30 shrink-0 select-none overflow-hidden"
      >
        {/* Sidebar Header */}
        <div className="h-16 flex items-center justify-between px-4 border-b border-border">
          <div className="flex items-center gap-3 overflow-hidden">
            <div className="w-9 h-9 rounded-md bg-primary text-primary-text font-bold text-sm flex items-center justify-center shrink-0 shadow-xs">
              SH
            </div>
            {!isCollapsed && (
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="truncate"
              >
                <div className="font-bold text-sm text-text tracking-tight truncate">
                  Smart Helpdesk
                </div>
                {user && (
                  <div className="text-[10px] uppercase font-bold tracking-wider text-text-dim truncate">
                    {user.role}
                  </div>
                )}
              </motion.div>
            )}
          </div>
          <button
            type="button"
            onClick={() => setIsCollapsed(!isCollapsed)}
            className="p-1 rounded-md text-text-muted hover:text-text hover:bg-surface-hover transition-colors"
            title={isCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          >
            {isCollapsed ? (
              <ChevronRightIcon className="w-4 h-4" />
            ) : (
              <ChevronLeftIcon className="w-4 h-4" />
            )}
          </button>
        </div>

        {/* Sidebar Navigation Tabs (if tabs provided) */}
        <div className="flex-1 py-4 px-2 space-y-1 overflow-y-auto">
          {tabs && tabs.length > 0 ? (
            <>
              {!isCollapsed && (
                <div className="px-2 pb-2 text-[10px] font-bold uppercase tracking-wider text-text-dim">
                  Views
                </div>
              )}
              {tabs.map((tab) => {
                const isActive = activeTab === tab.id;
                return (
                  <button
                    key={tab.id}
                    type="button"
                    onClick={() => onTabChange?.(tab.id)}
                    title={isCollapsed ? tab.label : undefined}
                    className={`w-full flex items-center ${
                      isCollapsed ? 'justify-center px-2' : 'justify-between px-3'
                    } py-2 text-xs font-medium rounded-md transition-colors duration-fast ${
                      isActive
                        ? 'bg-primary text-primary-text font-semibold shadow-xs'
                        : 'text-text-muted hover:text-text hover:bg-surface-hover'
                    }`}
                  >
                    <span className="truncate">
                      {isCollapsed ? tab.label.slice(0, 3) : tab.label}
                    </span>
                    {typeof tab.count === 'number' && (
                      <span
                        className={`px-1.5 py-0.2 rounded-full text-[10px] font-bold ${
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
            </>
          ) : (
            <div className="px-2 py-4 text-center">
              {!isCollapsed && user && (
                <div className="p-3 rounded-lg bg-surface-muted border border-border">
                  <div className="text-xs font-bold text-text capitalize">{user.role.toLowerCase()}</div>
                  <div className="text-[11px] text-text-muted mt-0.5">Workspace Active</div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Sidebar Footer with Theme & Logout */}
        <div className="p-3 border-t border-border space-y-2">
          {!isCollapsed && user && (
            <div className="px-1 py-1">
              <div className="text-xs font-semibold text-text truncate">{user.name}</div>
              <div className="text-[11px] text-text-muted truncate">{user.email}</div>
            </div>
          )}

          <div className={`flex items-center ${isCollapsed ? 'flex-col gap-2' : 'justify-between'}`}>
            <ThemeToggle showLabel={!isCollapsed} />
            {user && (
              <button
                type="button"
                onClick={logout}
                className="p-1.5 rounded-md text-text-muted hover:text-danger hover:bg-surface-hover transition-colors"
                title="Logout"
              >
                <LogOutIcon className="w-4 h-4" />
              </button>
            )}
          </div>
        </div>
      </motion.aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Top Header Bar for Main Content */}
        {(title || headerActions) && (
          <header className="bg-surface border-b border-border px-4 sm:px-8 py-4 sticky top-0 z-20 shadow-xs">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
              {title && (
                <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-text">
                  {title}
                </h1>
              )}
              {headerActions && (
                <div className="flex items-center gap-2 flex-wrap">{headerActions}</div>
              )}
            </div>
          </header>
        )}

        {/* Animated Page Transition Wrapper */}
        <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-8 py-6">
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
      </div>

      {/* Toast Feedback Overlay */}
      <ToastContainer />
    </div>
  );
};

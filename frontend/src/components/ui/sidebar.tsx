/**
 * Aceternity UI Sidebar Component
 * Source: https://ui.aceternity.com/components/sidebar
 * License: MIT
 * Adapted for Signal Desk tokens, React 19, and motion/react.
 */

import React, { createContext, useContext, useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';

interface SidebarContextProps {
  open: boolean;
  setOpen: React.Dispatch<React.SetStateAction<boolean>>;
  animate: boolean;
}

const SidebarContext = createContext<SidebarContextProps | undefined>(undefined);

export const useSidebar = () => {
  const context = useContext(SidebarContext);
  if (!context) {
    throw new Error('useSidebar must be used within a SidebarProvider');
  }
  return context;
};

export const SidebarProvider: React.FC<{
  children: React.ReactNode;
  open?: boolean;
  setOpen?: React.Dispatch<React.SetStateAction<boolean>>;
  animate?: boolean;
}> = ({ children, open: openProp, setOpen: setOpenProp, animate = true }) => {
  const [openState, setOpenState] = useState(false);
  const open = openProp !== undefined ? openProp : openState;
  const setOpen = setOpenProp !== undefined ? setOpenProp : setOpenState;

  return (
    <SidebarContext.Provider value={{ open, setOpen, animate }}>
      {children}
    </SidebarContext.Provider>
  );
};

export const Sidebar: React.FC<{
  children: React.ReactNode;
  open?: boolean;
  setOpen?: React.Dispatch<React.SetStateAction<boolean>>;
  animate?: boolean;
}> = ({ children, open, setOpen, animate }) => {
  return (
    <SidebarProvider open={open} setOpen={setOpen} animate={animate}>
      {children}
    </SidebarProvider>
  );
};

export const SidebarBody = (props: React.ComponentProps<typeof motion.div>) => {
  return (
    <>
      <DesktopSidebar {...props} />
      <MobileSidebar {...(props as React.ComponentProps<'div'>)} />
    </>
  );
};

export const DesktopSidebar = ({
  className = '',
  children,
  ...props
}: React.ComponentProps<typeof motion.div>) => {
  const { open, setOpen, animate } = useSidebar();
  return (
    <motion.aside
      className={`h-screen sticky top-0 px-3 py-4 hidden md:flex md:flex-col bg-surface border-r border-border shrink-0 select-none overflow-hidden z-30 ${className}`}
      animate={{
        width: animate ? (open ? '250px' : '72px') : '250px',
      }}
      transition={{
        type: 'spring',
        damping: 25,
        stiffness: 280,
      }}
      onMouseEnter={() => setOpen(true)}
      onMouseLeave={() => setOpen(false)}
      {...props}
    >
      {children}
    </motion.aside>
  );
};

export const MobileSidebar = ({
  className = '',
  children,
  ...props
}: React.ComponentProps<'div'>) => {
  const { open, setOpen } = useSidebar();
  return (
    <div className="md:hidden" {...props}>
      <AnimatePresence>
        {open && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.15 }}
              onClick={() => setOpen(false)}
              className="fixed inset-0 bg-black/60 z-50 md:hidden"
            />
            <motion.aside
              initial={{ x: '-100%' }}
              animate={{ x: 0 }}
              exit={{ x: '-100%' }}
              transition={{ type: 'spring', damping: 25, stiffness: 300 }}
              className={`fixed inset-y-0 left-0 z-50 w-72 bg-surface border-r border-border flex flex-col p-4 shadow-2xl md:hidden ${className}`}
            >
              {children}
            </motion.aside>
          </>
        )}
      </AnimatePresence>
    </div>
  );
};

export interface SidebarLinkProps {
  label: string;
  icon: React.ReactNode;
  active?: boolean;
  count?: number;
  onClick?: () => void;
  className?: string;
}

export const SidebarLink: React.FC<SidebarLinkProps> = ({
  label,
  icon,
  active = false,
  count,
  onClick,
  className = '',
}) => {
  const { open } = useSidebar();

  return (
    <button
      type="button"
      onClick={onClick}
      className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-xs font-semibold transition-colors duration-fast focus-ring ${
        active
          ? 'bg-primary text-primary-text shadow-sm'
          : 'text-text-muted hover:text-text hover:bg-surface-hover'
      } ${className}`}
      title={label}
    >
      <span className="shrink-0">{icon}</span>
      <motion.span
        animate={{
          display: open ? 'inline-block' : 'none',
          opacity: open ? 1 : 0,
        }}
        transition={{ duration: 0.15 }}
        className="truncate flex-1 text-left whitespace-pre"
      >
        {label}
      </motion.span>
      {typeof count === 'number' && open && (
        <span
          className={`px-1.5 py-0.5 rounded-full text-[10px] font-bold shrink-0 ${
            active ? 'bg-surface text-primary' : 'bg-surface-muted text-text-muted'
          }`}
        >
          {count}
        </span>
      )}
    </button>
  );
};

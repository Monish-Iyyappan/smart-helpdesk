/**
 * Aceternity UI - Stateful Button
 * Source: https://ui.aceternity.com/components/stateful-button
 * License: MIT
 */

import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { cn } from '../../lib/utils';
import { CheckCircleIcon, XCircleIcon } from '../Icons';

export interface StatefulButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  onClick?: (e: React.MouseEvent<HTMLButtonElement>) => Promise<any> | any;
  children: React.ReactNode;
  successText?: string;
  errorText?: string;
  loadingText?: string;
}

export const StatefulButton: React.FC<StatefulButtonProps> = ({
  onClick,
  children,
  className,
  disabled,
  type = 'button',
  successText = 'Success',
  errorText = 'Error',
  loadingText = 'Processing...',
  ...props
}) => {
  const [status, setStatus] = useState<'idle' | 'loading' | 'success' | 'error'>('idle');

  const handleClick = async (e: React.MouseEvent<HTMLButtonElement>) => {
    if (status === 'loading' || status === 'success') return;

    if (!onClick) return;

    try {
      const result = onClick(e);
      if (result && typeof result.then === 'function') {
        setStatus('loading');
        await result;
        setStatus('success');
        setTimeout(() => setStatus('idle'), 1600);
      }
    } catch {
      setStatus('error');
      setTimeout(() => setStatus('idle'), 1800);
    }
  };

  return (
    <motion.button
      layout
      type={type}
      onClick={handleClick}
      disabled={disabled || status === 'loading'}
      transition={{ type: 'spring', stiffness: 400, damping: 28 }}
      className={cn(
        'relative inline-flex items-center justify-center gap-2 px-3.5 py-1.5 text-xs font-semibold rounded-md transition-colors duration-fast shadow-xs select-none focus:outline-none focus:ring-2 focus:ring-accent focus:ring-offset-1',
        status === 'idle' && 'bg-primary text-primary-text hover:bg-primary-hover disabled:opacity-50',
        status === 'loading' && 'bg-primary/80 text-primary-text cursor-wait',
        status === 'success' && 'bg-success text-white shadow-sm',
        status === 'error' && 'bg-danger text-white shadow-sm',
        className
      )}
      {...(props as any)}
    >
      <AnimatePresence mode="wait" initial={false}>
        {status === 'idle' && (
          <motion.span
            key="idle"
            initial={{ opacity: 0, y: -4 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 4 }}
            transition={{ duration: 0.15 }}
            className="inline-flex items-center gap-1.5"
          >
            {children}
          </motion.span>
        )}

        {status === 'loading' && (
          <motion.span
            key="loading"
            initial={{ opacity: 0, y: -4 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 4 }}
            transition={{ duration: 0.15 }}
            className="inline-flex items-center gap-1.5"
          >
            <span className="w-3.5 h-3.5 border-2 border-current border-t-transparent rounded-full animate-spin" />
            <span>{loadingText}</span>
          </motion.span>
        )}

        {status === 'success' && (
          <motion.span
            key="success"
            initial={{ opacity: 0, scale: 0.8 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.8 }}
            transition={{ duration: 0.15 }}
            className="inline-flex items-center gap-1.5 font-bold"
          >
            <CheckCircleIcon className="w-3.5 h-3.5" />
            <span>{successText}</span>
          </motion.span>
        )}

        {status === 'error' && (
          <motion.span
            key="error"
            initial={{ opacity: 0, scale: 0.8 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.8 }}
            transition={{ duration: 0.15 }}
            className="inline-flex items-center gap-1.5 font-bold"
          >
            <XCircleIcon className="w-3.5 h-3.5" />
            <span>{errorText}</span>
          </motion.span>
        )}
      </AnimatePresence>
    </motion.button>
  );
};

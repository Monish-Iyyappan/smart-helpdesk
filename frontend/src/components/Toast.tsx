import React from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { useAuth } from '../auth/AuthContext';
import { AlertTriangleIcon, CheckCircleIcon, XCircleIcon } from './Icons';

export const ToastContainer: React.FC = () => {
  const { toasts, removeToast } = useAuth();

  return (
    <div
      aria-live="polite"
      className="fixed bottom-4 right-4 z-50 flex flex-col gap-2 max-w-sm w-full px-4 sm:px-0 pointer-events-none"
    >
      <AnimatePresence mode="popLayout">
        {toasts.map((toast) => {
          let containerTokens = 'bg-surface border-border text-text';
          let Icon = AlertTriangleIcon;

          if (toast.type === 'error') {
            containerTokens = 'bg-danger-bg text-danger-text border-danger-border';
            Icon = AlertTriangleIcon;
          } else if (toast.type === 'success') {
            containerTokens = 'bg-success-bg text-success-text border-success-border';
            Icon = CheckCircleIcon;
          }

          return (
            <motion.div
              key={toast.id}
              role="alert"
              layout
              initial={{ opacity: 0, y: 16, scale: 0.95 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, scale: 0.9, transition: { duration: 0.15 } }}
              transition={{ type: 'spring', damping: 25, stiffness: 350 }}
              className={`pointer-events-auto flex items-start justify-between gap-3 p-3.5 rounded-xl border shadow-md ${containerTokens}`}
            >
              <div className="flex items-start gap-2.5">
                <Icon className="w-5 h-5 flex-shrink-0 mt-0.5" />
                <p className="text-xs sm:text-sm font-medium leading-snug break-words">
                  {toast.message}
                </p>
              </div>
              <button
                type="button"
                onClick={() => removeToast(toast.id)}
                className="text-current opacity-70 hover:opacity-100 p-1 rounded-md transition-opacity focus-ring"
                aria-label="Close notification"
              >
                <XCircleIcon className="w-4 h-4" />
              </button>
            </motion.div>
          );
        })}
      </AnimatePresence>
    </div>
  );
};

import React from 'react';
import { useAuth } from '../auth/AuthContext';
import { AlertTriangleIcon, CheckCircleIcon, XCircleIcon } from './Icons';

export const ToastContainer: React.FC = () => {
  const { toasts, removeToast } = useAuth();

  if (toasts.length === 0) {
    return null;
  }

  return (
    <div
      aria-live="polite"
      className="fixed bottom-4 right-4 z-50 flex flex-col gap-2 max-w-sm w-full px-4 sm:px-0"
    >
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
          <div
            key={toast.id}
            role="alert"
            className={`flex items-start justify-between gap-3 p-3.5 rounded-lg border shadow-sm transition-all duration-fast ${containerTokens}`}
          >
            <div className="flex items-start gap-2.5">
              <Icon className="w-5 h-5 flex-shrink-0 mt-0.5" />
              <p className="text-sm font-medium leading-snug break-words">
                {toast.message}
              </p>
            </div>
            <button
              type="button"
              onClick={() => removeToast(toast.id)}
              className="text-current opacity-70 hover:opacity-100 p-0.5 rounded-sm transition-opacity"
              aria-label="Close notification"
            >
              <XCircleIcon className="w-4 h-4" />
            </button>
          </div>
        );
      })}
    </div>
  );
};

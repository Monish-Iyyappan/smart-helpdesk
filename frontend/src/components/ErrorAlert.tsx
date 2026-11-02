import React from 'react';
import { AlertTriangleIcon } from './Icons';
import { getErrorMessage, isRetryableError } from '../api/client';

export interface ErrorAlertProps {
  /** The error object, ApiError, Error, or error message string */
  error: unknown;
  /** Optional callback to repeat the failed action (shown as 'Try again' button) */
  onRetry?: () => void;
  /** Additional styling classes */
  className?: string;
  /** Explicitly force the retry button to appear or hide */
  canRetry?: boolean;
}

/**
 * Consistent Error Alert Component
 *
 * Displays a high-contrast red error message with both an icon and text (never color alone)
 * and role="alert" for screen reader accessibility. If the error is a server or network error,
 * a "Try again" button is rendered to let users conveniently retry the action.
 */
export const ErrorAlert: React.FC<ErrorAlertProps> = ({
  error,
  onRetry,
  className = '',
  canRetry,
}) => {
  // If there is no error, render nothing
  if (!error) return null;

  // Extract a clean, plain-English message with no raw codes or traces
  const message = typeof error === 'string' ? error : getErrorMessage(error);
  if (!message) return null;

  // Only show the 'Try again' button if the error is retryable (network/server) and a callback was provided
  const shouldShowRetry = (canRetry ?? isRetryableError(error)) && Boolean(onRetry);

  return (
    <div
      role="alert"
      aria-live="assertive"
      className={`px-3.5 py-2.5 rounded-xl bg-danger-bg border border-danger-border text-danger-text text-xs sm:text-sm font-medium flex items-center justify-between gap-3 shadow-xs ${className}`}
    >
      <div className="flex items-start gap-2.5 flex-1 min-w-0">
        {/* Warning icon ensures errors are identifiable not just by red color */}
        <AlertTriangleIcon className="w-4 h-4 text-danger shrink-0 mt-0.5" />
        <span className="leading-relaxed break-words">{message}</span>
      </div>

      {shouldShowRetry && onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="inline-flex items-center gap-1.5 px-2.5 py-1 text-xs font-semibold rounded-lg bg-surface text-danger hover:bg-surface-hover border border-danger-border shrink-0 transition-colors focus-ring cursor-pointer"
        >
          Try again
        </button>
      )}
    </div>
  );
};

import React from 'react';

export const LoadingScreen: React.FC = () => {
  return (
    <div className="min-h-screen bg-bg flex items-center justify-center font-sans">
      <div className="text-center">
        <div className="inline-flex items-center justify-center w-10 h-10 rounded-md bg-primary text-primary-text font-bold text-base mb-2">
          SH
        </div>
        <p className="text-xs font-medium text-text-muted">Loading session...</p>
      </div>
    </div>
  );
};

/**
 * Subtle CSS-Based Ambient Background
 * Pure CSS radial glow & pattern matrix without WebGL or shader overhead.
 * Fallbacks to static background under reduced motion and narrow mobile screens (< 640px).
 */

import React, { useEffect, useState } from 'react';
import { useReducedMotion } from 'motion/react';

export const AmbientBackground: React.FC = () => {
  const shouldReduceMotion = useReducedMotion();
  const [isNarrowScreen, setIsNarrowScreen] = useState<boolean>(() => {
    if (typeof window === 'undefined') return false;
    return window.innerWidth < 640;
  });

  useEffect(() => {
    const handleResize = () => {
      setIsNarrowScreen(window.innerWidth < 640);
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const isStatic = shouldReduceMotion || isNarrowScreen;

  return (
    <div
      aria-hidden="true"
      className="pointer-events-none fixed inset-0 overflow-hidden -z-10 bg-bg transition-colors"
    >
      {/* Subtle background grid pattern */}
      <div
        className="absolute inset-0 opacity-[0.03] dark:opacity-[0.06]"
        style={{
          backgroundImage: `radial-gradient(currentColor 1px, transparent 1px)`,
          backgroundSize: '24px 24px',
        }}
      />

      {/* Primary ambient radial glow */}
      <div
        className={`absolute -top-32 -left-32 w-96 h-96 rounded-full bg-primary/10 blur-3xl pointer-events-none ${
          isStatic ? '' : 'animate-pulse'
        }`}
        style={{
          animationDuration: '8s',
        }}
      />

      {/* Secondary accent ambient radial glow */}
      <div
        className={`absolute -bottom-32 -right-32 w-96 h-96 rounded-full bg-accent/10 blur-3xl pointer-events-none ${
          isStatic ? '' : 'animate-pulse'
        }`}
        style={{
          animationDuration: '10s',
        }}
      />
    </div>
  );
};

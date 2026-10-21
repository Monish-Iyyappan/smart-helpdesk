/**
 * React Bits ShinyText Component
 * Source: https://reactbits.dev/text-animations/shiny-text
 * License: MIT
 * Adapted for Signal Desk design tokens and reduced motion.
 */

import React from 'react';

export interface ShinyTextProps {
  text: string;
  disabled?: boolean;
  speed?: number;
  className?: string;
}

export const ShinyText: React.FC<ShinyTextProps> = ({
  text,
  disabled = false,
  speed = 4,
  className = '',
}) => {
  const animationDuration = `${speed}s`;

  return (
    <span
      className={`inline-block bg-clip-text text-transparent bg-gradient-to-r from-text via-primary to-text bg-[length:200%_auto] ${
        disabled ? '' : 'animate-shine'
      } ${className}`}
      style={{
        animationDuration,
      }}
    >
      {text}
    </span>
  );
};

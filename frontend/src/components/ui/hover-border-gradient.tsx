/**
 * Aceternity UI Hover Border Gradient Component
 * Source: https://ui.aceternity.com/components/hover-border-gradient
 * License: MIT
 * Adapted for Signal Desk tokens and motion/react.
 */

import React, { useState } from 'react';
import { motion } from 'motion/react';

export interface HoverBorderGradientProps {
  as?: React.ElementType;
  containerClassName?: string;
  className?: string;
  duration?: number;
  children: React.ReactNode;
  onClick?: () => void;
}

export const HoverBorderGradient: React.FC<HoverBorderGradientProps> = ({
  as: Tag = 'button',
  containerClassName = '',
  className = '',
  duration = 1,
  children,
  onClick,
  ...props
}) => {
  const [hovered, setHovered] = useState(false);

  return (
    <Tag
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      onClick={onClick}
      className={`relative p-[1px] flex rounded-xl border border-border content-center transition-colors duration-fast ${containerClassName}`}
      {...props}
    >
      <div
        className={`w-full z-10 px-4 py-2 rounded-[11px] bg-surface text-text text-xs font-semibold flex items-center justify-center gap-2 ${className}`}
      >
        {children}
      </div>
      <motion.div
        className="absolute inset-0 rounded-xl z-0 pointer-events-none"
        animate={{
          background: hovered
            ? 'linear-gradient(90deg, var(--color-primary), var(--color-accent), var(--color-primary))'
            : 'transparent',
          opacity: hovered ? 1 : 0,
        }}
        transition={{ duration }}
      />
    </Tag>
  );
};

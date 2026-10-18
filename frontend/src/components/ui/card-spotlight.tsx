/**
 * Aceternity UI - Card Spotlight
 * Source: https://ui.aceternity.com/components/card-spotlight
 * License: MIT
 */

import React from 'react';
import { motion, useMotionValue, useMotionTemplate } from 'motion/react';
import { cn } from '../../lib/utils';

export interface CardSpotlightProps extends React.HTMLAttributes<HTMLDivElement> {
  children: React.ReactNode;
  radius?: number;
  color?: string;
  className?: string;
}

export const CardSpotlight: React.FC<CardSpotlightProps> = ({
  children,
  radius = 280,
  color,
  className,
  ...props
}) => {
  const mouseX = useMotionValue(-1000);
  const mouseY = useMotionValue(-1000);

  function handleMouseMove({
    currentTarget,
    clientX,
    clientY,
  }: React.MouseEvent<HTMLDivElement>) {
    const { left, top } = currentTarget.getBoundingClientRect();
    mouseX.set(clientX - left);
    mouseY.set(clientY - top);
  }

  function handleMouseLeave() {
    mouseX.set(-1000);
    mouseY.set(-1000);
  }

  // Default spotlight gradient tone using CSS custom property or subtle tint
  const spotlightColor = color || 'rgba(67, 59, 255, 0.08)';

  const background = useMotionTemplate`
    radial-gradient(
      ${radius}px circle at ${mouseX}px ${mouseY}px,
      ${spotlightColor},
      transparent 80%
    )
  `;

  return (
    <div
      className={cn('group/spotlight relative overflow-hidden', className)}
      onMouseMove={handleMouseMove}
      onMouseLeave={handleMouseLeave}
      {...props}
    >
      {/* Spotlight Hover Layer */}
      <motion.div
        className="pointer-events-none absolute -inset-px rounded-xl opacity-0 transition-opacity duration-300 group-hover/spotlight:opacity-100 z-0"
        style={{ background }}
      />

      {/* Content */}
      <div className="relative z-10 w-full h-full">{children}</div>
    </div>
  );
};

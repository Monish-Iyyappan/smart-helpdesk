/**
 * React Bits ClickSpark Micro-Interaction Component
 * Source: https://reactbits.dev/animations/click-spark
 * License: MIT
 * Adapted for Signal Desk tokens and reduced motion support.
 */

import React, { useRef, useEffect } from 'react';

export interface ClickSparkProps {
  sparkColor?: string;
  sparkSize?: number;
  sparkRadius?: number;
  sparkCount?: number;
  duration?: number;
  children: React.ReactNode;
  className?: string;
}

export const ClickSpark: React.FC<ClickSparkProps> = ({
  sparkColor = 'var(--color-primary)',
  sparkSize = 8,
  sparkRadius = 20,
  sparkCount = 6,
  duration = 400,
  children,
  className = '',
}) => {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (prefersReducedMotion) return;

    const container = containerRef.current;
    if (!container) return;

    const handleClick = (e: MouseEvent) => {
      const rect = container.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const y = e.clientY - rect.top;

      for (let i = 0; i < sparkCount; i++) {
        const spark = document.createElement('div');
        const angle = (i * (360 / sparkCount) * Math.PI) / 180;
        const targetX = Math.cos(angle) * sparkRadius;
        const targetY = Math.sin(angle) * sparkRadius;

        spark.style.position = 'absolute';
        spark.style.left = `${x}px`;
        spark.style.top = `${y}px`;
        spark.style.width = `${sparkSize}px`;
        spark.style.height = `${sparkSize}px`;
        spark.style.borderRadius = '50%';
        spark.style.backgroundColor = sparkColor;
        spark.style.pointerEvents = 'none';
        spark.style.transform = 'translate(-50%, -50%) scale(1)';
        spark.style.transition = `all ${duration}ms cubic-bezier(0, 0, 0.2, 1)`;
        spark.style.zIndex = '9999';

        container.appendChild(spark);

        requestAnimationFrame(() => {
          spark.style.transform = `translate(calc(-50% + ${targetX}px), calc(-50% + ${targetY}px)) scale(0)`;
          spark.style.opacity = '0';
        });

        setTimeout(() => {
          if (container.contains(spark)) {
            container.removeChild(spark);
          }
        }, duration);
      }
    };

    container.addEventListener('click', handleClick);
    return () => container.removeEventListener('click', handleClick);
  }, [sparkColor, sparkSize, sparkRadius, sparkCount, duration]);

  return (
    <div ref={containerRef} className={`relative overflow-hidden ${className}`}>
      {children}
    </div>
  );
};

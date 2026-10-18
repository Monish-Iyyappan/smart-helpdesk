/**
 * React Bits - Blur Text
 * Source: https://reactbits.dev/text-animations/blur-text
 * Author: David Haz (https://github.com/DavidHDev/react-bits)
 * License: MIT
 */

import React, { useEffect, useState } from 'react';
import { motion, useReducedMotion } from 'motion/react';

export interface BlurTextProps {
  text: string;
  delay?: number;
  className?: string;
  animateBy?: 'words' | 'letters';
  direction?: 'top' | 'bottom';
}

export const BlurText: React.FC<BlurTextProps> = ({
  text,
  delay = 70,
  className = '',
  animateBy = 'words',
  direction = 'top',
}) => {
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

  // Static fallback under reduced motion or screens narrower than 640px
  if (shouldReduceMotion || isNarrowScreen) {
    return <span className={className}>{text}</span>;
  }

  const elements = animateBy === 'words' ? text.split(' ') : text.split('');

  return (
    <span className={`inline-block ${className}`}>
      {elements.map((segment, index) => (
        <motion.span
          key={index}
          initial={{
            filter: 'blur(8px)',
            opacity: 0,
            y: direction === 'top' ? -12 : 12,
          }}
          animate={{
            filter: 'blur(0px)',
            opacity: 1,
            y: 0,
          }}
          transition={{
            duration: 0.35,
            delay: (index * delay) / 1000,
            ease: 'easeOut',
          }}
          className="inline-block"
        >
          {segment}
          {animateBy === 'words' && index < elements.length - 1 && '\u00A0'}
        </motion.span>
      ))}
    </span>
  );
};

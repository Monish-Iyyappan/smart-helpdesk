import React, { useEffect, useState } from 'react';
import { animate, useReducedMotion } from 'motion/react';

interface CountUpProps {
  value: number;
  className?: string;
}

export const CountUp: React.FC<CountUpProps> = ({ value, className = '' }) => {
  const [displayValue, setDisplayValue] = useState(0);
  const shouldReduceMotion = useReducedMotion();

  useEffect(() => {
    if (shouldReduceMotion) {
      setDisplayValue(value);
      return;
    }

    const controls = animate(0, value, {
      duration: 0.3,
      ease: 'easeOut',
      onUpdate(latest) {
        setDisplayValue(Math.round(latest));
      },
    });

    return () => controls.stop();
  }, [value, shouldReduceMotion]);

  return <span className={className}>{displayValue}</span>;
};

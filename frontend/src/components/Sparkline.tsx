import React from 'react';

export interface SparklineProps {
  data: number[];
  color?: string;
  height?: number;
  width?: number;
  className?: string;
}

export const Sparkline: React.FC<SparklineProps> = ({
  data,
  color = 'var(--color-primary)',
  height = 36,
  width = 100,
  className = '',
}) => {
  if (!data || data.length < 2) {
    data = [1, 3, 2, 5, 4, 7, 6];
  }

  const min = Math.min(...data);
  const max = Math.max(...data);
  const range = max - min || 1;

  // Generate SVG path coordinates
  const points = data.map((val, idx) => {
    const x = (idx / (data.length - 1)) * (width - 4) + 2;
    const y = height - 4 - ((val - min) / range) * (height - 8);
    return `${x.toFixed(1)},${y.toFixed(1)}`;
  });

  const pathD = `M ${points.join(' L ')}`;
  const areaD = `${pathD} L ${width - 2},${height} L 2,${height} Z`;

  return (
    <svg
      viewBox={`0 0 ${width} ${height}`}
      className={`overflow-visible ${className}`}
      width={width}
      height={height}
      aria-hidden="true"
    >
      <defs>
        <linearGradient id={`sparkline-grad-${color.replace(/[^a-zA-Z0-9]/g, '')}`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={color} stopOpacity="0.25" />
          <stop offset="100%" stopColor={color} stopOpacity="0.0" />
        </linearGradient>
      </defs>
      <path
        d={areaD}
        fill={`url(#sparkline-grad-${color.replace(/[^a-zA-Z0-9]/g, '')})`}
      />
      <path
        d={pathD}
        fill="none"
        stroke={color}
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
};

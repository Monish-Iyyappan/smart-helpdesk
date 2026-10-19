import React, { useEffect, useRef } from 'react';

export interface EmptyStateProps {
  title: string;
  description: string;
  action?: React.ReactNode;
  iconType?: 'tickets' | 'inbox' | 'search';
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  title,
  description,
  action,
  iconType = 'tickets',
}) => {
  const svgRef = useRef<SVGSVGElement>(null);

  useEffect(() => {
    // Check user preference for reduced motion
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (prefersReducedMotion || !svgRef.current) return;

    // Lazy load Anime.js v4 for self-drawing SVG line art
    let cancelled = false;
    import('animejs')
      .then(({ animate }) => {
        if (cancelled || !svgRef.current) return;
        const paths = svgRef.current.querySelectorAll('path, rect, circle');
        paths.forEach((element) => {
          const el = element as SVGGeometryElement;
          if (typeof el.getTotalLength === 'function') {
            const length = el.getTotalLength();
            el.style.strokeDasharray = `${length}`;
            el.style.strokeDashoffset = `${length}`;

            animate(el, {
              strokeDashoffset: [length, 0],
              duration: 1200,
              ease: 'outQuad',
            });
          }
        });
      })
      .catch(() => {
        // Fallback gracefully without throwing
      });

    return () => {
      cancelled = true;
    };
  }, [iconType]);

  return (
    <div className="flex flex-col items-center justify-center p-8 sm:p-12 text-center rounded-2xl border border-border bg-surface">
      <div className="w-20 h-20 mb-4 rounded-2xl bg-surface-muted flex items-center justify-center text-primary">
        <svg
          ref={svgRef}
          viewBox="0 0 64 64"
          fill="none"
          stroke="currentColor"
          strokeWidth="2.5"
          strokeLinecap="round"
          strokeLinejoin="round"
          className="w-12 h-12"
          aria-hidden="true"
        >
          {iconType === 'search' ? (
            <>
              <circle cx="28" cy="28" r="18" />
              <path d="m42 42 14 14" />
              <path d="M22 28h12" />
            </>
          ) : iconType === 'inbox' ? (
            <>
              <rect x="8" y="12" width="48" height="40" rx="6" />
              <path d="M8 36h14a6 6 0 0 0 12 0h22" />
              <path d="M24 24h16" />
            </>
          ) : (
            <>
              {/* Ticket outline */}
              <rect x="10" y="14" width="44" height="36" rx="6" />
              <circle cx="10" cy="32" r="4" fill="var(--color-surface)" />
              <circle cx="54" cy="32" r="4" fill="var(--color-surface)" />
              <path d="M20 26h24" strokeDasharray="3 3" />
              <path d="M20 38h16" />
            </>
          )}
        </svg>
      </div>
      <h3 className="text-base font-bold text-text mb-1 tracking-tight">{title}</h3>
      <p className="text-xs sm:text-sm text-text-muted max-w-sm mb-6 leading-relaxed">
        {description}
      </p>
      {action && <div>{action}</div>}
    </div>
  );
};

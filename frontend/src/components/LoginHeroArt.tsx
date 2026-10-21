import React, { useEffect, useRef } from 'react';

export const LoginHeroArt: React.FC = () => {
  const svgRef = useRef<SVGSVGElement>(null);

  useEffect(() => {
    const isNarrow = window.innerWidth < 640;
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    if (isNarrow || prefersReducedMotion || !svgRef.current) return;

    let cancelled = false;

    // Lazy load Anime.js v4 for self-drawing SVG ticket line art
    import('animejs')
      .then(({ animate }) => {
        if (cancelled || !svgRef.current) return;
        const paths = svgRef.current.querySelectorAll('path, rect, circle, line');
        paths.forEach((el) => {
          const geom = el as SVGGeometryElement;
          if (typeof geom.getTotalLength === 'function') {
            const len = geom.getTotalLength();
            geom.style.strokeDasharray = `${len}`;
            geom.style.strokeDashoffset = `${len}`;

            animate(geom, {
              strokeDashoffset: [len, 0],
              duration: 1600,
              ease: 'outExpo',
            });
          }
        });
      })
      .catch(() => {
        // Fallback without throwing
      });

    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="relative w-full max-w-md mx-auto aspect-4/3 flex items-center justify-center p-4">
      {/* Decorative gradient glow behind art */}
      <div className="absolute inset-4 rounded-3xl bg-gradient-to-tr from-primary/10 to-accent/15 blur-2xl -z-10" />

      <svg
        ref={svgRef}
        viewBox="0 0 400 300"
        fill="none"
        stroke="currentColor"
        strokeWidth="2.5"
        strokeLinecap="round"
        strokeLinejoin="round"
        className="w-full h-full text-primary"
        aria-hidden="true"
      >
        {/* Desk / platform base */}
        <line x1="40" y1="260" x2="360" y2="260" stroke="var(--color-border)" strokeWidth="3" />

        {/* Main Ticket Card */}
        <rect
          x="70"
          y="40"
          width="260"
          height="180"
          rx="16"
          stroke="var(--color-primary)"
          fill="var(--color-surface)"
          fillOpacity="0.85"
        />

        {/* Ticket Header stripe */}
        <path d="M70 85 h260" stroke="var(--color-border)" />

        {/* Left notch */}
        <circle cx="70" cy="130" r="10" stroke="var(--color-primary)" fill="var(--color-bg)" />
        {/* Right notch */}
        <circle cx="330" cy="130" r="10" stroke="var(--color-primary)" fill="var(--color-bg)" />

        {/* Dashed line connecting notches */}
        <line
          x1="85"
          y1="130"
          x2="315"
          y2="130"
          stroke="var(--color-border)"
          strokeDasharray="6 6"
        />

        {/* Ticket Header items */}
        <circle cx="100" cy="62" r="8" fill="var(--color-primary)" fillOpacity="0.2" />
        <line x1="118" y1="62" x2="190" y2="62" strokeWidth="3" />
        <rect
          x="260"
          y="54"
          width="50"
          height="16"
          rx="4"
          stroke="var(--color-success)"
          fill="var(--color-success)"
          fillOpacity="0.1"
        />

        {/* Body content lines */}
        <line x1="100" y1="105" x2="240" y2="105" strokeWidth="2.5" />
        <line x1="100" y1="155" x2="280" y2="155" stroke="var(--color-text-dim)" strokeWidth="2" />
        <line x1="100" y1="175" x2="230" y2="175" stroke="var(--color-text-dim)" strokeWidth="2" />
        <line x1="100" y1="195" x2="180" y2="195" stroke="var(--color-text-dim)" strokeWidth="2" />

        {/* Floating Urgency Badge */}
        <rect
          x="265"
          y="170"
          width="48"
          height="32"
          rx="8"
          stroke="var(--color-accent)"
          fill="var(--color-accent)"
          fillOpacity="0.1"
        />
        <circle cx="289" cy="186" r="6" fill="var(--color-accent)" />
      </svg>
    </div>
  );
};

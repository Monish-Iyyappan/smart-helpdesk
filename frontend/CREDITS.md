# Open Source Component Credits & Attributions

This document records all third-party UI components copied or adapted into the Smart Helpdesk frontend, fulfilling licensing and attribution requirements.

All components used are free, open-source (MIT licensed), and contain no paid, Pro, or API-key gated features.

---

## 1. Aceternity UI
- **Website**: [ui.aceternity.com](https://ui.aceternity.com)
- **License**: MIT License
- **Components Used**:
  1. **Bento Grid** (`frontend/src/components/ui/bento-grid.tsx`)
     - Source: [https://ui.aceternity.com/components/bento-grid](https://ui.aceternity.com/components/bento-grid)
     - Usage: Role dashboard grid layouts (Student, Agent, Admin).
  2. **Sidebar** (`frontend/src/components/ui/sidebar.tsx`)
     - Source: [https://ui.aceternity.com/components/sidebar](https://ui.aceternity.com/components/sidebar)
     - Usage: Collapsible desktop sidebar and mobile navigation drawer for `Shell.tsx`.
  3. **Animated Tabs** (`frontend/src/components/ui/animated-tabs.tsx`)
     - Source: [https://ui.aceternity.com/components/tabs](https://ui.aceternity.com/components/tabs)
     - Usage: Admin section view switcher.
  4. **Animated Modal** (`frontend/src/components/ui/animated-modal.tsx`)
     - Source: [https://ui.aceternity.com/components/animated-modal](https://ui.aceternity.com/components/animated-modal)
     - Usage: Create ticket, Join ticket, Add agent, and Incident modals.
  5. **Stateful Button** (`frontend/src/components/ui/stateful-button.tsx`)
     - Source: [https://ui.aceternity.com/components/stateful-button](https://ui.aceternity.com/components/stateful-button)
     - Usage: Async action buttons (submitting tickets, comments, assignment, status update).
  6. **Hover Border Gradient** (`frontend/src/components/ui/hover-border-gradient.tsx`)
     - Source: [https://ui.aceternity.com/components/hover-border-gradient](https://ui.aceternity.com/components/hover-border-gradient)
     - Usage: Highlighted interactive buttons.

---

## 2. Componentry
- **Website**: [componentry.fun](https://componentry.fun)
- **License**: MIT License
- **Components Used**:
  7. **Card Spotlight** (`frontend/src/components/ui/card-spotlight.tsx`)
     - Source: [https://componentry.fun/components/card-spotlight](https://componentry.fun/components/card-spotlight)
     - Usage: Mouse hover spotlight effect on support ticket cards.

---

## 3. Skiper UI
- **Website**: [skiper-ui.com](https://skiper-ui.com)
- **License**: MIT License
- **License Note**: MIT License
- **Components Used**:
  8. **Command Palette** (`frontend/src/components/ui/command-palette.tsx`)
     - Source: [https://skiper-ui.com/components/command-palette](https://skiper-ui.com/components/command-palette)
     - Usage: Global `Ctrl+K` keyboard shortcut command launcher and navigation palette.

---

## 4. React Bits
- **Website**: [reactbits.dev](https://reactbits.dev)
- **License**: MIT License
- **Components Used**:
  9. **BlurText** (`frontend/src/components/ui/blur-text.tsx`)
     - Source: [https://reactbits.dev/text-animations/blur-text](https://reactbits.dev/text-animations/blur-text)
     - Usage: Animated blur-in entrance on login and hero titles.
  10. **ShinyText** (`frontend/src/components/ui/shiny-text.tsx`)
     - Source: [https://reactbits.dev/text-animations/shiny-text](https://reactbits.dev/text-animations/shiny-text)
     - Usage: Shimmering text gradient on dashboard greetings.
  11. **ClickSpark** (`frontend/src/components/ui/click-spark.tsx`)
     - Source: [https://reactbits.dev/animations/click-spark](https://reactbits.dev/animations/click-spark)
     - Usage: Micro-interaction spark particle effect on interactions.

---

## 5. OriginKit
- **Website**: [originkit.dev](https://originkit.dev)
- **License**: MIT License (no API key required)
- **Components Used**:
  12. **Skeleton Loader & Timeline**
     - Files: `frontend/src/components/ui/skeleton.tsx` and `frontend/src/components/ui/timeline.tsx`
     - Source: [https://originkit.dev/components/skeleton](https://originkit.dev/components/skeleton) and [https://originkit.dev/components/timeline](https://originkit.dev/components/timeline)
     - Usage: Loading state pulse placeholders and ticket detail comment event timeline.

---

## 6. Animation Engines & Tooling
- **Motion**: `motion/react` v14 (MIT License) - powering layout transitions, spring physics, and count-ups.
- **Anime.js v4**: `animejs` v4.5.0 (MIT License) - lazy-loaded for self-drawing SVG line art on empty states and login hero.
- **Realtime Colors**: Signal Desk color token contrast checking and palette validation.

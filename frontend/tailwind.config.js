/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        bg: 'var(--color-bg)',
        surface: 'var(--color-surface)',
        'surface-hover': 'var(--color-surface-hover)',
        'surface-muted': 'var(--color-surface-muted)',
        border: 'var(--color-border)',
        'border-subtle': 'var(--color-border-subtle)',
        primary: {
          DEFAULT: 'var(--color-primary)',
          hover: 'var(--color-primary-hover)',
          text: 'var(--color-primary-text)',
        },
        secondary: {
          DEFAULT: 'var(--color-secondary)',
          hover: 'var(--color-secondary-hover)',
          text: 'var(--color-secondary-text)',
        },
        accent: {
          DEFAULT: 'var(--color-accent)',
          text: 'var(--color-accent-text)',
          ring: 'var(--color-accent-ring)',
        },
        text: {
          DEFAULT: 'var(--color-text)',
          muted: 'var(--color-text-muted)',
          dim: 'var(--color-text-dim)',
        },
        danger: {
          DEFAULT: 'var(--color-danger)',
          hover: 'var(--color-danger-hover)',
          bg: 'var(--color-danger-bg)',
          text: 'var(--color-danger-text)',
          border: 'var(--color-danger-border)',
        },
        success: {
          DEFAULT: 'var(--color-success)',
          bg: 'var(--color-success-bg)',
          text: 'var(--color-success-text)',
          border: 'var(--color-success-border)',
        },
        overlay: 'var(--color-overlay)',
        urgency: {
          low: {
            bg: 'var(--color-urgency-low-bg)',
            text: 'var(--color-urgency-low-text)',
            border: 'var(--color-urgency-low-border)',
          },
          medium: {
            bg: 'var(--color-urgency-medium-bg)',
            text: 'var(--color-urgency-medium-text)',
            border: 'var(--color-urgency-medium-border)',
          },
          high: {
            bg: 'var(--color-urgency-high-bg)',
            text: 'var(--color-urgency-high-text)',
            border: 'var(--color-urgency-high-border)',
          },
          urgent: {
            bg: 'var(--color-urgency-urgent-bg)',
            text: 'var(--color-urgency-urgent-text)',
            border: 'var(--color-urgency-urgent-border)',
          },
        },
        overdue: {
          bg: 'var(--color-overdue-bg)',
          text: 'var(--color-overdue-text)',
          border: 'var(--color-overdue-border)',
          glow: 'var(--color-overdue-glow)',
          'card-bg': 'var(--color-overdue-card-bg)',
        },
        status: {
          open: {
            bg: 'var(--color-status-open-bg)',
            text: 'var(--color-status-open-text)',
          },
          assigned: {
            bg: 'var(--color-status-assigned-bg)',
            text: 'var(--color-status-assigned-text)',
          },
          progress: {
            bg: 'var(--color-status-progress-bg)',
            text: 'var(--color-status-progress-text)',
          },
          resolved: {
            bg: 'var(--color-status-resolved-bg)',
            text: 'var(--color-status-resolved-text)',
          },
          closed: {
            bg: 'var(--color-status-closed-bg)',
            text: 'var(--color-status-closed-text)',
          },
        },
      },
      fontFamily: {
        sans: ['var(--font-sans)', 'sans-serif'],
        mono: ['var(--font-mono)', 'monospace'],
      },
      borderRadius: {
        sm: 'var(--radius-sm)',
        md: 'var(--radius-md)',
        lg: 'var(--radius-lg)',
        xl: 'var(--radius-xl)',
        full: 'var(--radius-full)',
      },
      boxShadow: {
        sm: 'var(--shadow-sm)',
        md: 'var(--shadow-md)',
        lg: 'var(--shadow-lg)',
      },
      transitionDuration: {
        fast: 'var(--duration-fast)',
        normal: 'var(--duration-normal)',
        slow: 'var(--duration-slow)',
      },
    },
  },
  plugins: [],
}

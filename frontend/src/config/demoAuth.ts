/**
 * Configuration and constants for quick test sign-in demo accounts.
 * Gated by VITE_DEMO_LOGIN=true in the development environment only.
 */

export const IS_DEMO_LOGIN_ENABLED: boolean = import.meta.env.VITE_DEMO_LOGIN === 'true';

// Single centralized password constant, guarded by the environment flag
export const DEMO_PASSWORD: string = IS_DEMO_LOGIN_ENABLED ? 'Test@1234' : '';

export interface DemoAccount {
  label: string;
  email: string;
}

export const DEMO_ACCOUNTS: DemoAccount[] = [
  { label: 'Student Demo', email: 'student1@college.example' },
  { label: 'Agent Demo', email: 'agent1@college.example' },
  { label: 'Admin Demo', email: 'admin@college.example' },
];

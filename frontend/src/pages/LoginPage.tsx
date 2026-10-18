import React, { useState } from 'react';
import { motion } from 'motion/react';
import { useAuth } from '../auth/AuthContext';
import { ToastContainer } from '../components/Toast';
import { ThemeToggle } from '../components/ThemeToggle';
import { BlurText } from '../components/ui/blur-text';
import { AmbientBackground } from '../components/ui/ambient-background';

export const LoginPage: React.FC = () => {
  const { login, register, showToast } = useAuth();
  const [isRegister, setIsRegister] = useState(false);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg('');
    setIsSubmitting(true);

    try {
      if (isRegister) {
        if (!name.trim()) {
          setErrorMsg('Please enter your full name.');
          setIsSubmitting(false);
          return;
        }
        await register(name.trim(), email.trim(), password);
        showToast('Account created! Welcome to Smart Helpdesk.', 'success');
      } else {
        await login(email.trim(), password);
        // On success, App.tsx automatically switches to the role page.
        // No need to do anything here — user state update triggers re-render.
      }
    } catch {
      // The login() function already called showToast() with the error.
      // We also set a visible inline error message below the form.
      setErrorMsg(
        isRegister
          ? 'Registration failed. Email may already be in use.'
          : 'Login failed. Please check your email and password.'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleTabSwitch = (toRegister: boolean) => {
    setIsRegister(toRegister);
    setErrorMsg('');
    setName('');
    setEmail('');
    setPassword('');
  };

  return (
    <div className="min-h-screen bg-bg flex flex-col items-center justify-center p-4 relative font-sans">
      {/* Subtle CSS-based ambient background (React Bits pattern) */}
      <AmbientBackground />

      {/* Top right theme toggle */}
      <div className="absolute top-4 right-4 z-20">
        <ThemeToggle showLabel />
      </div>

      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25, ease: 'easeOut' }}
        className="max-w-md w-full bg-surface border border-border rounded-xl shadow-lg p-6 sm:p-8 z-10"
      >
        <div className="text-center mb-6">
          <div className="inline-flex items-center justify-center w-12 h-12 rounded-xl bg-primary text-primary-text font-bold text-xl mb-3 shadow-xs">
            SH
          </div>
          <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-text">
            <BlurText text="Smart Helpdesk" delay={60} />
          </h1>
          <p className="text-xs sm:text-sm text-text-muted mt-1">
            {isRegister
              ? 'Create a student account to get started'
              : 'Sign in to access your tickets and dashboard'}
          </p>
        </div>

        {/* Tab switch */}
        <div className="flex border-b border-border mb-6">
          <button
            type="button"
            onClick={() => handleTabSwitch(false)}
            className={`flex-1 py-2 text-center text-sm font-semibold border-b-2 transition-colors duration-fast ${
              !isRegister
                ? 'border-primary text-primary'
                : 'border-transparent text-text-muted hover:text-text'
            }`}
          >
            Sign In
          </button>
          <button
            type="button"
            onClick={() => handleTabSwitch(true)}
            className={`flex-1 py-2 text-center text-sm font-semibold border-b-2 transition-colors duration-fast ${
              isRegister
                ? 'border-primary text-primary'
                : 'border-transparent text-text-muted hover:text-text'
            }`}
          >
            Register
          </button>
        </div>

        {/* Inline error banner */}
        {errorMsg && (
          <div className="mb-4 px-3 py-2 rounded-md bg-danger-bg border border-danger-border text-danger-text text-xs font-medium">
            {errorMsg}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {isRegister && (
            <div>
              <label className="block text-xs font-semibold text-text mb-1" htmlFor="name">
                Full Name
              </label>
              <input
                id="name"
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Jane Doe"
                className="w-full px-3 py-2 text-sm rounded-md border border-border bg-surface text-text placeholder-text-dim focus:outline-none focus:ring-1 focus:ring-primary"
                required
                autoComplete="name"
              />
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-text mb-1" htmlFor="email">
              Email Address
            </label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="user@college.example"
              className="w-full px-3 py-2 text-sm rounded-md border border-border bg-surface text-text placeholder-text-dim focus:outline-none focus:ring-1 focus:ring-primary"
              required
              autoComplete="email"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-text mb-1" htmlFor="password">
              Password
            </label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              className="w-full px-3 py-2 text-sm rounded-md border border-border bg-surface text-text placeholder-text-dim focus:outline-none focus:ring-1 focus:ring-primary"
              required
              autoComplete={isRegister ? 'new-password' : 'current-password'}
            />
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full py-2.5 px-4 text-sm font-semibold rounded-md bg-primary text-primary-text hover:bg-primary-hover disabled:opacity-50 transition-colors duration-fast shadow-xs"
          >
            {isSubmitting
              ? 'Please wait...'
              : isRegister
              ? 'Create Student Account'
              : 'Sign In'}
          </button>
        </form>

        {/* Seed account hints */}
        <div className="mt-6 pt-4 border-t border-border-subtle text-center text-xs text-text-dim space-y-1">
          <div className="font-semibold text-text-muted">Test Accounts (Password: <span className="font-mono">Test@1234</span>)</div>
          <div className="space-y-0.5">
            <div className="font-mono">admin@college.example <span className="text-text-dim">— Admin</span></div>
            <div className="font-mono">agent1@college.example <span className="text-text-dim">— Agent</span></div>
            <div className="font-mono">student1@college.example <span className="text-text-dim">— Student</span></div>
          </div>
        </div>
      </motion.div>

      <ToastContainer />
    </div>
  );
};

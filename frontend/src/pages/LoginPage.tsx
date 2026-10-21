import React, { useState } from 'react';
import { motion } from 'motion/react';
import { useAuth } from '../auth/AuthContext';
import { ToastContainer } from '../components/Toast';
import { ThemeToggle } from '../components/ThemeToggle';
import { BlurText } from '../components/ui/blur-text';
import { AmbientBackground } from '../components/ui/ambient-background';
import { LoginHeroArt } from '../components/LoginHeroArt';
import { StatefulButton } from '../components/ui/stateful-button';
import { IS_DEMO_LOGIN_ENABLED, DEMO_PASSWORD, DEMO_ACCOUNTS } from '../config/demoAuth';

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
      }
    } catch {
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

  const fillCredentials = (roleEmail: string) => {
    if (!IS_DEMO_LOGIN_ENABLED) return;
    setEmail(roleEmail);
    setPassword(DEMO_PASSWORD);
    setIsRegister(false);
    setErrorMsg('');
  };

  return (
    <div className="min-h-screen bg-bg flex flex-col justify-center items-center p-4 sm:p-6 lg:p-8 relative font-sans overflow-x-hidden">
      {/* Subtle CSS ambient background (only one animated background on screen) */}
      <AmbientBackground />

      {/* Top right theme toggle */}
      <div className="absolute top-4 right-4 z-20">
        <ThemeToggle showLabel />
      </div>

      <div className="w-full max-w-5xl mx-auto grid grid-cols-1 lg:grid-cols-2 gap-8 lg:gap-12 items-center z-10 py-8">
        {/* Left Side: Hero Branding & Animated SVG Ticket Art */}
        <motion.div
          initial={{ opacity: 0, x: -16 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.3, ease: 'easeOut' }}
          className="flex flex-col justify-center text-center lg:text-left space-y-4"
        >
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-secondary text-secondary-text text-[11px] font-bold uppercase tracking-wider w-fit mx-auto lg:mx-0 border border-border">
            <span className="w-2 h-2 rounded-full bg-primary animate-pulse" />
            Signal Desk Support Engine
          </div>

          <h1 className="text-2xl sm:text-3xl lg:text-4xl font-extrabold tracking-tight text-text leading-tight">
            <BlurText text="Smart Helpdesk System" delay={60} />
          </h1>

          <p className="text-xs sm:text-sm text-text-muted max-w-md mx-auto lg:mx-0 leading-relaxed">
            Enterprise college support portal with automated urgency scoring, SLA deadline tracking, incident grouping, and multi-agent coordination.
          </p>

          {/* Self-drawing SVG Ticket Art (Desktop animated, static below 640px) */}
          <div className="py-2">
            <LoginHeroArt />
          </div>

          {/* Quick Demo Credentials Pill Bar - Only shown in development when VITE_DEMO_LOGIN is enabled */}
          {IS_DEMO_LOGIN_ENABLED && (
            <div className="pt-2 border-t border-border/60">
              <div className="text-[11px] font-bold text-text-muted mb-2">
                Quick Test Sign In:
              </div>
              <div className="flex items-center gap-2 justify-center lg:justify-start flex-wrap">
                {DEMO_ACCOUNTS.map((acc) => (
                  <button
                    key={acc.email}
                    type="button"
                    onClick={() => fillCredentials(acc.email)}
                    className="px-2.5 py-1 text-xs font-semibold rounded-lg border border-border bg-surface text-text hover:bg-surface-hover hover:border-primary transition-colors focus-ring"
                  >
                    {acc.label}
                  </button>
                ))}
              </div>
            </div>
          )}
        </motion.div>

        {/* Right Side: Glass Form Card */}
        <motion.div
          initial={{ opacity: 0, x: 16 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.3, ease: 'easeOut' }}
          className="w-full max-w-md mx-auto glass-panel rounded-3xl shadow-2xl p-6 sm:p-8 border border-border"
        >
          <div className="text-center mb-6">
            <div className="inline-flex items-center justify-center w-12 h-12 rounded-2xl bg-primary text-primary-text font-bold text-xl mb-3 shadow-sm ring-2 ring-accent/20">
              SH
            </div>
            <h2 className="text-lg sm:text-xl font-bold tracking-tight text-text">
              {isRegister ? 'Create an Account' : 'Sign in to Portal'}
            </h2>
            <p className="text-xs text-text-muted mt-0.5">
              {isRegister
                ? 'Register as a student to submit support tickets'
                : 'Enter your credentials to access your dashboard'}
            </p>
          </div>

          {/* Tab Switch */}
          <div className="flex border-b border-border mb-6">
            <button
              type="button"
              onClick={() => handleTabSwitch(false)}
              className={`flex-1 py-2 text-center text-xs font-bold border-b-2 transition-colors duration-fast focus-ring ${
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
              className={`flex-1 py-2 text-center text-xs font-bold border-b-2 transition-colors duration-fast focus-ring ${
                isRegister
                  ? 'border-primary text-primary'
                  : 'border-transparent text-text-muted hover:text-text'
              }`}
            >
              Register
            </button>
          </div>

          {/* Inline Error Banner */}
          {errorMsg && (
            <div className="mb-4 px-3.5 py-2.5 rounded-xl bg-danger-bg border border-danger-border text-danger-text text-xs font-medium">
              {errorMsg}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            {isRegister && (
              <div>
                <label className="block text-xs font-bold text-text mb-1" htmlFor="name">
                  Full Name
                </label>
                <input
                  id="name"
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="Jane Doe"
                  className="w-full px-3.5 py-2 text-xs sm:text-sm rounded-xl border border-border bg-surface text-text placeholder-text-dim focus-ring"
                  required
                  autoComplete="name"
                />
              </div>
            )}

            <div>
              <label className="block text-xs font-bold text-text mb-1" htmlFor="email">
                Email Address
              </label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="user@college.example"
                className="w-full px-3.5 py-2 text-xs sm:text-sm rounded-xl border border-border bg-surface text-text placeholder-text-dim focus-ring"
                required
                autoComplete="email"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-text mb-1" htmlFor="password">
                Password
              </label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full px-3.5 py-2 text-xs sm:text-sm rounded-xl border border-border bg-surface text-text placeholder-text-dim focus-ring"
                required
                autoComplete={isRegister ? 'new-password' : 'current-password'}
              />
            </div>

            <div className="pt-2">
              <StatefulButton
                type="submit"
                disabled={isSubmitting}
                className="w-full py-2.5 text-xs sm:text-sm font-bold rounded-xl"
                loadingText={isRegister ? 'Creating account...' : 'Signing in...'}
                successText="Success!"
              >
                {isRegister ? 'Create Student Account' : 'Sign In'}
              </StatefulButton>
            </div>
          </form>


        </motion.div>
      </div>

      <ToastContainer />
    </div>
  );
};

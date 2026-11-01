import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { useAuth } from '../auth/AuthContext';
import { ToastContainer } from '../components/Toast';
import { ThemeToggle } from '../components/ThemeToggle';
import { BlurText } from '../components/ui/blur-text';
import { AmbientBackground } from '../components/ui/ambient-background';
import { LoginHeroArt } from '../components/LoginHeroArt';
import { StatefulButton } from '../components/ui/stateful-button';
import { AnimatedTabs } from '../components/ui/animated-tabs';
import {
  MailIcon,
  LockIcon,
  UserIcon,
  EyeIcon,
  EyeOffIcon,
  CheckCircleIcon,
  AlertTriangleIcon,
  SparklesIcon,
} from '../components/Icons';
import { IS_DEMO_LOGIN_ENABLED, DEMO_PASSWORD, DEMO_ACCOUNTS } from '../config/demoAuth';

export const LoginPage: React.FC = () => {
  const { login, register, showToast } = useAuth();
  const [isRegister, setIsRegister] = useState(false);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  // Inline validation touched states (Postel's Law & Zeigarnik Effect)
  const [touched, setTouched] = useState<{ name?: boolean; email?: boolean; password?: boolean }>({});

  const isValidEmail = (val: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val.trim());
  const isValidPassword = (val: string) => val.length >= 6;

  const emailError = touched.email && email.trim().length > 0 && !isValidEmail(email)
    ? 'Please enter a valid email address.'
    : '';
  const passwordError = touched.password && isRegister && password.length > 0 && !isValidPassword(password)
    ? 'Password must be at least 6 characters.'
    : '';
  const nameError = touched.name && isRegister && name.trim().length === 0
    ? 'Please enter your full name.'
    : '';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg('');

    // Mark fields as touched
    setTouched({ name: true, email: true, password: true });

    const trimmedEmail = email.trim();
    if (!isValidEmail(trimmedEmail)) {
      setErrorMsg('Please enter a valid email address.');
      return;
    }

    if (isRegister) {
      if (!name.trim()) {
        setErrorMsg('Please enter your full name.');
        return;
      }
      if (!isValidPassword(password)) {
        setErrorMsg('Password must be at least 6 characters long.');
        return;
      }
    }

    setIsSubmitting(true);

    try {
      if (isRegister) {
        await register(name.trim(), trimmedEmail, password);
        showToast('Account created! Welcome to Smart Helpdesk.', 'success');
      } else {
        await login(trimmedEmail, password);
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
    setShowPassword(false);
    setTouched({});
  };

  const fillCredentials = (roleEmail: string) => {
    if (!IS_DEMO_LOGIN_ENABLED) return;
    setEmail(roleEmail);
    setPassword(DEMO_PASSWORD);
    setIsRegister(false);
    setErrorMsg('');
    setTouched({ email: true, password: true });
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
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-secondary text-secondary-text text-[11px] font-bold uppercase tracking-wider w-fit mx-auto lg:mx-0 border border-border shadow-xs">
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
        </motion.div>

        {/* Right Side: Glass Form Card (Common Region & High Aesthetic-Usability) */}
        <motion.div
          initial={{ opacity: 0, x: 16 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.3, ease: 'easeOut' }}
          className="w-full max-w-md mx-auto glass-panel rounded-3xl shadow-2xl p-6 sm:p-8 border border-border relative overflow-hidden"
        >
          {/* Brand header */}
          <div className="text-center mb-6">
            <div className="inline-flex items-center justify-center w-12 h-12 rounded-2xl bg-primary text-primary-text font-bold text-xl mb-3 shadow-md ring-2 ring-accent/20">
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

          {/* Animated Segmented Tabs (Hick's Law, Prägnanz & Common Region) */}
          <AnimatedTabs
            tabs={[
              { id: 'login', label: 'Sign In' },
              { id: 'register', label: 'Register' },
            ]}
            activeTab={isRegister ? 'register' : 'login'}
            onTabChange={(id) => handleTabSwitch(id === 'register')}
            className="w-full mb-5"
            tabClassName="flex-1 justify-center py-2 text-xs font-bold"
          />

          {/* In-Card Quick Test Demo Pills (Fitts's Law & Law of Proximity) */}
          {IS_DEMO_LOGIN_ENABLED && !isRegister && (
            <div className="mb-5 p-3 rounded-2xl bg-surface-muted/70 border border-border/70 shadow-xs">
              <div className="flex items-center justify-between mb-2">
                <span className="text-[11px] font-bold text-text-muted flex items-center gap-1.5">
                  <SparklesIcon className="w-3.5 h-3.5 text-accent" />
                  Quick Test Sign-In
                </span>
                <span className="text-[10px] uppercase font-bold tracking-wider px-1.5 py-0.5 rounded-md bg-accent/10 text-accent border border-accent/20">
                  Dev Fast-Fill
                </span>
              </div>
              <div className="grid grid-cols-3 gap-2">
                {DEMO_ACCOUNTS.map((acc) => (
                  <button
                    key={acc.email}
                    type="button"
                    onClick={() => fillCredentials(acc.email)}
                    className="px-2 py-1.5 text-xs font-semibold rounded-xl border border-border bg-surface text-text hover:bg-surface-hover hover:border-primary transition-all text-center truncate shadow-xs focus-ring active:scale-95"
                    title={`Fill ${acc.label}`}
                  >
                    {acc.label}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Inline Error Banner (Doherty Threshold & Peak-End Rule) */}
          <AnimatePresence>
            {errorMsg && (
              <motion.div
                initial={{ opacity: 0, height: 0, y: -6 }}
                animate={{ opacity: 1, height: 'auto', y: 0 }}
                exit={{ opacity: 0, height: 0 }}
                transition={{ duration: 0.2 }}
                role="alert"
                aria-live="polite"
                className="mb-4 px-3.5 py-2.5 rounded-xl bg-danger-bg border border-danger-border text-danger-text text-xs font-medium flex items-start gap-2.5 shadow-xs"
              >
                <AlertTriangleIcon className="w-4 h-4 text-danger shrink-0 mt-0.5" />
                <span className="leading-relaxed flex-1">{errorMsg}</span>
              </motion.div>
            )}
          </AnimatePresence>

          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            {/* Full Name Field (Register Mode Only) */}
            {isRegister && (
              <motion.div
                initial={{ opacity: 0, height: 0 }}
                animate={{ opacity: 1, height: 'auto' }}
                exit={{ opacity: 0, height: 0 }}
                transition={{ duration: 0.2 }}
              >
                <label className="block text-xs font-bold text-text mb-1" htmlFor="name">
                  Full Name
                </label>
                <div className="relative flex items-center">
                  <span className="absolute left-3.5 text-text-dim pointer-events-none">
                    <UserIcon className="w-4 h-4" />
                  </span>
                  <input
                    id="name"
                    type="text"
                    value={name}
                    onChange={(e) => {
                      setName(e.target.value);
                      if (errorMsg) setErrorMsg('');
                    }}
                    onBlur={() => setTouched((prev) => ({ ...prev, name: true }))}
                    placeholder="Jane Doe"
                    aria-invalid={!!nameError}
                    aria-describedby={nameError ? 'name-error' : undefined}
                    className="w-full pl-10 pr-3.5 py-2.5 text-xs sm:text-sm rounded-xl border border-border bg-surface text-text placeholder-text-dim focus-ring transition-colors shadow-xs"
                    required
                    autoComplete="name"
                  />
                </div>
                {nameError && (
                  <p id="name-error" className="mt-1 text-[11px] text-danger font-medium">
                    {nameError}
                  </p>
                )}
              </motion.div>
            )}

            {/* Email Address Field (Postel's Law: forgiving input handling & live feedback) */}
            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="block text-xs font-bold text-text" htmlFor="email">
                  Email Address
                </label>
                {email && isValidEmail(email) && (
                  <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-success">
                    <CheckCircleIcon className="w-3.5 h-3.5" />
                    Valid
                  </span>
                )}
              </div>
              <div className="relative flex items-center">
                <span className="absolute left-3.5 text-text-dim pointer-events-none">
                  <MailIcon className="w-4 h-4" />
                </span>
                <input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(e) => {
                    setEmail(e.target.value);
                    if (errorMsg) setErrorMsg('');
                  }}
                  onBlur={() => {
                    setTouched((prev) => ({ ...prev, email: true }));
                    // Postel's Law: Trim trailing/leading whitespace gracefully
                    setEmail((prev) => prev.trim());
                  }}
                  placeholder="user@college.example"
                  aria-invalid={!!emailError}
                  aria-describedby={emailError ? 'email-error' : undefined}
                  className="w-full pl-10 pr-9 py-2.5 text-xs sm:text-sm rounded-xl border border-border bg-surface text-text placeholder-text-dim focus-ring transition-colors shadow-xs"
                  required
                  autoComplete="email"
                  autoCapitalize="none"
                  spellCheck="false"
                />
              </div>
              {emailError && (
                <p id="email-error" className="mt-1 text-[11px] text-danger font-medium">
                  {emailError}
                </p>
              )}
            </div>

            {/* Password Field (Jakob's Law: standard Show/Hide toggle & microcopy) */}
            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="block text-xs font-bold text-text" htmlFor="password">
                  Password
                </label>
                {isRegister && (
                  <span className="text-[11px] text-text-dim">
                    Min. 6 characters
                  </span>
                )}
              </div>
              <div className="relative flex items-center">
                <span className="absolute left-3.5 text-text-dim pointer-events-none">
                  <LockIcon className="w-4 h-4" />
                </span>
                <input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => {
                    setPassword(e.target.value);
                    if (errorMsg) setErrorMsg('');
                  }}
                  onBlur={() => setTouched((prev) => ({ ...prev, password: true }))}
                  placeholder="••••••••"
                  aria-invalid={!!passwordError}
                  aria-describedby={passwordError ? 'password-error' : undefined}
                  className="w-full pl-10 pr-10 py-2.5 text-xs sm:text-sm rounded-xl border border-border bg-surface text-text placeholder-text-dim focus-ring transition-colors shadow-xs"
                  required
                  autoComplete={isRegister ? 'new-password' : 'current-password'}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((prev) => !prev)}
                  className="absolute right-3 p-1 text-text-dim hover:text-text rounded-md focus-ring transition-colors"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                  title={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? (
                    <EyeOffIcon className="w-4 h-4" />
                  ) : (
                    <EyeIcon className="w-4 h-4" />
                  )}
                </button>
              </div>
              {passwordError && (
                <p id="password-error" className="mt-1 text-[11px] text-danger font-medium">
                  {passwordError}
                </p>
              )}
            </div>

            {/* Primary Submit Button (Von Restorff Effect & Doherty Threshold) */}
            <div className="pt-2">
              <StatefulButton
                type="submit"
                disabled={isSubmitting}
                loading={isSubmitting}
                className="w-full py-3 text-xs sm:text-sm font-bold rounded-xl shadow-md hover:shadow-lg transition-all"
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
import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import type { User } from '../api/types';
import { api } from '../api/endpoints';
import {
  clearStoredCredentials,
  encodeBasicAuth,
  getStoredCredentials,
  setErrorToastHandler,
  setLoginAttemptFlag,
  setStoredCredentials,
  setUnauthorizedHandler,
} from '../api/client';

export interface ToastInfo {
  id: number;
  message: string;
  type: 'error' | 'success' | 'info';
}

interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  toasts: ToastInfo[];
  login: (email: string, password: string) => Promise<User>;
  register: (name: string, email: string, password: string) => Promise<User>;
  logout: () => void;
  showToast: (message: string, type?: 'error' | 'success' | 'info') => void;
  removeToast: (id: number) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

let toastCounter = 0;

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [toasts, setToasts] = useState<ToastInfo[]>([]);

  const removeToast = useCallback((id: number) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const showToast = useCallback(
    (message: string, type: 'error' | 'success' | 'info' = 'error') => {
      const id = ++toastCounter;
      setToasts((prev) => [...prev, { id, message, type }]);
      setTimeout(() => {
        setToasts((prev) => prev.filter((t) => t.id !== id));
      }, 5000);
    },
    []
  );

  const logout = useCallback(() => {
    clearStoredCredentials();
    setUser(null);
  }, []);

  // Configure global API handlers once
  useEffect(() => {
    setUnauthorizedHandler(() => {
      // Called when any non-login request returns 401 (session expired)
      clearStoredCredentials();
      setUser(null);
    });

    setErrorToastHandler((errorMessage: string) => {
      showToast(errorMessage, 'error');
    });
  }, [showToast]);

  // Restore session on page load
  useEffect(() => {
    const stored = getStoredCredentials();
    if (!stored) {
      setIsLoading(false);
      return;
    }

    api.auth
      .me()
      .then((u) => {
        setUser(u);
      })
      .catch(() => {
        // Stored credentials no longer valid
        clearStoredCredentials();
        setUser(null);
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, []);

  const login = async (email: string, password: string): Promise<User> => {
    const creds = encodeBasicAuth(email, password);

    // Store credentials so the request() call picks them up
    setStoredCredentials(creds);

    // Flag that this is a login attempt so the 401 handler doesn't fire
    setLoginAttemptFlag(true);

    try {
      const profile = await api.auth.me();
      setUser(profile);
      return profile;
    } catch (err: unknown) {
      // Login failed — clean up
      clearStoredCredentials();
      setUser(null);

      // Extract a readable message and show it as a toast
      let msg = 'Login failed. Please check your email and password.';
      if (err && typeof err === 'object' && 'errorMessage' in err) {
        const apiErr = err as { errorMessage: string };
        if (apiErr.errorMessage) msg = apiErr.errorMessage;
      } else if (err instanceof Error && err.message) {
        msg = err.message;
      }
      showToast(msg, 'error');

      throw err;
    } finally {
      setLoginAttemptFlag(false);
    }
  };

  const register = async (name: string, email: string, password: string): Promise<User> => {
    // POST /api/auth/register (no auth required)
    await api.auth.register({ name, email, password });
    // Automatically sign in with the new credentials
    return login(email, password);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isLoading,
        toasts,
        login,
        register,
        logout,
        showToast,
        removeToast,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

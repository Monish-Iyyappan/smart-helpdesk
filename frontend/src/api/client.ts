/**
 * HTTP Client with Basic Auth using sessionStorage only.
 * Keeps all API code free of styling and UI components.
 */

// In dev mode, Vite proxies /api/* to http://localhost:8080 (see vite.config.ts).
// In production builds, set VITE_API_URL to your deployed backend URL.
const API_BASE = (import.meta.env.VITE_API_URL as string) || '';
const CREDENTIALS_KEY = 'smart_helpdesk_basic_auth';

type UnauthorizedHandler = () => void;
type ErrorToastHandler = (message: string) => void;

let unauthorizedHandler: UnauthorizedHandler | null = null;
let errorToastHandler: ErrorToastHandler | null = null;

// When true, a login attempt is in progress — suppress the global 401 handler
// so that wrong-password 401s don't forcibly clear state mid-login.
let isLoginAttempt = false;

export function setUnauthorizedHandler(handler: UnauthorizedHandler): void {
  unauthorizedHandler = handler;
}

export function setErrorToastHandler(handler: ErrorToastHandler): void {
  errorToastHandler = handler;
}

export function setLoginAttemptFlag(active: boolean): void {
  isLoginAttempt = active;
}

export function getStoredCredentials(): string | null {
  try {
    return sessionStorage.getItem(CREDENTIALS_KEY);
  } catch {
    return null;
  }
}

export function setStoredCredentials(credentials: string): void {
  try {
    sessionStorage.setItem(CREDENTIALS_KEY, credentials);
  } catch {
    // sessionStorage disabled or full
  }
}

export function clearStoredCredentials(): void {
  try {
    sessionStorage.removeItem(CREDENTIALS_KEY);
  } catch {
    // sessionStorage error
  }
}

export function encodeBasicAuth(username: string, password: string): string {
  return btoa(unescape(encodeURIComponent(`${username}:${password}`)));
}

export class ApiError extends Error {
  status: number;
  statusText: string;
  errorMessage: string;
  data?: unknown;

  constructor(
    status: number,
    statusText: string,
    errorMessage: string,
    data?: unknown
  ) {
    super(errorMessage || `Request failed with status ${status}`);
    this.name = 'ApiError';
    this.status = status;
    this.statusText = statusText;
    this.errorMessage = errorMessage;
    this.data = data;
  }
}

export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const url = `${API_BASE}${path.startsWith('/') ? path : `/${path}`}`;
  const headers = new Headers(options.headers || {});

  if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  const credentials = getStoredCredentials();
  if (credentials) {
    headers.set('Authorization', `Basic ${credentials}`);
  }

  let response: Response;
  try {
    response = await fetch(url, { ...options, headers });
  } catch (networkErr) {
    const msg = 'Cannot reach the server. Make sure the backend is running on port 8080.';
    if (errorToastHandler) errorToastHandler(msg);
    throw new ApiError(0, 'Network Error', msg);
  }

  if (response.status === 401) {
    // Only trigger the global logout handler when NOT doing a login attempt.
    // During login, wrong credentials return 401 — we want to report the error
    // to the caller, not silently clear state.
    if (!isLoginAttempt) {
      clearStoredCredentials();
      if (unauthorizedHandler) {
        unauthorizedHandler();
      }
    }
    throw new ApiError(401, 'Unauthorized', 'Invalid email or password. Please try again.');
  }

  if (!response.ok) {
    let errorText = `Request failed (${response.status})`;
    try {
      const errorJson = await response.json();
      if (errorJson && typeof errorJson.error === 'string') {
        errorText = errorJson.error;
      } else if (errorJson && typeof errorJson.message === 'string') {
        errorText = errorJson.message;
      }
    } catch {
      // response was not JSON
    }

    // Show toast for 400/403/409 responses
    if ([400, 403, 409].includes(response.status) && errorToastHandler) {
      errorToastHandler(errorText);
    }

    throw new ApiError(response.status, response.statusText, errorText);
  }

  if (response.status === 204) {
    return {} as T;
  }

  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    return (await response.json()) as T;
  }

  return (await response.text()) as unknown as T;
}

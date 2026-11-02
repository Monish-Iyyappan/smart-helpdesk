/**
 * HTTP Client with Basic Auth using sessionStorage only.
 * Centralized API client that intercepts all network requests and standardizes
 * error messages into user-friendly, plain-English sentences.
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

/**
 * Checks whether candidate text is a short readable sentence from the backend,
 * rejecting HTML markup, raw JSON, stack traces, HTTP status phrases, and technical tokens.
 */
function extractReadableSentence(raw: unknown): string | null {
  if (typeof raw !== 'string') return null;
  const trimmed = raw.trim();

  // Too short or unreasonably long to be a single friendly sentence
  if (trimmed.length < 3 || trimmed.length > 200) return null;

  // Reject HTML content (e.g. 502/504 gateway error pages or Spring whitelabel pages)
  if (/<[a-z][\s\S]*>/i.test(trimmed) || trimmed.includes('<!DOCTYPE') || trimmed.includes('<html')) {
    return null;
  }

  // Reject raw JSON objects/arrays
  if (trimmed.startsWith('{') || trimmed.startsWith('[') || trimmed === '[object Object]') {
    return null;
  }

  // Reject Java/Node stack traces
  if (
    trimmed.includes('at com.') ||
    trimmed.includes('at org.') ||
    trimmed.includes('Exception:') ||
    trimmed.includes('Traceback') ||
    /\bat [a-zA-Z0-9_.]+\([a-zA-Z0-9_.]+\.java:\d+\)/.test(trimmed)
  ) {
    return null;
  }

  // Reject raw HTTP status phrases and boilerplates
  if (
    /^(request failed with status code|status code \d{3}|\d{3} (bad gateway|gateway timeout|service unavailable|internal server error|not found|unauthorized|forbidden|bad request))/i.test(trimmed)
  ) {
    return null;
  }

  // Reject generic single HTTP reason words that provide no user context
  if (/^(bad request|unauthorized|forbidden|not found|conflict|unprocessable entity|internal server error)$/i.test(trimmed)) {
    return null;
  }

  // Reject generic network or code errors
  if (/^(failed to fetch|network error|econnrefused|err_name_not_resolved)$/i.test(trimmed)) {
    return null;
  }

  return trimmed;
}

/**
 * Maps an HTTP status code (and optional candidate backend text) to a standardized
 * plain-English message following user requirements.
 */
export function mapStatusToMessage(
  status: number,
  candidateText?: unknown,
  isLogin: boolean = false
): string {
  // 1. No response / network error / timeout
  if (status === 0) {
    return 'Cannot connect to the server. Check your internet connection or try again in a moment.';
  }

  // 2. Gateway / Server starting up
  if (status === 502 || status === 503 || status === 504) {
    return 'The server is starting up or temporarily unavailable. Please wait a few seconds and try again.';
  }

  // 3. Unauthorized
  if (status === 401) {
    if (isLogin) {
      return 'Invalid email or password.';
    }
    return 'Your session has expired. Please log in again.';
  }

  // 4. Forbidden
  if (status === 403) {
    return "You don't have permission to do this.";
  }

  // 5. Not Found
  if (status === 404) {
    return "We couldn't find what you were looking for.";
  }

  // 6. Validation / Conflict / Unprocessable Entity
  if (status === 400 || status === 409 || status === 422) {
    const readable = extractReadableSentence(candidateText);
    if (readable) {
      return readable;
    }
    return 'Please check your input and try again.';
  }

  // 7. Rate Limiting
  if (status === 429) {
    return 'Too many attempts. Please wait a moment and try again.';
  }

  // 8. 500 or any other unmapped status
  return 'Something went wrong on our side. Please try again.';
}

/**
 * Checks if an error is a server or network failure that can benefit from a "Try again" action.
 */
export function isRetryableError(error: unknown): boolean {
  if (error instanceof ApiError) {
    return error.isNetworkOrServerError;
  }
  if (error && typeof error === 'object' && 'status' in error) {
    const s = Number((error as any).status);
    return s === 0 || [500, 502, 503, 504].includes(s);
  }
  if (error instanceof Error) {
    const msg = error.message.toLowerCase();
    return (
      msg.includes('network') ||
      msg.includes('failed to fetch') ||
      msg.includes('connection') ||
      msg.includes('timeout') ||
      msg.includes('unavailable') ||
      msg.includes('starting up')
    );
  }
  return false;
}

/**
 * Single helper function to get a user-friendly error message from any error object,
 * ApiError, Error, or raw input.
 * Guarantees that users never see status codes, stack traces, JSON, or HTML.
 */
export function getErrorMessage(error: unknown): string {
  // If it's an ApiError, its message is already mapped and sanitized
  if (error instanceof ApiError) {
    return error.errorMessage;
  }

  // If it has a status property (e.g. from axios or custom error)
  if (error && typeof error === 'object' && 'status' in error) {
    const status = Number((error as any).status);
    const candidate = (error as any).errorMessage || (error as any).message;
    return mapStatusToMessage(status, candidate);
  }

  // If it's a standard JS error
  if (error instanceof Error) {
    const msg = error.message || '';
    const lower = msg.toLowerCase();

    if (
      lower.includes('network') ||
      lower.includes('failed to fetch') ||
      lower.includes('load failed') ||
      lower.includes('connection refused') ||
      lower.includes('timeout') ||
      lower.includes('cannot connect')
    ) {
      return 'Cannot connect to the server. Check your internet connection or try again in a moment.';
    }

    const clean = extractReadableSentence(msg);
    if (clean) return clean;

    return 'Something went wrong on our side. Please try again.';
  }

  // If it's a plain string
  if (typeof error === 'string') {
    const clean = extractReadableSentence(error);
    if (clean) return clean;
  }

  return 'Something went wrong on our side. Please try again.';
}

export class ApiError extends Error {
  status: number;
  statusText: string;
  errorMessage: string;
  isNetworkOrServerError: boolean;
  isLoginAttempt: boolean;
  data?: unknown;

  constructor(
    status: number,
    statusText: string,
    userFacingMessage: string,
    isLogin: boolean = false,
    data?: unknown
  ) {
    super(userFacingMessage);
    this.name = 'ApiError';
    this.status = status;
    this.statusText = statusText;
    this.errorMessage = userFacingMessage;
    this.isLoginAttempt = isLogin;
    // Network errors (0) and 5xx server errors can be retried
    this.isNetworkOrServerError = status === 0 || [500, 502, 503, 504].includes(status);
    this.data = data;
  }
}

/**
 * The single gateway for all API requests throughout the React frontend.
 * Intercepts responses, sanitizes payloads, and throws structured ApiError instances.
 */
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
  } catch {
    // Fetch failed due to network failure, connection refused, or timeout
    const networkMessage = mapStatusToMessage(0);
    throw new ApiError(0, 'Network Error', networkMessage, isLoginAttempt);
  }

  // Handle 401 Unauthorized
  if (response.status === 401) {
    const authMessage = mapStatusToMessage(401, undefined, isLoginAttempt);

    // On non-login requests, 401 means the stored session has expired
    if (!isLoginAttempt) {
      clearStoredCredentials();
      if (unauthorizedHandler) {
        unauthorizedHandler();
      }
      if (errorToastHandler) {
        errorToastHandler(authMessage);
      }
    }

    throw new ApiError(401, 'Unauthorized', authMessage, isLoginAttempt);
  }

  // Handle other non-2xx responses
  if (!response.ok) {
    let candidateMessage: string | undefined;

    // Try parsing JSON error response
    const contentType = response.headers.get('content-type') || '';
    if (contentType.includes('application/json')) {
      try {
        const errorJson = await response.json();
        if (errorJson && typeof errorJson === 'object') {
          if (typeof errorJson.message === 'string' && errorJson.message.trim()) {
            candidateMessage = errorJson.message.trim();
          } else if (typeof errorJson.error === 'string' && errorJson.error.trim()) {
            candidateMessage = errorJson.error.trim();
          } else if (Array.isArray(errorJson.errors) && errorJson.errors[0]?.defaultMessage) {
            candidateMessage = errorJson.errors[0].defaultMessage;
          }
        }
      } catch {
        // Not valid JSON
      }
    } else {
      try {
        const rawText = await response.text();
        // If the body is HTML (e.g. 502 Bad Gateway nginx page), ignore it completely
        if (!rawText.includes('<html') && !rawText.includes('<!DOCTYPE') && !rawText.includes('<body')) {
          candidateMessage = rawText.trim();
        }
      } catch {
        // Failed to read text
      }
    }

    const friendlyMessage = mapStatusToMessage(response.status, candidateMessage, isLoginAttempt);

    // If an error toast handler is registered and not already handled by a modal/form,
    // toast unexpected errors (excluding login attempts, which are displayed inline on LoginPage)
    if (!isLoginAttempt && errorToastHandler && [403, 404, 429].includes(response.status)) {
      errorToastHandler(friendlyMessage);
    }

    throw new ApiError(response.status, response.statusText, friendlyMessage, isLoginAttempt);
  }

  // 204 No Content
  if (response.status === 204) {
    return {} as T;
  }

  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    return (await response.json()) as T;
  }

  return (await response.text()) as unknown as T;
}

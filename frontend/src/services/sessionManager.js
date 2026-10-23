/**
 * MediGuide Care - Session Storage Manager
 *
 * Provides tab-scoped session continuity using browser sessionStorage.
 *
 * Security & Design Principles:
 * - Tab-scoped: Token survives page reloads (F5) in the current tab, but is
 *   automatically cleared when the tab is closed.
 * - Stores ONLY the in-memory JWT accessToken and minimal safe user fields
 *   (id, fullName, email, role).
 * - Never stores passwords, password hashes, or secrets.
 * - Strictly validates roles: only 'PATIENT' and 'DOCTOR' are permitted.
 * - Never stores anything in localStorage.
 * - Fails safely: malformed, missing, or unsupported data clears storage
 *   and restores the signed-out state.
 *
 * NOTE (XSS Risk Disclosure):
 * Like any client-side JavaScript storage (localStorage or sessionStorage),
 * bearer tokens in sessionStorage are accessible to scripts running in the origin.
 * Backend authorization filters remain the authoritative security boundary.
 */

export const SESSION_TOKEN_KEY = 'mediguide_auth_token';
export const SESSION_USER_KEY = 'mediguide_auth_user';

export const SUPPORTED_ROLES = Object.freeze(['PATIENT', 'DOCTOR']);

/**
 * Validates that a user object contains expected fields and a recognized role.
 *
 * @param {any} user
 * @returns {boolean}
 */
export function isValidUser(user) {
  if (!user || typeof user !== 'object') {
    return false;
  }
  if (user.id === undefined || user.id === null) {
    return false;
  }
  if (typeof user.fullName !== 'string' || user.fullName.trim().length === 0) {
    return false;
  }
  if (typeof user.email !== 'string' || user.email.trim().length === 0) {
    return false;
  }
  if (!SUPPORTED_ROLES.includes(user.role)) {
    return false;
  }
  return true;
}

/**
 * Persists authenticated session data into tab-scoped sessionStorage.
 *
 * @param {Object} params
 * @param {string} params.token - JWT access token string
 * @param {Object} params.user - User profile { id, fullName, email, role }
 */
export function saveSession({ token, user }) {
  if (typeof window === 'undefined' || !window.sessionStorage) {
    return;
  }

  if (!token || typeof token !== 'string' || token.trim().length === 0 || !isValidUser(user)) {
    clearSession();
    return;
  }

  try {
    const safeUser = {
      id: user.id,
      publicId: user.publicId ? String(user.publicId).trim() : null,
      fullName: user.fullName.trim(),
      email: user.email.trim(),
      role: user.role,
    };
    window.sessionStorage.setItem(SESSION_TOKEN_KEY, token.trim());
    window.sessionStorage.setItem(SESSION_USER_KEY, JSON.stringify(safeUser));
  } catch {
    clearSession();
  }
}

/**
 * Purges authentication tokens and user information from sessionStorage.
 */
export function clearSession() {
  if (typeof window === 'undefined' || !window.sessionStorage) {
    return;
  }
  try {
    window.sessionStorage.removeItem(SESSION_TOKEN_KEY);
    window.sessionStorage.removeItem(SESSION_USER_KEY);
  } catch {
    // Gracefully handle storage errors
  }
}

/**
 * Retrieves and validates the current session from sessionStorage.
 * If data is missing, corrupted, or specifies an unsupported role,
 * storage is purged and null is returned.
 *
 * @returns {{ isAuthenticated: boolean, token: string, user: Object } | null}
 */
export function loadStoredSession() {
  if (typeof window === 'undefined' || !window.sessionStorage) {
    return null;
  }

  try {
    const storedToken = window.sessionStorage.getItem(SESSION_TOKEN_KEY);
    const storedUserRaw = window.sessionStorage.getItem(SESSION_USER_KEY);

    // If neither exists, clean return
    if (!storedToken && !storedUserRaw) {
      return null;
    }

    // Incomplete storage state (one exists without the other)
    if (!storedToken || !storedUserRaw) {
      clearSession();
      return null;
    }

    if (typeof storedToken !== 'string' || storedToken.trim().length === 0) {
      clearSession();
      return null;
    }

    let parsedUser;
    try {
      parsedUser = JSON.parse(storedUserRaw);
    } catch {
      // Malformed JSON payload
      clearSession();
      return null;
    }

    if (!isValidUser(parsedUser)) {
      clearSession();
      return null;
    }

    return {
      isAuthenticated: true,
      token: storedToken.trim(),
      user: {
        id: parsedUser.id,
        publicId: parsedUser.publicId || null,
        fullName: parsedUser.fullName,
        email: parsedUser.email,
        role: parsedUser.role,
      },
    };
  } catch {
    clearSession();
    return null;
  }
}

/**
 * MediGuide Care - Authentication API Service
 * Handles communication with Spring Boot authentication and verification endpoints:
 * - POST /api/v1/auth/register
 * - POST /api/v1/auth/login
 * - GET  /api/v1/test/protected
 *
 * Utilizes the central, reusable apiClient for consistent header handling,
 * error extraction, and in-memory JWT bearer authentication.
 */

import { apiClient, API_BASE_URL, verifyProtectedToken } from './apiClient';

/**
 * Registers a new PATIENT or DOCTOR user account.
 *
 * @param {Object} data - { fullName, email, password, role }
 * @returns {Promise<Object>} Safe user response { id, fullName, email, role }
 */
export async function registerUser({ fullName, email, password, role }) {
  return apiClient('/api/v1/auth/register', {
    method: 'POST',
    body: {
      fullName: fullName ? fullName.trim() : '',
      email: email ? email.trim() : '',
      password,
      role,
    },
  });
}

/**
 * Authenticates user credentials and receives a signed JWT access token.
 *
 * @param {Object} credentials - { email, password }
 * @returns {Promise<Object>} Login response { accessToken, tokenType, expiresIn, user }
 */
export async function loginUser({ email, password }) {
  return apiClient('/api/v1/auth/login', {
    method: 'POST',
    body: {
      email: email ? email.trim() : '',
      password,
    },
  });
}

/**
 * Verifies an email address using a single-use verification token.
 *
 * @param {string} token - Raw verification token
 * @returns {Promise<Object>} { message, verified, email }
 */
export async function verifyEmail(token) {
  return apiClient('/api/v1/auth/verify-email', {
    method: 'POST',
    body: { token },
  });
}

/**
 * Requests a new email verification link with rate limiting.
 *
 * @param {string} email - User email address
 * @returns {Promise<Object>} { message }
 */
export async function resendVerification(email) {
  return apiClient('/api/v1/auth/resend-verification', {
    method: 'POST',
    body: { email: email ? email.trim() : '' },
  });
}

export { apiClient, API_BASE_URL, verifyProtectedToken };

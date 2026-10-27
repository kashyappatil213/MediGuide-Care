/**
 * MediGuide Care - Reusable Authenticated API Client
 *
 * Lightweight, zero-dependency client built on the browser Fetch API.
 * - Dispatches authenticated requests using the in-memory JWT bearer token.
 * - Prevents token leakage to third-party endpoints.
 * - Sets content headers conditionally only when a payload is provided.
 * - Gracefully parses empty, text, or JSON responses without crashing.
 * - Normalizes non-2xx errors with HTTP status codes and safe user messages.
 * - Never logs tokens, passwords, or private response payloads.
 */

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ||
  (typeof window !== 'undefined' && window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1'
    ? 'https://f39aa767c13738.lhr.life'
    : 'http://localhost:8080');

/**
 * Sends an HTTP request to the MediGuide Care API.
 *
 * @param {string} endpoint - Relative API route (e.g. '/api/v1/test/protected')
 * @param {Object} [options]
 * @param {string} [options.method='GET'] - HTTP method (GET, POST, PUT, DELETE)
 * @param {any} [options.body=null] - Request payload object or string
 * @param {string|null} [options.token=null] - In-memory JWT access token
 * @param {Object} [options.headers={}] - Additional custom request headers
 * @returns {Promise<any>} Parsed response data, or null for empty responses
 */
export async function apiClient(endpoint, {
  method = 'GET',
  body = null,
  token = null,
  headers = {},
} = {}) {
  // Security guard: Ensure tokens and requests are only sent to the configured backend base URL
  let targetUrl;
  if (endpoint.startsWith('http://') || endpoint.startsWith('https://')) {
    if (!endpoint.startsWith(API_BASE_URL)) {
      throw new Error('Security Error: Bearer tokens cannot be dispatched to untrusted external URLs.');
    }
    targetUrl = endpoint;
  } else {
    const cleanEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
    targetUrl = `${API_BASE_URL}${cleanEndpoint}`;
  }

  const requestHeaders = {
    'Accept': 'application/json',
    'bypass-tunnel-reminder': 'true',
    ...headers,
  };

  // Attach Authorization: Bearer <token> only if token is present
  if (token && typeof token === 'string' && token.trim().length > 0) {
    requestHeaders['Authorization'] = `Bearer ${token.trim()}`;
  }

  const fetchOptions = {
    method: method.toUpperCase(),
    headers: requestHeaders,
  };

  // Only attach Content-Type: application/json if there is an actual request payload
  if (body !== null && body !== undefined) {
    requestHeaders['Content-Type'] = 'application/json';
    fetchOptions.body = typeof body === 'object' ? JSON.stringify(body) : String(body);
  }

  let response;
  try {
    response = await fetch(targetUrl, fetchOptions);
  } catch {
    // Normalise network / connection refused errors
    const networkError = new Error(
      `Unable to connect to MediGuide backend at ${API_BASE_URL}. Ensure the Spring Boot server is online.`
    );
    networkError.status = 0;
    throw networkError;
  }

  // Safely extract and parse response body without throwing on empty payloads
  let data = null;
  const responseText = await response.text();
  if (responseText && responseText.trim().length > 0) {
    try {
      data = JSON.parse(responseText);
    } catch {
      data = responseText;
    }
  }

  // Handle non-2xx HTTP responses
  if (!response.ok) {
    const message = (data && typeof data === 'object' && data.message)
      ? data.message
      : `Request failed with HTTP status ${response.status}`;
    const error = new Error(message);
    error.status = response.status;
    error.fieldErrors = (data && typeof data === 'object' && data.fieldErrors) ? data.fieldErrors : null;
    error.data = data;
    throw error;
  }

  return data;
}

/**
 * Sends an authenticated GET request to the backend verification endpoint.
 *
 * @param {string} token - The in-memory JWT bearer token
 * @returns {Promise<Object>} Protected response { status, message, userId, email, role }
 */
export async function verifyProtectedToken(token) {
  if (!token) {
    const error = new Error('No authentication token provided in React memory.');
    error.status = 401;
    throw error;
  }
  return apiClient('/api/v1/test/protected', {
    method: 'GET',
    token,
  });
}

/**
 * MediGuide Care - Date and Time Utility Functions
 *
 * Helpers for formatting ISO-8601 timestamps from the Spring Boot backend
 * and converting to/from HTML datetime-local input values.
 */

/**
 * Formats an ISO-8601 string into a human-readable clinic date-time string.
 *
 * @param {string} isoString - e.g. "2026-10-20T09:00:00"
 * @returns {string} e.g. "Oct 20, 2026, 09:00 AM"
 */
export function formatDateTime(isoString) {
  if (!isoString) return '—';
  try {
    const date = new Date(isoString);
    if (isNaN(date.getTime())) return isoString;
    return date.toLocaleString(undefined, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return isoString;
  }
}

/**
 * Converts an ISO-8601 string or Date into "YYYY-MM-DDTHH:mm" for HTML datetime-local input.
 *
 * @param {string|Date} value
 * @returns {string}
 */
export function toInputDateTime(value) {
  if (!value) return '';
  if (value instanceof Date) {
    const pad = (n) => String(n).padStart(2, '0');
    const yyyy = value.getFullYear();
    const mm = pad(value.getMonth() + 1);
    const dd = pad(value.getDate());
    const hh = pad(value.getHours());
    const mi = pad(value.getMinutes());
    return `${yyyy}-${mm}-${dd}T${hh}:${mi}`;
  }
  if (typeof value === 'string') {
    return value.substring(0, 16);
  }
  return '';
}

/**
 * Converts a datetime-local input value ("YYYY-MM-DDTHH:mm") into a complete
 * ISO-8601 string ("YYYY-MM-DDTHH:mm:ss") expected by Spring Boot's LocalDateTime deserializer.
 *
 * @param {string} inputValue
 * @returns {string}
 */
export function toIsoLocalDateTime(inputValue) {
  if (!inputValue) return '';
  const trimmed = inputValue.trim();
  if (trimmed.length === 16) {
    return `${trimmed}:00`;
  }
  return trimmed;
}

/**
 * Generates default future datetime-local strings for convenient form pre-filling.
 * Defaults to tomorrow at 09:00 AM and 11:00 AM.
 *
 * @returns {{ defaultStart: string, defaultEnd: string }}
 */
export function getDefaultFutureInterval() {
  const start = new Date();
  start.setDate(start.getDate() + 1);
  start.setHours(9, 0, 0, 0);

  const end = new Date(start);
  end.setHours(12, 0, 0, 0);

  return {
    defaultStart: toInputDateTime(start),
    defaultEnd: toInputDateTime(end),
  };
}

/**
 * Returns a local date key in "YYYY-MM-DD" format from a Date or ISO string.
 * Avoids UTC timezone shifts by reading local year, month, and date.
 *
 * @param {string|Date} value
 * @returns {string} e.g. "2026-10-20"
 */
export function toDateKey(value) {
  if (!value) return '';
  const d = value instanceof Date ? value : new Date(value);
  if (isNaN(d.getTime())) {
    if (typeof value === 'string' && value.length >= 10) {
      return value.substring(0, 10);
    }
    return '';
  }
  const pad = (n) => String(n).padStart(2, '0');
  const yyyy = d.getFullYear();
  const mm = pad(d.getMonth() + 1);
  const dd = pad(d.getDate());
  return `${yyyy}-${mm}-${dd}`;
}

/**
 * Formats an ISO-8601 string to display time only.
 *
 * @param {string} isoString - e.g. "2026-10-20T09:00:00"
 * @returns {string} e.g. "09:00 AM"
 */
export function formatTimeOnly(isoString) {
  if (!isoString) return '—';
  try {
    const d = new Date(isoString);
    if (isNaN(d.getTime())) return isoString;
    return d.toLocaleTimeString(undefined, {
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return isoString;
  }
}

/**
 * Formats a Date or ISO string into a human-readable clinic date string.
 *
 * @param {string|Date} value
 * @returns {string} e.g. "Tuesday, Oct 20, 2026"
 */
export function formatDateOnly(value) {
  if (!value) return '—';
  try {
    const d = value instanceof Date ? value : new Date(value);
    if (isNaN(d.getTime())) return String(value);
    return d.toLocaleDateString(undefined, {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  } catch {
    return String(value);
  }
}

/**
 * Combines a "YYYY-MM-DD" date string and a "HH:mm" time string into
 * a complete ISO-8601 string ("YYYY-MM-DDTHH:mm:ss") for Spring Boot.
 *
 * @param {string} dateKey - "YYYY-MM-DD"
 * @param {string} timeStr - "HH:mm"
 * @returns {string} e.g. "2026-10-20T09:00:00"
 */
export function toIsoFromDateAndTime(dateKey, timeStr) {
  if (!dateKey || !timeStr) return '';
  const trimmedTime = timeStr.trim();
  const formattedTime = trimmedTime.length === 5 ? `${trimmedTime}:00` : trimmedTime;
  return `${dateKey.trim()}T${formattedTime}`;
}


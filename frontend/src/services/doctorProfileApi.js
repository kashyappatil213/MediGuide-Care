/**
 * MediGuide Care - Doctor Profile and Specialty/Location Search API Service
 *
 * Provides API methods for:
 * - Doctor: Fetching and updating their own clinic profile
 * - Patient / Authenticated user: Searching published doctor profiles with filters
 * - Patient / Authenticated user: Fetching a public doctor profile by MediGuide public ID
 */

import { apiClient } from './apiClient';

/**
 * Retrieves the profile of the currently authenticated doctor.
 *
 * @param {string} token - In-memory JWT access token
 * @returns {Promise<Object>} DoctorProfileResponse
 */
export async function getMyDoctorProfile(token) {
  return apiClient('/api/v1/doctor/profile', {
    method: 'GET',
    token,
  });
}

/**
 * Creates or updates the clinical and clinic profile of the authenticated doctor.
 *
 * @param {string} token - In-memory JWT access token
 * @param {Object} profileData
 * @param {string} profileData.specialty - Doctor specialty (e.g. "Cardiology")
 * @param {string} profileData.clinicName - Clinic/hospital name
 * @param {string} [profileData.clinicAddress] - Optional physical address
 * @param {string} profileData.city - City
 * @param {string} [profileData.area] - Optional area/locality
 * @param {string} [profileData.pinCode] - Optional 6-digit Indian PIN code
 * @param {string} [profileData.clinicPhone] - Optional public clinic contact phone
 * @param {string} [profileData.personalPhone] - Optional private doctor personal phone
 * @returns {Promise<Object>} Updated DoctorProfileResponse
 */
export async function updateMyDoctorProfile(token, profileData) {
  return apiClient('/api/v1/doctor/profile', {
    method: 'PUT',
    token,
    body: profileData,
  });
}

/**
 * Searches published doctor profiles by specialty, city, area, and/or PIN code.
 *
 * @param {string} token - In-memory JWT access token
 * @param {Object} [filters={}]
 * @param {string} [filters.specialty]
 * @param {string} [filters.city]
 * @param {string} [filters.area]
 * @param {string} [filters.pinCode]
 * @param {number} [filters.page=0]
 * @param {number} [filters.size=20]
 * @returns {Promise<Array<Object>>} List of PublicDoctorProfileResponse objects
 */
export async function searchDoctors(token, filters = {}) {
  const params = new URLSearchParams();

  if (filters.specialty && filters.specialty.trim()) {
    params.set('specialty', filters.specialty.trim());
  }
  if (filters.city && filters.city.trim()) {
    params.set('city', filters.city.trim());
  }
  if (filters.area && filters.area.trim()) {
    params.set('area', filters.area.trim());
  }
  if (filters.pinCode && filters.pinCode.trim()) {
    params.set('pinCode', filters.pinCode.trim());
  }
  if (filters.page !== undefined && filters.page !== null) {
    params.set('page', String(filters.page));
  }
  if (filters.size !== undefined && filters.size !== null) {
    params.set('size', String(filters.size));
  }

  const queryString = params.toString();
  const endpoint = queryString ? `/api/v1/doctors/search?${queryString}` : '/api/v1/doctors/search';

  return apiClient(endpoint, {
    method: 'GET',
    token,
  });
}

/**
 * Retrieves a single doctor's public profile by their public MediGuide ID.
 *
 * @param {string} token - In-memory JWT access token
 * @param {string} publicId - Public MediGuide ID (e.g. "DOC-3P8N61")
 * @returns {Promise<Object>} PublicDoctorProfileResponse
 */
export async function getDoctorByPublicId(token, publicId) {
  return apiClient(`/api/v1/doctors/${encodeURIComponent(publicId)}`, {
    method: 'GET',
    token,
  });
}

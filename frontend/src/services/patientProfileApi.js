/**
 * MediGuide Care - Patient Profile API Service
 *
 * Provides service methods for authenticated patients to view and update
 * their personal profile details (e.g. contact phone number).
 */

import { apiClient } from './apiClient';

/**
 * Retrieves the profile of the currently authenticated patient.
 *
 * @param {string} token - In-memory JWT access token
 * @returns {Promise<Object>} PatientProfileResponse { publicId, fullName, email, phoneNumber }
 */
export async function getPatientProfile(token) {
  return apiClient('/api/v1/patient/profile', {
    method: 'GET',
    token,
  });
}

/**
 * Updates the contact phone number of the authenticated patient.
 *
 * @param {string} token - In-memory JWT access token
 * @param {Object} profileData
 * @param {string} [profileData.phoneNumber] - Updated contact phone number
 * @returns {Promise<Object>} Updated PatientProfileResponse
 */
export async function updatePatientProfile(token, { phoneNumber }) {
  return apiClient('/api/v1/patient/profile', {
    method: 'PUT',
    token,
    body: { phoneNumber },
  });
}

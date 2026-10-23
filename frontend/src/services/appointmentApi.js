/**
 * MediGuide Care - Appointment and Availability API Service
 *
 * Provides service methods for interacting with Spring Boot appointment
 * and doctor availability endpoints:
 * - Patient: discover active availability slots
 * - Patient: book an appointment
 * - Patient & Doctor: retrieve personal appointments
 * - Patient & Assigned Doctor: cancel an appointment
 * - Doctor: create availability slot
 * - Doctor: retrieve doctor's own availability slots
 * - Doctor: deactivate an availability slot
 * - Doctor: mark an appointment as completed
 *
 * Utilizes the central, reusable apiClient for consistent Bearer-token handling,
 * header management, and error normalization.
 */

import { apiClient } from './apiClient';

/**
 * Retrieves all active future doctor availability slots across all doctors.
 * Accessible to authenticated PATIENT users.
 *
 * @param {string} token - In-memory JWT access token
 * @returns {Promise<Array<Object>>} List of AvailabilityResponse objects
 */
export async function getActiveAvailabilities(token) {
  return apiClient('/api/v1/doctor/availabilities/active', {
    method: 'GET',
    token,
  });
}

/**
 * Books a new consultation appointment for the authenticated patient.
 *
 * @param {string} token - In-memory JWT access token
 * @param {Object} params
 * @param {number} params.availabilityId - ID of the target doctor availability slot
 * @param {string} params.startTime - ISO-8601 appointment start time (e.g. "2026-10-20T10:00:00")
 * @param {string} params.endTime - ISO-8601 appointment end time (e.g. "2026-10-20T10:30:00")
 * @param {number} [params.patientId] - Optional patient ID (must match caller principal)
 * @returns {Promise<Object>} Created AppointmentResponse object
 */
export async function bookAppointment(token, { availabilityId, startTime, endTime, patientId }) {
  const body = {
    availabilityId,
    startTime,
    endTime,
  };
  if (patientId !== undefined && patientId !== null) {
    body.patientId = patientId;
  }

  return apiClient('/api/v1/appointments', {
    method: 'POST',
    token,
    body,
  });
}

/**
 * Retrieves all appointments for the currently authenticated user.
 * Returns personal booked appointments for PATIENT and assigned consultations for DOCTOR.
 *
 * @param {string} token - In-memory JWT access token
 * @returns {Promise<Array<Object>>} List of AppointmentResponse objects
 */
export async function getMyAppointments(token) {
  return apiClient('/api/v1/appointments/my', {
    method: 'GET',
    token,
  });
}

/**
 * Cancels an existing consultation appointment.
 * Permitted only for the assigned patient or assigned doctor.
 *
 * @param {string} token - In-memory JWT access token
 * @param {number} appointmentId - ID of the appointment to cancel
 * @returns {Promise<Object>} Updated AppointmentResponse with status 'CANCELLED'
 */
export async function cancelAppointment(token, appointmentId) {
  return apiClient(`/api/v1/appointments/${appointmentId}/cancel`, {
    method: 'PATCH',
    token,
  });
}

/**
 * Creates a new availability slot for the authenticated doctor.
 *
 * @param {string} token - In-memory JWT access token
 * @param {Object} params
 * @param {string} params.startTime - ISO-8601 slot start time (e.g. "2026-10-20T09:00:00")
 * @param {string} params.endTime - ISO-8601 slot end time (e.g. "2026-10-20T13:00:00")
 * @param {boolean} [params.active=true] - Initial active status
 * @param {number} [params.doctorId] - Optional doctor ID (must match caller principal)
 * @returns {Promise<Object>} Created AvailabilityResponse object
 */
export async function createAvailability(token, { startTime, endTime, active = true, doctorId }) {
  const body = {
    startTime,
    endTime,
    active,
  };
  if (doctorId !== undefined && doctorId !== null) {
    body.doctorId = doctorId;
  }

  return apiClient('/api/v1/doctor/availabilities', {
    method: 'POST',
    token,
    body,
  });
}

/**
 * Pre-schedules multiple availability time slots for the authenticated doctor in a single batch.
 *
 * @param {string} token - In-memory JWT access token
 * @param {Object} params
 * @param {Array<Object>} params.slots - Array of slot objects: { startTime, endTime, active }
 * @param {number} [params.doctorId] - Optional doctor ID (must match caller principal)
 * @returns {Promise<Object>} BatchAvailabilityResponse detailing successful and rejected slots
 */
export async function createBatchAvailability(token, { slots, doctorId }) {
  const body = {
    slots,
  };
  if (doctorId !== undefined && doctorId !== null) {
    body.doctorId = doctorId;
  }

  return apiClient('/api/v1/doctor/availabilities/batch', {
    method: 'POST',
    token,
    body,
  });
}

/**
 * Retrieves all availability slots created by the authenticated doctor.
 *
 * @param {string} token - In-memory JWT access token
 * @returns {Promise<Array<Object>>} List of doctor's AvailabilityResponse objects
 */
export async function getMyAvailabilities(token) {
  return apiClient('/api/v1/doctor/availabilities', {
    method: 'GET',
    token,
  });
}

/**
 * Deactivates an existing availability slot belonging to the authenticated doctor.
 * Fails with 409 Conflict if future booked appointments depend on this slot.
 *
 * @param {string} token - In-memory JWT access token
 * @param {number} availabilityId - ID of the slot to deactivate
 * @returns {Promise<Object>} Updated AvailabilityResponse with active=false
 */
export async function deactivateAvailability(token, availabilityId) {
  return apiClient(`/api/v1/doctor/availabilities/${availabilityId}/deactivate`, {
    method: 'PATCH',
    token,
  });
}

/**
 * Marks a booked consultation appointment as completed.
 * Permitted only for the assigned doctor.
 *
 * @param {string} token - In-memory JWT access token
 * @param {number} appointmentId - ID of the appointment to complete
 * @returns {Promise<Object>} Updated AppointmentResponse with status 'COMPLETED'
 */
export async function completeAppointment(token, appointmentId) {
  return apiClient(`/api/v1/appointments/${appointmentId}/complete`, {
    method: 'PATCH',
    token,
  });
}

/**
 * Retrieves the consultation record for a specific appointment.
 * Accessible to the assigned doctor or booked patient.
 *
 * @param {string} token - In-memory JWT access token
 * @param {number} appointmentId - ID of the appointment
 * @returns {Promise<Object>} ConsultationResponse
 */
export async function getConsultation(token, appointmentId) {
  return apiClient(`/api/v1/appointments/${appointmentId}/consultation`, {
    method: 'GET',
    token,
  });
}

/**
 * Creates a consultation record for a specific appointment.
 * Permitted only for the assigned doctor.
 *
 * @param {string} token - In-memory JWT access token
 * @param {number} appointmentId - ID of the appointment
 * @param {Object} params
 * @param {string} params.notes - Clinical notes or diagnosis
 * @param {string} [params.treatment] - Treatment or medical advice
 * @param {string} [params.followUpDate] - Recommended follow-up date ("YYYY-MM-DD")
 * @param {string} [params.followUpNotes] - Optional follow-up instructions / notes
 * @returns {Promise<Object>} Created ConsultationResponse
 */
export async function createConsultation(token, appointmentId, { notes, treatment, followUpDate, followUpNotes }) {
  const body = { notes };
  if (treatment !== undefined && treatment !== null) body.treatment = treatment;
  if (followUpDate) body.followUpDate = followUpDate;
  if (followUpNotes !== undefined && followUpNotes !== null) body.followUpNotes = followUpNotes;

  return apiClient(`/api/v1/appointments/${appointmentId}/consultation`, {
    method: 'POST',
    token,
    body,
  });
}

/**
 * Updates an existing consultation record for a specific appointment.
 * Permitted only for the assigned doctor.
 *
 * @param {string} token - In-memory JWT access token
 * @param {number} appointmentId - ID of the appointment
 * @param {Object} params
 * @param {string} params.notes - Updated clinical notes or diagnosis
 * @param {string} [params.treatment] - Updated treatment or medical advice
 * @param {string|null} [params.followUpDate] - Updated recommended follow-up date ("YYYY-MM-DD")
 * @param {string|null} [params.followUpNotes] - Updated follow-up instructions / notes
 * @returns {Promise<Object>} Updated ConsultationResponse
 */
export async function updateConsultation(token, appointmentId, { notes, treatment, followUpDate, followUpNotes }) {
  const body = { notes };
  if (treatment !== undefined && treatment !== null) body.treatment = treatment;
  body.followUpDate = followUpDate || null;
  body.followUpNotes = followUpNotes !== undefined && followUpNotes !== null ? followUpNotes : null;

  return apiClient(`/api/v1/appointments/${appointmentId}/consultation`, {
    method: 'PUT',
    token,
    body,
  });
}

/**
 * Updates only the recommended follow-up date and notes for an appointment's consultation.
 * Permitted only for the assigned doctor.
 *
 * @param {string} token - In-memory JWT access token
 * @param {number} appointmentId - ID of the appointment
 * @param {Object} params
 * @param {string|null} [params.followUpDate] - Recommended follow-up date ("YYYY-MM-DD")
 * @param {string|null} [params.followUpNotes] - Optional follow-up instructions / notes
 * @returns {Promise<Object>} Updated ConsultationResponse
 */
export async function updateFollowUp(token, appointmentId, { followUpDate, followUpNotes }) {
  return apiClient(`/api/v1/appointments/${appointmentId}/consultation/follow-up`, {
    method: 'PATCH',
    token,
    body: {
      followUpDate: followUpDate || null,
      followUpNotes: followUpNotes !== undefined && followUpNotes !== null ? followUpNotes : null,
    },
  });
}

/**
 * Adds a prescribed medication item to an appointment.
 * Permitted only for the assigned doctor.
 *
 * @param {string} token - In-memory JWT access token
 * @param {number} appointmentId - ID of the appointment
 * @param {Object} params
 * @param {string} params.medicationName - Medication name
 * @param {string} params.dosage - Dosage instructions (e.g. "500mg")
 * @param {string} params.instructions - Frequency / usage instructions
 * @param {string} [params.duration] - Course duration (e.g. "5 days")
 * @returns {Promise<Object>} Created PrescriptionResponse
 */
export async function createPrescription(token, appointmentId, { medicationName, dosage, instructions, duration }) {
  return apiClient(`/api/v1/appointments/${appointmentId}/prescription`, {
    method: 'POST',
    token,
    body: { medicationName, dosage, instructions, duration },
  });
}

/**
 * Retrieves all prescribed medications for an appointment.
 * Accessible to the assigned doctor or booked patient.
 *
 * @param {string} token - In-memory JWT access token
 * @param {number} appointmentId - ID of the appointment
 * @returns {Promise<Array<Object>>} List of PrescriptionResponse objects
 */
export async function getPrescriptions(token, appointmentId) {
  return apiClient(`/api/v1/appointments/${appointmentId}/prescription`, {
    method: 'GET',
    token,
  });
}

/**
 * Retrieves the chronological consultation history for the authenticated user.
 * For patients: returns all personal clinical consultations, newest first.
 * For doctors: returns consultations conducted by this doctor, or filters by patientId.
 *
 * @param {string} token - In-memory JWT access token
 * @param {number} [patientId] - Optional patient ID for doctors reviewing a specific patient
 * @returns {Promise<Array<Object>>} List of ConsultationHistoryResponse objects, newest first
 */
export async function getConsultationHistory(token, patientId) {
  const query = patientId ? `?patientId=${patientId}` : '';
  return apiClient(`/api/v1/consultations/history${query}`, {
    method: 'GET',
    token,
  });
}

/**
 * Requests an AI-assisted clinical history summary for an authorized patient.
 * Permitted exclusively for doctors who have consulted this patient.
 *
 * @param {string} token - In-memory JWT access token
 * @param {number} patientId - Target patient ID
 * @returns {Promise<Object>} ClinicalHistorySummaryResponse
 */
export async function generateClinicalHistorySummary(token, patientId) {
  return apiClient('/api/v1/ai/clinical-history-summary', {
    method: 'POST',
    token,
    body: { patientId },
  });
}



import { useState, useEffect, useCallback, useMemo } from 'react';
import {
  getActiveAvailabilities,
  bookAppointment,
  getMyAppointments,
  cancelAppointment,
  getConsultationHistory,
} from '../services/appointmentApi';
import { searchDoctors } from '../services/doctorProfileApi';
import { getPatientProfile, updatePatientProfile } from '../services/patientProfileApi';
import {
  formatDateTime,
  formatDateOnly,
  toInputDateTime,
  toIsoLocalDateTime,
} from '../utils/dateTimeUtils';
import PatientClinicalDrawer from './PatientClinicalDrawer';

/**
 * Patient Care Portal component for MediGuide Care.
 *
 * Implements:
 * 1. Doctor Discovery & Specialty Search: search published doctors by specialty, city, area, and PIN code.
 * 2. Active Doctor Availability Discovery: browse active future slots across all doctors or filter by selected doctor.
 * 3. Appointment Booking: select a slot, adjust desired consultation interval, and book.
 * 4. Appointment Management: view personal booked/completed/cancelled appointments and clinical drawers.
 * 5. Consultation History: chronological history of all clinical diagnoses, prescriptions, and follow-ups.
 *
 * @param {Object} props
 * @param {Object} props.user - Safe authenticated patient profile { id, fullName, email, role }
 * @param {string} props.token - In-memory JWT access token
 */
export default function PatientPortal({ user, token }) {
  // --- Doctor Discovery & Search State ---
  const [searchSpecialty, setSearchSpecialty] = useState('');
  const [searchCity, setSearchCity] = useState('');
  const [searchArea, setSearchArea] = useState('');
  const [searchPinCode, setSearchPinCode] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [hasSearched, setHasSearched] = useState(false);
  const [searchingDoctors, setSearchingDoctors] = useState(false);
  const [searchError, setSearchError] = useState(null);
  const [selectedDoctorFilter, setSelectedDoctorFilter] = useState(null);

  // --- Patient Profile State ---
  const [patientProfile, setPatientProfile] = useState(null);
  const [patientPhone, setPatientPhone] = useState('');
  const [loadingPatientProfile, setLoadingPatientProfile] = useState(false);
  const [savingPatientPhone, setSavingPatientPhone] = useState(false);
  const [patientProfileError, setPatientProfileError] = useState(null);
  const [patientProfileSuccess, setPatientProfileSuccess] = useState(null);

  // --- Availabilities State ---
  const [availabilities, setAvailabilities] = useState([]);
  const [loadingSlots, setLoadingSlots] = useState(false);
  const [slotsError, setSlotsError] = useState(null);

  // --- Appointments State ---
  const [appointments, setAppointments] = useState([]);
  const [loadingAppointments, setLoadingAppointments] = useState(false);
  const [appointmentsError, setAppointmentsError] = useState(null);

  // --- Consultation History State ---
  const [consultationHistory, setConsultationHistory] = useState([]);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [historyError, setHistoryError] = useState(null);

  // --- Booking Form State ---
  const [selectedSlot, setSelectedSlot] = useState(null);
  const [bookingStart, setBookingStart] = useState('');
  const [bookingEnd, setBookingEnd] = useState('');
  const [bookingLoading, setBookingLoading] = useState(false);
  const [bookingError, setBookingError] = useState(null);
  const [bookingSuccess, setBookingSuccess] = useState(null);

  // --- Action Loading State (Cancellation per appointment ID) ---
  const [actionLoadingId, setActionLoadingId] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);

  // --- Active Clinical Consultation Drawer State ---
  const [activeClinicalAppId, setActiveClinicalAppId] = useState(null);

  // 1. Fetch active future availabilities for booking
  const fetchAvailabilities = useCallback(async () => {
    if (!token) return;
    setLoadingSlots(true);
    setSlotsError(null);
    try {
      const data = await getActiveAvailabilities(token);
      setAvailabilities(Array.isArray(data) ? data : []);
    } catch (err) {
      setSlotsError(err.message || 'Unable to retrieve active doctor availability schedules.');
      setAvailabilities([]);
    } finally {
      setLoadingSlots(false);
    }
  }, [token]);

  // 2. Fetch patient's appointments
  const fetchAppointments = useCallback(async () => {
    if (!token) return;
    setLoadingAppointments(true);
    setAppointmentsError(null);
    try {
      const data = await getMyAppointments(token);
      setAppointments(Array.isArray(data) ? data : []);
    } catch (err) {
      setAppointmentsError(err.message || 'Unable to load personal consultation appointments.');
      setAppointments([]);
    } finally {
      setLoadingAppointments(false);
    }
  }, [token]);

  // 3. Fetch patient's consultation history
  const fetchConsultationHistory = useCallback(async () => {
    if (!token) return;
    setLoadingHistory(true);
    setHistoryError(null);
    try {
      const data = await getConsultationHistory(token);
      setConsultationHistory(Array.isArray(data) ? data : []);
    } catch (err) {
      setHistoryError(err.message || 'Unable to load consultation history.');
      setConsultationHistory([]);
    } finally {
      setLoadingHistory(false);
    }
  }, [token]);

  // 4. Fetch patient's personal profile and contact details
  const fetchPatientProfile = useCallback(async () => {
    if (!token) return;
    setLoadingPatientProfile(true);
    setPatientProfileError(null);
    try {
      const data = await getPatientProfile(token);
      if (data) {
        setPatientProfile(data);
        setPatientPhone(data.phoneNumber || '');
      }
    } catch (err) {
      setPatientProfileError(err.message || 'Unable to load personal profile.');
    } finally {
      setLoadingPatientProfile(false);
    }
  }, [token]);

  // Handle saving patient phone number
  const handleSavePatientPhone = async (e) => {
    if (e) e.preventDefault();
    setPatientProfileError(null);
    setPatientProfileSuccess(null);

    const phoneRegex = /^[+]?[0-9\s\-()]{7,20}$/;
    if (patientPhone && patientPhone.trim() && !phoneRegex.test(patientPhone.trim())) {
      setPatientProfileError('Phone number must be between 7 and 20 digits (e.g. +91 98765 43210).');
      return;
    }

    setSavingPatientPhone(true);
    try {
      const updated = await updatePatientProfile(token, {
        phoneNumber: patientPhone.trim() || '',
      });
      setPatientProfile(updated);
      setPatientPhone(updated.phoneNumber || '');
      setPatientProfileSuccess('Personal phone number updated successfully!');
    } catch (err) {
      setPatientProfileError(err.message || 'Failed to update phone number.');
    } finally {
      setSavingPatientPhone(false);
    }
  };

  // Initial load
  useEffect(() => {
    let active = true;
    const timer = setTimeout(() => {
      if (active) {
        fetchPatientProfile();
        fetchAvailabilities();
        fetchAppointments();
        fetchConsultationHistory();
      }
    }, 0);
    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, [fetchPatientProfile, fetchAvailabilities, fetchAppointments, fetchConsultationHistory]);

  // Handle doctor search submission
  const handleSearchDoctors = async (e) => {
    if (e) e.preventDefault();
    if (!token) return;

    if (searchPinCode && searchPinCode.trim()) {
      const pinRegex = /^[1-9][0-9]{5}$/;
      if (!pinRegex.test(searchPinCode.trim())) {
        setSearchError('PIN code must be a valid 6-digit Indian postal code (e.g. 560001).');
        return;
      }
    }

    setSearchError(null);
    setSearchingDoctors(true);
    try {
      const results = await searchDoctors(token, {
        specialty: searchSpecialty.trim(),
        city: searchCity.trim(),
        area: searchArea.trim(),
        pinCode: searchPinCode.trim(),
      });
      setSearchResults(Array.isArray(results) ? results : []);
      setHasSearched(true);
    } catch (err) {
      setSearchError(err.message || 'Failed to search published doctors.');
      setSearchResults([]);
    } finally {
      setSearchingDoctors(false);
    }
  };

  // Clear / reset doctor search
  const handleResetSearch = () => {
    setSearchSpecialty('');
    setSearchCity('');
    setSearchArea('');
    setSearchPinCode('');
    setSearchResults([]);
    setHasSearched(false);
    setSearchError(null);
  };

  // Connect doctor search selection to availability schedule view
  const handleSelectDoctorForBooking = (doctor) => {
    setSelectedDoctorFilter({
      publicId: doctor.publicId,
      doctorName: doctor.doctorName,
      specialty: doctor.specialty,
    });
    // Cancel any previous slot selection
    setSelectedSlot(null);
    setBookingStart('');
    setBookingEnd('');

    // Smooth scroll down to availability schedules
    const elem = document.getElementById('section-available-slots');
    if (elem) {
      elem.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  };

  const handleClearDoctorFilter = () => {
    setSelectedDoctorFilter(null);
  };

  // Filter available slots based on selected doctor (if any)
  const filteredAvailabilities = useMemo(() => {
    if (!selectedDoctorFilter) return availabilities;
    const target = selectedDoctorFilter.doctorName.toLowerCase().replace(/^dr\.?\s*/i, '').trim();
    return availabilities.filter((slot) => {
      if (!slot.doctorName) return false;
      const slotName = slot.doctorName.toLowerCase().replace(/^dr\.?\s*/i, '').trim();
      return slotName.includes(target) || target.includes(slotName);
    });
  }, [availabilities, selectedDoctorFilter]);

  // Slot selection for booking
  const handleSelectSlot = (slot) => {
    setSelectedSlot(slot);
    setBookingStart(toInputDateTime(slot.startTime));
    setBookingEnd(toInputDateTime(slot.endTime));
    setBookingError(null);
    setBookingSuccess(null);
  };

  const handleCancelSelection = () => {
    setSelectedSlot(null);
    setBookingStart('');
    setBookingEnd('');
    setBookingError(null);
  };

  // Submit appointment booking
  const handleBookSubmit = async (e) => {
    e.preventDefault();
    if (!selectedSlot || bookingLoading) return;

    setBookingError(null);
    setBookingSuccess(null);

    if (!bookingStart || !bookingEnd) {
      setBookingError('Both appointment start time and end time are required.');
      return;
    }

    const isoStart = toIsoLocalDateTime(bookingStart);
    const isoEnd = toIsoLocalDateTime(bookingEnd);

    if (new Date(isoStart) >= new Date(isoEnd)) {
      setBookingError('Appointment end time must be strictly after start time.');
      return;
    }

    setBookingLoading(true);

    try {
      const response = await bookAppointment(token, {
        availabilityId: selectedSlot.id,
        startTime: isoStart,
        endTime: isoEnd,
      });

      setBookingSuccess(
        `Appointment successfully booked with Dr. ${response.doctorName || selectedSlot.doctorName || 'Doctor'}! Reference ID #${response.id}`
      );
      setSelectedSlot(null);
      setBookingStart('');
      setBookingEnd('');

      // Refresh appointments and availability slots to reflect up-to-date schedule
      fetchAppointments();
      fetchAvailabilities();
    } catch (err) {
      setBookingError(err.message || 'Failed to book appointment. Please try again.');
    } finally {
      setBookingLoading(false);
    }
  };

  // Cancel an existing BOOKED appointment
  const handleCancelAppointment = async (appointmentId) => {
    if (actionLoadingId) return;

    setActionError(null);
    setActionSuccess(null);
    setActionLoadingId(appointmentId);

    try {
      const updated = await cancelAppointment(token, appointmentId);
      setActionSuccess(`Appointment #${updated.id} was successfully cancelled.`);
      // Refresh list
      fetchAppointments();
      fetchAvailabilities();
    } catch (err) {
      setActionError(err.message || `Failed to cancel appointment #${appointmentId}.`);
    } finally {
      setActionLoadingId(null);
    }
  };

  return (
    <div className="portal-workflow patient-workflow" id="patient-workflow-root">
      {/* Patient Header Banner */}
      <div className="portal-header">
        <div className="portal-badge-row">
          <span className="portal-role-tag badge-patient-tag">
            🧑‍⚕️ Patient Care Portal
          </span>
          {user.publicId && (
            <span className="portal-public-id-badge" id="patient-public-id-badge">
              🆔 MediGuide ID: <strong>{user.publicId}</strong>
            </span>
          )}
        </div>
        <h3 className="portal-greeting">Welcome, {user.fullName}!</h3>
        <p className="portal-subtitle">
          {user.publicId ? `Patient ID: ${user.publicId}` : `Patient ID #${user.id}`} &bull; {user.email} &bull; Clinical Appointment Management
        </p>
      </div>

      {/* Global Action Alerts */}
      {actionSuccess && (
        <div className="portal-alert alert-success" role="alert">
          <span>✓</span>
          <div>{actionSuccess}</div>
          <button className="alert-close-btn" onClick={() => setActionSuccess(null)}>✕</button>
        </div>
      )}
      {actionError && (
        <div className="portal-alert alert-error" role="alert">
          <span>⚠️</span>
          <div>{actionError}</div>
          <button className="alert-close-btn" onClick={() => setActionError(null)}>✕</button>
        </div>
      )}

      {/* SECTION: Patient Contact Profile */}
      <section className="workflow-section" id="section-patient-profile">
        <div className="section-header-row">
          <div>
            <h4 className="section-title">👤 My Contact Profile</h4>
            <p className="section-sub">
              Manage your personal contact details. Your phone number is strictly private and never shared publicly.
            </p>
          </div>
          <button
            type="button"
            className="btn-refresh"
            onClick={fetchPatientProfile}
            disabled={loadingPatientProfile}
            title="Reload personal profile"
          >
            {loadingPatientProfile ? <span className="spinner-sm" /> : '↻ Reload Profile'}
          </button>
        </div>

        {patientProfileSuccess && (
          <div className="portal-alert alert-success" role="alert">
            <span>✓</span>
            <div>{patientProfileSuccess}</div>
            <button className="alert-close-btn" onClick={() => setPatientProfileSuccess(null)}>✕</button>
          </div>
        )}

        {patientProfileError && (
          <div className="portal-alert alert-error" role="alert">
            <span>⚠️</span>
            <div>{patientProfileError}</div>
            <button className="alert-close-btn" onClick={() => setPatientProfileError(null)}>✕</button>
          </div>
        )}

        <div className="patient-profile-card">
          <form className="patient-profile-form" onSubmit={handleSavePatientPhone} id="patient-profile-form">
            <div className="profile-form-grid">
              <div className="profile-input-group">
                <label className="profile-label" htmlFor="patient-display-name">
                  Full Name
                </label>
                <input
                  type="text"
                  id="patient-display-name"
                  className="profile-input"
                  value={patientProfile?.fullName || user.fullName || ''}
                  readOnly
                  disabled
                />
              </div>

              <div className="profile-input-group">
                <label className="profile-label" htmlFor="patient-display-email">
                  Login Email
                </label>
                <input
                  type="email"
                  id="patient-display-email"
                  className="profile-input"
                  value={patientProfile?.email || user.email || ''}
                  readOnly
                  disabled
                />
              </div>

              <div className="profile-input-group">
                <label className="profile-label" htmlFor="patient-display-public-id">
                  Public MediGuide ID
                </label>
                <input
                  type="text"
                  id="patient-display-public-id"
                  className="profile-input"
                  value={patientProfile?.publicId || user.publicId || 'PAT-PENDING'}
                  readOnly
                  disabled
                />
              </div>

              <div className="profile-input-group">
                <label className="profile-label" htmlFor="patient-phone-input">
                  Personal Phone Number <span className="profile-input-hint">(Private &amp; confidential)</span>
                </label>
                <input
                  type="tel"
                  id="patient-phone-input"
                  className="profile-input"
                  placeholder="e.g. +91 98765 43210"
                  value={patientPhone}
                  onChange={(e) => {
                    setPatientPhone(e.target.value);
                    setPatientProfileError(null);
                    setPatientProfileSuccess(null);
                  }}
                  maxLength={25}
                />
              </div>
            </div>

            <div className="profile-actions-row">
              <button
                type="submit"
                className="btn-save-profile btn-save-patient-profile"
                id="btn-save-patient-phone"
                disabled={savingPatientPhone || loadingPatientProfile}
              >
                {savingPatientPhone ? (
                  <>
                    <span className="spinner-sm" /> Saving...
                  </>
                ) : (
                  <>💾 Save Contact Details</>
                )}
              </button>
            </div>
          </form>
        </div>
      </section>

      {/* SECTION: Doctor Discovery & Specialty/Location Search */}
      <section className="workflow-section" id="section-doctor-search">
        <div className="section-header-row">
          <div>
            <h4 className="section-title">🔍 Find a Doctor &amp; Clinic</h4>
            <p className="section-sub">
              Search published physicians by medical specialty, city, locality, or PIN code.
            </p>
          </div>
        </div>

        {searchError && (
          <div className="portal-alert alert-error" role="alert">
            <span>⚠️</span>
            <div>{searchError}</div>
            <button className="alert-close-btn" onClick={() => setSearchError(null)}>✕</button>
          </div>
        )}

        <form className="doctor-search-box" onSubmit={handleSearchDoctors} id="doctor-search-form">
          <div className="search-fields-grid">
            <div className="search-field-group">
              <label className="search-field-label" htmlFor="search-specialty">Specialty</label>
              <input
                type="text"
                id="search-specialty"
                className="search-input"
                placeholder="e.g. Cardiology, Pediatrics"
                value={searchSpecialty}
                onChange={(e) => setSearchSpecialty(e.target.value)}
              />
            </div>

            <div className="search-field-group">
              <label className="search-field-label" htmlFor="search-city">City</label>
              <input
                type="text"
                id="search-city"
                className="search-input"
                placeholder="e.g. Bengaluru, Mumbai"
                value={searchCity}
                onChange={(e) => setSearchCity(e.target.value)}
              />
            </div>

            <div className="search-field-group">
              <label className="search-field-label" htmlFor="search-area">Area / Locality</label>
              <input
                type="text"
                id="search-area"
                className="search-input"
                placeholder="e.g. Indiranagar, Bandra"
                value={searchArea}
                onChange={(e) => setSearchArea(e.target.value)}
              />
            </div>

            <div className="search-field-group">
              <label className="search-field-label" htmlFor="search-pincode">PIN Code</label>
              <input
                type="text"
                id="search-pincode"
                className="search-input"
                placeholder="e.g. 560038"
                value={searchPinCode}
                onChange={(e) => setSearchPinCode(e.target.value)}
                maxLength={10}
              />
            </div>
          </div>

          <div className="search-actions-bar">
            {hasSearched && (
              <button
                type="button"
                className="btn-search-reset"
                onClick={handleResetSearch}
                id="btn-reset-doctor-search"
              >
                ✕ Clear Search
              </button>
            )}
            <button
              type="submit"
              className="btn-search-primary"
              id="btn-submit-doctor-search"
              disabled={searchingDoctors}
            >
              {searchingDoctors ? (
                <>
                  <span className="spinner-sm" /> Searching...
                </>
              ) : (
                <>🔍 Search Doctors</>
              )}
            </button>
          </div>
        </form>

        {/* Doctor Search Results */}
        {hasSearched && (
          <div className="doctor-search-results" id="doctor-search-results">
            <div className="search-results-summary">
              <span>
                Found <strong>{searchResults.length}</strong> matching published doctor(s)
              </span>
            </div>

            {searchResults.length === 0 ? (
              <div className="portal-empty-state">
                <span className="empty-icon">🔍</span>
                <p className="empty-text">No doctors matched your search criteria.</p>
                <small className="empty-hint">Try adjusting or clearing your specialty, city, or PIN code filters.</small>
              </div>
            ) : (
              <div className="doctor-cards-grid">
                {searchResults.map((doc) => (
                  <div key={doc.publicId} className="doctor-card" id={`doctor-card-${doc.publicId}`}>
                    <div className="doctor-card-header">
                      <div>
                        <h5 className="doctor-card-title">
                          🩺 {doc.doctorName ? `Dr. ${doc.doctorName.replace(/^Dr\.?\s*/i, '')}` : 'Doctor'}
                        </h5>
                        <span className="doctor-card-specialty">{doc.specialty}</span>
                      </div>
                      <span className="portal-public-id-badge">{doc.publicId}</span>
                    </div>

                    <div className="doctor-card-details">
                      <div className="doctor-detail-row">
                        <span className="doctor-detail-label">Clinic:</span>
                        <span><strong>{doc.clinicName}</strong></span>
                      </div>
                      <div className="doctor-detail-row">
                        <span className="doctor-detail-label">Location:</span>
                        <span>
                          {doc.city}{doc.area ? `, ${doc.area}` : ''}{doc.pinCode ? ` - ${doc.pinCode}` : ''}
                        </span>
                      </div>
                      {doc.clinicAddress && (
                        <div className="doctor-detail-row">
                          <span className="doctor-detail-label">Address:</span>
                          <span>{doc.clinicAddress}</span>
                        </div>
                      )}
                      {doc.clinicPhone && (
                        <div className="doctor-detail-row">
                          <span className="doctor-detail-label">Clinic Contact:</span>
                          <span><strong>📞 {doc.clinicPhone}</strong></span>
                        </div>
                      )}
                    </div>

                    <div className="doctor-card-actions">
                      <button
                        type="button"
                        className="btn-select-doctor"
                        onClick={() => handleSelectDoctorForBooking(doc)}
                        id={`btn-select-doctor-${doc.publicId}`}
                      >
                        📅 View Schedules &amp; Book →
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </section>

      {/* SECTION 1: Active Doctor Availability Discovery */}
      <section className="workflow-section" id="section-available-slots">
        <div className="section-header-row">
          <div>
            <h4 className="section-title">📅 Available Doctor Schedules</h4>
            <p className="section-sub">
              Active future consultation windows published by clinic doctors.
            </p>
          </div>
          <button
            type="button"
            className="btn-refresh"
            onClick={fetchAvailabilities}
            disabled={loadingSlots}
            title="Refresh available doctor schedules"
          >
            {loadingSlots ? <span className="spinner-sm" /> : '↻ Refresh'}
          </button>
        </div>

        {/* Selected Doctor Filter Banner */}
        {selectedDoctorFilter && (
          <div className="doctor-filter-active-banner" id="doctor-filter-banner">
            <div>
              <span>
                Showing available consultation schedules for <strong>{selectedDoctorFilter.doctorName}</strong> ({selectedDoctorFilter.publicId})
              </span>
            </div>
            <button
              type="button"
              className="btn-clear-doctor-filter"
              onClick={handleClearDoctorFilter}
              id="btn-clear-doctor-filter"
            >
              ✕ Show All Doctors
            </button>
          </div>
        )}

        {slotsError && (
          <div className="portal-alert alert-error">
            <span>⚠️</span>
            <div>{slotsError}</div>
          </div>
        )}

        {loadingSlots && availabilities.length === 0 ? (
          <div className="portal-loading-state">
            <span className="spinner-sm" />
            <span>Loading active doctor availability slots...</span>
          </div>
        ) : filteredAvailabilities.length === 0 ? (
          <div className="portal-empty-state">
            <span className="empty-icon">🩺</span>
            {selectedDoctorFilter ? (
              <>
                <p className="empty-text">No active schedules found for {selectedDoctorFilter.doctorName}.</p>
                <small className="empty-hint">This doctor currently has no upcoming consultation slots open for booking.</small>
                <button
                  type="button"
                  className="btn-clear-doctor-filter"
                  style={{ marginTop: '8px' }}
                  onClick={handleClearDoctorFilter}
                >
                  Show All Doctor Schedules
                </button>
              </>
            ) : (
              <>
                <p className="empty-text">No doctor availability slots are currently open for booking.</p>
                <small className="empty-hint">Please check back later once clinic physicians publish upcoming consultation hours.</small>
              </>
            )}
          </div>
        ) : (
          <div className="slots-grid">
            {filteredAvailabilities.map((slot) => {
              const isSelected = selectedSlot?.id === slot.id;
              return (
                <div
                  key={slot.id}
                  className={`slot-card ${isSelected ? 'slot-selected' : ''}`}
                >
                  <div className="slot-card-header">
                    <span className="slot-doctor-name">
                      🩺 {slot.doctorName ? `Dr. ${slot.doctorName.replace(/^Dr\.?\s*/i, '')}` : `Doctor #${slot.doctorId}`}
                    </span>
                    <span className="slot-badge-active">Available</span>
                  </div>
                  <div className="slot-times">
                    <div className="time-row">
                      <span className="time-label">From:</span>
                      <span className="time-val">{formatDateTime(slot.startTime)}</span>
                    </div>
                    <div className="time-row">
                      <span className="time-label">To:</span>
                      <span className="time-val">{formatDateTime(slot.endTime)}</span>
                    </div>
                  </div>
                  <div className="slot-actions">
                    <button
                      type="button"
                      className={`btn-slot ${isSelected ? 'btn-slot-active' : ''}`}
                      onClick={() => handleSelectSlot(slot)}
                      id={`btn-select-slot-${slot.id}`}
                    >
                      {isSelected ? '✓ Slot Selected' : 'Book Consultation →'}
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>

      {/* SECTION 2: Appointment Booking Panel (Displays when a slot is selected) */}
      {selectedSlot && (
        <section className="workflow-section booking-panel" id="section-booking-form">
          <div className="booking-panel-header">
            <div>
              <h4 className="section-title">Confirm Appointment Booking</h4>
              <p className="section-sub">
                Booking with <strong>Dr. {selectedSlot.doctorName?.replace(/^Dr\.?\s*/i, '') || selectedSlot.doctorId}</strong> (Slot #{selectedSlot.id})
              </p>
            </div>
            <button
              type="button"
              className="btn-text-cancel"
              onClick={handleCancelSelection}
            >
              ✕ Cancel Selection
            </button>
          </div>

          {bookingError && (
            <div className="portal-alert alert-error">
              <span>⚠️</span>
              <div>{bookingError}</div>
            </div>
          )}

          {bookingSuccess && (
            <div className="portal-alert alert-success">
              <span>✓</span>
              <div>{bookingSuccess}</div>
            </div>
          )}

          <form className="booking-form" onSubmit={handleBookSubmit}>
            <div className="form-row-2">
              <div className="form-group">
                <label className="form-label" htmlFor="booking-start">
                  Appointment Start Time <span className="req">*</span>
                </label>
                <input
                  id="booking-start"
                  type="datetime-local"
                  className="form-input"
                  value={bookingStart}
                  onChange={(e) => setBookingStart(e.target.value)}
                  disabled={bookingLoading}
                  required
                />
                <span className="field-hint">Slot starts at {formatDateTime(selectedSlot.startTime)}</span>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="booking-end">
                  Appointment End Time <span className="req">*</span>
                </label>
                <input
                  id="booking-end"
                  type="datetime-local"
                  className="form-input"
                  value={bookingEnd}
                  onChange={(e) => setBookingEnd(e.target.value)}
                  disabled={bookingLoading}
                  required
                />
                <span className="field-hint">Slot ends at {formatDateTime(selectedSlot.endTime)}</span>
              </div>
            </div>

            <div className="booking-form-actions">
              <button
                id="btn-confirm-booking"
                type="submit"
                className="btn btn-primary"
                disabled={bookingLoading}
              >
                {bookingLoading ? (
                  <>
                    <span className="spinner-sm" />
                    Booking Appointment...
                  </>
                ) : (
                  'Confirm Appointment Booking'
                )}
              </button>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={handleCancelSelection}
                disabled={bookingLoading}
              >
                Cancel
              </button>
            </div>
          </form>
        </section>
      )}

      {/* SECTION 3: Patient's Personal Appointments */}
      <section className="workflow-section" id="section-my-appointments">
        <div className="section-header-row">
          <div>
            <h4 className="section-title">📋 My Scheduled Consultations</h4>
            <p className="section-sub">Your personal consultation appointments and their current status.</p>
          </div>
          <button
            type="button"
            className="btn-refresh"
            onClick={fetchAppointments}
            disabled={loadingAppointments}
            title="Refresh appointments"
          >
            {loadingAppointments ? <span className="spinner-sm" /> : '↻ Refresh'}
          </button>
        </div>

        {appointmentsError && (
          <div className="portal-alert alert-error">
            <span>⚠️</span>
            <div>{appointmentsError}</div>
          </div>
        )}

        {loadingAppointments && appointments.length === 0 ? (
          <div className="portal-loading-state">
            <span className="spinner-sm" />
            <span>Loading your consultation appointments...</span>
          </div>
        ) : appointments.length === 0 ? (
          <div className="portal-empty-state">
            <span className="empty-icon">📝</span>
            <p className="empty-text">You have no consultation appointments booked yet.</p>
            <small className="empty-hint">Select any open doctor availability slot above to schedule your first consultation.</small>
          </div>
        ) : (
          <div className="appointments-list">
            {appointments.map((app) => {
              const isCancelling = actionLoadingId === app.id;
              const canCancel = app.status === 'BOOKED';

              return (
                <div key={app.id} className="appointment-item-container">
                  <div className="appointment-card" id={`patient-appointment-${app.id}`}>
                  <div className="appointment-card-main">
                    <div className="appointment-meta-row">
                      <span className="app-id-pill">Appointment #{app.id}</span>
                      <span className={`status-tag status-${app.status?.toLowerCase()}`}>
                        {app.status === 'BOOKED' && '● Booked'}
                        {app.status === 'COMPLETED' && '✓ Completed'}
                        {app.status === 'CANCELLED' && '✕ Cancelled'}
                      </span>
                    </div>

                    <div className="app-details-grid">
                      <div className="app-detail-item">
                        <span className="detail-label">Attending Doctor</span>
                        <span className="detail-val doctor-name">
                          🩺 Dr. {app.doctorName ? app.doctorName.replace(/^Dr\.?\s*/i, '') : `Doctor #${app.doctorId}`}
                        </span>
                      </div>

                      <div className="app-detail-item">
                        <span className="detail-label">Consultation Start</span>
                        <span className="detail-val">{formatDateTime(app.startTime)}</span>
                      </div>

                      <div className="app-detail-item">
                        <span className="detail-label">Consultation End</span>
                        <span className="detail-val">{formatDateTime(app.endTime)}</span>
                      </div>
                    </div>
                  </div>

                  <div className="appointment-card-actions">
                    {app.status !== 'CANCELLED' && (
                      <button
                        id={`btn-view-clinical-${app.id}`}
                        type="button"
                        className={`btn-clinical-action ${activeClinicalAppId === app.id ? 'active' : ''}`}
                        onClick={() => setActiveClinicalAppId((prev) => (prev === app.id ? null : app.id))}
                        title="View physician diagnosis, clinical advice, and prescriptions"
                      >
                        {activeClinicalAppId === app.id ? '▲ Close Medical Summary' : '📄 Clinical Notes & Rx'}
                      </button>
                    )}

                    {canCancel && (
                      <button
                        id={`btn-cancel-app-${app.id}`}
                        type="button"
                        className="btn-cancel-action"
                        onClick={() => handleCancelAppointment(app.id)}
                        disabled={isCancelling}
                        title="Cancel this booked consultation"
                      >
                        {isCancelling ? (
                          <>
                            <span className="spinner-sm" />
                            Cancelling...
                          </>
                        ) : (
                          'Cancel Appointment'
                        )}
                      </button>
                    )}
                  </div>
                </div>

                {/* Integrated Patient Clinical Consultation & Prescription View */}
                {activeClinicalAppId === app.id && (
                  <PatientClinicalDrawer
                    appointment={app}
                    token={token}
                    onClose={() => setActiveClinicalAppId(null)}
                  />
                )}
              </div>
            );
          })}
          </div>
        )}
      </section>

      {/* SECTION 4: Patient's Consultation & Clinical History */}
      <section className="workflow-section" id="section-patient-consultation-history">
        <div className="section-header-row">
          <div>
            <h4 className="section-title">📜 My Consultation History</h4>
            <p className="section-sub">
              Your historical clinical records, physician diagnoses, prescribed medications, and recommended follow-ups.
            </p>
          </div>
          <button
            type="button"
            className="btn-refresh"
            onClick={fetchConsultationHistory}
            disabled={loadingHistory}
            title="Refresh consultation history"
          >
            {loadingHistory ? <span className="spinner-sm" /> : '↻ Refresh History'}
          </button>
        </div>

        {historyError && (
          <div className="portal-alert alert-error" role="alert">
            <span>⚠️</span>
            <div>{historyError}</div>
          </div>
        )}

        {loadingHistory && consultationHistory.length === 0 ? (
          <div className="portal-loading-state">
            <span className="spinner-sm" />
            <span>Loading your consultation history...</span>
          </div>
        ) : consultationHistory.length === 0 ? (
          <div className="portal-empty-state" id="patient-history-empty">
            <span className="empty-icon">📋</span>
            <p className="empty-text">No consultation history recorded yet.</p>
            <small className="empty-hint">
              When your attending physician completes an appointment and documents clinical notes, your full consultation history will be displayed here.
            </small>
          </div>
        ) : (
          <div className="history-timeline" id="patient-history-list">
            {consultationHistory.map((item) => (
              <div key={item.consultationId} className="history-card" id={`patient-history-card-${item.consultationId}`}>
                <div className="history-card-header">
                  <div className="history-header-main">
                    <span className="history-date-badge">
                      📅 {formatDateTime(item.appointmentStartTime)}
                    </span>
                    <span className="history-doctor-name">
                      🩺 Dr. {item.doctorName ? item.doctorName.replace(/^Dr\.?\s*/i, '') : `Doctor #${item.doctorId}`}
                    </span>
                  </div>
                  <div className="history-header-meta">
                    <span className="app-id-pill">Consultation #{item.consultationId}</span>
                    <span className={`status-tag status-${item.appointmentStatus?.toLowerCase()}`}>
                      {item.appointmentStatus === 'COMPLETED' ? '✓ Completed' : item.appointmentStatus}
                    </span>
                  </div>
                </div>

                <div className="history-card-body">
                  {/* Diagnosis & Notes */}
                  <div className="history-section">
                    <span className="history-label">Clinical Diagnosis &amp; Notes</span>
                    <p className="history-text">{item.notes}</p>
                  </div>

                  {/* Treatment Advice */}
                  {item.treatment && (
                    <div className="history-section">
                      <span className="history-label">Treatment &amp; Medical Recommendations</span>
                      <p className="history-text">{item.treatment}</p>
                    </div>
                  )}

                  {/* Prescribed Medications */}
                  <div className="history-section">
                    <span className="history-label">Prescribed Medications (Rx)</span>
                    {item.prescriptions && item.prescriptions.length > 0 ? (
                      <div className="history-rx-grid">
                        {item.prescriptions.map((rx) => (
                          <div key={rx.id} className="history-rx-item">
                            <div className="history-rx-header">
                              <span className="rx-med-name">💊 {rx.medicationName}</span>
                              <span className="rx-dosage-pill">{rx.dosage}</span>
                            </div>
                            <span className="rx-instructions">👉 {rx.instructions}</span>
                            {rx.duration && (
                              <span className="rx-duration">⏱ {rx.duration}</span>
                            )}
                          </div>
                        ))}
                      </div>
                    ) : (
                      <span className="history-text-muted">No medications prescribed for this visit.</span>
                    )}
                  </div>

                  {/* Follow-up Recommendation */}
                  <div className="history-section">
                    <span className="history-label">Recommended Follow-up</span>
                    {item.followUpDate ? (
                      <div className="history-follow-up-card">
                        <span className="follow-up-badge">
                          📅 Recommended Date: <strong>{formatDateOnly(item.followUpDate)}</strong>
                        </span>
                        {item.followUpNotes && (
                          <p className="history-follow-up-notes">
                            <strong>Note:</strong> {item.followUpNotes}
                          </p>
                        )}
                      </div>
                    ) : (
                      <span className="history-text-muted">No follow-up was recommended.</span>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

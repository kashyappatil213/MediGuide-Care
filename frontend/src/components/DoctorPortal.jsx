import { useState, useEffect, useCallback, useMemo } from 'react';
import {
  getMyAvailabilities,
  deactivateAvailability,
  getMyAppointments,
  completeAppointment,
  cancelAppointment,
  getConsultationHistory,
  generateClinicalHistorySummary,
} from '../services/appointmentApi';
import {
  getMyDoctorProfile,
  updateMyDoctorProfile,
} from '../services/doctorProfileApi';
import { formatDateTime, formatDateOnly } from '../utils/dateTimeUtils';
import DoctorClinicalDrawer from './DoctorClinicalDrawer';
import DoctorAvailabilityCalendar from './DoctorAvailabilityCalendar';

/**
 * Doctor Clinical Portal component for MediGuide.
 *
 * Implements:
 * 1. Calendar-Based Availability Planner: month view, day agenda, slot creation, and deactivation.
 * 2. Clinical Consultations Queue: view appointments assigned to the doctor.
 * 3. Appointment Lifecycle Actions: mark BOOKED appointments as COMPLETED or CANCELLED with real-time feedback.
 * 4. Clinical Documentation & Digital Prescriptions: integrated drawer for diagnoses, treatment advice, and Rx.
 * 5. Consultation History & AI Clinical History Summarizer: review historical records and generate structured AI draft summaries.
 * 6. Doctor Clinic & Specialty Profile Management: configure and publish clinic details for patient discovery.
 *
 * @param {Object} props
 * @param {Object} props.user - Safe authenticated doctor profile { id, fullName, email, role }
 * @param {string} props.token - In-memory JWT access token
 */
export default function DoctorPortal({ user, token }) {
  // --- Doctor Profile State ---
  const [profile, setProfile] = useState({
    specialty: '',
    clinicName: '',
    clinicAddress: '',
    city: '',
    area: '',
    pinCode: '',
    clinicPhone: '',
    personalPhone: '',
  });
  const [loadingProfile, setLoadingProfile] = useState(false);
  const [savingProfile, setSavingProfile] = useState(false);
  const [profileError, setProfileError] = useState(null);
  const [profileSuccess, setProfileSuccess] = useState(null);
  const [hasExistingProfile, setHasExistingProfile] = useState(false);

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
  const [selectedPatientFilter, setSelectedPatientFilter] = useState(null);

  // --- AI Clinical Summarizer State ---
  const [aiSummary, setAiSummary] = useState(null);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiError, setAiError] = useState(null);
  const [showAiDisclosure, setShowAiDisclosure] = useState(false);
  const [aiDisclosureAccepted, setAiDisclosureAccepted] = useState(false);
  const [copySuccess, setCopySuccess] = useState(false);

  // Derive unique patients that the doctor has consulted or has queued
  const availablePatients = useMemo(() => {
    const map = new Map();
    appointments.forEach((a) => {
      if (a.patientId) {
        map.set(String(a.patientId), {
          id: a.patientId,
          name: a.patientName || `Patient #${a.patientId}`,
        });
      }
    });
    consultationHistory.forEach((c) => {
      if (c.patientId) {
        map.set(String(c.patientId), {
          id: c.patientId,
          name: c.patientName || `Patient #${c.patientId}`,
        });
      }
    });
    return Array.from(map.values());
  }, [appointments, consultationHistory]);

  // --- Action Loading State (Deactivation or Appointment completion/cancellation) ---
  const [actionLoadingKey, setActionLoadingKey] = useState(null); // e.g. "deactivate-1" or "complete-2" or "cancel-3"
  const [actionError, setActionError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);

  // --- Active Clinical Consultation Drawer State ---
  const [activeClinicalAppId, setActiveClinicalAppId] = useState(null);

  // 1. Fetch doctor's availability slots
  const fetchAvailabilities = useCallback(async () => {
    if (!token) return;
    setLoadingSlots(true);
    setSlotsError(null);
    try {
      const data = await getMyAvailabilities(token);
      setAvailabilities(Array.isArray(data) ? data : []);
    } catch (err) {
      setSlotsError(err.message || 'Unable to load doctor availability schedules.');
      setAvailabilities([]);
    } finally {
      setLoadingSlots(false);
    }
  }, [token]);

  // 2. Fetch doctor's appointments
  const fetchAppointments = useCallback(async () => {
    if (!token) return;
    setLoadingAppointments(true);
    setAppointmentsError(null);
    try {
      const data = await getMyAppointments(token);
      setAppointments(Array.isArray(data) ? data : []);
    } catch (err) {
      setAppointmentsError(err.message || 'Unable to load consultation queue.');
      setAppointments([]);
    } finally {
      setLoadingAppointments(false);
    }
  }, [token]);

  // 3. Fetch doctor's consultation history
  const fetchConsultationHistory = useCallback(async (patientId) => {
    if (!token) return;
    setLoadingHistory(true);
    setHistoryError(null);
    try {
      const data = await getConsultationHistory(token, patientId);
      setConsultationHistory(Array.isArray(data) ? data : []);
    } catch (err) {
      setHistoryError(err.message || 'Unable to load clinical consultation history.');
      setConsultationHistory([]);
    } finally {
      setLoadingHistory(false);
    }
  }, [token]);

  // Select patient and fetch history (and reset AI draft)
  const selectPatientAndFetch = useCallback(
    (patientInfo) => {
      setSelectedPatientFilter(patientInfo);
      setAiSummary(null);
      setAiError(null);
      fetchConsultationHistory(patientInfo ? patientInfo.id : undefined);
    },
    [fetchConsultationHistory]
  );

  // Execute AI summary request
  const executeAiSummaryRequest = useCallback(async () => {
    if (!selectedPatientFilter?.id || aiLoading) return;
    setAiLoading(true);
    setAiError(null);
    try {
      const result = await generateClinicalHistorySummary(token, selectedPatientFilter.id);
      setAiSummary(result);
    } catch (err) {
      setAiError(err.message || 'Failed to generate AI clinical history summary.');
    } finally {
      setAiLoading(false);
    }
  }, [token, selectedPatientFilter, aiLoading]);

  // Handle Generate AI Summary click
  const handleGenerateAiSummary = () => {
    if (!selectedPatientFilter?.id || aiLoading) return;
    if (!aiDisclosureAccepted) {
      setShowAiDisclosure(true);
      return;
    }
    executeAiSummaryRequest();
  };

  // Handle Confirm Disclosure and Generate
  const handleConfirmDisclosureAndGenerate = () => {
    setAiDisclosureAccepted(true);
    setShowAiDisclosure(false);
    executeAiSummaryRequest();
  };

  // Handle Copy Summary to Clipboard
  const handleCopySummary = async () => {
    if (!aiSummary?.summary) return;
    try {
      if (navigator?.clipboard?.writeText) {
        await navigator.clipboard.writeText(aiSummary.summary);
      } else {
        const textarea = document.createElement('textarea');
        textarea.value = aiSummary.summary;
        document.body.appendChild(textarea);
        textarea.select();
        document.execCommand('copy');
        document.body.removeChild(textarea);
      }
      setCopySuccess(true);
      setTimeout(() => setCopySuccess(false), 2500);
    } catch {
      setCopySuccess(false);
    }
  };

  // Fetch doctor's clinic & specialty profile
  const fetchProfile = useCallback(async () => {
    if (!token) return;
    setLoadingProfile(true);
    setProfileError(null);
    try {
      const data = await getMyDoctorProfile(token);
      if (data) {
        setProfile({
          specialty: data.specialty || '',
          clinicName: data.clinicName || '',
          clinicAddress: data.clinicAddress || '',
          city: data.city || '',
          area: data.area || '',
          pinCode: data.pinCode || '',
          clinicPhone: data.clinicPhone || '',
          personalPhone: data.personalPhone || '',
        });
        setHasExistingProfile(true);
      }
    } catch (err) {
      if (err.status === 404 || err.message?.toLowerCase().includes('not been configured') || err.message?.toLowerCase().includes('not found')) {
        setHasExistingProfile(false);
      } else {
        setProfileError(err.message || 'Unable to load clinic profile.');
      }
    } finally {
      setLoadingProfile(false);
    }
  }, [token]);

  // Handle profile input changes
  const handleProfileFieldChange = (field, value) => {
    setProfile((prev) => ({ ...prev, [field]: value }));
    setProfileError(null);
    setProfileSuccess(null);
  };

  // Handle saving clinic profile
  const handleSaveProfile = async (e) => {
    if (e) e.preventDefault();
    setProfileError(null);
    setProfileSuccess(null);

    if (!profile.specialty.trim()) {
      setProfileError('Medical specialty is required.');
      return;
    }
    if (!profile.clinicName.trim()) {
      setProfileError('Clinic or hospital name is required.');
      return;
    }
    if (!profile.city.trim()) {
      setProfileError('City is required.');
      return;
    }
    if (profile.pinCode && profile.pinCode.trim()) {
      const pinRegex = /^[1-9][0-9]{5}$/;
      if (!pinRegex.test(profile.pinCode.trim())) {
        setProfileError('PIN code must be a valid 6-digit Indian postal code (e.g. 560001).');
        return;
      }
    }

    const phoneRegex = /^[+]?[0-9\s\-()]{7,20}$/;
    if (profile.clinicPhone && profile.clinicPhone.trim()) {
      if (!phoneRegex.test(profile.clinicPhone.trim())) {
        setProfileError('Clinic contact number must be between 7 and 20 digits (e.g. +91 80 2345 6789).');
        return;
      }
    }
    if (profile.personalPhone && profile.personalPhone.trim()) {
      if (!phoneRegex.test(profile.personalPhone.trim())) {
        setProfileError('Personal phone number must be between 7 and 20 digits (e.g. +91 98765 43210).');
        return;
      }
    }

    setSavingProfile(true);
    try {
      const updated = await updateMyDoctorProfile(token, {
        specialty: profile.specialty.trim(),
        clinicName: profile.clinicName.trim(),
        clinicAddress: profile.clinicAddress.trim() || null,
        city: profile.city.trim(),
        area: profile.area.trim() || null,
        pinCode: profile.pinCode.trim() || null,
        clinicPhone: profile.clinicPhone.trim() || null,
        personalPhone: profile.personalPhone.trim() || null,
      });

      setProfile({
        specialty: updated.specialty || '',
        clinicName: updated.clinicName || '',
        clinicAddress: updated.clinicAddress || '',
        city: updated.city || '',
        area: updated.area || '',
        pinCode: updated.pinCode || '',
        clinicPhone: updated.clinicPhone || '',
        personalPhone: updated.personalPhone || '',
      });
      setHasExistingProfile(true);
      setProfileSuccess('Clinic profile saved and published successfully!');
    } catch (err) {
      setProfileError(err.message || 'Failed to save doctor profile.');
    } finally {
      setSavingProfile(false);
    }
  };

  // Initial data load
  useEffect(() => {
    let active = true;
    const timer = setTimeout(() => {
      if (active) {
        fetchProfile();
        fetchAvailabilities();
        fetchAppointments();
        fetchConsultationHistory();
      }
    }, 0);
    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, [fetchProfile, fetchAvailabilities, fetchAppointments, fetchConsultationHistory]);

  // Handle Slot Deactivation
  const handleDeactivateSlot = async (slotId) => {
    const actionKey = `deactivate-${slotId}`;
    if (actionLoadingKey) return;

    setActionError(null);
    setActionSuccess(null);
    setActionLoadingKey(actionKey);

    try {
      const updated = await deactivateAvailability(token, slotId);
      setActionSuccess(`Availability slot #${updated.id} was deactivated successfully.`);
      fetchAvailabilities();
    } catch (err) {
      setActionError(err.message || `Failed to deactivate availability slot #${slotId}.`);
    } finally {
      setActionLoadingKey(null);
    }
  };

  // Handle Appointment Completion
  const handleCompleteAppointment = async (appointmentId) => {
    const actionKey = `complete-${appointmentId}`;
    if (actionLoadingKey) return;

    setActionError(null);
    setActionSuccess(null);
    setActionLoadingKey(actionKey);

    try {
      const updated = await completeAppointment(token, appointmentId);
      setActionSuccess(`Consultation appointment #${updated.id} marked as COMPLETED.`);
      fetchAppointments();
    } catch (err) {
      setActionError(err.message || `Failed to complete appointment #${appointmentId}.`);
    } finally {
      setActionLoadingKey(null);
    }
  };

  // Handle Appointment Cancellation
  const handleCancelAppointment = async (appointmentId) => {
    const actionKey = `cancel-${appointmentId}`;
    if (actionLoadingKey) return;

    setActionError(null);
    setActionSuccess(null);
    setActionLoadingKey(actionKey);

    try {
      const updated = await cancelAppointment(token, appointmentId);
      setActionSuccess(`Consultation appointment #${updated.id} was cancelled.`);
      fetchAppointments();
    } catch (err) {
      setActionError(err.message || `Failed to cancel appointment #${appointmentId}.`);
    } finally {
      setActionLoadingKey(null);
    }
  };

  return (
    <div className="portal-workflow doctor-workflow" id="doctor-workflow-root">
      {/* Doctor Header Banner */}
      <div className="portal-header">
        <div className="portal-badge-row">
          <span className="portal-role-tag badge-doctor-tag">
            🩺 Doctor Clinical Portal
          </span>
          {user.publicId && (
            <span className="portal-public-id-badge" id="doctor-public-id-badge">
              🆔 MediGuide ID: <strong>{user.publicId}</strong>
            </span>
          )}
        </div>
        <h3 className="portal-greeting">Welcome, Dr. {user.fullName}!</h3>
        <p className="portal-subtitle">
          {user.publicId ? `Doctor ID: ${user.publicId}` : `Doctor ID #${user.id}`} &bull; {user.email} &bull; Clinical Schedule &amp; Consultations
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

      {slotsError && (
        <div className="portal-alert alert-error" role="alert">
          <span>⚠️</span>
          <div>{slotsError}</div>
        </div>
      )}

      {/* SECTION: Doctor Clinic & Specialty Profile */}
      <section className="workflow-section" id="section-doctor-profile">
        <div className="section-header-row">
          <div>
            <h4 className="section-title">🏥 Clinic &amp; Specialty Profile</h4>
            <p className="section-sub">
              Manage your professional specialty, clinic details, and location for patient discovery.
            </p>
          </div>
          <button
            type="button"
            className="btn-refresh"
            onClick={fetchProfile}
            disabled={loadingProfile}
            title="Reload saved profile"
          >
            {loadingProfile ? <span className="spinner-sm" /> : '↻ Reload Profile'}
          </button>
        </div>

        {profileSuccess && (
          <div className="portal-alert alert-success" role="alert">
            <span>✓</span>
            <div>{profileSuccess}</div>
            <button className="alert-close-btn" onClick={() => setProfileSuccess(null)}>✕</button>
          </div>
        )}

        {profileError && (
          <div className="portal-alert alert-error" role="alert">
            <span>⚠️</span>
            <div>{profileError}</div>
            <button className="alert-close-btn" onClick={() => setProfileError(null)}>✕</button>
          </div>
        )}

        <div className="doctor-profile-container">
          <div className="profile-status-row">
            <div className="profile-id-group">
              <span>Public MediGuide ID:</span>
              <strong id="profile-display-public-id">{user.publicId || 'DOC-PENDING'}</strong>
              <small className="profile-input-hint">(Immutable system identifier)</small>
            </div>
            <div>
              {hasExistingProfile ? (
                <span className="profile-status-pill status-published">
                  ✓ Profile Published &amp; Discoverable
                </span>
              ) : (
                <span className="profile-status-pill status-incomplete">
                  ⚠️ Profile Incomplete (Not Discoverable Yet)
                </span>
              )}
            </div>
          </div>

          <form className="doctor-profile-form" onSubmit={handleSaveProfile} id="doctor-profile-form">
            <div className="profile-form-grid">
              <div className="profile-input-group">
                <label className="profile-label" htmlFor="profile-specialty">
                  Specialty <span className="label-required">*</span>
                </label>
                <input
                  type="text"
                  id="profile-specialty"
                  className="profile-input"
                  placeholder="e.g. Cardiology, Pediatrics, Dermatology"
                  value={profile.specialty}
                  onChange={(e) => handleProfileFieldChange('specialty', e.target.value)}
                  maxLength={100}
                  required
                />
              </div>

              <div className="profile-input-group">
                <label className="profile-label" htmlFor="profile-clinic-name">
                  Clinic / Hospital Name <span className="label-required">*</span>
                </label>
                <input
                  type="text"
                  id="profile-clinic-name"
                  className="profile-input"
                  placeholder="e.g. Apex Heart Hospital"
                  value={profile.clinicName}
                  onChange={(e) => handleProfileFieldChange('clinicName', e.target.value)}
                  maxLength={150}
                  required
                />
              </div>

              <div className="profile-input-group full-width">
                <label className="profile-label" htmlFor="profile-clinic-address">
                  Clinic Physical Address
                </label>
                <input
                  type="text"
                  id="profile-clinic-address"
                  className="profile-input"
                  placeholder="e.g. Suite 402, 12 Ring Road"
                  value={profile.clinicAddress}
                  onChange={(e) => handleProfileFieldChange('clinicAddress', e.target.value)}
                  maxLength={255}
                />
              </div>

              <div className="profile-input-group">
                <label className="profile-label" htmlFor="profile-city">
                  City <span className="label-required">*</span>
                </label>
                <input
                  type="text"
                  id="profile-city"
                  className="profile-input"
                  placeholder="e.g. Bengaluru, Mumbai, Delhi"
                  value={profile.city}
                  onChange={(e) => handleProfileFieldChange('city', e.target.value)}
                  maxLength={100}
                  required
                />
              </div>

              <div className="profile-input-group">
                <label className="profile-label" htmlFor="profile-area">
                  Area / Locality
                </label>
                <input
                  type="text"
                  id="profile-area"
                  className="profile-input"
                  placeholder="e.g. Indiranagar, Bandra West"
                  value={profile.area}
                  onChange={(e) => handleProfileFieldChange('area', e.target.value)}
                  maxLength={100}
                />
              </div>

              <div className="profile-input-group">
                <label className="profile-label" htmlFor="profile-pincode">
                  PIN Code <span className="profile-input-hint">(6 digits)</span>
                </label>
                <input
                  type="text"
                  id="profile-pincode"
                  className="profile-input"
                  placeholder="e.g. 560038"
                  value={profile.pinCode}
                  onChange={(e) => handleProfileFieldChange('pinCode', e.target.value)}
                  maxLength={10}
                />
              </div>

              <div className="profile-input-group">
                <label className="profile-label" htmlFor="profile-clinic-phone">
                  Clinic Contact Number <span className="profile-input-hint">(Public on clinic profile)</span>
                </label>
                <input
                  type="tel"
                  id="profile-clinic-phone"
                  className="profile-input"
                  placeholder="e.g. +91 80 2345 6789"
                  value={profile.clinicPhone}
                  onChange={(e) => handleProfileFieldChange('clinicPhone', e.target.value)}
                  maxLength={25}
                />
              </div>

              <div className="profile-input-group">
                <label className="profile-label" htmlFor="profile-personal-phone">
                  Personal Phone Number <span className="profile-input-hint">(Private &amp; confidential)</span>
                </label>
                <input
                  type="tel"
                  id="profile-personal-phone"
                  className="profile-input"
                  placeholder="e.g. +91 98765 43210"
                  value={profile.personalPhone}
                  onChange={(e) => handleProfileFieldChange('personalPhone', e.target.value)}
                  maxLength={25}
                />
              </div>
            </div>

            <div className="profile-actions-row">
              <button
                type="submit"
                className="btn-save-profile"
                id="btn-save-doctor-profile"
                disabled={savingProfile || loadingProfile}
              >
                {savingProfile ? (
                  <>
                    <span className="spinner-sm" /> Saving Profile...
                  </>
                ) : (
                  <>💾 Save Profile</>
                )}
              </button>
            </div>
          </form>
        </div>
      </section>

      {/* SECTION 1: Calendar-Based Availability Planner */}
      <section className="workflow-section" id="section-doctor-calendar">
        <div className="section-header-row">
          <div>
            <h4 className="section-title">📅 Consultation Availability Calendar</h4>
            <p className="section-sub">
              Manage your upcoming consultation slots through an interactive monthly calendar.
            </p>
          </div>
          <button
            type="button"
            className="btn-refresh"
            onClick={fetchAvailabilities}
            disabled={loadingSlots}
            title="Refresh availability slots"
          >
            {loadingSlots ? <span className="spinner-sm" /> : '↻ Refresh Calendar'}
          </button>
        </div>

        <DoctorAvailabilityCalendar
          availabilities={availabilities}
          onRefresh={fetchAvailabilities}
          onDeactivateSlot={handleDeactivateSlot}
          actionLoadingKey={actionLoadingKey}
          token={token}
        />
      </section>


      {/* SECTION 3: Doctor's Assigned Appointments */}
      <section className="workflow-section" id="section-doctor-appointments">
        <div className="section-header-row">
          <div>
            <h4 className="section-title">👥 Clinical Consultations Queue</h4>
            <p className="section-sub">Patient appointments scheduled with you.</p>
          </div>
          <button
            type="button"
            className="btn-refresh"
            onClick={fetchAppointments}
            disabled={loadingAppointments}
            title="Refresh consultations"
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
            <span>Loading consultation queue...</span>
          </div>
        ) : appointments.length === 0 ? (
          <div className="portal-empty-state">
            <span className="empty-icon">📋</span>
            <p className="empty-text">No patient consultations currently scheduled in your queue.</p>
            <small className="empty-hint">When patients book consultations in your active slots, they will appear here.</small>
          </div>
        ) : (
          <div className="appointments-list">
            {appointments.map((app) => {
              const isCompleting = actionLoadingKey === `complete-${app.id}`;
              const isCancelling = actionLoadingKey === `cancel-${app.id}`;
              const isActionInProgress = isCompleting || isCancelling;
              const canAct = app.status === 'BOOKED';

              return (
                <div key={app.id} className="appointment-item-container">
                  <div className="appointment-card" id={`doctor-appointment-${app.id}`}>
                  <div className="appointment-card-main">
                    <div className="appointment-meta-row">
                      <span className="app-id-pill">Consultation #{app.id}</span>
                      <span className={`status-tag status-${app.status?.toLowerCase()}`}>
                        {app.status === 'BOOKED' && '● Booked'}
                        {app.status === 'COMPLETED' && '✓ Completed'}
                        {app.status === 'CANCELLED' && '✕ Cancelled'}
                      </span>
                    </div>

                    <div className="app-details-grid">
                      <div className="app-detail-item">
                        <span className="detail-label">Patient Name</span>
                        <span className="detail-val patient-name">
                          🧑‍⚕️ {app.patientName || `Patient #${app.patientId}`}
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

                  <div className="appointment-card-actions doctor-actions-group">
                    {app.status !== 'CANCELLED' && (
                      <button
                        id={`btn-clinical-app-${app.id}`}
                        type="button"
                        className={`btn-clinical-action ${activeClinicalAppId === app.id ? 'active' : ''}`}
                        onClick={() => setActiveClinicalAppId((prev) => (prev === app.id ? null : app.id))}
                        title="Document clinical diagnosis and prescribe medications"
                      >
                        {activeClinicalAppId === app.id ? '▲ Close Clinical Record' : '📝 Clinical Record & Rx'}
                      </button>
                    )}

                    {app.patientId && (
                      <button
                        id={`btn-patient-history-${app.id}`}
                        type="button"
                        className="btn-history-filter-action"
                        onClick={() => {
                          const patientInfo = { id: app.patientId, name: app.patientName || `Patient #${app.patientId}` };
                          selectPatientAndFetch(patientInfo);
                          const el = document.getElementById('section-doctor-consultation-history');
                          if (el) el.scrollIntoView({ behavior: 'smooth' });
                        }}
                        title="Review clinical history you documented for this patient"
                      >
                        📜 Patient History
                      </button>
                    )}

                    {canAct && (
                      <>
                        <button
                          id={`btn-complete-app-${app.id}`}
                          type="button"
                          className="btn-complete-action"
                          onClick={() => handleCompleteAppointment(app.id)}
                          disabled={isActionInProgress}
                          title="Mark consultation as completed"
                        >
                          {isCompleting ? (
                            <>
                              <span className="spinner-sm" />
                              Completing...
                            </>
                          ) : (
                            '✓ Mark Completed'
                          )}
                        </button>

                        <button
                          id={`btn-cancel-doctor-app-${app.id}`}
                          type="button"
                          className="btn-cancel-action"
                          onClick={() => handleCancelAppointment(app.id)}
                          disabled={isActionInProgress}
                          title="Cancel this appointment"
                        >
                          {isCancelling ? (
                            <>
                              <span className="spinner-sm" />
                              Cancelling...
                            </>
                          ) : (
                            'Cancel'
                          )}
                        </button>
                      </>
                    )}
                  </div>
                </div>

                {/* Integrated Clinical Consultation & Prescription Drawer */}
                {activeClinicalAppId === app.id && (
                  <DoctorClinicalDrawer
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

      {/* SECTION 4: Doctor's Consultation History */}
      <section className="workflow-section" id="section-doctor-consultation-history">
        <div className="section-header-row">
          <div>
            <h4 className="section-title">📜 Clinical Consultation History</h4>
            <p className="section-sub">
              Historical record of consultations, diagnoses, and treatments you have documented.
            </p>
          </div>
          <div className="section-header-actions">
            {availablePatients.length > 0 && (
              <select
                id="select-history-patient"
                className="select-patient-dropdown"
                value={selectedPatientFilter ? String(selectedPatientFilter.id) : ''}
                onChange={(e) => {
                  const val = e.target.value;
                  if (!val) {
                    selectPatientAndFetch(null);
                  } else {
                    const found = availablePatients.find((p) => String(p.id) === val);
                    if (found) {
                      selectPatientAndFetch(found);
                    }
                  }
                }}
                aria-label="Filter clinical history by patient"
              >
                <option value="">-- All Patients --</option>
                {availablePatients.map((p) => (
                  <option key={p.id} value={p.id}>
                    🧑‍⚕️ {p.name} (Patient #{p.id})
                  </option>
                ))}
              </select>
            )}

            {selectedPatientFilter && (
              <button
                type="button"
                className="btn-clear-filter"
                onClick={() => selectPatientAndFetch(null)}
                title="View all consultations across all patients"
              >
                ✕ Clear Patient Filter
              </button>
            )}
            <button
              type="button"
              className="btn-refresh"
              onClick={() => fetchConsultationHistory(selectedPatientFilter?.id)}
              disabled={loadingHistory}
              title="Refresh consultation history"
            >
              {loadingHistory ? <span className="spinner-sm" /> : '↻ Refresh History'}
            </button>
          </div>
        </div>

        {selectedPatientFilter && (
          <div className="filter-active-banner">
            <span>🔍 Showing clinical history for: <strong>{selectedPatientFilter.name}</strong> (Patient #{selectedPatientFilter.id})</span>
          </div>
        )}

        {/* AI Clinical History Summarizer Card */}
        {selectedPatientFilter && (
          <div className="ai-summarizer-control" id="ai-summarizer-control">
            <div className="ai-summarizer-header">
              <div className="ai-summarizer-title-group">
                <h5 className="ai-summarizer-heading">
                  ✨ AI Clinical History Summarizer
                </h5>
                <p className="ai-summarizer-sub">
                  Summarize {selectedPatientFilter.name}&apos;s recorded consultations, diagnosis notes, medications, and follow-ups.
                </p>
              </div>

              <button
                type="button"
                id="btn-generate-ai-summary"
                className="btn-generate-ai"
                onClick={handleGenerateAiSummary}
                disabled={aiLoading}
                title="Generate concise, structured AI summary of this patient's clinical history"
              >
                {aiLoading ? (
                  <>
                    <span className="spinner-sm" />
                    <span>Summarizing Clinical History...</span>
                  </>
                ) : (
                  <>
                    <span>✨</span>
                    <span>{aiSummary ? '↻ Regenerate AI Summary' : 'Generate AI Clinical Summary'}</span>
                  </>
                )}
              </button>
            </div>

            {/* Privacy & External Transmission Disclosure Box */}
            {showAiDisclosure && (
              <div className="ai-disclosure-box" id="ai-disclosure-box">
                <div className="ai-disclosure-header">
                  <span>🔒</span>
                  <span>External AI Processing &amp; Privacy Notice</span>
                </div>
                <p className="ai-disclosure-text">
                  When you generate an AI summary, the recorded consultation notes, treatment instructions, and prescribed medications for this patient are transmitted to the configured external AI service to produce a structured draft. Direct identifiers (patient and doctor names, emails, and account identifiers) are excluded by the server prior to transmission. AI output is an untrusted draft for doctor review.
                </p>
                <div className="ai-disclosure-actions">
                  <button
                    type="button"
                    id="btn-confirm-ai-disclosure"
                    className="btn-confirm-disclosure"
                    onClick={handleConfirmDisclosureAndGenerate}
                    disabled={aiLoading}
                  >
                    ✓ Acknowledge &amp; Generate Draft
                  </button>
                  <button
                    type="button"
                    className="btn-cancel-disclosure"
                    onClick={() => setShowAiDisclosure(false)}
                    disabled={aiLoading}
                  >
                    Cancel
                  </button>
                </div>
              </div>
            )}

            {/* AI Error Notification */}
            {aiError && (
              <div className="portal-alert alert-error" role="alert" id="ai-summary-error">
                <span>⚠️</span>
                <div>{aiError}</div>
                <button
                  type="button"
                  className="alert-close-btn"
                  onClick={() => setAiError(null)}
                  title="Close error message"
                >
                  ✕
                </button>
              </div>
            )}

            {/* AI-Generated Summary Result Panel */}
            {aiSummary && (
              <div className="ai-summary-panel" id="ai-summary-panel">
                {/* Medical Safety Disclaimer Banner */}
                <div className="ai-draft-disclaimer-banner" id="ai-draft-disclaimer">
                  <span className="disclaimer-icon">⚠️</span>
                  <span className="disclaimer-text">
                    {aiSummary.draftDisclaimer ||
                      'AI-generated clinical summary — draft for doctor review. Verify against the original records. This is not a diagnosis or treatment recommendation.'}
                  </span>
                </div>

                {/* Truncation Warning Banner (if history exceeded safe input budget) */}
                {aiSummary.truncated && (
                  <div className="ai-truncation-warning-banner" id="ai-truncation-warning">
                    <span>⚠️</span>
                    <span>
                      <strong>History Truncated:</strong> Clinical history exceeded input safety limits. Summarized{' '}
                      <strong>{aiSummary.consultationsIncluded}</strong> of{' '}
                      <strong>{aiSummary.totalEligibleConsultations}</strong> recorded consultations (most recent visits prioritized).
                    </span>
                  </div>
                )}

                {/* Metadata & Actions Header */}
                <div className="ai-summary-meta-header">
                  <div className="ai-summary-meta-info">
                    <span className="ai-meta-badge">
                      📋 {aiSummary.consultationsIncluded} of {aiSummary.totalEligibleConsultations} Consultations Summarized
                    </span>
                    {aiSummary.generatedAt && (
                      <span className="ai-meta-time">
                        Draft Generated: {formatDateTime(aiSummary.generatedAt)}
                      </span>
                    )}
                  </div>
                  <div className="ai-summary-actions">
                    <button
                      type="button"
                      id="btn-copy-ai-summary"
                      className="btn-copy-summary"
                      onClick={handleCopySummary}
                      title="Copy AI summary text to clipboard"
                    >
                      {copySuccess ? '✓ Copied!' : '📋 Copy Summary'}
                    </button>
                    <button
                      type="button"
                      id="btn-regenerate-ai-summary-inner"
                      className="btn-regenerate-summary"
                      onClick={executeAiSummaryRequest}
                      disabled={aiLoading}
                      title="Regenerate summary draft"
                    >
                      {aiLoading ? <span className="spinner-sm" /> : '↻ Regenerate'}
                    </button>
                    <button
                      type="button"
                      className="btn-close-summary"
                      onClick={() => setAiSummary(null)}
                      title="Dismiss AI summary panel"
                    >
                      ✕ Dismiss
                    </button>
                  </div>
                </div>

                {/* Structured Summary Text Content Box (rendered safely as text, not HTML) */}
                <div className="ai-summary-content" id="ai-summary-content">
                  <pre className="ai-summary-text">{aiSummary.summary}</pre>
                </div>

                <small className="ai-disclosure-footnote">
                  🔒 Transmission Notice: Consultation notes de-identified and processed via the configured backend AI provider. Not saved to permanent records.
                </small>
              </div>
            )}
          </div>
        )}

        {historyError && (
          <div className="portal-alert alert-error" role="alert">
            <span>⚠️</span>
            <div>{historyError}</div>
          </div>
        )}

        {loadingHistory && consultationHistory.length === 0 ? (
          <div className="portal-loading-state">
            <span className="spinner-sm" />
            <span>Loading consultation history...</span>
          </div>
        ) : consultationHistory.length === 0 ? (
          <div className="portal-empty-state" id="doctor-history-empty">
            <span className="empty-icon">📋</span>
            <p className="empty-text">
              {selectedPatientFilter
                ? `No recorded consultations found with ${selectedPatientFilter.name}.`
                : 'No consultation records documented yet.'}
            </p>
            <small className="empty-hint">
              When you document consultation notes for patient visits, they will appear in your clinical history timeline.
            </small>
          </div>
        ) : (
          <div className="history-timeline" id="doctor-history-list">
            {consultationHistory.map((item) => (
              <div key={item.consultationId} className="history-card" id={`doctor-history-card-${item.consultationId}`}>
                <div className="history-card-header">
                  <div className="history-header-main">
                    <span className="history-date-badge">
                      📅 {formatDateTime(item.appointmentStartTime)}
                    </span>
                    <span className="history-patient-name">
                      🧑‍⚕️ Patient: <strong>{item.patientName || `Patient #${item.patientId}`}</strong>
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

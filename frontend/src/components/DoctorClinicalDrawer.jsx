import { useState, useEffect, useCallback } from 'react';
import {
  getConsultation,
  createConsultation,
  updateConsultation,
  getPrescriptions,
  createPrescription,
} from '../services/appointmentApi';
import { formatDateTime, formatDateOnly, toDateKey } from '../utils/dateTimeUtils';

/**
 * DoctorClinicalDrawer component.
 * Allows the attending doctor to document clinical consultation notes/diagnosis,
 * treatment advice, recommended follow-up date and notes, and prescribe medications for an appointment.
 *
 * @param {Object} props
 * @param {Object} props.appointment - Appointment object
 * @param {string} props.token - In-memory JWT access token
 * @param {Function} [props.onClose] - Callback when closing the clinical view
 */
export default function DoctorClinicalDrawer({ appointment, token, onClose }) {
  const [consultation, setConsultation] = useState(null);
  const [prescriptions, setPrescriptions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [apiError, setApiError] = useState(null);
  const [apiSuccess, setApiSuccess] = useState(null);

  // Safe today's date key initialized once on mount
  const [todayKey] = useState(() => toDateKey(new Date()));

  // Consultation Form state
  const [notes, setNotes] = useState('');
  const [treatment, setTreatment] = useState('');
  const [followUpDate, setFollowUpDate] = useState('');
  const [followUpNotes, setFollowUpNotes] = useState('');
  const [savingConsultation, setSavingConsultation] = useState(false);
  const [isEditing, setIsEditing] = useState(false);

  // Prescription Form state
  const [medicationName, setMedicationName] = useState('');
  const [dosage, setDosage] = useState('');
  const [instructions, setInstructions] = useState('');
  const [duration, setDuration] = useState('');
  const [savingPrescription, setSavingPrescription] = useState(false);

  const appointmentId = appointment?.id;

  // Fetch consultation and prescriptions
  const loadClinicalData = useCallback(async () => {
    if (!token || !appointmentId) return;
    setLoading(true);
    setApiError(null);

    try {
      // 1. Fetch consultation (may be 404 if not yet recorded)
      try {
        const consultData = await getConsultation(token, appointmentId);
        setConsultation(consultData);
        setNotes(consultData.notes || '');
        setTreatment(consultData.treatment || '');
        setFollowUpDate(consultData.followUpDate || '');
        setFollowUpNotes(consultData.followUpNotes || '');
        setIsEditing(false);
      } catch (err) {
        if (err.status === 404) {
          setConsultation(null);
          setNotes('');
          setTreatment('');
          setFollowUpDate('');
          setFollowUpNotes('');
          setIsEditing(true);
        } else {
          throw err;
        }
      }

      // 2. Fetch prescriptions
      const rxData = await getPrescriptions(token, appointmentId);
      setPrescriptions(Array.isArray(rxData) ? rxData : []);
    } catch (err) {
      setApiError(err.message || 'Unable to load clinical records for this appointment.');
    } finally {
      setLoading(false);
    }
  }, [token, appointmentId]);

  useEffect(() => {
    let active = true;
    const timer = setTimeout(() => {
      if (active) {
        loadClinicalData();
      }
    }, 0);
    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, [loadClinicalData]);

  // Handle Save / Update Consultation Record
  const handleSaveConsultation = async (e) => {
    e.preventDefault();
    if (savingConsultation) return;

    if (!notes.trim()) {
      setApiError('Clinical notes or diagnosis are required.');
      return;
    }

    if (followUpDate) {
      if (followUpDate < todayKey) {
        setApiError('Recommended follow-up date cannot be in the past.');
        return;
      }
    }

    if (followUpNotes && followUpNotes.length > 1000) {
      setApiError('Follow-up notes cannot exceed 1000 characters.');
      return;
    }

    setSavingConsultation(true);
    setApiError(null);
    setApiSuccess(null);

    try {
      const payload = {
        notes: notes.trim(),
        treatment: treatment.trim(),
        followUpDate: followUpDate || null,
        followUpNotes: followUpNotes.trim() || null,
      };

      let saved;
      if (consultation?.id) {
        // Update existing record
        saved = await updateConsultation(token, appointment.id, payload);
        setApiSuccess(`Consultation record #${saved.id} updated successfully.`);
      } else {
        // Create new record
        saved = await createConsultation(token, appointment.id, payload);
        setApiSuccess(`Consultation record #${saved.id} created successfully.`);
      }

      setConsultation(saved);
      setIsEditing(false);
    } catch (err) {
      setApiError(err.message || 'Failed to save consultation record.');
    } finally {
      setSavingConsultation(false);
    }
  };

  // Handle Add Prescription Item
  const handleAddPrescription = async (e) => {
    e.preventDefault();
    if (savingPrescription) return;

    if (!medicationName.trim() || !dosage.trim() || !instructions.trim()) {
      setApiError('Medication name, dosage, and instructions are required.');
      return;
    }

    setSavingPrescription(true);
    setApiError(null);
    setApiSuccess(null);

    try {
      const added = await createPrescription(token, appointment.id, {
        medicationName: medicationName.trim(),
        dosage: dosage.trim(),
        instructions: instructions.trim(),
        duration: duration.trim(),
      });

      setApiSuccess(`Prescription item "${added.medicationName}" added.`);
      setMedicationName('');
      setDosage('');
      setInstructions('');
      setDuration('');

      // Refresh prescriptions list
      const updatedRx = await getPrescriptions(token, appointment.id);
      setPrescriptions(Array.isArray(updatedRx) ? updatedRx : []);
    } catch (err) {
      setApiError(err.message || 'Failed to prescribe medication.');
    } finally {
      setSavingPrescription(false);
    }
  };

  return (
    <div className="clinical-drawer" id={`doctor-clinical-drawer-${appointment.id}`}>
      <div className="clinical-drawer-header">
        <div>
          <h4 className="clinical-title">
            📋 Clinical Documentation &amp; Prescriptions
          </h4>
          <span className="clinical-subtitle">
            Consultation #{appointment.id} &bull; Patient: <strong>{appointment.patientName || `Patient #${appointment.patientId}`}</strong>
          </span>
        </div>
        {onClose && (
          <button type="button" className="btn-close-drawer" onClick={onClose} title="Close clinical view">
            ✕
          </button>
        )}
      </div>

      {apiSuccess && (
        <div className="portal-alert alert-success" role="alert">
          <span>✓</span>
          <div>{apiSuccess}</div>
          <button className="alert-close-btn" onClick={() => setApiSuccess(null)}>✕</button>
        </div>
      )}

      {apiError && (
        <div className="portal-alert alert-error" role="alert">
          <span>⚠️</span>
          <div>{apiError}</div>
          <button className="alert-close-btn" onClick={() => setApiError(null)}>✕</button>
        </div>
      )}

      {loading ? (
        <div className="portal-loading-state">
          <span className="spinner-sm" />
          <span>Loading clinical records...</span>
        </div>
      ) : (
        <div className="clinical-content-grid">
          {/* LEFT: Consultation Notes & Treatment */}
          <div className="clinical-box consultation-box">
            <div className="box-header">
              <h5 className="box-title">📝 Diagnosis &amp; Consultation Notes</h5>
              {consultation && !isEditing && (
                <button
                  type="button"
                  className="btn-text-action"
                  onClick={() => setIsEditing(true)}
                  id="btn-edit-consultation"
                >
                  ✎ Edit Notes
                </button>
              )}
            </div>

            {consultation && !isEditing ? (
              <div className="consultation-display">
                <div className="display-group">
                  <span className="display-label">Clinical Notes / Diagnosis</span>
                  <p className="display-text">{consultation.notes}</p>
                </div>

                {consultation.treatment && (
                  <div className="display-group">
                    <span className="display-label">Treatment Advice &amp; Recommendations</span>
                    <p className="display-text">{consultation.treatment}</p>
                  </div>
                )}

                <div className="display-group follow-up-summary-group">
                  <span className="display-label">Recommended Follow-up</span>
                  {consultation.followUpDate ? (
                    <div className="follow-up-card-view" id={`follow-up-display-${appointment.id}`}>
                      <div className="follow-up-header-row">
                        <span className="follow-up-badge">
                          📅 Follow-up: <strong>{formatDateOnly(consultation.followUpDate)}</strong>
                        </span>
                        <span className="follow-up-date-iso">({consultation.followUpDate})</span>
                      </div>
                      {consultation.followUpNotes ? (
                        <p className="follow-up-notes-text">{consultation.followUpNotes}</p>
                      ) : (
                        <span className="display-text-muted">No additional follow-up notes recorded.</span>
                      )}
                    </div>
                  ) : (
                    <div className="follow-up-empty-card">
                      <span className="display-text-muted">No follow-up recommendation scheduled for this visit.</span>
                    </div>
                  )}
                </div>

                <div className="display-meta">
                  <span>Documented on {formatDateTime(consultation.createdAt)}</span>
                </div>
              </div>
            ) : (
              <form className="consultation-form" onSubmit={handleSaveConsultation}>
                <div className="form-group">
                  <label className="form-label" htmlFor={`notes-${appointment.id}`}>
                    Clinical Diagnosis &amp; Notes <span className="req">*</span>
                  </label>
                  <textarea
                    id={`notes-${appointment.id}`}
                    className="form-textarea"
                    rows="3"
                    placeholder="Enter clinical observations, examination findings, and primary diagnosis..."
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                    disabled={savingConsultation}
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label" htmlFor={`treatment-${appointment.id}`}>
                    Treatment Advice &amp; Medical Recommendations
                  </label>
                  <textarea
                    id={`treatment-${appointment.id}`}
                    className="form-textarea"
                    rows="2"
                    placeholder="Enter patient instructions, lifestyle guidance, or non-pharmacological care..."
                    value={treatment}
                    onChange={(e) => setTreatment(e.target.value)}
                    disabled={savingConsultation}
                  />
                </div>

                <div className="form-group follow-up-form-section">
                  <div className="follow-up-section-header">
                    <label className="form-label" htmlFor={`follow-up-date-${appointment.id}`}>
                      📅 Recommended Follow-up Schedule (Optional)
                    </label>
                    {followUpDate && (
                      <button
                        type="button"
                        className="btn-text-clear"
                        onClick={() => {
                          setFollowUpDate('');
                          setFollowUpNotes('');
                        }}
                        disabled={savingConsultation}
                      >
                        Clear Follow-up
                      </button>
                    )}
                  </div>

                  <div className="form-row-2">
                    <div className="form-field">
                      <label className="field-sublabel" htmlFor={`follow-up-date-${appointment.id}`}>
                        Follow-up Date
                      </label>
                      <input
                        id={`follow-up-date-${appointment.id}`}
                        type="date"
                        className="form-input"
                        min={todayKey}
                        value={followUpDate}
                        onChange={(e) => setFollowUpDate(e.target.value)}
                        disabled={savingConsultation}
                      />
                    </div>
                    <div className="form-field">
                      <label className="field-sublabel" htmlFor={`follow-up-notes-${appointment.id}`}>
                        Follow-up Notes / Reason
                      </label>
                      <input
                        id={`follow-up-notes-${appointment.id}`}
                        type="text"
                        className="form-input"
                        maxLength={1000}
                        placeholder="e.g. Review lab results, symptom check..."
                        value={followUpNotes}
                        onChange={(e) => setFollowUpNotes(e.target.value)}
                        disabled={savingConsultation}
                      />
                    </div>
                  </div>
                </div>

                <div className="box-form-actions">
                  <button
                    id="btn-save-consultation"
                    type="submit"
                    className="btn btn-primary btn-sm"
                    disabled={savingConsultation}
                  >
                    {savingConsultation ? (
                      <>
                        <span className="spinner-sm" />
                        Saving...
                      </>
                    ) : (
                      consultation?.id ? 'Update Consultation' : 'Save Consultation Record'
                    )}
                  </button>

                  {consultation && (
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      onClick={() => {
                        setNotes(consultation.notes || '');
                        setTreatment(consultation.treatment || '');
                        setFollowUpDate(consultation.followUpDate || '');
                        setFollowUpNotes(consultation.followUpNotes || '');
                        setIsEditing(false);
                      }}
                      disabled={savingConsultation}
                    >
                      Cancel Edit
                    </button>
                  )}
                </div>
              </form>
            )}
          </div>

          {/* RIGHT: Prescriptions (Rx) */}
          <div className="clinical-box prescription-box">
            <div className="box-header">
              <h5 className="box-title">💊 Prescribed Medications (Rx)</h5>
              <span className="rx-count-pill">{prescriptions.length} Prescribed</span>
            </div>

            {/* Prescriptions List */}
            {prescriptions.length === 0 ? (
              <div className="rx-empty-state">
                <span className="rx-empty-icon">💊</span>
                <p>No medications prescribed for this consultation yet.</p>
              </div>
            ) : (
              <div className="rx-list">
                {prescriptions.map((rx) => (
                  <div key={rx.id} className="rx-item">
                    <div className="rx-item-header">
                      <span className="rx-med-name">{rx.medicationName}</span>
                      <span className="rx-dosage-pill">{rx.dosage}</span>
                    </div>
                    <div className="rx-item-details">
                      <span className="rx-instructions">👉 {rx.instructions}</span>
                      {rx.duration && (
                        <span className="rx-duration">⏱ Duration: {rx.duration}</span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}

            {/* Add Prescription Form */}
            <form className="add-prescription-form" onSubmit={handleAddPrescription}>
              <h6 className="form-subheading">+ Add Medication to Prescription</h6>

              <div className="form-row-2">
                <div className="form-group">
                  <label className="form-label" htmlFor={`rx-med-${appointment.id}`}>
                    Medication Name <span className="req">*</span>
                  </label>
                  <input
                    id={`rx-med-${appointment.id}`}
                    type="text"
                    className="form-input"
                    placeholder="e.g. Amoxicillin 500mg"
                    value={medicationName}
                    onChange={(e) => setMedicationName(e.target.value)}
                    disabled={savingPrescription}
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label" htmlFor={`rx-dosage-${appointment.id}`}>
                    Dosage <span className="req">*</span>
                  </label>
                  <input
                    id={`rx-dosage-${appointment.id}`}
                    type="text"
                    className="form-input"
                    placeholder="e.g. 1 capsule"
                    value={dosage}
                    onChange={(e) => setDosage(e.target.value)}
                    disabled={savingPrescription}
                    required
                  />
                </div>
              </div>

              <div className="form-row-2">
                <div className="form-group">
                  <label className="form-label" htmlFor={`rx-inst-${appointment.id}`}>
                    Instructions <span className="req">*</span>
                  </label>
                  <input
                    id={`rx-inst-${appointment.id}`}
                    type="text"
                    className="form-input"
                    placeholder="e.g. Twice daily after meals"
                    value={instructions}
                    onChange={(e) => setInstructions(e.target.value)}
                    disabled={savingPrescription}
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label" htmlFor={`rx-dur-${appointment.id}`}>
                    Duration
                  </label>
                  <input
                    id={`rx-dur-${appointment.id}`}
                    type="text"
                    className="form-input"
                    placeholder="e.g. 5 days"
                    value={duration}
                    onChange={(e) => setDuration(e.target.value)}
                    disabled={savingPrescription}
                  />
                </div>
              </div>

              <div className="box-form-actions">
                <button
                  id="btn-add-prescription"
                  type="submit"
                  className="btn btn-primary btn-sm"
                  disabled={savingPrescription}
                >
                  {savingPrescription ? (
                    <>
                      <span className="spinner-sm" />
                      Prescribing...
                    </>
                  ) : (
                    '+ Add Medication'
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

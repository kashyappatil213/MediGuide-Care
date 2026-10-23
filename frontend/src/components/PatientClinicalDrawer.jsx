import { useState, useEffect, useCallback } from 'react';
import { getConsultation, getPrescriptions } from '../services/appointmentApi';
import { formatDateTime, formatDateOnly } from '../utils/dateTimeUtils';

/**
 * PatientClinicalDrawer component.
 * Read-only clinical records view for patients to review physician notes,
 * diagnosis, treatment guidance, recommended follow-up schedule, and prescribed medications.
 *
 * @param {Object} props
 * @param {Object} props.appointment - Appointment object
 * @param {string} props.token - In-memory JWT access token
 * @param {Function} [props.onClose] - Callback to dismiss the drawer
 */
export default function PatientClinicalDrawer({ appointment, token, onClose }) {
  const [consultation, setConsultation] = useState(null);
  const [prescriptions, setPrescriptions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const appointmentId = appointment?.id;

  const loadClinicalData = useCallback(async () => {
    if (!token || !appointmentId) return;
    setLoading(true);
    setError(null);

    try {
      // 1. Fetch consultation notes (404 handled gracefully as not-yet-created)
      try {
        const consultData = await getConsultation(token, appointmentId);
        setConsultation(consultData);
      } catch (err) {
        if (err.status === 404) {
          setConsultation(null);
        } else {
          throw err;
        }
      }

      // 2. Fetch prescriptions
      const rxData = await getPrescriptions(token, appointmentId);
      setPrescriptions(Array.isArray(rxData) ? rxData : []);
    } catch (err) {
      setError(err.message || 'Unable to retrieve clinical consultation records.');
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

  return (
    <div className="clinical-drawer patient-clinical-view" id={`patient-clinical-drawer-${appointment.id}`}>
      <div className="clinical-drawer-header">
        <div>
          <h4 className="clinical-title">
            📄 Clinical Summary &amp; Prescriptions
          </h4>
          <span className="clinical-subtitle">
            Consultation #{appointment.id} &bull; Attending Physician: <strong>Dr. {appointment.doctorName?.replace(/^Dr\.?\s*/i, '') || appointment.doctorId}</strong>
          </span>
        </div>
        {onClose && (
          <button type="button" className="btn-close-drawer" onClick={onClose} title="Close summary">
            ✕
          </button>
        )}
      </div>

      {error && (
        <div className="portal-alert alert-error" role="alert">
          <span>⚠️</span>
          <div>{error}</div>
        </div>
      )}

      {loading ? (
        <div className="portal-loading-state">
          <span className="spinner-sm" />
          <span>Retrieving your consultation records &amp; prescriptions...</span>
        </div>
      ) : (
        <div className="clinical-content-grid">
          {/* LEFT: Consultation Notes & Advice */}
          <div className="clinical-box consultation-box">
            <div className="box-header">
              <h5 className="box-title">📝 Doctor's Clinical Diagnosis &amp; Notes</h5>
              {consultation && (
                <span className="recorded-date">Recorded: {formatDateTime(consultation.createdAt)}</span>
              )}
            </div>

            {consultation ? (
              <div className="consultation-display">
                <div className="display-group">
                  <span className="display-label">Diagnosis &amp; Observations</span>
                  <p className="display-text">{consultation.notes}</p>
                </div>

                {consultation.treatment ? (
                  <div className="display-group">
                    <span className="display-label">Treatment Advice &amp; Lifestyle Guidance</span>
                    <p className="display-text">{consultation.treatment}</p>
                  </div>
                ) : (
                  <div className="display-group">
                    <span className="display-label">Treatment Advice</span>
                    <p className="display-text-muted">No specific treatment advice documented.</p>
                  </div>
                )}

                <div className="display-group follow-up-patient-group">
                  <span className="display-label">Recommended Follow-up</span>
                  {consultation.followUpDate ? (
                    <div className="patient-follow-up-card" id={`patient-follow-up-${appointment.id}`}>
                      <div className="patient-follow-up-badge-row">
                        <span className="follow-up-badge">
                          📅 Recommended Date: <strong>{formatDateOnly(consultation.followUpDate)}</strong>
                        </span>
                        <span className="follow-up-date-iso">({consultation.followUpDate})</span>
                      </div>
                      {consultation.followUpNotes && (
                        <div className="patient-follow-up-notes">
                          <span className="field-sublabel">Physician Notes / Instructions:</span>
                          <p className="display-text">{consultation.followUpNotes}</p>
                        </div>
                      )}
                      <p className="patient-follow-up-hint">
                        ℹ️ This is a clinical recommendation from your physician, not an automatically booked appointment. You can book an appointment near this date through the appointment portal when slots become available.
                      </p>
                    </div>
                  ) : (
                    <p className="display-text-muted">No follow-up was recommended for this visit.</p>
                  )}
                </div>
              </div>
            ) : (
              <div className="consultation-empty-notice">
                <span className="empty-icon-sm">📋</span>
                <p>Clinical notes have not yet been documented by your physician for this consultation.</p>
                <small>Once your attending doctor records diagnosis and treatment advice, it will appear here.</small>
              </div>
            )}
          </div>

          {/* RIGHT: Prescribed Medications (Rx) */}
          <div className="clinical-box prescription-box">
            <div className="box-header">
              <h5 className="box-title">💊 Prescribed Medications (Rx)</h5>
              <span className="rx-count-pill">{prescriptions.length} Meds</span>
            </div>

            {prescriptions.length === 0 ? (
              <div className="rx-empty-state">
                <span className="rx-empty-icon">💊</span>
                <p>No medications have been prescribed for this consultation.</p>
                <small>If your physician issues a digital prescription, medication details and dosage will appear here.</small>
              </div>
            ) : (
              <div className="rx-list">
                {prescriptions.map((rx) => (
                  <div key={rx.id} className="rx-item">
                    <div className="rx-item-header">
                      <span className="rx-med-name">💊 {rx.medicationName}</span>
                      <span className="rx-dosage-pill">{rx.dosage}</span>
                    </div>
                    <div className="rx-item-details">
                      <span className="rx-instructions">👉 Instructions: {rx.instructions}</span>
                      {rx.duration && (
                        <span className="rx-duration">⏱ Course: {rx.duration}</span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

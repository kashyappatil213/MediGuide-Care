import DoctorPortal from './DoctorPortal';
import PatientPortal from './PatientPortal';
import './RolePortal.css';

/**
 * Role-based landing view component for authenticated MediGuide users.
 *
 * Coordinates and renders the respective clinical portal based on user role:
 * - PATIENT: Patient Care Portal with active doctor availability discovery, booking, and personal appointments.
 * - DOCTOR: Doctor Clinical Portal with availability schedule management and patient consultations queue.
 *
 * NOTE: These are UI views intended to guide user workflow. Backend Spring Security
 * filters remain authoritative for all role permissions and data access.
 *
 * @param {Object} props
 * @param {Object} props.user - Safe authenticated user { id, fullName, email, role }
 * @param {string} props.token - In-memory JWT access token
 */
export default function RolePortal({ user, token }) {
  if (!user || (user.role !== 'PATIENT' && user.role !== 'DOCTOR')) {
    return null;
  }

  const isDoctor = user.role === 'DOCTOR';

  return (
    <div
      className={`role-portal-card ${isDoctor ? 'doctor-portal' : 'patient-portal'}`}
      id={isDoctor ? 'doctor-portal-view' : 'patient-portal-view'}
    >
      {isDoctor ? (
        <DoctorPortal user={user} token={token} />
      ) : (
        <PatientPortal user={user} token={token} />
      )}
    </div>
  );
}

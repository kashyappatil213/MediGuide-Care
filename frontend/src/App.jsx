import { useState, useEffect } from 'react';
import AuthCard from './components/AuthCard';
import RolePortal from './components/RolePortal';
import { API_BASE_URL } from './services/authApi';
import { saveSession, clearSession, loadStoredSession, SUPPORTED_ROLES } from './services/sessionManager';
import './App.css';

export default function App() {
  const [healthData, setHealthData] = useState(null);
  const [isBackendHealthy, setIsBackendHealthy] = useState(null);

  // Authentication state initialized from tab-scoped sessionStorage if present and valid
  const [auth, setAuth] = useState(() => {
    const stored = loadStoredSession();
    return stored || {
      isAuthenticated: false,
      token: null,
      user: null,
    };
  });

  const handleLoginSuccess = ({ accessToken, user }) => {
    // Safety check: only persist if user has a valid supported role
    if (!user || !SUPPORTED_ROLES.includes(user.role)) {
      handleLogout();
      return;
    }

    saveSession({ token: accessToken, user });
    setAuth({
      isAuthenticated: true,
      token: accessToken,
      user,
    });
  };

  const handleLogout = () => {
    clearSession();
    setAuth({
      isAuthenticated: false,
      token: null,
      user: null,
    });
  };

  // Fail-safe: if in-memory user ever lacks a supported role, clear and return to signed-out view
  useEffect(() => {
    if (auth.isAuthenticated && (!auth.user || !SUPPORTED_ROLES.includes(auth.user.role))) {
      const timer = setTimeout(() => {
        handleLogout();
      }, 0);
      return () => clearTimeout(timer);
    }
  }, [auth]);

  // Subtle background health check for operational readiness
  const checkHealth = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/health`, {
        headers: {
          'Accept': 'application/json',
          'bypass-tunnel-reminder': 'true',
          'ngrok-skip-browser-warning': 'true',
        },
      });
      if (response.ok) {
        const data = await response.json();
        setHealthData(data);
        setIsBackendHealthy(data.status === 'UP');
      } else {
        setIsBackendHealthy(false);
      }
    } catch {
      setIsBackendHealthy(false);
    }
  };

  useEffect(() => {
    checkHealth();
  }, []);

  return (
    <div className="app-container">
      {/* Polished Top Navigation */}
      <header className="navbar">
        <div className="nav-brand">
          <div className="logo-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M12 2v20M2 12h20" />
            </svg>
          </div>
          <div>
            <span className="brand-title">MediGuide Care</span>
            <span className="brand-sub">Clinical Consultation &amp; Care Platform</span>
          </div>
        </div>

        <div className="nav-actions">
          {auth.isAuthenticated && auth.user ? (
            <div className="nav-user-cluster">
              <div className="nav-user-badge">
                <span className="user-icon">{auth.user.role === 'DOCTOR' ? '🩺' : '🧑‍⚕️'}</span>
                <span className="user-name">
                  {auth.user.role === 'DOCTOR' ? `Dr. ${auth.user.fullName?.replace(/^Dr\.?\s*/i, '')}` : auth.user.fullName}
                </span>
                <span className={`role-pill ${auth.user.role === 'DOCTOR' ? 'pill-doctor' : 'pill-patient'}`}>
                  {auth.user.role}
                </span>
                {auth.user.publicId && (
                  <span className="nav-id-tag">{auth.user.publicId}</span>
                )}
              </div>
              <button
                id="nav-btn-signout"
                className="btn-nav-signout"
                onClick={handleLogout}
                type="button"
                title="Sign out of MediGuide"
              >
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                  <polyline points="16 17 21 12 16 7" />
                  <line x1="21" y1="12" x2="9" y2="12" />
                </svg>
                Sign Out
              </button>
            </div>
          ) : (
            <div className="nav-status-badge">
              <span className={`status-dot ${isBackendHealthy ? 'dot-online' : isBackendHealthy === false ? 'dot-offline' : 'dot-checking'}`} />
              <span className="status-text">
                {isBackendHealthy ? 'System Active' : isBackendHealthy === false ? 'Service Offline' : 'Connecting...'}
              </span>
            </div>
          )}
        </div>
      </header>

      {/* Main Content Area */}
      <main className="main-content">
        {auth.isAuthenticated && auth.user ? (
          /* Clean Authenticated Clinical Experience */
          <div className="authenticated-portal-wrapper">
            <RolePortal user={auth.user} token={auth.token} />
          </div>
        ) : (
          /* Clean Landing & Authentication Experience */
          <div className="landing-wrapper">
            <section className="hero-section">
              <span className="hero-pill-badge">✨ Next-Generation Healthcare Workflow</span>
              <h1 className="hero-title">
                Doctor–Patient Consultation &amp; <br />
                <span className="gradient-text">Clinic Management System</span>
              </h1>
              <p className="hero-subtitle">
                Connect with verified medical doctors, manage outpatient appointments,
                generate clinical prescriptions, and access comprehensive digital health records securely.
              </p>
            </section>

            {/* Authentication Component */}
            <AuthCard
              authState={auth}
              onLoginSuccess={handleLoginSuccess}
              onLogout={handleLogout}
            />
          </div>
        )}
      </main>

      {/* Clean Modern Footer */}
      <footer className="footer">
        <div className="footer-content">
          <p className="footer-brand">
            <strong>MediGuide Care</strong> &bull; Secure Clinic Consultation System
          </p>
          <p className="footer-sub">
            Built for certified medical professionals and patients. All consultation records and health data are protected.
          </p>
        </div>
      </footer>
    </div>
  );
}

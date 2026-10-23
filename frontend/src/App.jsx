import { useState, useEffect } from 'react';
import AuthCard from './components/AuthCard';
import { API_BASE_URL } from './services/authApi';
import { saveSession, clearSession, loadStoredSession, SUPPORTED_ROLES } from './services/sessionManager';
import './App.css';

export default function App() {
  const [healthData, setHealthData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [latency, setLatency] = useState(null);
  const [lastChecked, setLastChecked] = useState(null);
  const [showRawJson, setShowRawJson] = useState(false);

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

  const fetchHealthStatus = async () => {
    setLoading(true);
    setError(null);
    const startTime = performance.now();

    try {
      const response = await fetch(`${API_BASE_URL}/api/health`, {
        headers: {
          'Accept': 'application/json',
        },
      });

      const elapsed = Math.round(performance.now() - startTime);
      setLatency(elapsed);

      if (!response.ok) {
        throw new Error(`Server responded with HTTP status ${response.status}`);
      }

      const data = await response.json();
      setHealthData(data);
      setLastChecked(new Date().toLocaleTimeString());
    } catch (err) {
      setError(err.message || 'Unable to connect to Spring Boot backend');
      setHealthData(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const timer = setTimeout(() => {
      fetchHealthStatus();
    }, 0);
    return () => clearTimeout(timer);
  }, []);

  const isConnected = healthData?.status === 'UP' && healthData?.database === 'Connected';

  return (
    <div className="app-container">
      {/* Top Navigation */}
      <header className="navbar">
        <div className="nav-brand">
          <div className="logo-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M12 2v20M2 12h20" />
            </svg>
          </div>
          <div>
            <span className="brand-title">MediGuide</span>
            <span className="brand-sub">Clinic Consultation System</span>
          </div>
        </div>

        <div className="nav-badges">
          {auth.isAuthenticated && (
            <span className="badge badge-user">
              {auth.user.role === 'DOCTOR' ? '🩺 Dr. ' : '🧑‍⚕️ '}
              {auth.user.fullName}
            </span>
          )}
        </div>
      </header>

      {/* Main Content */}
      <main className="main-content">
        {/* Hero Section */}
        <section className="hero-section">
          <h1 className="hero-title">
            Doctor–Patient Consultation &amp; <br />
            <span className="gradient-text">Clinic Management System</span>
          </h1>
          <p className="hero-subtitle">
            Secure, reliable clinic consultation and follow-up platform built with Spring Boot,
            PostgreSQL, and React.
          </p>

          <div className="hero-actions">
            <a
              id="btn-access-portal"
              href="#auth-portal"
              className="btn btn-primary"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4" />
                <polyline points="10 17 15 12 10 7" />
                <line x1="15" y1="12" x2="3" y2="12" />
              </svg>
              {auth.isAuthenticated ? 'Active User Portal' : 'Sign In / Register'}
            </a>

            <button
              id="btn-test-health"
              className="btn btn-secondary"
              onClick={fetchHealthStatus}
              disabled={loading}
            >
              {loading ? (
                <>
                  <span className="spinner"></span>
                  Checking Backend...
                </>
              ) : (
                <>
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M23 4v6h-6M1 20v-6h6" />
                    <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
                  </svg>
                  Re-test Diagnostics
                </>
              )}
            </button>

            <a
              id="link-swagger-docs"
              href={`${API_BASE_URL}/swagger-ui.html`}
              target="_blank"
              rel="noreferrer"
              className="btn btn-secondary"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                <polyline points="14 2 14 8 20 8" />
                <line x1="16" y1="13" x2="8" y2="13" />
                <line x1="16" y1="17" x2="8" y2="17" />
                <polyline points="10 9 9 9 8 9" />
              </svg>
              Swagger Docs
            </a>
          </div>
        </section>

        {/* Authentication Section (Login & Registration UI) */}
        <AuthCard
          authState={auth}
          onLoginSuccess={handleLoginSuccess}
          onLogout={handleLogout}
        />

        {/* Live Diagnostics Card */}
        <section className="diagnostics-card">
          <div className="diagnostics-header">
            <div className="diagnostics-title-group">
              <div className={`status-indicator-dot ${isConnected ? 'dot-online' : error ? 'dot-error' : 'dot-pending'}`} />
              <div>
                <h2>Fullstack System Status</h2>
                <p className="section-desc">Live synchronization between React UI, Spring Boot API, and PostgreSQL</p>
              </div>
            </div>

            <div className="header-meta">
              {lastChecked && <span className="timestamp-badge">Checked at {lastChecked}</span>}
              <button
                className="btn-icon"
                onClick={fetchHealthStatus}
                title="Refresh Status"
                disabled={loading}
              >
                ↻
              </button>
            </div>
          </div>

          {/* Status Metric Grid */}
          <div className="metrics-grid">
            <div className="metric-box">
              <span className="metric-label">Spring Boot Backend</span>
              <div className="metric-val-row">
                <span className={`status-pill ${healthData?.status === 'UP' ? 'pill-success' : 'pill-danger'}`}>
                  {healthData?.status === 'UP' ? 'ONLINE (8080)' : (loading ? 'CONNECTING...' : 'OFFLINE')}
                </span>
              </div>
              <span className="metric-hint">REST Controller /api/health</span>
            </div>

            <div className="metric-box">
              <span className="metric-label">PostgreSQL Database</span>
              <div className="metric-val-row">
                <span className={`status-pill ${healthData?.database === 'Connected' ? 'pill-success' : 'pill-danger'}`}>
                  {healthData?.database || (loading ? 'CHECKING...' : 'DISCONNECTED')}
                </span>
              </div>
              <span className="metric-hint">
                {healthData?.databaseProduct ? `${healthData.databaseProduct} ${healthData.databaseVersion || ''}` : 'Database port 5432'}
              </span>
            </div>

            <div className="metric-box">
              <span className="metric-label">React Frontend Client</span>
              <div className="metric-val-row">
                <span className="status-pill pill-success">ACTIVE (Vite)</span>
              </div>
              <span className="metric-hint">React 19 + HMR Enabled</span>
            </div>

            <div className="metric-box">
              <span className="metric-label">API Response Latency</span>
              <div className="metric-val-row">
                <span className="latency-val">{latency !== null ? `${latency} ms` : '—'}</span>
              </div>
              <span className="metric-hint">End-to-end HTTP roundtrip</span>
            </div>
          </div>

          {/* Error Notice if any */}
          {error && (
            <div className="error-banner">
              <div className="error-icon">⚠️</div>
              <div>
                <strong>Backend Connection Notice:</strong>
                <p>{error}</p>
                <small>Ensure the Spring Boot backend is running on <code>localhost:8080</code> with PostgreSQL active.</small>
              </div>
            </div>
          )}

          {/* Raw JSON Toggle */}
          {healthData && (
            <div className="json-inspector">
              <button
                className="btn-text"
                onClick={() => setShowRawJson(!showRawJson)}
              >
                {showRawJson ? '▲ Hide Raw API Response' : '▼ View Raw Backend JSON Response'}
              </button>
              {showRawJson && (
                <pre className="json-code-block">
                  {JSON.stringify(healthData, null, 2)}
                </pre>
              )}
            </div>
          )}
        </section>

        {/* Phase Architecture & Stack Details */}
        <section className="architecture-grid">
          <div className="arch-card">
            <div className="arch-card-icon backend-icon">☕</div>
            <h3>Backend Layer</h3>
            <p className="arch-desc">Java 17 with Spring Boot 3.2</p>
            <ul className="arch-list">
              <li>Controller → Service → Repository design</li>
              <li>Spring Web Starter for REST endpoints</li>
              <li>Clean separation of concerns</li>
            </ul>
          </div>

          <div className="arch-card">
            <div className="arch-card-icon db-icon">🐘</div>
            <h3>Persistence Layer</h3>
            <p className="arch-desc">PostgreSQL &amp; Spring Data JPA</p>
            <ul className="arch-list">
              <li>Hibernate ORM schema auto-management</li>
              <li>Environment-variable secured credentials</li>
              <li>HikariCP high-performance connection pool</li>
            </ul>
          </div>

          <div className="arch-card">
            <div className="arch-card-icon frontend-icon">⚛️</div>
            <h3>Frontend Client</h3>
            <p className="arch-desc">React.js &amp; Modern Vanilla CSS</p>
            <ul className="arch-list">
              <li>Zero bloat design tokens &amp; components</li>
              <li>Responsive layout with dark medical theme</li>
              <li>Prepared for Doctor/Patient dashboards</li>
            </ul>
          </div>

          <div className="arch-card">
            <div className="arch-card-icon docs-icon">📖</div>
            <h3>API Documentation</h3>
            <p className="arch-desc">SpringDoc OpenAPI &amp; Swagger</p>
            <ul className="arch-list">
              <li>Automated interactive Swagger UI</li>
              <li>Ready for Postman collection exports</li>
              <li>Self-documenting clinic endpoints</li>
            </ul>
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer className="footer">
        <p>MediGuide — Clinic Consultation System</p>
        <p className="footer-sub">Simple and secure doctor–patient consultation and clinical follow-up management</p>
      </footer>
    </div>
  );
}


import { useState, useEffect, useCallback } from 'react';
import { registerUser, loginUser, verifyProtectedToken } from '../services/authApi';
import RolePortal from './RolePortal';
import './AuthCard.css';

/**
 * Authentication card component providing Login and Registration views
 * for Patients and Doctors in MediGuide Care.
 */
export default function AuthCard({ authState, onLoginSuccess, onLogout }) {
  const [mode, setMode] = useState('login'); // 'login' | 'register'
  const [loading, setLoading] = useState(false);
  const [apiError, setApiError] = useState(null);
  const [apiSuccess, setApiSuccess] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});

  // Protected API client verification states
  const [protectedStatus, setProtectedStatus] = useState('idle'); // 'idle' | 'checking' | 'success' | 'error'
  const [protectedData, setProtectedData] = useState(null);
  const [protectedError, setProtectedError] = useState(null);
  const [lastVerifiedAt, setLastVerifiedAt] = useState(null);

  // Authenticated connection verification check
  const checkProtectedConnection = useCallback(async (token) => {
    if (!token) return;
    setProtectedStatus('checking');
    setProtectedError(null);

    try {
      const data = await verifyProtectedToken(token);
      setProtectedData(data);
      setProtectedStatus('success');
      setLastVerifiedAt(new Date().toLocaleTimeString());
    } catch (err) {
      setProtectedStatus('error');
      setProtectedData(null);
      setProtectedError({
        status: err.status ?? 'Unknown',
        message: err.message || 'Verification failed. Could not validate JWT with backend.',
        isAuthFailure: err.status === 401 || err.status === 403,
      });
      setLastVerifiedAt(new Date().toLocaleTimeString());
    }
  }, []);

  // Run initial verification once when authenticated state is entered
  useEffect(() => {
    let active = true;

    if (authState?.isAuthenticated && authState?.token) {
      const timer = setTimeout(() => {
        if (active) {
          checkProtectedConnection(authState.token);
        }
      }, 0);

      return () => {
        active = false;
        clearTimeout(timer);
      };
    } else {
      const resetTimer = setTimeout(() => {
        if (active) {
          setProtectedStatus('idle');
          setProtectedData(null);
          setProtectedError(null);
          setLastVerifiedAt(null);
        }
      }, 0);

      return () => {
        active = false;
        clearTimeout(resetTimer);
      };
    }
  }, [authState?.isAuthenticated, authState?.token, checkProtectedConnection]);

  // Form states
  const [loginForm, setLoginForm] = useState({
    email: '',
    password: '',
  });

  const [registerForm, setRegisterForm] = useState({
    fullName: '',
    email: '',
    password: '',
    role: 'PATIENT', // default role
  });

  // Switch between views cleanly
  const switchMode = (targetMode) => {
    setMode(targetMode);
    setApiError(null);
    setFieldErrors({});
  };

  // Handle Login input changes
  const handleLoginChange = (e) => {
    const { name, value } = e.target;
    setLoginForm((prev) => ({ ...prev, [name]: value }));
    if (fieldErrors[name]) {
      setFieldErrors((prev) => ({ ...prev, [name]: null }));
    }
  };

  // Handle Register input changes
  const handleRegisterChange = (e) => {
    const { name, value } = e.target;
    setRegisterForm((prev) => ({ ...prev, [name]: value }));
    if (fieldErrors[name]) {
      setFieldErrors((prev) => ({ ...prev, [name]: null }));
    }
  };

  // Handle Register Form Submission
  const handleRegisterSubmit = async (e) => {
    e.preventDefault();
    if (loading) return;

    setApiError(null);
    setApiSuccess(null);
    const errors = {};

    // Basic client-side required field checks
    if (!registerForm.fullName.trim()) {
      errors.fullName = 'Full name is required';
    }
    if (!registerForm.email.trim()) {
      errors.email = 'Email address is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(registerForm.email.trim())) {
      errors.email = 'Please provide a valid email address';
    }
    if (!registerForm.password) {
      errors.password = 'Password is required';
    } else if (registerForm.password.length < 8) {
      errors.password = 'Password must be at least 8 characters long';
    }
    if (!registerForm.role || (registerForm.role !== 'PATIENT' && registerForm.role !== 'DOCTOR')) {
      errors.role = 'Please select a valid role (Patient or Doctor)';
    }

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }

    setLoading(true);

    try {
      const response = await registerUser({
        fullName: registerForm.fullName,
        email: registerForm.email,
        password: registerForm.password,
        role: registerForm.role,
      });

      // Feedback on success with public ID
      setApiSuccess(
        response.publicId
          ? `Account created successfully for ${response.fullName || registerForm.fullName}! Your MediGuide ID is ${response.publicId}. You can now sign in.`
          : `Account created successfully for ${response.fullName || registerForm.fullName}! You can now sign in.`
      );

      // Pre-fill email in login form and switch to login tab
      setLoginForm({
        email: registerForm.email.trim(),
        password: '',
      });

      // Reset registration form without storing password
      setRegisterForm({
        fullName: '',
        email: '',
        password: '',
        role: 'PATIENT',
      });

      setFieldErrors({});
      setMode('login');
    } catch (err) {
      setApiError(err.message || 'Registration could not be completed. Please try again.');
      if (err.fieldErrors) {
        setFieldErrors(err.fieldErrors);
      }
    } finally {
      setLoading(false);
    }
  };

  // Handle Login Form Submission
  const handleLoginSubmit = async (e) => {
    e.preventDefault();
    if (loading) return;

    setApiError(null);
    setApiSuccess(null);
    const errors = {};

    if (!loginForm.email.trim()) {
      errors.email = 'Email address is required';
    }
    if (!loginForm.password) {
      errors.password = 'Password is required';
    }

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }

    setLoading(true);

    try {
      const response = await loginUser({
        email: loginForm.email,
        password: loginForm.password,
      });

      // Clear entered password immediately
      setLoginForm((prev) => ({ ...prev, password: '' }));

      if (onLoginSuccess) {
        onLoginSuccess({
          accessToken: response.accessToken,
          user: response.user,
        });
      }

      setApiSuccess(`Welcome back, ${response.user?.fullName || 'User'}!`);
      setFieldErrors({});
    } catch (err) {
      setApiError(err.message || 'Invalid email or password');
      if (err.fieldErrors) {
        setFieldErrors(err.fieldErrors);
      }
    } finally {
      setLoading(false);
    }
  };

  // If user is currently authenticated, render active session view
  if (authState?.isAuthenticated && authState.user) {
    const { user, token } = authState;

    // Safety guard: Fail safely if role is missing or unsupported
    if (!user.role || (user.role !== 'PATIENT' && user.role !== 'DOCTOR')) {
      return null;
    }

    const isDoctor = user.role === 'DOCTOR';

    return (
      <div className="auth-container auth-container-wide" id="auth-portal">
        <div className="auth-card">
          <div className="session-card">
            {/* Role-based landing portal view */}
            <RolePortal user={user} token={token} />

            <div className="session-details-grid">
              <div className="session-detail-item">
                <span className="session-detail-label">Assigned Role</span>
                <span className={isDoctor ? 'badge-doctor' : 'badge-patient'}>
                  {user.role}
                </span>
              </div>

              <div className="session-detail-item">
                <span className="session-detail-label">System User ID</span>
                <span className="session-detail-value">#{user.id}</span>
              </div>

              <div className="session-detail-item">
                <span className="session-detail-label">Authentication Token</span>
                <span className="session-detail-value" style={{ color: '#34d399', fontSize: '0.82rem' }}>
                  {token ? 'Active (JWT Bearer Stored)' : 'Not available'}
                </span>
              </div>

              <div className="session-detail-item">
                <span className="session-detail-label">Access Level</span>
                <span className="session-detail-value" style={{ color: '#38bdf8', fontSize: '0.82rem' }}>
                  {isDoctor ? 'Clinical Management' : 'Patient Care Portal'}
                </span>
              </div>
            </div>

            {/* Protected Endpoint Verification Box */}
            <div className="protected-check-box" id="protected-verification-panel">
              <div className="protected-check-header">
                <div className="protected-check-title">
                  <span className="protected-icon">🛡️</span>
                  <div>
                    <h4 className="protected-heading">Authenticated API Route Test</h4>
                    <span className="protected-subtitle">
                      Target: <code>GET /api/v1/test/protected</code>
                    </span>
                  </div>
                </div>
                <button
                  id="btn-retry-protected"
                  type="button"
                  className="btn-retry"
                  onClick={() => checkProtectedConnection(token)}
                  disabled={protectedStatus === 'checking'}
                  title="Send test request to backend with current in-memory JWT"
                >
                  {protectedStatus === 'checking' ? (
                    <>
                      <span className="spinner-sm"></span>
                      Verifying...
                    </>
                  ) : (
                    '↻ Re-test Connection'
                  )}
                </button>
              </div>

              {/* Checking State */}
              {protectedStatus === 'checking' && (
                <div className="protected-status-block checking">
                  <span className="spinner-sm"></span>
                  <span>Sending <code>Authorization: Bearer &lt;in-memory-jwt&gt;</code> to Spring Boot backend...</span>
                </div>
              )}

              {/* Success State */}
              {protectedStatus === 'success' && protectedData && (
                <div className="protected-status-block success" id="protected-check-success">
                  <div className="status-badge-row">
                    <span className="pill-badge pill-badge-success">✓ 200 OK — Authenticated Session Active</span>
                    {lastVerifiedAt && <span className="timestamp-hint">Verified at {lastVerifiedAt}</span>}
                  </div>
                  <p className="status-msg">{protectedData.message}</p>
                  <div className="claims-grid">
                    <div className="claim-item">
                      <span className="claim-label">Principal ID</span>
                      <span className="claim-val">#{protectedData.userId}</span>
                    </div>
                    <div className="claim-item">
                      <span className="claim-label">Token Email</span>
                      <span className="claim-val">{protectedData.email}</span>
                    </div>
                    <div className="claim-item">
                      <span className="claim-label">Principal Role</span>
                      <span className="claim-val">{protectedData.role}</span>
                    </div>
                  </div>
                </div>
              )}

              {/* Error / Auth Failure State */}
              {protectedStatus === 'error' && protectedError && (
                <div className="protected-status-block error" id="protected-check-error">
                  <div className="status-badge-row">
                    <span className="pill-badge pill-badge-danger">
                      {protectedError.isAuthFailure
                        ? `⚠️ Authentication Failed (HTTP ${protectedError.status})`
                        : `⚠️ Backend Notice (HTTP ${protectedError.status})`}
                    </span>
                    {lastVerifiedAt && <span className="timestamp-hint">Checked at {lastVerifiedAt}</span>}
                  </div>

                  <p className="status-error-msg">{protectedError.message}</p>

                  {protectedError.isAuthFailure ? (
                    <div className="auth-failure-notice">
                      <p>
                        <strong>Authentication Error:</strong> The backend rejected this token as invalid or expired.
                        Please sign out and sign in again to receive a fresh token.
                      </p>
                      <button
                        type="button"
                        className="btn btn-sm btn-danger"
                        onClick={onLogout}
                      >
                        Sign Out &amp; Re-login
                      </button>
                    </div>
                  ) : (
                    <small className="status-subtext">
                      Ensure the Spring Boot backend is running on <code>http://localhost:8080</code> with PostgreSQL active.
                    </small>
                  )}
                </div>
              )}
            </div>

            <div className="session-actions">
              <button
                id="btn-signout"
                className="btn btn-secondary"
                onClick={onLogout}
                type="button"
              >
                Sign Out / Switch Account
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-container" id="auth-portal">
      <div className="auth-card">
        {/* View Switcher Tabs */}
        <div className="auth-tabs" role="tablist">
          <button
            id="tab-login"
            role="tab"
            aria-selected={mode === 'login'}
            className={`auth-tab-btn ${mode === 'login' ? 'active' : ''}`}
            onClick={() => switchMode('login')}
            type="button"
          >
            Sign In
          </button>
          <button
            id="tab-register"
            role="tab"
            aria-selected={mode === 'register'}
            className={`auth-tab-btn ${mode === 'register' ? 'active' : ''}`}
            onClick={() => switchMode('register')}
            type="button"
          >
            Create Account
          </button>
        </div>

        {/* View Header */}
        <div className="auth-header">
          <h2>
            {mode === 'login' ? 'Welcome Back to MediGuide' : 'Register for MediGuide Care'}
          </h2>
          <p>
            {mode === 'login'
              ? 'Enter your credentials to access your clinical dashboard'
              : 'Select your clinical role and create your secure account'}
          </p>
        </div>

        {/* Success Alert Banner */}
        {apiSuccess && (
          <div className="auth-alert auth-alert-success" role="alert">
            <span className="auth-alert-icon">✓</span>
            <div>{apiSuccess}</div>
          </div>
        )}

        {/* Error Alert Banner */}
        {apiError && (
          <div className="auth-alert auth-alert-error" role="alert">
            <span className="auth-alert-icon">⚠️</span>
            <div>{apiError}</div>
          </div>
        )}

        {/* LOGIN FORM */}
        {mode === 'login' && (
          <form className="auth-form" onSubmit={handleLoginSubmit} noValidate>
            <div className="form-group">
              <label className="form-label" htmlFor="login-email">
                <span>Email Address <span className="req">*</span></span>
              </label>
              <div className="form-input-wrapper">
                <input
                  id="login-email"
                  name="email"
                  type="email"
                  autoComplete="email"
                  className={`form-input ${fieldErrors.email ? 'input-error' : ''}`}
                  placeholder="e.g. priya.rao@mediguide.local"
                  value={loginForm.email}
                  onChange={handleLoginChange}
                  disabled={loading}
                />
              </div>
              {fieldErrors.email && (
                <span className="field-error">⚠ {fieldErrors.email}</span>
              )}
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="login-password">
                <span>Password <span className="req">*</span></span>
              </label>
              <div className="form-input-wrapper">
                <input
                  id="login-password"
                  name="password"
                  type="password"
                  autoComplete="current-password"
                  className={`form-input ${fieldErrors.password ? 'input-error' : ''}`}
                  placeholder="Enter your account password"
                  value={loginForm.password}
                  onChange={handleLoginChange}
                  disabled={loading}
                />
              </div>
              {fieldErrors.password && (
                <span className="field-error">⚠ {fieldErrors.password}</span>
              )}
            </div>

            <button
              id="btn-login-submit"
              type="submit"
              className="btn btn-primary btn-submit"
              disabled={loading}
            >
              {loading ? (
                <>
                  <span className="spinner"></span>
                  Signing In...
                </>
              ) : (
                'Sign In to MediGuide'
              )}
            </button>

            <div className="auth-switch">
              Don't have an account?{' '}
              <button
                type="button"
                className="auth-switch-btn"
                onClick={() => switchMode('register')}
                disabled={loading}
              >
                Create an account here
              </button>
            </div>
          </form>
        )}

        {/* REGISTRATION FORM */}
        {mode === 'register' && (
          <form className="auth-form" onSubmit={handleRegisterSubmit} noValidate>
            {/* Role Selection */}
            <div className="form-group">
              <label className="form-label">
                <span>Select Your Role <span className="req">*</span></span>
              </label>
              <div className="role-grid">
                <div
                  id="role-option-patient"
                  className={`role-card ${registerForm.role === 'PATIENT' ? 'selected' : ''}`}
                  onClick={() => !loading && setRegisterForm((prev) => ({ ...prev, role: 'PATIENT' }))}
                >
                  <div className="role-card-header">
                    <span className="role-card-title">
                      <span className="role-icon">🧑‍⚕️</span> Patient
                    </span>
                    <span className="role-radio-dot"></span>
                  </div>
                  <span className="role-card-desc">
                    Book appointments, receive consultations, and access medical records.
                  </span>
                </div>

                <div
                  id="role-option-doctor"
                  className={`role-card ${registerForm.role === 'DOCTOR' ? 'selected' : ''}`}
                  onClick={() => !loading && setRegisterForm((prev) => ({ ...prev, role: 'DOCTOR' }))}
                >
                  <div className="role-card-header">
                    <span className="role-card-title">
                      <span className="role-icon">🩺</span> Doctor
                    </span>
                    <span className="role-radio-dot"></span>
                  </div>
                  <span className="role-card-desc">
                    Manage appointments, prescribe medications, and track patient follow-ups.
                  </span>
                </div>
              </div>
              {fieldErrors.role && (
                <span className="field-error">⚠ {fieldErrors.role}</span>
              )}
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="register-fullName">
                <span>Full Name <span className="req">*</span></span>
              </label>
              <div className="form-input-wrapper">
                <input
                  id="register-fullName"
                  name="fullName"
                  type="text"
                  autoComplete="name"
                  className={`form-input ${fieldErrors.fullName ? 'input-error' : ''}`}
                  placeholder="e.g. Dr. Ramesh Sen or Anjali Gupta"
                  value={registerForm.fullName}
                  onChange={handleRegisterChange}
                  disabled={loading}
                />
              </div>
              {fieldErrors.fullName && (
                <span className="field-error">⚠ {fieldErrors.fullName}</span>
              )}
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="register-email">
                <span>Email Address <span className="req">*</span></span>
              </label>
              <div className="form-input-wrapper">
                <input
                  id="register-email"
                  name="email"
                  type="email"
                  autoComplete="email"
                  className={`form-input ${fieldErrors.email ? 'input-error' : ''}`}
                  placeholder="e.g. patient.care@mediguide.local"
                  value={registerForm.email}
                  onChange={handleRegisterChange}
                  disabled={loading}
                />
              </div>
              {fieldErrors.email && (
                <span className="field-error">⚠ {fieldErrors.email}</span>
              )}
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="register-password">
                <span>Password <span className="req">*</span></span>
              </label>
              <div className="form-input-wrapper">
                <input
                  id="register-password"
                  name="password"
                  type="password"
                  autoComplete="new-password"
                  className={`form-input ${fieldErrors.password ? 'input-error' : ''}`}
                  placeholder="Minimum 8 characters"
                  value={registerForm.password}
                  onChange={handleRegisterChange}
                  disabled={loading}
                />
              </div>
              <span className="field-hint">Must be at least 8 characters long</span>
              {fieldErrors.password && (
                <span className="field-error">⚠ {fieldErrors.password}</span>
              )}
            </div>

            <button
              id="btn-register-submit"
              type="submit"
              className="btn btn-primary btn-submit"
              disabled={loading}
            >
              {loading ? (
                <>
                  <span className="spinner"></span>
                  Creating Account...
                </>
              ) : (
                'Create MediGuide Account'
              )}
            </button>

            <div className="auth-switch">
              Already have an account?{' '}
              <button
                type="button"
                className="auth-switch-btn"
                onClick={() => switchMode('login')}
                disabled={loading}
              >
                Sign in here
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}

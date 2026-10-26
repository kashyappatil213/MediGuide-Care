import { useState, useEffect } from 'react';
import { registerUser, loginUser, verifyEmail, resendVerification } from '../services/authApi';
import './AuthCard.css';

/**
 * Clean, modern authentication card component providing Login and Registration views
 * for Patients and Doctors in MediGuide Care.
 */
export default function AuthCard({ authState, onLoginSuccess }) {
  // If user is already authenticated, the portal is rendered directly by App.jsx
  if (authState?.isAuthenticated && authState.user) {
    return null;
  }

  const [mode, setMode] = useState('login'); // 'login' | 'register'
  const [loading, setLoading] = useState(false);
  const [apiError, setApiError] = useState(null);
  const [apiSuccess, setApiSuccess] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});

  // Email verification states
  const [verifyingEmail, setVerifyingEmail] = useState(false);
  const [emailVerifiedNotice, setEmailVerifiedNotice] = useState(null);
  const [emailVerifyError, setEmailVerifyError] = useState(null);
  const [showResendBox, setShowResendBox] = useState(false);
  const [resendEmail, setResendEmail] = useState('');
  const [resendLoading, setResendLoading] = useState(false);
  const [resendSuccess, setResendSuccess] = useState(null);
  const [resendError, setResendError] = useState(null);

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

  // Run verification if URL contains ?verify-email=<token>
  useEffect(() => {
    let active = true;
    try {
      const params = new URLSearchParams(window.location.search);
      const token = params.get('verify-email');
      if (token) {
        const timer = setTimeout(() => {
          if (!active) return;
          setVerifyingEmail(true);
          verifyEmail(token)
            .then((res) => {
              if (!active) return;
              setEmailVerifiedNotice(res.message || 'Email verified successfully! You can now sign in.');
              window.history.replaceState({}, document.title, window.location.pathname);
            })
            .catch((err) => {
              if (!active) return;
              setEmailVerifyError(err.message || 'Verification link is invalid or has expired.');
            })
            .finally(() => {
              if (active) {
                setVerifyingEmail(false);
              }
            });
        }, 0);

        return () => {
          active = false;
          clearTimeout(timer);
        };
      }
    } catch {
      // Ignore URL parsing errors
    }
  }, []);

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

      setApiSuccess(
        response.publicId
          ? `Account created successfully for ${response.fullName || registerForm.fullName}! Your MediGuide ID is ${response.publicId}. A verification email was dispatched to ${registerForm.email}. You can sign in immediately!`
          : `Account created successfully! A verification email was sent to ${registerForm.email}. You can sign in right away.`
      );

      // Pre-fill email in login form and switch to login tab
      setLoginForm({
        email: registerForm.email.trim(),
        password: '',
      });

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

      setLoginForm((prev) => ({ ...prev, password: '' }));

      if (onLoginSuccess) {
        onLoginSuccess({
          accessToken: response.accessToken,
          user: response.user,
        });
      }

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

  // Handle Resend Verification Form Submission
  const handleResendSubmit = async (e) => {
    e.preventDefault();
    if (resendLoading || !resendEmail.trim()) return;

    setResendError(null);
    setResendSuccess(null);
    setResendLoading(true);

    try {
      const res = await resendVerification(resendEmail.trim());
      setResendSuccess(res.message || 'If an unverified account exists for that email, a verification link has been sent.');
    } catch (err) {
      setResendError(err.message || 'Failed to resend verification link. Please wait before trying again.');
    } finally {
      setResendLoading(false);
    }
  };

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

        {/* Email Verification Alerts */}
        {verifyingEmail && (
          <div className="auth-alert auth-alert-info" role="alert">
            <span className="auth-alert-icon">⏳</span>
            <div>Verifying your email address with MediGuide Care...</div>
          </div>
        )}

        {emailVerifiedNotice && (
          <div className="auth-alert auth-alert-success" role="alert">
            <span className="auth-alert-icon">✓</span>
            <div>{emailVerifiedNotice}</div>
          </div>
        )}

        {emailVerifyError && (
          <div className="auth-alert auth-alert-error" role="alert">
            <span className="auth-alert-icon">⚠️</span>
            <div>
              {emailVerifyError}
              <div style={{ marginTop: '6px' }}>
                <button
                  type="button"
                  className="btn-link"
                  style={{ color: '#b91c1c', textDecoration: 'underline', background: 'none', border: 'none', padding: 0, cursor: 'pointer', fontSize: '0.85rem' }}
                  onClick={() => setShowResendBox(true)}
                >
                  Resend a new verification email
                </button>
              </div>
            </div>
          </div>
        )}

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
                  placeholder="doctor@example.com or patient@example.com"
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
              Don&apos;t have an account?{' '}
              <button
                type="button"
                className="auth-switch-btn"
                onClick={() => switchMode('register')}
                disabled={loading}
              >
                Create an account here
              </button>
            </div>

            <div style={{ textAlign: 'center', marginTop: '12px' }}>
              <button
                type="button"
                className="btn-link"
                style={{ color: '#0284c7', background: 'none', border: 'none', padding: 0, cursor: 'pointer', fontSize: '0.84rem' }}
                onClick={() => {
                  setShowResendBox((prev) => !prev);
                  if (!resendEmail && loginForm.email) {
                    setResendEmail(loginForm.email);
                  }
                }}
              >
                {showResendBox ? '▲ Hide Resend Verification' : '✉ Resend Email Verification Link'}
              </button>
            </div>

            {showResendBox && (
              <div style={{ background: '#f8fafc', padding: '14px', borderRadius: '8px', border: '1px solid #cbd5e1', marginTop: '12px' }}>
                <h5 style={{ margin: '0 0 6px', fontSize: '0.9rem', color: '#0f172a' }}>Resend Verification Link</h5>
                <p style={{ margin: '0 0 10px', fontSize: '0.8rem', color: '#475569' }}>
                  Enter your email address to receive a fresh verification link.
                </p>
                {resendSuccess && (
                  <div className="auth-alert auth-alert-success" style={{ padding: '8px 12px', fontSize: '0.82rem', marginBottom: '10px' }}>
                    {resendSuccess}
                  </div>
                )}
                {resendError && (
                  <div className="auth-alert auth-alert-error" style={{ padding: '8px 12px', fontSize: '0.82rem', marginBottom: '10px' }}>
                    {resendError}
                  </div>
                )}
                <div className="resend-inputs-row">
                  <input
                    type="email"
                    className="resend-input"
                    placeholder="Enter your registered email"
                    value={resendEmail}
                    onChange={(e) => setResendEmail(e.target.value)}
                    required
                  />
                  <button
                    type="button"
                    className="btn-resend-action"
                    onClick={handleResendSubmit}
                    disabled={resendLoading || !resendEmail.trim()}
                  >
                    {resendLoading ? 'Sending...' : 'Send Link'}
                  </button>
                </div>
              </div>
            )}
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
                  placeholder="e.g. Dr. Jane Smith or John Doe"
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
                  placeholder="e.g. you@example.com"
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

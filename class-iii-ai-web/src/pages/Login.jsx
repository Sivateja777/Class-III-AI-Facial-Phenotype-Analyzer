import React, { useState } from 'react';
import { auth, db, googleProvider } from '../firebase';
import { signInWithEmailAndPassword, createUserWithEmailAndPassword, signInWithPopup } from 'firebase/auth';
import { doc, setDoc, getDoc } from 'firebase/firestore';

const Login = () => {
  const [isDoctor, setIsDoctor] = useState(false);
  const [isRegistering, setIsRegistering] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [license, setLicense] = useState('');
  const [privacyConsent, setPrivacyConsent] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const saveToDatabase = async (user, displayName, role) => {
    const emailKey = user.email.replace(/\./g, '_').replace(/@/g, '_');
    const userRef = doc(db, 'users', emailKey);
    const userSnap = await getDoc(userRef);

    if (!userSnap.exists()) {
      await setDoc(userRef, {
        email: user.email,
        displayName: displayName || user.displayName || 'Unknown User',
        role: role,
        timestamp: Date.now(),
        profilePic: user.photoURL || ''
      });
    }
  };

  const handleEmailAuth = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    
    if (isDoctor && isRegistering && !license.match(/^[A-Za-z0-9-]{5,20}$/)) {
      setError('Invalid Medical License format.');
      setLoading(false);
      return;
    }

    if (isRegistering && !privacyConsent) {
      setError('You must agree to the Data Privacy & GDPR Policy to register.');
      setLoading(false);
      return;
    }

    try {
      if (isRegistering) {
        const userCredential = await createUserWithEmailAndPassword(auth, email, password);
        await saveToDatabase(userCredential.user, name, isDoctor ? 'doctor' : 'patient');
      } else {
        const userCredential = await signInWithEmailAndPassword(auth, email, password);
        await saveToDatabase(userCredential.user, name, isDoctor ? 'doctor' : 'patient');
      }
    } catch (err) {
      console.error("Login Error:", err);
      let errorMsg = "An unexpected error occurred. Please try again.";
      if (err.code === 'auth/invalid-credential' || err.code === 'auth/user-not-found' || err.code === 'auth/wrong-password') {
        errorMsg = "Invalid email or password.";
      } else if (err.code === 'auth/email-already-in-use') {
        errorMsg = "An account with this email already exists.";
      } else if (err.code === 'auth/operation-not-allowed') {
        errorMsg = "Login is currently disabled by the administrator.";
      } else if (err.code === 'auth/invalid-api-key') {
        errorMsg = "System configuration error. Please check Firebase API keys.";
      } else if (err.message && err.message.includes("Missing or insufficient permissions")) {
        errorMsg = "Database access denied. Please check Firestore Rules.";
      } else {
        errorMsg = err.message;
      }
      setError(errorMsg);
    }
    setLoading(false);
  };

  const handleGoogleAuth = async () => {
    setError('');
    
    if (isRegistering && !privacyConsent) {
      setError('You must agree to the Data Privacy & GDPR Policy to register via Google.');
      return;
    }
    
    setLoading(true);
    try {
      const result = await signInWithPopup(auth, googleProvider);
      await saveToDatabase(result.user, result.user.displayName, isDoctor ? 'doctor' : 'patient');
    } catch (err) {
      console.error("Google Auth Error:", err);
      let errorMsg = "An unexpected error occurred during Google Sign-In.";
      if (err.code === 'auth/popup-closed-by-user') {
        errorMsg = "Sign-in popup was closed before completing.";
      } else if (err.code === 'auth/unauthorized-domain') {
        errorMsg = "This domain (localhost) is not authorized in your Firebase Console.";
      } else if (err.message && err.message.includes("Missing or insufficient permissions")) {
        errorMsg = "Database access denied. Please check Firestore Rules.";
      }
      setError(errorMsg);
    }
    setLoading(false);
  };

  return (
    <div className="container flex-center" style={{ minHeight: '100vh' }}>
      <div className="glass-card animate-fade-in" style={{ maxWidth: '500px', width: '100%' }}>
        
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <h1 style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>Class III AI</h1>
          <p style={{ color: 'var(--text-secondary)' }}>Advanced Facial Phenotype Analysis</p>
        </div>

        <div style={{ display: 'flex', gap: '1rem', marginBottom: '2rem' }}>
          <button 
            className={`btn ${!isDoctor ? '' : 'btn-secondary'}`} 
            style={{ flex: 1, backgroundColor: !isDoctor ? 'var(--primary)' : 'rgba(255,255,255,0.1)' }}
            onClick={() => setIsDoctor(false)}>
            Patient
          </button>
          <button 
            className={`btn ${isDoctor ? '' : 'btn-secondary'}`} 
            style={{ flex: 1, backgroundColor: isDoctor ? 'var(--primary)' : 'rgba(255,255,255,0.1)' }}
            onClick={() => setIsDoctor(true)}>
            Doctor
          </button>
        </div>

        <h2 style={{ marginBottom: '1.5rem', textAlign: 'center' }}>
          {isRegistering ? 'Create Account' : 'Welcome Back'}
        </h2>

        {error && (
          <div style={{ padding: '1rem', backgroundColor: 'rgba(239, 68, 68, 0.2)', color: 'var(--error)', borderRadius: '8px', marginBottom: '1.5rem' }}>
            {error}
          </div>
        )}

        <form onSubmit={handleEmailAuth}>
          {isRegistering && (
            <div className="form-group">
              <label className="input-label">Full Name</label>
              <input 
                type="text" 
                className="input-field" 
                placeholder="John Doe" 
                value={name} 
                onChange={(e) => setName(e.target.value)} 
                required 
              />
            </div>
          )}

          {isDoctor && isRegistering && (
            <div className="form-group">
              <label className="input-label">Medical License Number</label>
              <input 
                type="text" 
                className="input-field" 
                placeholder="e.g. MED-12345" 
                value={license} 
                onChange={(e) => setLicense(e.target.value)} 
                required 
              />
            </div>
          )}

          <div className="form-group">
            <label className="input-label">Email Address</label>
            <input 
              type="email" 
              className="input-field" 
              placeholder="name@example.com" 
              value={email} 
              onChange={(e) => setEmail(e.target.value)} 
              required 
            />
          </div>

          <div className="form-group">
            <label className="input-label">Password</label>
            <input 
              type="password" 
              className="input-field" 
              placeholder="••••••••" 
              value={password} 
              onChange={(e) => setPassword(e.target.value)} 
              required 
              minLength="6"
            />
          </div>

          {isRegistering && (
            <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '1rem', marginBottom: '1.5rem' }}>
              <input 
                type="checkbox" 
                id="privacyConsent"
                checked={privacyConsent}
                onChange={(e) => setPrivacyConsent(e.target.checked)}
                style={{ width: '18px', height: '18px', cursor: 'pointer' }}
              />
              <label htmlFor="privacyConsent" style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', cursor: 'pointer', lineHeight: '1.4' }}>
                I agree to the <strong>Data Privacy Policy</strong>. I understand that medical image data is securely processed and anonymized per HIPAA and GDPR regulations.
              </label>
            </div>
          )}

          <button type="submit" className="btn" style={{ width: '100%', marginBottom: '1rem' }} disabled={loading}>
            {loading ? 'Processing...' : (isRegistering ? 'Sign Up' : 'Login')}
          </button>
        </form>

        <div style={{ textAlign: 'center', marginBottom: '1rem', color: 'var(--text-secondary)' }}>
          OR
        </div>

        <button 
          onClick={handleGoogleAuth} 
          className="btn" 
          style={{ width: '100%', backgroundColor: 'white', color: '#333' }}
          disabled={loading}>
          <img src="https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/google.svg" alt="Google" style={{ width: '20px', marginRight: '8px' }}/>
          Continue with Google
        </button>

        <div style={{ textAlign: 'center', marginTop: '1.5rem', fontSize: '0.9rem' }}>
          {isRegistering ? 'Already have an account? ' : "Don't have an account? "}
          <span 
            style={{ color: 'var(--primary)', cursor: 'pointer', fontWeight: 'bold' }} 
            onClick={() => setIsRegistering(!isRegistering)}>
            {isRegistering ? 'Login here' : 'Register here'}
          </span>
        </div>
      </div>
    </div>
  );
};

export default Login;

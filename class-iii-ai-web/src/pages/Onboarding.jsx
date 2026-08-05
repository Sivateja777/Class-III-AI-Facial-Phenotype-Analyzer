import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { auth, db } from '../firebase';
import { doc, getDoc, updateDoc } from 'firebase/firestore';

const Onboarding = () => {
  const navigate = useNavigate();
  const [age, setAge] = useState('');
  const [gender, setGender] = useState('');
  const [mobile, setMobile] = useState('');
  const [license, setLicense] = useState('');
  const [role, setRole] = useState('patient');
  const [loading, setLoading] = useState(false);
  const [fetchingData, setFetchingData] = useState(true);

  useEffect(() => {
    fetchUserRole();
  }, []);

  const fetchUserRole = async () => {
    if (!auth.currentUser) return;
    try {
      const emailKey = auth.currentUser.email.replace(/\./g, '_').replace(/@/g, '_');
      const userRef = doc(db, 'users', emailKey);
      const userSnap = await getDoc(userRef);
      if (userSnap.exists()) {
        const data = userSnap.data();
        setRole(data.role || 'patient');
        if (data.medicalLicenseNumber) {
          setLicense(data.medicalLicenseNumber);
        }
      }
    } catch (err) {
      console.error(err);
    }
    setFetchingData(false);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (role === 'doctor' && (!license || !license.match(/^[A-Za-z0-9-]{5,20}$/))) {
      alert("Please enter a valid Medical License Number (5-20 characters)");
      return;
    }
    
    setLoading(true);
    
    try {
      const emailKey = auth.currentUser.email.replace(/\./g, '_').replace(/@/g, '_');
      const userRef = doc(db, 'users', emailKey);
      
      const updatePayload = {
        age: age,
        gender: gender,
        mobileNumber: mobile,
        isProfileComplete: true
      };
      
      if (role === 'doctor') {
        updatePayload.medicalLicenseNumber = license;
      }
      
      await updateDoc(userRef, updatePayload);
      
      // Navigate to correct dashboard based on a fresh fetch or force a reload
      window.location.href = '/'; 
    } catch (err) {
      console.error("Onboarding Error:", err);
      if (err.message && err.message.includes("Missing or insufficient permissions")) {
          alert('Database access denied. Please check your Firebase Firestore rules to allow writes.');
      } else {
          alert('Failed to update profile: ' + err.message);
      }
    }
    setLoading(false);
  };

  if (fetchingData) {
    return <div className="container flex-center" style={{ minHeight: '100vh' }}>Loading...</div>;
  }

  return (
    <div className="container flex-center" style={{ minHeight: '100vh' }}>
      <div className="glass-card animate-fade-in" style={{ maxWidth: '500px', width: '100%' }}>
        <h1 style={{ marginBottom: '1.5rem', textAlign: 'center' }}>Complete Your Profile</h1>
        <p style={{ color: 'var(--text-secondary)', textAlign: 'center', marginBottom: '2rem' }}>
          Please provide a few more details to continue.
        </p>
        
        <form onSubmit={handleSubmit}>
          {role === 'doctor' && (
            <div className="form-group">
              <label className="input-label">Medical License Number</label>
              <input 
                type="text" 
                className="input-field" 
                placeholder="e.g. MED-12345"
                value={license} 
                onChange={e => setLicense(e.target.value)} 
                required 
              />
            </div>
          )}
          
          <div className="form-group">
            <label className="input-label">Age</label>
            <input type="number" className="input-field" value={age} onChange={e => setAge(e.target.value)} required />
          </div>
          <div className="form-group">
            <label className="input-label">Gender</label>
            <select className="input-field" value={gender} onChange={e => setGender(e.target.value)} required>
              <option value="">Select Gender</option>
              <option value="Male">Male</option>
              <option value="Female">Female</option>
              <option value="Other">Other</option>
            </select>
          </div>
          <div className="form-group">
            <label className="input-label">Mobile Number</label>
            <input type="tel" className="input-field" value={mobile} onChange={e => setMobile(e.target.value)} required />
          </div>
          
          <button type="submit" className="btn" style={{ width: '100%' }} disabled={loading}>
            {loading ? 'Saving...' : 'Complete Setup'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default Onboarding;

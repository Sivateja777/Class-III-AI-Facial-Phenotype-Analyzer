import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { auth, db } from '../firebase';
import { doc, updateDoc } from 'firebase/firestore';

const Onboarding = () => {
  const navigate = useNavigate();
  const [age, setAge] = useState('');
  const [gender, setGender] = useState('');
  const [mobile, setMobile] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    
    try {
      const emailKey = auth.currentUser.email.replace(/\./g, '_').replace(/@/g, '_');
      const userRef = doc(db, 'users', emailKey);
      
      await updateDoc(userRef, {
        age: age,
        gender: gender,
        mobileNumber: mobile,
        isProfileComplete: true
      });
      
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

  return (
    <div className="container flex-center" style={{ minHeight: '100vh' }}>
      <div className="glass-card animate-fade-in" style={{ maxWidth: '500px', width: '100%' }}>
        <h1 style={{ marginBottom: '1.5rem', textAlign: 'center' }}>Complete Your Profile</h1>
        <p style={{ color: 'var(--text-secondary)', textAlign: 'center', marginBottom: '2rem' }}>
          Please provide a few more details to continue.
        </p>
        
        <form onSubmit={handleSubmit}>
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

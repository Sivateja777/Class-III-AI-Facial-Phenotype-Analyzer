import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { auth, db } from '../firebase';
import { doc, getDoc, collection, query, where, getDocs } from 'firebase/firestore';
import { ArrowLeft, User as UserIcon } from 'lucide-react';

const Profile = () => {
  const navigate = useNavigate();
  const [userData, setUserData] = useState(null);
  const [patientData, setPatientData] = useState(null);

  useEffect(() => {
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    if (!auth.currentUser) return;
    try {
      const emailKey = auth.currentUser.email.replace(/\./g, '_').replace(/@/g, '_');
      const userSnap = await getDoc(doc(db, 'users', emailKey));
      let role = '';
      if (userSnap.exists()) {
        setUserData(userSnap.data());
        role = userSnap.data().role;
      }
      
      if (role === 'patient') {
        const q = query(collection(db, 'patients'), where('patientEmail', '==', auth.currentUser.email));
        const pSnap = await getDocs(q);
        if (!pSnap.empty) {
          setPatientData(pSnap.docs[0].data());
        }
      }
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="container" style={{ padding: '2rem' }}>
      <button onClick={() => navigate(-1)} className="btn" style={{ backgroundColor: 'transparent', padding: '0.5rem 0', marginBottom: '2rem', color: 'var(--text-secondary)' }}>
        <ArrowLeft size={18} /> Back
      </button>

      <div className="glass-card flex-center" style={{ flexDirection: 'column', maxWidth: '600px', margin: '0 auto' }}>
        <div style={{ width: '100px', height: '100px', borderRadius: '50%', backgroundColor: 'var(--surface-border)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '1.5rem', overflow: 'hidden' }}>
          {userData?.profilePic ? (
            <img src={userData.profilePic} alt="Profile" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
          ) : (
            <UserIcon size={48} color="var(--text-secondary)" />
          )}
        </div>
        
        <h2 style={{ marginBottom: '0.5rem' }}>{userData?.displayName || auth.currentUser?.displayName || 'User'}</h2>
        <p style={{ color: 'var(--text-secondary)', marginBottom: '2rem' }}>{userData?.role === 'doctor' ? 'Clinical Professional' : 'Patient Account'}</p>

        <div style={{ width: '100%', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', padding: '1rem', borderBottom: '1px solid var(--surface-border)' }}>
            <span style={{ color: 'var(--text-secondary)' }}>Email</span>
            <span>{userData?.email || auth.currentUser?.email}</span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', padding: '1rem', borderBottom: '1px solid var(--surface-border)' }}>
            <span style={{ color: 'var(--text-secondary)' }}>Age</span>
            <span>{userData?.age || 'Not provided'}</span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', padding: '1rem', borderBottom: '1px solid var(--surface-border)' }}>
            <span style={{ color: 'var(--text-secondary)' }}>Gender</span>
            <span>{userData?.gender || 'Not provided'}</span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', padding: '1rem', borderBottom: '1px solid var(--surface-border)' }}>
            <span style={{ color: 'var(--text-secondary)' }}>Mobile</span>
            <span>{userData?.mobileNumber || 'Not provided'}</span>
          </div>

          {userData?.role === 'patient' && (
            <>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '1rem', borderBottom: '1px solid var(--surface-border)' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Ethnicity</span>
                <span>{patientData?.ethnicity || 'Not provided'}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '1rem', borderBottom: '1px solid var(--surface-border)' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Growth Status</span>
                <span>{patientData?.growthStatus || 'Not provided'}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '1rem', borderBottom: '1px solid var(--surface-border)' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Clinical Diagnosis</span>
                <span>{patientData?.clinicalDiagnosis || 'Not provided'}</span>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', padding: '1rem', borderBottom: '1px solid var(--surface-border)' }}>
                <span style={{ color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>Cephalometric Values</span>
                <span style={{ whiteSpace: 'pre-wrap' }}>{patientData?.cephValues || 'Not provided'}</span>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};

export default Profile;

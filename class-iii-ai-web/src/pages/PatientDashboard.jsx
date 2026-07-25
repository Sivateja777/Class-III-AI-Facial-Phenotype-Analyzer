import React, { useState, useEffect } from 'react';
import { auth, db } from '../firebase';
import { collection, query, where, getDocs } from 'firebase/firestore';
import { LogOut, Activity } from 'lucide-react';

const PatientDashboard = () => {
  const [reports, setReports] = useState([]);
  const [nextVisit, setNextVisit] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    if (!auth.currentUser) return;
    try {
      const q = query(collection(db, 'analysis_reports'), where('patientEmail', '==', auth.currentUser.email));
      const querySnapshot = await getDocs(q);
      const reps = [];
      querySnapshot.forEach((doc) => {
        reps.push({ id: doc.id, ...doc.data() });
      });
      reps.sort((a, b) => b.timestamp - a.timestamp);
      setReports(reps);

      const apptQ = query(collection(db, 'appointments'), where('patientEmail', '==', auth.currentUser.email));
      const apptSnap = await getDocs(apptQ);
      const appts = [];
      apptSnap.forEach(doc => {
        appts.push({ id: doc.id, ...doc.data() });
      });
      appts.sort((a, b) => b.timestamp - a.timestamp); // Should sort by date if it was a real date, but timestamp is all we have
      
      // In a real app we'd filter for future dates. Here we'll just show the most recently requested appointment
      if (appts.length > 0) {
        setNextVisit(appts[0]);
      }
    } catch (err) {
      console.error("Error fetching data:", err);
    }
    setLoading(false);
  };

  const handleLogout = () => {
    auth.signOut();
  };

  return (
    <div className="container" style={{ padding: '2rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '3rem' }}>
        <div>
          <h1 style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>Patient Dashboard</h1>
          <p style={{ color: 'var(--text-secondary)' }}>Welcome back, {auth.currentUser?.displayName}</p>
        </div>
        <div style={{ display: 'flex', gap: '1rem' }}>
          <button onClick={() => window.location.href = '/profile'} className="btn" style={{ backgroundColor: 'rgba(255,255,255,0.1)' }}>
            Profile
          </button>
          <button onClick={handleLogout} className="btn" style={{ backgroundColor: 'rgba(239, 68, 68, 0.2)', color: 'var(--error)' }}>
            <LogOut size={18} /> Logout
          </button>
        </div>
      </div>

      {nextVisit && (
        <div style={{ backgroundColor: 'rgba(16, 185, 129, 0.2)', border: '1px solid #10B981', padding: '1.5rem', borderRadius: '12px', marginBottom: '2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <h3 style={{ color: '#10B981', marginBottom: '0.25rem' }}>Next Upcoming Visit</h3>
            <p style={{ color: 'var(--text-secondary)' }}>Status: {nextVisit.status} | Dr. {nextVisit.doctorEmail}</p>
          </div>
          <div style={{ textAlign: 'right' }}>
            <strong style={{ display: 'block', fontSize: '1.2rem' }}>{new Date(nextVisit.timestamp).toLocaleDateString()}</strong>
            <span style={{ fontSize: '0.9rem', color: 'var(--text-secondary)' }}>Requested</span>
          </div>
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', marginBottom: '2rem' }}>
        <button onClick={() => window.location.href = '/patient/simulation'} className="btn" style={{ height: '100px', fontSize: '1.2rem', display: 'flex', flexDirection: 'column', gap: '0.5rem', backgroundColor: 'rgba(15, 23, 42, 0.5)', border: '1px solid var(--primary)' }}>
          <Activity size={24} color="var(--primary)" />
          Morphing Simulator
        </button>
        <button onClick={() => window.location.href = '/patient/analysis'} className="btn" style={{ height: '100px', fontSize: '1.2rem', display: 'flex', flexDirection: 'column', gap: '0.5rem', backgroundColor: 'rgba(15, 23, 42, 0.5)', border: '1px solid #8B5CF6' }}>
          <Activity size={24} color="#8B5CF6" />
          Start AI Scan
        </button>
        <button onClick={() => window.location.href = '/patient/visits'} className="btn" style={{ height: '100px', fontSize: '1.2rem', display: 'flex', flexDirection: 'column', gap: '0.5rem', backgroundColor: 'rgba(15, 23, 42, 0.5)', border: '1px solid var(--secondary)' }}>
          <Activity size={24} color="var(--secondary)" />
          My Clinical Visits
        </button>
        <button onClick={() => window.location.href = '/patient/book-appointment'} className="btn" style={{ height: '100px', fontSize: '1.2rem', display: 'flex', flexDirection: 'column', gap: '0.5rem', backgroundColor: 'rgba(15, 23, 42, 0.5)', border: '1px solid #F59E0B' }}>
          <Activity size={24} color="#F59E0B" />
          Book Appointment
        </button>
      </div>

      <div className="glass-card">
        <h2 style={{ marginBottom: '2rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <Activity size={24} color="var(--primary)" /> My Analysis Reports
        </h2>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '2rem' }}>Loading reports...</div>
        ) : reports.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-secondary)' }}>
            No analysis reports found for your account yet.
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {reports.map((report) => (
              <div key={report.id} style={{ backgroundColor: 'rgba(15, 23, 42, 0.5)', padding: '1.5rem', borderRadius: '12px', border: '1px solid var(--surface-border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <h3 style={{ fontSize: '1.2rem', marginBottom: '0.5rem', color: report.severityScore > 50 ? 'var(--error)' : 'var(--secondary)' }}>
                    {report.diagnosis}
                  </h3>
                  <div style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
                    Severity Score: <strong style={{ color: 'var(--text-primary)' }}>{report.severityScore.toFixed(1)}/100</strong>
                    <span style={{ margin: '0 0.5rem' }}>•</span>
                    {new Date(report.timestamp).toLocaleDateString()}
                  </div>
                </div>
                {report.frontalImagePath && (
                  <img src={report.frontalImagePath} alt="Scan" style={{ width: '60px', height: '60px', borderRadius: '8px', objectFit: 'cover' }} />
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default PatientDashboard;

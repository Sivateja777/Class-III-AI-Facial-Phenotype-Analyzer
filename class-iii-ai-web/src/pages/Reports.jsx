import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { auth, db } from '../firebase';
import { collection, query, getDocs } from 'firebase/firestore';
import { ArrowLeft, FileText } from 'lucide-react';

const Reports = () => {
  const navigate = useNavigate();
  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchReports();
  }, []);

  const fetchReports = async () => {
    if (!auth.currentUser) return;
    try {
      // Doctor sees all reports
      const q = query(collection(db, 'analysis_reports'));
      const querySnapshot = await getDocs(q);
      const reps = [];
      querySnapshot.forEach((doc) => {
        reps.push({ id: doc.id, ...doc.data() });
      });
      reps.sort((a, b) => b.timestamp - a.timestamp);
      setReports(reps);
    } catch (err) {
      console.error("Error fetching reports:", err);
    }
    setLoading(false);
  };

  return (
    <div className="container" style={{ padding: '2rem' }}>
      <button onClick={() => navigate('/doctor')} className="btn" style={{ backgroundColor: 'transparent', padding: '0.5rem 0', marginBottom: '2rem', color: 'var(--text-secondary)' }}>
        <ArrowLeft size={18} /> Back to Dashboard
      </button>

      <div className="glass-card">
        <h2 style={{ marginBottom: '2rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <FileText size={24} color="var(--secondary)" /> Clinic Reports Database
        </h2>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '2rem' }}>Loading reports...</div>
        ) : reports.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-secondary)' }}>
            No analysis reports found in the clinic database.
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--surface-border)', color: 'var(--text-secondary)' }}>
                  <th style={{ padding: '1rem' }}>Patient Name</th>
                  <th style={{ padding: '1rem' }}>Diagnosis</th>
                  <th style={{ padding: '1rem' }}>Severity</th>
                  <th style={{ padding: '1rem' }}>Date</th>
                </tr>
              </thead>
              <tbody>
                {reports.map((report) => (
                  <tr key={report.id} style={{ borderBottom: '1px solid var(--surface-border)' }}>
                    <td style={{ padding: '1rem' }}>{report.patientName}</td>
                    <td style={{ padding: '1rem', color: report.severityScore > 50 ? 'var(--error)' : 'var(--secondary)' }}>{report.diagnosis}</td>
                    <td style={{ padding: '1rem' }}>{report.severityScore.toFixed(1)}/100</td>
                    <td style={{ padding: '1rem', color: 'var(--text-secondary)' }}>{new Date(report.timestamp).toLocaleDateString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default Reports;

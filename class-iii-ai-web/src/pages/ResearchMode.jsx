import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { auth, db } from '../firebase';
import { collection, query, where, getDocs } from 'firebase/firestore';
import { ArrowLeft, Download, Beaker } from 'lucide-react';

const ResearchMode = () => {
  const navigate = useNavigate();
  const [patients, setPatients] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchPatients();
  }, []);

  const fetchPatients = async () => {
    if (!auth.currentUser) return;
    try {
      const q = query(collection(db, 'patients'), where('doctorEmail', '==', auth.currentUser.email));
      const querySnapshot = await getDocs(q);
      const pts = [];
      querySnapshot.forEach((doc) => {
        pts.push({ id: doc.id, ...doc.data() });
      });
      setPatients(pts);
    } catch (err) {
      console.error(err);
    }
    setLoading(false);
  };

  const exportCsv = () => {
    if (patients.length === 0) {
      alert("No data to export");
      return;
    }
    
    let csvContent = "data:text/csv;charset=utf-8,";
    csvContent += "Patient_ID,Age,Gender,Ethnicity,Growth_Status\n"; // Anonymized headers matching Android
    
    patients.forEach(p => {
      const anonymizedId = `PAT-${p.id.substring(0, 8).toUpperCase()}`;
      const row = `${anonymizedId},${p.age || 'N/A'},${p.gender || 'N/A'},Unknown,Unknown\n`;
      csvContent += row;
    });

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", `class_iii_research_export_${Date.now()}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="container" style={{ padding: '2rem' }}>
      <button onClick={() => navigate(-1)} className="btn" style={{ backgroundColor: 'transparent', padding: '0.5rem 0', marginBottom: '2rem', color: 'var(--text-secondary)' }}>
        <ArrowLeft size={18} /> Back
      </button>

      <div className="glass-card flex-center" style={{ flexDirection: 'column', maxWidth: '600px', margin: '0 auto', textAlign: 'center' }}>
        <Beaker size={48} color="var(--primary)" style={{ marginBottom: '1rem' }} />
        <h2 style={{ marginBottom: '1rem' }}>Clinical Research Mode</h2>
        <p style={{ color: 'var(--text-secondary)', marginBottom: '2rem', lineHeight: '1.5' }}>
          Research Mode allows you to export anonymized patient data for longitudinal studies and batch analysis. 
          All Personally Identifiable Information (PII) is automatically stripped before export.
        </p>

        <div style={{ backgroundColor: 'rgba(15, 23, 42, 0.5)', padding: '2rem', borderRadius: '12px', border: '1px solid var(--surface-border)', width: '100%', marginBottom: '2rem' }}>
          <h1 style={{ fontSize: '3rem', color: 'var(--text-primary)', marginBottom: '0.5rem' }}>{loading ? '...' : patients.length}</h1>
          <p style={{ color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '2px', fontSize: '0.8rem' }}>Total Eligible Cases</p>
        </div>

        <button onClick={exportCsv} className="btn btn-secondary" style={{ width: '100%', display: 'flex', justifyContent: 'center', gap: '0.5rem' }} disabled={loading || patients.length === 0}>
          <Download size={20} /> Export Anonymized CSV
        </button>
      </div>
    </div>
  );
};

export default ResearchMode;

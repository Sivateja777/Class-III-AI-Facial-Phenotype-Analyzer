import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { auth, db } from '../firebase';
import { collection, query, where, getDocs, addDoc, getDoc, doc } from 'firebase/firestore';
import { ArrowLeft, Upload, AlertCircle, CheckCircle2 } from 'lucide-react';

const AnalysisPortal = () => {
  const navigate = useNavigate();
  const [patients, setPatients] = useState([]);
  const [selectedPatient, setSelectedPatient] = useState('');
  
  const [frontalImage, setFrontalImage] = useState(null);
  const [lateralImage, setLateralImage] = useState(null);
  const [frontalPreview, setFrontalPreview] = useState(null);
  const [lateralPreview, setLateralPreview] = useState(null);
  const [showGrid, setShowGrid] = useState(false);
  
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');

  const [isPatient, setIsPatient] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    checkRoleAndFetchPatients();
  }, []);

  const checkRoleAndFetchPatients = async () => {
    if (!auth.currentUser) return;
    
    // Check if the current user is a patient
    const emailKey = auth.currentUser.email.replace(/\./g, '_').replace(/@/g, '_');
    const userDoc = await getDoc(doc(db, "users", emailKey));
    
    if (userDoc.exists() && userDoc.data().role === 'patient') {
      setIsPatient(true);
      // For a patient, we just use their own profile for the selected patient
      setPatients([{
        id: 'self',
        name: auth.currentUser.displayName || 'Self',
        patientEmail: auth.currentUser.email,
        doctorEmail: '' // Optional: could fetch their assigned doctor if needed
      }]);
      setSelectedPatient('self');
    } else {
      setIsPatient(false);
      const q = query(collection(db, 'patients'), where('doctorEmail', '==', auth.currentUser.email));
      const querySnapshot = await getDocs(q);
      const pts = [];
      querySnapshot.forEach((doc) => {
        pts.push({ id: doc.id, ...doc.data() });
      });
      setPatients(pts);
    }
    setLoading(false);
  };

  const handleFileChange = (e, type) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      const previewUrl = URL.createObjectURL(file);
      if (type === 'frontal') {
        setFrontalImage(file);
        setFrontalPreview(previewUrl);
      }
      if (type === 'lateral') {
        setLateralImage(file);
        setLateralPreview(previewUrl);
      }
    }
  };

  const handleAnalyze = async () => {
    if (!frontalImage || !lateralImage || !selectedPatient) {
      setError('Please select a patient and upload both Frontal and Lateral scans.');
      return;
    }
    
    setError('');
    setIsAnalyzing(true);
    setResult(null);

    const formData = new FormData();
    formData.append('frontal', frontalImage);
    formData.append('lateral', lateralImage);

    try {
      // Direct call to local Python backend
      const response = await fetch('http://192.168.137.57:8000/analyze', {
        method: 'POST',
        body: formData,
      });

      if (!response.ok) {
        throw new Error(`Backend Error: ${response.statusText}`);
      }

      const data = await response.json();
      
      // Save result to Firestore
      const patientData = patients.find(p => p.id === selectedPatient);
      const report = {
        patientName: patientData.name,
        patientEmail: patientData.patientEmail || '',
        doctorEmail: isPatient ? patientData.doctorEmail : auth.currentUser.email,
        diagnosis: data.diagnosis || 'Class I - Normal',
        severityScore: data.severityScore || 15.0,
        severityCategory: data.severityCategory || 'Mild',
        clinicalReasoning: data.features?.clinical_reasoning || 'No clinical reasoning provided.',
        heatmapUrl: data.heatmapUrl || '',
        reportUrl: data.reportUrl || '',
        timestamp: Date.now()
      };

      await addDoc(collection(db, 'analysis_reports'), report);
      
      setResult(report);
    } catch (err) {
      console.error(err);
      setError('Failed to connect to the AI Backend. Ensure the Python server is running at 192.168.137.57:8000.');
    }
    setIsAnalyzing(false);
  };

  return (
    <div className="container" style={{ padding: '2rem' }}>
      <button onClick={() => navigate(-1)} className="btn" style={{ backgroundColor: 'transparent', padding: '0.5rem 0', marginBottom: '2rem', color: 'var(--text-secondary)' }}>
        <ArrowLeft size={18} /> Back to Dashboard
      </button>

      <h1 style={{ fontSize: '2rem', marginBottom: '2rem' }}>AI Dual Scan Analysis</h1>

      {error && (
        <div style={{ padding: '1rem', backgroundColor: 'rgba(239, 68, 68, 0.2)', color: 'var(--error)', borderRadius: '8px', marginBottom: '2rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <AlertCircle size={20} /> {error}
        </div>
      )}

      {result && (
        <div className="glass-card animate-fade-in" style={{ marginBottom: '2rem', border: `1px solid ${result.severityScore > 50 ? 'var(--error)' : 'var(--secondary)'}` }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem' }}>
            <div>
              <h2 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem', color: result.severityScore > 50 ? 'var(--error)' : 'var(--secondary)' }}>
                <CheckCircle2 /> Analysis Complete
              </h2>
              <div style={{ fontSize: '1.2rem', marginBottom: '0.5rem' }}>Phenotype Diagnosis: <strong>{result.diagnosis}</strong></div>
              <div style={{ fontSize: '1.2rem', marginBottom: '0.5rem' }}>Severity Index: <strong>{result.severityScore.toFixed(1)}/100 ({result.severityCategory})</strong></div>
              {result.severityCategory === 'Severe' && (
                <div style={{ marginTop: '0.5rem', display: 'inline-block', backgroundColor: 'rgba(239, 68, 68, 0.2)', color: 'var(--error)', padding: '0.5rem 1rem', borderRadius: '4px', fontWeight: 'bold', marginRight: '0.5rem' }}>
                  Surgical Intervention Highly Likely
                </div>
              )}
              {!result.diagnosis.includes('Class I') && (
                <div style={{ marginTop: '0.5rem', display: 'inline-block', backgroundColor: 'rgba(234, 179, 8, 0.2)', color: '#eab308', padding: '0.5rem 1rem', borderRadius: '4px', fontWeight: 'bold' }}>
                  Medical Recommendation: Please visit a doctor for further clinical evaluation.
                </div>
              )}
              {result.clinicalReasoning && (
                <div style={{ marginTop: '1.5rem', padding: '1rem', backgroundColor: 'rgba(16, 185, 129, 0.1)', borderLeft: '4px solid var(--primary)', borderRadius: '4px' }}>
                  <h3 style={{ fontSize: '1rem', color: 'var(--primary)', marginBottom: '0.5rem' }}>AI Clinical Reasoning</h3>
                  <p style={{ fontSize: '0.95rem', color: 'var(--text-secondary)', lineHeight: '1.5' }}>
                    {result.clinicalReasoning}
                  </p>
                </div>
              )}
            </div>
            {result.reportUrl && (
              <a href={result.reportUrl} target="_blank" rel="noreferrer" className="btn btn-secondary" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}>
                Download PDF Report
              </a>
            )}
          </div>
        </div>
      )}

      <div className="glass-card" style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
        {loading ? (
          <div>Loading portal...</div>
        ) : (
          <>
            {isPatient ? (
              <div className="form-group" style={{ backgroundColor: 'rgba(59, 130, 246, 0.1)', padding: '1rem', borderRadius: '8px', border: '1px solid rgba(59, 130, 246, 0.3)' }}>
                <div style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '0.25rem' }}>Analyzing Profile For:</div>
                <div style={{ fontSize: '1.2rem', fontWeight: 'bold', color: 'var(--primary)' }}>{auth.currentUser?.displayName || 'Your Profile'}</div>
              </div>
            ) : (
              <div className="form-group">
                <label className="input-label">Select Patient</label>
                <select className="input-field" value={selectedPatient} onChange={e => setSelectedPatient(e.target.value)}>
                  <option value="">-- Choose Patient --</option>
                  {patients.map(p => (
                    <option key={p.id} value={p.id}>{p.name} {p.patientEmail ? `(${p.patientEmail})` : ''}</option>
                  ))}
                </select>
              </div>
            )}
          </>
        )}

        <div style={{ display: 'flex', gap: '2rem', flexWrap: 'wrap' }}>
          
          <div style={{ flex: 1, minWidth: '250px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
              <label className="input-label" style={{ margin: 0 }}>Frontal Scan</label>
              <button className="btn" style={{ padding: '0.2rem 0.5rem', fontSize: '0.8rem', backgroundColor: 'rgba(255,255,255,0.1)' }} onClick={() => setShowGrid(!showGrid)}>
                Toggle Grid
              </button>
            </div>
            <div style={{ border: '2px dashed var(--surface-border)', borderRadius: '12px', textAlign: 'center', backgroundColor: 'rgba(0,0,0,0.2)', position: 'relative', overflow: 'hidden', height: frontalPreview ? '300px' : 'auto', padding: frontalPreview ? '0' : '2rem' }}>
              {frontalPreview ? (
                <>
                  <img src={frontalPreview} alt="Frontal preview" style={{ width: '100%', height: '100%', objectFit: 'contain' }} />
                  {showGrid && (
                    <div style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, pointerEvents: 'none' }}>
                      <div style={{ position: 'absolute', top: '50%', left: 0, right: 0, borderTop: '1px solid rgba(16, 185, 129, 0.7)' }}></div>
                      <div style={{ position: 'absolute', left: '50%', top: 0, bottom: 0, borderLeft: '1px solid rgba(16, 185, 129, 0.7)' }}></div>
                      <div style={{ position: 'absolute', top: '25%', left: 0, right: 0, borderTop: '1px dashed rgba(16, 185, 129, 0.4)' }}></div>
                      <div style={{ position: 'absolute', top: '75%', left: 0, right: 0, borderTop: '1px dashed rgba(16, 185, 129, 0.4)' }}></div>
                    </div>
                  )}
                  <input type="file" id="frontal" accept="image/*" style={{ display: 'none' }} onChange={(e) => handleFileChange(e, 'frontal')} />
                  <label htmlFor="frontal" style={{ position: 'absolute', bottom: '10px', right: '10px', backgroundColor: 'var(--primary)', padding: '0.5rem 1rem', borderRadius: '4px', cursor: 'pointer', fontSize: '0.9rem' }}>Change</label>
                </>
              ) : (
                <>
                  <input type="file" id="frontal" accept="image/*" style={{ display: 'none' }} onChange={(e) => handleFileChange(e, 'frontal')} />
                  <label htmlFor="frontal" style={{ cursor: 'pointer', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.5rem', height: '100%', justifyContent: 'center' }}>
                    <Upload size={32} color="var(--text-secondary)" />
                    <span style={{ color: 'var(--text-secondary)' }}>Upload Frontal Image</span>
                  </label>
                </>
              )}
            </div>
          </div>

          <div style={{ flex: 1, minWidth: '250px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
              <label className="input-label" style={{ margin: 0 }}>Lateral Profile Scan</label>
              <button className="btn" style={{ padding: '0.2rem 0.5rem', fontSize: '0.8rem', backgroundColor: 'rgba(255,255,255,0.1)' }} onClick={() => setShowGrid(!showGrid)}>
                Toggle Grid
              </button>
            </div>
            <div style={{ border: '2px dashed var(--surface-border)', borderRadius: '12px', textAlign: 'center', backgroundColor: 'rgba(0,0,0,0.2)', position: 'relative', overflow: 'hidden', height: lateralPreview ? '300px' : 'auto', padding: lateralPreview ? '0' : '2rem' }}>
              {lateralPreview ? (
                <>
                  <img src={lateralPreview} alt="Lateral preview" style={{ width: '100%', height: '100%', objectFit: 'contain' }} />
                  {showGrid && (
                    <div style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, pointerEvents: 'none' }}>
                      <div style={{ position: 'absolute', top: '50%', left: 0, right: 0, borderTop: '1px solid rgba(16, 185, 129, 0.7)' }}></div>
                      <div style={{ position: 'absolute', left: '50%', top: 0, bottom: 0, borderLeft: '1px solid rgba(16, 185, 129, 0.7)' }}></div>
                    </div>
                  )}
                  <input type="file" id="lateral" accept="image/*" style={{ display: 'none' }} onChange={(e) => handleFileChange(e, 'lateral')} />
                  <label htmlFor="lateral" style={{ position: 'absolute', bottom: '10px', right: '10px', backgroundColor: 'var(--primary)', padding: '0.5rem 1rem', borderRadius: '4px', cursor: 'pointer', fontSize: '0.9rem' }}>Change</label>
                </>
              ) : (
                <>
                  <input type="file" id="lateral" accept="image/*" style={{ display: 'none' }} onChange={(e) => handleFileChange(e, 'lateral')} />
                  <label htmlFor="lateral" style={{ cursor: 'pointer', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.5rem', height: '100%', justifyContent: 'center' }}>
                    <Upload size={32} color="var(--text-secondary)" />
                    <span style={{ color: 'var(--text-secondary)' }}>Upload Lateral Image</span>
                  </label>
                </>
              )}
            </div>
          </div>
        </div>

        <button 
          className="btn btn-secondary" 
          style={{ width: '100%', marginTop: '1rem', padding: '1rem', fontSize: '1.1rem' }}
          onClick={handleAnalyze}
          disabled={isAnalyzing}>
          {isAnalyzing ? 'Processing via AI Engine...' : 'Run Dual Scan Analysis'}
        </button>

      </div>
    </div>
  );
};

export default AnalysisPortal;

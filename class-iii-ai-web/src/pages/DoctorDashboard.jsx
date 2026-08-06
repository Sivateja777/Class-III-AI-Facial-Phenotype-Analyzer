import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { auth, db } from '../firebase';
import { collection, query, where, onSnapshot, addDoc } from 'firebase/firestore';
import { Plus, Activity, FileText, LogOut, Users, Calendar } from 'lucide-react';
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from 'chart.js';
import { Pie } from 'react-chartjs-2';

ChartJS.register(ArcElement, Tooltip, Legend);

const DoctorDashboard = () => {
  const navigate = useNavigate();
  const [patients, setPatients] = useState([]);
  const [showModal, setShowModal] = useState(false);
  
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [age, setAge] = useState('');
  const [gender, setGender] = useState('');
  const [ethnicity, setEthnicity] = useState('');
  const [growthStatus, setGrowthStatus] = useState('Adult');
  const [clinicalDiagnosis, setClinicalDiagnosis] = useState('');
  const [cephValues, setCephValues] = useState('');

  const [stats, setStats] = useState({
    patientCount: 0,
    reportCount: 0,
    apptCount: 0,
    classI: 0,
    classII: 0,
    classIII: 0
  });
  
  useEffect(() => {
    if (!auth.currentUser) return;

    const unsubPatients = onSnapshot(query(collection(db, 'patients'), where('doctorEmail', '==', auth.currentUser.email)), (snapshot) => {
      const pts = [];
      snapshot.forEach((doc) => pts.push({ id: doc.id, ...doc.data() }));
      setPatients(pts);
      setStats(prev => ({ ...prev, patientCount: pts.length }));
    });

    const unsubAppts = onSnapshot(query(collection(db, 'appointments'), where('doctorEmail', '==', auth.currentUser.email)), (snapshot) => {
      setStats(prev => ({ ...prev, apptCount: snapshot.size }));
    });

    const unsubReports = onSnapshot(query(collection(db, 'analysis_reports'), where('doctorEmail', '==', auth.currentUser.email)), (snapshot) => {
      let c1 = 0, c2 = 0, c3 = 0;
      snapshot.forEach(doc => {
        const diag = doc.data().diagnosis || '';
        if (diag.includes('Class I')) c1++;
        else if (diag.includes('Class II')) c2++;
        else if (diag.includes('Class III')) c3++;
      });
      setStats(prev => ({
        ...prev,
        reportCount: snapshot.size,
        classI: c1,
        classII: c2,
        classIII: c3
      }));
    });

    return () => {
      unsubPatients();
      unsubAppts();
      unsubReports();
    };
  }, []);

  const handleLogout = () => {
    auth.signOut();
  };

  const handleAddPatient = async (e) => {
    e.preventDefault();
    try {
      await addDoc(collection(db, 'patients'), {
        name,
        patientEmail: email,
        age,
        gender,
        ethnicity,
        growthStatus,
        clinicalDiagnosis,
        cephValues,
        doctorEmail: auth.currentUser.email,
        timestamp: Date.now()
      });
      setShowModal(false);
      // Removed fetchPatients() as onSnapshot handles updates
      setName(''); setEmail(''); setAge(''); setGender('');
      setEthnicity(''); setGrowthStatus('Adult'); setClinicalDiagnosis(''); setCephValues('');
    } catch (err) {
      console.error('Error adding patient:', err);
    }
  };

  return (
    <div className="container" style={{ padding: '2rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '3rem' }}>
        <div>
          <h1 style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>Doctor Dashboard</h1>
          <p style={{ color: 'var(--text-secondary)' }}>Welcome, {auth.currentUser?.displayName}</p>
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

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2rem', marginBottom: '3rem' }}>
        <div className="glass-card" style={{ display: 'flex', flexDirection: 'column', gap: '1rem', justifyContent: 'center' }}>
          <h2 style={{ marginBottom: '1rem' }}>Clinical Overview</h2>
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <div style={{ backgroundColor: 'rgba(59, 130, 246, 0.2)', padding: '1rem', borderRadius: '12px' }}><Users size={24} color="#3B82F6" /></div>
            <div>
              <div style={{ fontSize: '1.5rem', fontWeight: 'bold' }}>{stats.patientCount}</div>
              <div style={{ color: 'var(--text-secondary)' }}>Total Patients</div>
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <div style={{ backgroundColor: 'rgba(139, 92, 246, 0.2)', padding: '1rem', borderRadius: '12px' }}><FileText size={24} color="#8B5CF6" /></div>
            <div>
              <div style={{ fontSize: '1.5rem', fontWeight: 'bold' }}>{stats.reportCount}</div>
              <div style={{ color: 'var(--text-secondary)' }}>Analyses Performed</div>
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <div style={{ backgroundColor: 'rgba(245, 158, 11, 0.2)', padding: '1rem', borderRadius: '12px' }}><Calendar size={24} color="#F59E0B" /></div>
            <div>
              <div style={{ fontSize: '1.5rem', fontWeight: 'bold' }}>{stats.apptCount}</div>
              <div style={{ color: 'var(--text-secondary)' }}>Appointments</div>
            </div>
          </div>
        </div>

        <div className="glass-card flex-center" style={{ flexDirection: 'column' }}>
          <h2 style={{ alignSelf: 'flex-start', marginBottom: '1rem' }}>Classifications Breakdown</h2>
          {stats.reportCount === 0 ? (
             <div style={{ color: 'var(--text-secondary)', padding: '2rem' }}>No data available yet.</div>
          ) : (
            <div style={{ width: '250px', height: '250px' }}>
              <Pie 
                data={{
                  labels: ['Class I', 'Class II', 'Class III'],
                  datasets: [{
                    data: [stats.classI, stats.classII, stats.classIII],
                    backgroundColor: ['#3B82F6', '#F97316', '#EF4444'],
                    borderWidth: 0,
                  }]
                }} 
                options={{ 
                  plugins: { legend: { position: 'bottom', labels: { color: 'white' } } },
                  cutout: '60%' // Donut shape like the Android PieChart
                }} 
              />
            </div>
          )}
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '2rem', marginBottom: '3rem' }}>
        
        <div className="glass-card flex-center" style={{ flexDirection: 'column', cursor: 'pointer', transition: 'all 0.3s' }} 
             onClick={() => navigate('/doctor/analysis')}>
          <div style={{ backgroundColor: 'var(--primary)', padding: '1.5rem', borderRadius: '50%', marginBottom: '1rem' }}>
            <Activity size={32} color="white" />
          </div>
          <h3 style={{ fontSize: '1.25rem', marginBottom: '0.5rem' }}>Start Analysis</h3>
          <p style={{ color: 'var(--text-secondary)', textAlign: 'center' }}>Upload dual scans for AI analysis</p>
        </div>

        <div className="glass-card flex-center" style={{ flexDirection: 'column', cursor: 'pointer', transition: 'all 0.3s' }}
             onClick={() => navigate('/doctor/reports')}>
          <div style={{ backgroundColor: 'var(--secondary)', padding: '1.5rem', borderRadius: '50%', marginBottom: '1rem' }}>
            <FileText size={32} color="white" />
          </div>
          <h3 style={{ fontSize: '1.25rem', marginBottom: '0.5rem' }}>View Reports</h3>
          <p style={{ color: 'var(--text-secondary)', textAlign: 'center' }}>Access clinic-wide analysis reports</p>
        </div>
        
        <div className="glass-card flex-center" style={{ flexDirection: 'column', cursor: 'pointer', transition: 'all 0.3s' }}
             onClick={() => navigate('/doctor/research')}>
          <div style={{ backgroundColor: '#F59E0B', padding: '1.5rem', borderRadius: '50%', marginBottom: '1rem' }}>
            <Activity size={32} color="white" />
          </div>
          <h3 style={{ fontSize: '1.25rem', marginBottom: '0.5rem' }}>Research Mode</h3>
          <p style={{ color: 'var(--text-secondary)', textAlign: 'center' }}>Export anonymized clinical data</p>
        </div>

      </div>

      <div className="glass-card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
          <h2>My Patients</h2>
          <button className="btn btn-secondary" onClick={() => setShowModal(true)}>
            <Plus size={18} /> Add Patient
          </button>
        </div>
        
        {patients.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-secondary)' }}>
            No patients added yet.
          </div>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--surface-border)', color: 'var(--text-secondary)' }}>
                <th style={{ padding: '1rem' }}>Name</th>
                <th style={{ padding: '1rem' }}>Email</th>
                <th style={{ padding: '1rem' }}>Age</th>
                <th style={{ padding: '1rem' }}>Gender</th>
                <th style={{ padding: '1rem' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {patients.map(p => (
                <tr key={p.id} style={{ borderBottom: '1px solid var(--surface-border)' }}>
                  <td style={{ padding: '1rem' }}>{p.name}</td>
                  <td style={{ padding: '1rem', color: 'var(--text-secondary)' }}>{p.patientEmail || 'N/A'}</td>
                  <td style={{ padding: '1rem' }}>{p.age}</td>
                  <td style={{ padding: '1rem' }}>{p.gender}</td>
                  <td style={{ padding: '1rem' }}>
                    {p.patientEmail && (
                      <button className="btn" style={{ padding: '0.4rem 0.8rem', fontSize: '0.8rem', backgroundColor: 'var(--secondary)' }} onClick={() => navigate(`/doctor/visits?patientEmail=${encodeURIComponent(p.patientEmail)}`)}>
                        Visits
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {showModal && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div className="glass-card animate-fade-in" style={{ width: '100%', maxWidth: '500px' }}>
            <h2 style={{ marginBottom: '1.5rem' }}>Add New Patient</h2>
            <form onSubmit={handleAddPatient}>
              <div className="form-group">
                <label className="input-label">Full Name</label>
                <input type="text" className="input-field" value={name} onChange={e => setName(e.target.value)} required />
              </div>
              <div className="form-group">
                <label className="input-label">Patient Email (For them to log in)</label>
                <input type="email" className="input-field" value={email} onChange={e => setEmail(e.target.value)} />
              </div>
              <div style={{ display: 'flex', gap: '1rem', marginBottom: '1.5rem' }}>
                <div style={{ flex: 1 }}>
                  <label className="input-label">Age</label>
                  <input type="number" className="input-field" value={age} onChange={e => setAge(e.target.value)} required />
                </div>
                <div style={{ flex: 1 }}>
                  <label className="input-label">Gender</label>
                  <select className="input-field" value={gender} onChange={e => setGender(e.target.value)} required>
                    <option value="">Select</option>
                    <option value="Male">Male</option>
                    <option value="Female">Female</option>
                    <option value="Other">Other</option>
                  </select>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '1rem', marginBottom: '1.5rem' }}>
                <div style={{ flex: 1 }}>
                  <label className="input-label">Ethnicity</label>
                  <input type="text" className="input-field" value={ethnicity} onChange={e => setEthnicity(e.target.value)} placeholder="e.g. Asian, Caucasian" />
                </div>
                <div style={{ flex: 1 }}>
                  <label className="input-label">Growth Status</label>
                  <select className="input-field" value={growthStatus} onChange={e => setGrowthStatus(e.target.value)}>
                    <option value="Pre-pubertal">Pre-pubertal</option>
                    <option value="Pubertal">Pubertal</option>
                    <option value="Adult">Adult</option>
                  </select>
                </div>
              </div>

              <div className="form-group">
                <label className="input-label">Clinical Diagnosis (Optional)</label>
                <input type="text" className="input-field" value={clinicalDiagnosis} onChange={e => setClinicalDiagnosis(e.target.value)} placeholder="e.g. Skeletal Class III with open bite" />
              </div>

              <div className="form-group" style={{ marginBottom: '1.5rem' }}>
                <label className="input-label">Cephalometric Values (Optional)</label>
                <textarea className="input-field" rows="2" value={cephValues} onChange={e => setCephValues(e.target.value)} placeholder="SNA, SNB, ANB values..." />
              </div>

              <div style={{ display: 'flex', gap: '1rem' }}>
                <button type="button" className="btn" style={{ flex: 1, backgroundColor: 'rgba(255,255,255,0.1)' }} onClick={() => setShowModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-secondary" style={{ flex: 1 }}>Save Patient</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default DoctorDashboard;

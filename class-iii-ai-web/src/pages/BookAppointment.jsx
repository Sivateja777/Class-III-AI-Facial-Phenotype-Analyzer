import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { auth, db } from '../firebase';
import { collection, query, where, getDocs, addDoc } from 'firebase/firestore';
import { ArrowLeft, CalendarPlus } from 'lucide-react';

const BookAppointment = () => {
  const navigate = useNavigate();
  const [doctors, setDoctors] = useState([]);
  const [loading, setLoading] = useState(true);
  
  const [selectedDoctor, setSelectedDoctor] = useState('');
  const [age, setAge] = useState('');
  const [complaint, setComplaint] = useState('');
  const [history, setHistory] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    const fetchDoctors = async () => {
      try {
        const q = query(collection(db, 'users'), where('role', '==', 'doctor'));
        const querySnapshot = await getDocs(q);
        const docs = [];
        querySnapshot.forEach((doc) => {
          docs.push({ id: doc.id, ...doc.data() });
        });
        setDoctors(docs);
      } catch (err) {
        console.error("Error fetching doctors", err);
      }
      setLoading(false);
    };
    fetchDoctors();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!selectedDoctor || !age || !complaint) {
      alert("Please fill all required fields.");
      return;
    }
    setSubmitting(true);
    try {
      await addDoc(collection(db, 'appointments'), {
        patientName: auth.currentUser?.displayName || 'Unknown',
        patientEmail: auth.currentUser?.email,
        age: parseInt(age),
        doctorEmail: selectedDoctor,
        chiefComplaint: complaint,
        clinicalHistory: history,
        status: 'Pending',
        timestamp: Date.now()
      });
      alert('Appointment Requested Successfully!');
      navigate(-1);
    } catch (err) {
      console.error(err);
      alert('Failed to request appointment');
    }
    setSubmitting(false);
  };

  return (
    <div className="container" style={{ padding: '2rem' }}>
      <button onClick={() => navigate(-1)} className="btn" style={{ backgroundColor: 'transparent', padding: '0.5rem 0', marginBottom: '2rem', color: 'var(--text-secondary)' }}>
        <ArrowLeft size={18} /> Back
      </button>

      <div className="glass-card flex-center" style={{ flexDirection: 'column', maxWidth: '600px', margin: '0 auto' }}>
        <h2 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '2rem' }}>
          <CalendarPlus size={28} color="var(--primary)" /> Book Appointment
        </h2>
        
        {loading ? (
          <div>Loading available doctors...</div>
        ) : (
          <form onSubmit={handleSubmit} style={{ width: '100%' }}>
            <div className="form-group">
              <label className="input-label">Select Doctor *</label>
              <select className="input-field" value={selectedDoctor} onChange={(e) => setSelectedDoctor(e.target.value)} required>
                <option value="">-- Choose a Doctor --</option>
                {doctors.map(doc => (
                  <option key={doc.id} value={doc.email}>Dr. {doc.displayName}</option>
                ))}
              </select>
            </div>
            
            <div className="form-group">
              <label className="input-label">Patient Age *</label>
              <input type="number" className="input-field" value={age} onChange={(e) => setAge(e.target.value)} required />
            </div>

            <div className="form-group">
              <label className="input-label">Chief Complaint *</label>
              <textarea className="input-field" rows="3" value={complaint} onChange={(e) => setComplaint(e.target.value)} required placeholder="Briefly describe the main reason for visit" />
            </div>

            <div className="form-group">
              <label className="input-label">Clinical History (Optional)</label>
              <textarea className="input-field" rows="3" value={history} onChange={(e) => setHistory(e.target.value)} placeholder="Any previous treatments or relevant history" />
            </div>

            <button type="submit" className="btn btn-secondary" style={{ width: '100%', marginTop: '1rem' }} disabled={submitting}>
              {submitting ? 'Submitting...' : 'Request Appointment'}
            </button>
          </form>
        )}
      </div>
    </div>
  );
};

export default BookAppointment;

import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { auth, db } from '../firebase';
import { collection, query, where, onSnapshot, addDoc } from 'firebase/firestore';
import { ArrowLeft, Calendar, Plus } from 'lucide-react';

const PatientVisits = ({ isDoctorView }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);
  const patientEmailFromUrl = searchParams.get('patientEmail');

  const [visits, setVisits] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [notes, setNotes] = useState('');

  const targetEmail = isDoctorView ? patientEmailFromUrl : auth.currentUser?.email;

  useEffect(() => {
    if (!targetEmail) {
      setLoading(false);
      return;
    }
    const q = query(collection(db, 'visits'), where('patientEmail', '==', targetEmail));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const fetchedVisits = [];
      snapshot.forEach((doc) => {
        fetchedVisits.push({ id: doc.id, ...doc.data() });
      });
      fetchedVisits.sort((a, b) => b.timestamp - a.timestamp);
      setVisits(fetchedVisits);
      setLoading(false);
    }, (err) => {
      console.error(err);
      setLoading(false);
    });

    return () => unsubscribe();
  }, [targetEmail]);

  const handleAddVisit = async (e) => {
    e.preventDefault();
    if (!notes.trim()) return;
    
    try {
      await addDoc(collection(db, 'visits'), {
        patientEmail: targetEmail,
        doctorEmail: auth.currentUser.email,
        notes: notes,
        timestamp: Date.now()
      });
      setNotes('');
      setShowModal(false);
    } catch (err) {
      console.error('Error adding visit', err);
    }
  };

  return (
    <div className="container" style={{ padding: '2rem' }}>
      <button onClick={() => navigate(-1)} className="btn" style={{ backgroundColor: 'transparent', padding: '0.5rem 0', marginBottom: '2rem', color: 'var(--text-secondary)' }}>
        <ArrowLeft size={18} /> Back
      </button>

      <div className="glass-card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
          <h2 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Calendar size={24} color="var(--primary)" /> Clinical Visits
          </h2>
          {isDoctorView && (
            <button className="btn btn-secondary" onClick={() => setShowModal(true)}>
              <Plus size={18} /> Log Visit
            </button>
          )}
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '2rem' }}>Loading visits...</div>
        ) : visits.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-secondary)' }}>
            No visits recorded yet.
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {visits.map((visit) => (
              <div key={visit.id} style={{ backgroundColor: 'rgba(15, 23, 42, 0.5)', padding: '1.5rem', borderRadius: '12px', border: '1px solid var(--surface-border)' }}>
                <div style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '0.5rem' }}>
                  {new Date(visit.timestamp).toLocaleDateString()} at {new Date(visit.timestamp).toLocaleTimeString()}
                </div>
                <div style={{ color: 'var(--text-primary)', lineHeight: '1.5' }}>
                  {visit.notes}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {showModal && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div className="glass-card animate-fade-in" style={{ width: '100%', maxWidth: '500px' }}>
            <h2 style={{ marginBottom: '1.5rem' }}>Log New Visit</h2>
            <form onSubmit={handleAddVisit}>
              <div className="form-group">
                <label className="input-label">Clinical Notes</label>
                <textarea 
                  className="input-field" 
                  rows="4"
                  value={notes} 
                  onChange={e => setNotes(e.target.value)} 
                  required 
                  placeholder="Enter treatment details, adjustments made, etc."
                />
              </div>
              <div style={{ display: 'flex', gap: '1rem' }}>
                <button type="button" className="btn" style={{ flex: 1, backgroundColor: 'rgba(255,255,255,0.1)' }} onClick={() => setShowModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-secondary" style={{ flex: 1 }}>Save Log</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default PatientVisits;

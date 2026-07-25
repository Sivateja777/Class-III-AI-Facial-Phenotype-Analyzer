import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { auth, db } from '../firebase';
import { collection, getDocs } from 'firebase/firestore';
import { Shield, Users, Activity, FileText, Calendar, LogOut } from 'lucide-react';

const AdminDashboard = () => {
  const navigate = useNavigate();
  const [stats, setStats] = useState({
    users: 0,
    patients: 0,
    reports: 0,
    appointments: 0
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchStats();
  }, []);

  const fetchStats = async () => {
    try {
      // In a real production app with thousands of documents, you would use Firebase Aggregation queries (e.g., getCountFromServer)
      // For this exact parity match, we will just count the docs.
      
      const usersSnap = await getDocs(collection(db, 'users'));
      const patientsSnap = await getDocs(collection(db, 'patients'));
      const reportsSnap = await getDocs(collection(db, 'analysis_reports'));
      const apptSnap = await getDocs(collection(db, 'appointments'));
      
      setStats({
        users: usersSnap.size,
        patients: patientsSnap.size,
        reports: reportsSnap.size,
        appointments: apptSnap.size
      });
    } catch (err) {
      console.error("Error fetching admin stats:", err);
    }
    setLoading(false);
  };

  const handleLogout = () => {
    auth.signOut();
  };

  const StatCard = ({ icon, title, value, color }) => (
    <div className="glass-card flex-center" style={{ flexDirection: 'column', padding: '2rem' }}>
      <div style={{ backgroundColor: color, padding: '1rem', borderRadius: '50%', marginBottom: '1rem', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        {icon}
      </div>
      <h3 style={{ fontSize: '2.5rem', marginBottom: '0.5rem', color: 'var(--text-primary)' }}>
        {loading ? '...' : value}
      </h3>
      <p style={{ color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '1px', fontSize: '0.85rem' }}>{title}</p>
    </div>
  );

  return (
    <div className="container" style={{ padding: '2rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '3rem' }}>
        <div>
          <h1 style={{ fontSize: '2rem', marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Shield size={32} color="var(--primary)" /> System Administration
          </h1>
          <p style={{ color: 'var(--text-secondary)' }}>Welcome, Super Admin ({auth.currentUser?.email})</p>
        </div>
        <div style={{ display: 'flex', gap: '1rem' }}>
          <button onClick={() => navigate('/profile')} className="btn" style={{ backgroundColor: 'rgba(255,255,255,0.1)' }}>
            Profile
          </button>
          <button onClick={handleLogout} className="btn" style={{ backgroundColor: 'rgba(239, 68, 68, 0.2)', color: 'var(--error)' }}>
            <LogOut size={18} /> Logout
          </button>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '2rem' }}>
        <StatCard icon={<Users size={28} color="white" />} title="Total Users" value={stats.users} color="#3B82F6" />
        <StatCard icon={<Activity size={28} color="white" />} title="Registered Patients" value={stats.patients} color="#10B981" />
        <StatCard icon={<FileText size={28} color="white" />} title="AI Analyses Run" value={stats.reports} color="#8B5CF6" />
        <StatCard icon={<Calendar size={28} color="white" />} title="Appointments" value={stats.appointments} color="#F59E0B" />
      </div>
    </div>
  );
};

export default AdminDashboard;

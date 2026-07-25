import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { onAuthStateChanged } from 'firebase/auth';
import { doc, getDoc } from 'firebase/firestore';
import { auth, db } from './firebase';

import Login from './pages/Login';
import Onboarding from './pages/Onboarding';
import DoctorDashboard from './pages/DoctorDashboard';
import PatientDashboard from './pages/PatientDashboard';
import AnalysisPortal from './pages/AnalysisPortal';
import Reports from './pages/Reports';
import Profile from './pages/Profile';
import PatientVisits from './pages/PatientVisits';
import Simulation from './pages/Simulation';
import AdminDashboard from './pages/AdminDashboard';
import BookAppointment from './pages/BookAppointment';
import ResearchMode from './pages/ResearchMode';

function App() {
  const [user, setUser] = useState(null);
  const [role, setRole] = useState(null);
  const [isProfileComplete, setIsProfileComplete] = useState(true);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (currentUser) => {
      if (currentUser) {
        setUser(currentUser);
        try {
          const emailKey = currentUser.email.replace(/\./g, '_').replace(/@/g, '_');
          const userDoc = await getDoc(doc(db, "users", emailKey));
          if (userDoc.exists()) {
            const data = userDoc.data();
            setRole(data.role || 'patient');
            setIsProfileComplete(!!data.isProfileComplete);
          } else {
            setRole('patient');
            setIsProfileComplete(false);
          }
        } catch (error) {
          console.error("Error fetching user role:", error);
          setRole('patient');
          setIsProfileComplete(false);
        }
      } else {
        setUser(null);
        setRole(null);
        setIsProfileComplete(true);
      }
      setLoading(false);
    });

    return () => unsubscribe();
  }, []);

  if (loading) {
    return <div className="flex-center" style={{ height: '100vh' }}>Loading Class III AI...</div>;
  }

  // Guard components
  const RequireAuth = ({ children, requiredRole }) => {
    if (!user) return <Navigate to="/" />;
    if (!isProfileComplete) return <Navigate to="/onboarding" />;
    
    // Check if user has the required role (if specified)
    if (requiredRole && role !== requiredRole) {
       // If they are admin, redirect to admin
       if (role === 'admin') return <Navigate to="/admin" />;
       if (role === 'doctor') return <Navigate to="/doctor" />;
       return <Navigate to="/patient" />;
    }
    return children;
  };

  const getDashboardRoute = () => {
    if (role === 'admin') return <Navigate to="/admin" />;
    if (role === 'doctor') return <Navigate to="/doctor" />;
    return <Navigate to="/patient" />;
  };

  return (
    <Router>
      <Routes>
        <Route path="/" element={
          !user ? <Login /> : 
          (!isProfileComplete ? <Navigate to="/onboarding" /> : getDashboardRoute())
        } />
        
        <Route path="/onboarding" element={user && !isProfileComplete ? <Onboarding /> : <Navigate to="/" />} />
        
        {/* Admin Routes */}
        <Route path="/admin" element={<RequireAuth requiredRole="admin"><AdminDashboard /></RequireAuth>} />
        
        {/* Doctor Routes */}
        <Route path="/doctor" element={<RequireAuth requiredRole="doctor"><DoctorDashboard /></RequireAuth>} />
        <Route path="/doctor/analysis" element={<RequireAuth requiredRole="doctor"><AnalysisPortal /></RequireAuth>} />
        <Route path="/doctor/reports" element={<RequireAuth requiredRole="doctor"><Reports /></RequireAuth>} />
        <Route path="/doctor/visits" element={<RequireAuth requiredRole="doctor"><PatientVisits isDoctorView={true} /></RequireAuth>} />
        <Route path="/doctor/research" element={<RequireAuth requiredRole="doctor"><ResearchMode /></RequireAuth>} />
        
        {/* Patient Routes */}
        <Route path="/patient" element={<RequireAuth requiredRole="patient"><PatientDashboard /></RequireAuth>} />
        <Route path="/patient/visits" element={<RequireAuth requiredRole="patient"><PatientVisits isDoctorView={false} /></RequireAuth>} />
        <Route path="/patient/simulation" element={<RequireAuth requiredRole="patient"><Simulation /></RequireAuth>} />
        <Route path="/patient/book-appointment" element={<RequireAuth requiredRole="patient"><BookAppointment /></RequireAuth>} />
        <Route path="/patient/analysis" element={<RequireAuth requiredRole="patient"><AnalysisPortal /></RequireAuth>} />
        
        {/* Shared Routes */}
        <Route path="/profile" element={<RequireAuth><Profile /></RequireAuth>} />
      </Routes>
    </Router>
  );
}

export default App;

package com.classiiiai.app;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.classiiiai.app.data.AppDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdminFragment extends Fragment {

    private static final String TAG = "AdminFragment";
    private TextView tvTotalUsers, tvTotalPatients, tvTotalAnalyses, tvTotalAppointments;
    private ExecutorService executor;
    private FirebaseFirestore mFirestore;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        tvTotalUsers = view.findViewById(R.id.tvTotalUsers);
        tvTotalPatients = view.findViewById(R.id.tvTotalPatients);
        tvTotalAnalyses = view.findViewById(R.id.tvTotalAnalyses);
        tvTotalAppointments = view.findViewById(R.id.tvTotalAppointments);

        executor = Executors.newSingleThreadExecutor();
        mFirestore = FirebaseFirestore.getInstance();
        
        loadStats();
    }

    private com.google.firebase.firestore.ListenerRegistration usersListener;
    private com.google.firebase.firestore.ListenerRegistration patientsListener;
    private com.google.firebase.firestore.ListenerRegistration reportsListener;
    private com.google.firebase.firestore.ListenerRegistration apptsListener;

    private void loadStats() {
        // Fetch all counts directly from Firestore for real-time parity with Web App
        
        // Users
        usersListener = mFirestore.collection("users").addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                fallbackLoadUsers();
                return;
            }
            if (snapshot != null && getActivity() != null) {
                tvTotalUsers.setText(String.valueOf(snapshot.size()));
            }
        });
        
        // Patients
        patientsListener = mFirestore.collection("patients").addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                fallbackLoadPatients();
                return;
            }
            if (snapshot != null && getActivity() != null) {
                tvTotalPatients.setText(String.valueOf(snapshot.size()));
            }
        });
        
        // Reports
        reportsListener = mFirestore.collection("analysis_reports").addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                fallbackLoadReports();
                return;
            }
            if (snapshot != null && getActivity() != null) {
                tvTotalAnalyses.setText(String.valueOf(snapshot.size()));
            }
        });
        
        // Appointments
        apptsListener = mFirestore.collection("appointments").addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                fallbackLoadAppointments();
                return;
            }
            if (snapshot != null && getActivity() != null) {
                tvTotalAppointments.setText(String.valueOf(snapshot.size()));
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (usersListener != null) usersListener.remove();
        if (patientsListener != null) patientsListener.remove();
        if (reportsListener != null) reportsListener.remove();
        if (apptsListener != null) apptsListener.remove();
        if (executor != null) executor.shutdown();
    }

    private void fallbackLoadUsers() {
        executor.execute(() -> {
            int count = AppDatabase.getDatabase(requireContext()).userDao().getTotalUsers();
            if (getActivity() != null) getActivity().runOnUiThread(() -> tvTotalUsers.setText(String.valueOf(count)));
        });
    }
    
    private void fallbackLoadPatients() {
        executor.execute(() -> {
            int count = AppDatabase.getDatabase(requireContext()).patientRecordDao().getTotalPatients();
            if (getActivity() != null) getActivity().runOnUiThread(() -> tvTotalPatients.setText(String.valueOf(count)));
        });
    }
    
    private void fallbackLoadReports() {
        executor.execute(() -> {
            int count = AppDatabase.getDatabase(requireContext()).analysisReportDao().getTotalReports();
            if (getActivity() != null) getActivity().runOnUiThread(() -> tvTotalAnalyses.setText(String.valueOf(count)));
        });
    }
    
    private void fallbackLoadAppointments() {
        executor.execute(() -> {
            int count = AppDatabase.getDatabase(requireContext()).appointmentDao().getTotalAppointments();
            if (getActivity() != null) getActivity().runOnUiThread(() -> tvTotalAppointments.setText(String.valueOf(count)));
        });
    }
}

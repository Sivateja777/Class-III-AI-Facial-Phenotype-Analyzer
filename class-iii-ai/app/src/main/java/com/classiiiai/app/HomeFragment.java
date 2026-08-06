package com.classiiiai.app;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.classiiiai.app.data.AppDatabase;
import com.classiiiai.app.data.User;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private TextView tvStatPatients, tvStatReports, tvStatAppointments;
    private PieChart pieChart;
    private ExecutorService executor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvStatPatients = view.findViewById(R.id.tvStatPatients);
        tvStatReports = view.findViewById(R.id.tvStatReports);
        tvStatAppointments = view.findViewById(R.id.tvStatAppointments);
        pieChart = view.findViewById(R.id.pieChart);
        
        executor = Executors.newSingleThreadExecutor();

        view.findViewById(R.id.btnManagePatients).setOnClickListener(v -> 
            requireActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new PatientsFragment()).commit()
        );

        view.findViewById(R.id.btnResearchMode).setOnClickListener(v -> 
            requireActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new ResearchModeFragment()).addToBackStack(null).commit()
        );

        TextView btnLogout = view.findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                final android.content.Context ctx = getContext();
                if (ctx == null) return;
                executor.execute(() -> {
                    try {
                        AppDatabase.getDatabase(ctx).userDao().deleteAllUsers();
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(ctx, "Logged out successfully", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(ctx, RoleSelectionActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            });
        }

        loadDashboardStats();
    }

    private void setupPieChart(int class1, int class2, int class3) {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleRadius(61f);
        
        ArrayList<PieEntry> entries = new ArrayList<>();
        if (class1 == 0 && class2 == 0 && class3 == 0) {
            // Avoid empty chart crash
            entries.add(new PieEntry(1f, "No Data"));
        } else {
            if (class1 > 0) entries.add(new PieEntry(class1, "Class I"));
            if (class2 > 0) entries.add(new PieEntry(class2, "Class II"));
            if (class3 > 0) entries.add(new PieEntry(class3, "Class III"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Classifications");
        if (class1 == 0 && class2 == 0 && class3 == 0) {
             dataSet.setColors(Color.parseColor("#9CA3AF")); // Gray
        } else {
             dataSet.setColors(Color.parseColor("#3b82f6"), Color.parseColor("#f97316"), Color.parseColor("#ef4444"));
        }
        dataSet.setSliceSpace(3f);

        PieData data = new PieData(dataSet);
        data.setValueTextSize(10f);
        data.setValueTextColor(Color.WHITE);

        pieChart.setData(data);
        pieChart.invalidate(); // refresh
    }

    private com.google.firebase.firestore.ListenerRegistration patientListener;
    private com.google.firebase.firestore.ListenerRegistration apptListener;
    private com.google.firebase.firestore.ListenerRegistration reportListener;

    private void loadDashboardStats() {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(requireContext());
            User user = db.userDao().getLastLoggedInUser();
            
            if (user != null) {
                com.google.firebase.firestore.FirebaseFirestore firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance();
                
                // Real-time listener for patients
                patientListener = firestore.collection("patients")
                    .whereEqualTo("doctorEmail", user.email)
                    .addSnapshotListener((snapshot, e) -> {
                        if (e != null) return;
                        if (snapshot != null) {
                            int patientCount = snapshot.size();
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> tvStatPatients.setText(String.valueOf(patientCount)));
                            }
                        }
                    });

                // Real-time listener for appointments
                com.google.firebase.firestore.Query apptQuery = firestore.collection("appointments");
                if ("patient".equals(user.role)) {
                    apptQuery = apptQuery.whereEqualTo("patientEmail", user.email);
                } else {
                    apptQuery = apptQuery.whereEqualTo("doctorEmail", user.email);
                }
                apptListener = apptQuery.addSnapshotListener((snapshot, e) -> {
                    if (e != null) return;
                    if (snapshot != null) {
                        int apptCount = snapshot.size();
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> tvStatAppointments.setText(String.valueOf(apptCount)));
                        }
                    }
                });

                // Real-time listener for reports
                com.google.firebase.firestore.Query reportQuery = firestore.collection("analysis_reports");
                if ("patient".equals(user.role)) {
                    reportQuery = reportQuery.whereEqualTo("patientEmail", user.email);
                } else {
                    reportQuery = reportQuery.whereEqualTo("doctorEmail", user.email);
                }
                reportListener = reportQuery.addSnapshotListener((snapshot, e) -> {
                    if (e != null) return;
                    if (snapshot != null) {
                        int reportCount = snapshot.size();
                        int c1 = 0, c2 = 0, c3 = 0;
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                            String diagnosis = doc.getString("diagnosis");
                            if (diagnosis != null) {
                                if (diagnosis.contains("Class I")) c1++;
                                else if (diagnosis.contains("Class II")) c2++;
                                else if (diagnosis.contains("Class III")) c3++;
                            }
                        }
                        
                        final int finalC1 = c1, finalC2 = c2, finalC3 = c3;
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                tvStatReports.setText(String.valueOf(reportCount));
                                setupPieChart(finalC1, finalC2, finalC3);
                                
                                View llStatConfidence = requireView().findViewById(R.id.llStatConfidence);
                                if (reportCount == 0) {
                                    pieChart.setVisibility(View.GONE);
                                    if (llStatConfidence != null) llStatConfidence.setVisibility(View.GONE);
                                } else {
                                    pieChart.setVisibility(View.VISIBLE);
                                    if (llStatConfidence != null) llStatConfidence.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (patientListener != null) patientListener.remove();
        if (apptListener != null) apptListener.remove();
        if (reportListener != null) reportListener.remove();
        if (executor != null) executor.shutdown();
    }
}

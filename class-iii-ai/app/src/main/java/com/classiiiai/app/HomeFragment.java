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

    private void loadDashboardStats() {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(requireContext());
            User user = db.userDao().getLastLoggedInUser();
            
            if (user != null) {
                int patientCount = db.patientRecordDao().getPatientCountForDoctor(user.email);
                int apptCount = db.appointmentDao().getAppointmentsForUser(user.email).size();
                
                // Fetch from Firebase
                com.classiiiai.app.network.FirebaseApiService api = com.classiiiai.app.network.FirebaseClient.getClient().create(com.classiiiai.app.network.FirebaseApiService.class);
                api.getAllReports().enqueue(new retrofit2.Callback<java.util.Map<String, com.classiiiai.app.data.AnalysisReport>>() {
                    @Override
                    public void onResponse(retrofit2.Call<java.util.Map<String, com.classiiiai.app.data.AnalysisReport>> call, retrofit2.Response<java.util.Map<String, com.classiiiai.app.data.AnalysisReport>> response) {
                        java.util.List<com.classiiiai.app.data.AnalysisReport> reports = new ArrayList<>();
                        if (response.isSuccessful() && response.body() != null) {
                            for (com.classiiiai.app.data.AnalysisReport r : response.body().values()) {
                                if ("doctor".equals(user.role) || user.email.equals(r.patientEmail)) {
                                    reports.add(r);
                                }
                            }
                        }
                        
                        int reportCount = reports.size();
                        int c1 = 0, c2 = 0, c3 = 0;
                        for (com.classiiiai.app.data.AnalysisReport r : reports) {
                            if (r.diagnosis != null) {
                                if (r.diagnosis.contains("Class I")) c1++;
                                else if (r.diagnosis.contains("Class II")) c2++;
                                else if (r.diagnosis.contains("Class III")) c3++;
                            }
                        }
                        
                        final int finalC1 = c1, finalC2 = c2, finalC3 = c3;
                        
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                tvStatPatients.setText(String.valueOf(patientCount));
                                tvStatReports.setText(String.valueOf(reportCount));
                                tvStatAppointments.setText(String.valueOf(apptCount));
                                
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

                    @Override
                    public void onFailure(retrofit2.Call<java.util.Map<String, com.classiiiai.app.data.AnalysisReport>> call, Throwable t) {
                        // Fallback to local DB if offline
                        java.util.List<com.classiiiai.app.data.AnalysisReport> reports = "doctor".equals(user.role) ? 
                            db.analysisReportDao().getAllReports() : db.analysisReportDao().getReportsForPatient(user.email);
                        
                        int reportCount = reports.size();
                        int c1 = 0, c2 = 0, c3 = 0;
                        for (com.classiiiai.app.data.AnalysisReport r : reports) {
                            if (r.diagnosis != null) {
                                if (r.diagnosis.contains("Class I")) c1++;
                                else if (r.diagnosis.contains("Class II")) c2++;
                                else if (r.diagnosis.contains("Class III")) c3++;
                            }
                        }
                        
                        final int finalC1 = c1, finalC2 = c2, finalC3 = c3;
                        
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                tvStatPatients.setText(String.valueOf(patientCount));
                                tvStatReports.setText(String.valueOf(reportCount));
                                tvStatAppointments.setText(String.valueOf(apptCount));
                                setupPieChart(finalC1, finalC2, finalC3);
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
        if (executor != null) executor.shutdown();
    }
}

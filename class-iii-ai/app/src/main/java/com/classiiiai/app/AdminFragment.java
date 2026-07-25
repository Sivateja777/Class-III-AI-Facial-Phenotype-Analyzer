package com.classiiiai.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.classiiiai.app.data.AppDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdminFragment extends Fragment {

    private TextView tvTotalUsers, tvTotalPatients, tvTotalAnalyses, tvTotalAppointments;
    private ExecutorService executor;

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
        loadStats();
    }

    private void loadStats() {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(requireContext());
            
            com.classiiiai.app.network.FirebaseApiService api = com.classiiiai.app.network.FirebaseClient.getClient().create(com.classiiiai.app.network.FirebaseApiService.class);
            
            // Try to fetch users from Firebase
            api.getAllUsers().enqueue(new retrofit2.Callback<java.util.Map<String, com.classiiiai.app.data.User>>() {
                @Override
                public void onResponse(retrofit2.Call<java.util.Map<String, com.classiiiai.app.data.User>> call, retrofit2.Response<java.util.Map<String, com.classiiiai.app.data.User>> response) {
                    final int users = (response.isSuccessful() && response.body() != null) ? response.body().size() : db.userDao().getTotalUsers();
                    
                    api.getAllReports().enqueue(new retrofit2.Callback<java.util.Map<String, com.classiiiai.app.data.AnalysisReport>>() {
                        @Override
                        public void onResponse(retrofit2.Call<java.util.Map<String, com.classiiiai.app.data.AnalysisReport>> call, retrofit2.Response<java.util.Map<String, com.classiiiai.app.data.AnalysisReport>> response) {
                            final int reports = (response.isSuccessful() && response.body() != null) ? response.body().size() : db.analysisReportDao().getTotalReports();
                            
                            executor.execute(() -> {
                                int patients = db.patientRecordDao().getTotalPatients();
                                int appointments = db.appointmentDao().getTotalAppointments();
                                
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        tvTotalUsers.setText(String.valueOf(users));
                                        tvTotalPatients.setText(String.valueOf(patients));
                                        tvTotalAnalyses.setText(String.valueOf(reports));
                                        tvTotalAppointments.setText(String.valueOf(appointments));
                                    });
                                }
                            });
                        }
                        
                        @Override
                        public void onFailure(retrofit2.Call<java.util.Map<String, com.classiiiai.app.data.AnalysisReport>> call, Throwable t) {
                            fallbackLoad(db);
                        }
                    });
                }

                @Override
                public void onFailure(retrofit2.Call<java.util.Map<String, com.classiiiai.app.data.User>> call, Throwable t) {
                    fallbackLoad(db);
                }
            });
        });
    }

    private void fallbackLoad(AppDatabase db) {
        executor.execute(() -> {
            int users = db.userDao().getTotalUsers();
            int patients = db.patientRecordDao().getTotalPatients();
            int reports = db.analysisReportDao().getTotalReports();
            int appointments = db.appointmentDao().getTotalAppointments();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    tvTotalUsers.setText(String.valueOf(users));
                    tvTotalPatients.setText(String.valueOf(patients));
                    tvTotalAnalyses.setText(String.valueOf(reports));
                    tvTotalAppointments.setText(String.valueOf(appointments));
                });
            }
        });
    }
}

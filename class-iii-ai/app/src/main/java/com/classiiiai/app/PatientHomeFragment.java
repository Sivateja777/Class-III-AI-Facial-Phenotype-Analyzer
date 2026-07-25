package com.classiiiai.app;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.classiiiai.app.data.AppDatabase;
import com.classiiiai.app.data.Appointment;
import com.classiiiai.app.data.User;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PatientHomeFragment extends Fragment {

    private TextView tvPatientWelcome, tvNextVisit;
    private ExecutorService executor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvPatientWelcome = view.findViewById(R.id.tvPatientWelcome);
        tvNextVisit = view.findViewById(R.id.tvNextVisit);
        executor = Executors.newSingleThreadExecutor();

        View cardStartScan = view.findViewById(R.id.cardStartScan);
        View cardSimulator = view.findViewById(R.id.cardSimulator);
        View cardBookAppt = view.findViewById(R.id.cardBookAppt);
        
        if (cardBookAppt != null) {
            cardBookAppt.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), BookAppointmentActivity.class);
                startActivity(intent);
            });
        }

        if (cardStartScan != null) {
            cardStartScan.setOnClickListener(v -> {
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new AnalysisFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        if (cardSimulator != null) {
            cardSimulator.setOnClickListener(v -> {
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new SimulationFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }
        

        
        TextView btnLogout = view.findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                final android.content.Context ctx = getContext();
                if (ctx == null) return;
                executor.execute(() -> {
                    try {
                        com.classiiiai.app.data.AppDatabase.getDatabase(ctx).userDao().deleteAllUsers();
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

        loadPatientData();
    }
    


    private void loadPatientData() {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(requireContext());
            User user = db.userDao().getLastLoggedInUser();
            
            if (user != null) {
                List<Appointment> appointments = db.appointmentDao().getAppointmentsForUser(user.email);
                int reportCount = db.analysisReportDao().getReportsForPatient(user.email).size();
                
                requireActivity().runOnUiThread(() -> {
                    tvPatientWelcome.setText("Welcome " + user.displayName + ", let's track your progress.");
                    
                    if (appointments != null && !appointments.isEmpty()) {
                        Appointment nextAppt = appointments.get(0);
                        tvNextVisit.setText(nextAppt.dateTime);
                    } else {
                        tvNextVisit.setText("No upcoming visits");
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

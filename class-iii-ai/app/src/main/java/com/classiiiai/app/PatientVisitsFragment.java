package com.classiiiai.app;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.classiiiai.app.data.AppDatabase;
import com.classiiiai.app.data.Appointment;
import com.classiiiai.app.data.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PatientVisitsFragment extends Fragment {
    
    private RecyclerView rvAppointments;
    private View llEmptyState;
    private AppointmentAdapter adapter;
    private ExecutorService executor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_visits, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        rvAppointments = view.findViewById(R.id.rvAppointments);
        llEmptyState = view.findViewById(R.id.llEmptyState);
        FloatingActionButton fabBookAppointment = view.findViewById(R.id.fabBookAppointment);
        
        rvAppointments.setLayoutManager(new LinearLayoutManager(requireContext()));
        executor = Executors.newSingleThreadExecutor();
        
        fabBookAppointment.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(getActivity(), BookAppointmentActivity.class);
            startActivity(intent);
        });
        
        loadAppointments();
    }
    
    private void loadAppointments() {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(requireContext());
            User user = db.userDao().getLastLoggedInUser();
            if (user != null) {
                final String role = user.role != null ? user.role : "patient";
                List<Appointment> list = db.appointmentDao().getAppointmentsForUser(user.email);
                
                requireActivity().runOnUiThread(() -> {
                    FloatingActionButton fabBookAppointment = requireView().findViewById(R.id.fabBookAppointment);
                    if ("doctor".equals(role)) {
                        fabBookAppointment.setVisibility(View.GONE);
                    } else {
                        fabBookAppointment.setVisibility(View.VISIBLE);
                    }
                    
                    if (list == null || list.isEmpty()) {
                        rvAppointments.setVisibility(View.GONE);
                        llEmptyState.setVisibility(View.VISIBLE);
                    } else {
                        rvAppointments.setVisibility(View.VISIBLE);
                        llEmptyState.setVisibility(View.GONE);
                        if (adapter == null) {
                            adapter = new AppointmentAdapter(list, role);
                            rvAppointments.setAdapter(adapter);
                        } else {
                            adapter.updateData(list);
                        }
                    }
                });
            }
        });
    }
    

    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }
}

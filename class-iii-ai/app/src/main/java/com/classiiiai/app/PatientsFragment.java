package com.classiiiai.app;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.classiiiai.app.data.AppDatabase;
import com.classiiiai.app.data.PatientRecord;
import com.classiiiai.app.data.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PatientsFragment extends Fragment {

    private RecyclerView rvPatients;
    private TextView tvEmptyState;
    private FloatingActionButton fabAddPatient;
    private PatientAdapter adapter;
    private ExecutorService executor;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patients, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        rvPatients = view.findViewById(R.id.rvPatients);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        fabAddPatient = view.findViewById(R.id.fabAddPatient);

        rvPatients.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new PatientAdapter(new ArrayList<>(), this::deletePatient, this::viewPatientProfile);
        rvPatients.setAdapter(adapter);

        executor = Executors.newSingleThreadExecutor();

        loadPatients();

        fabAddPatient.setOnClickListener(v -> showAddPatientDialog());
    }

    private void loadPatients() {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(requireContext());
            currentUser = db.userDao().getLastLoggedInUser();
            
            if (currentUser != null) {
                List<PatientRecord> patients = db.patientRecordDao().getPatientsForDoctor(currentUser.email);
                requireActivity().runOnUiThread(() -> {
                    adapter.setPatients(patients);
                    tvEmptyState.setVisibility(patients.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }
        });
    }

    private void showAddPatientDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add New Patient");

        View viewInflated = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_patient, (ViewGroup) getView(), false);
        final EditText etName = viewInflated.findViewById(R.id.etPatientName);
        final EditText etEmail = viewInflated.findViewById(R.id.etPatientEmail);
        final EditText etAge = viewInflated.findViewById(R.id.etPatientAge);
        final EditText etGender = viewInflated.findViewById(R.id.etPatientGender);
        final EditText etEthnicity = viewInflated.findViewById(R.id.etPatientEthnicity);
        final EditText etGrowthStatus = viewInflated.findViewById(R.id.etGrowthStatus);
        final EditText etClinicalNotes = viewInflated.findViewById(R.id.etClinicalNotes);
        final EditText etCephValues = viewInflated.findViewById(R.id.etCephValues);

        builder.setView(viewInflated);

        builder.setPositiveButton("Save", (dialog, which) -> {
            dialog.dismiss();
            String name = etName.getText().toString().trim();
            String email = etEmail != null ? etEmail.getText().toString().trim() : "";
            String age = etAge.getText().toString().trim();
            String gender = etGender.getText().toString().trim();
            String ethnicity = etEthnicity.getText().toString().trim();
            String growth = etGrowthStatus.getText().toString().trim();
            String notes = etClinicalNotes.getText().toString().trim();
            String ceph = etCephValues.getText().toString().trim();
            
            if (!name.isEmpty() && !age.isEmpty() && currentUser != null) {
                PatientRecord newPatient = new PatientRecord(name, age, gender, ethnicity, growth, notes, ceph, currentUser.email, email, System.currentTimeMillis());
                executor.execute(() -> {
                    AppDatabase.getDatabase(requireContext()).patientRecordDao().insert(newPatient);
                    loadPatients();
                });
            } else {
                Toast.makeText(requireContext(), "Name and Age are required", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void deletePatient(PatientRecord patient) {
        executor.execute(() -> {
            AppDatabase.getDatabase(requireContext()).patientRecordDao().delete(patient);
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Patient deleted", Toast.LENGTH_SHORT).show();
                loadPatients();
            });
        });
    }

    private void viewPatientProfile(PatientRecord patient) {
        requireActivity().runOnUiThread(() -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, PatientProfileFragment.newInstance(patient.id))
                    .addToBackStack(null)
                    .commit();
        });
    }
}

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

    private com.google.firebase.firestore.ListenerRegistration patientsListener;

    private void loadPatients() {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(requireContext());
            currentUser = db.userDao().getLastLoggedInUser();
            
            if (currentUser != null) {
                // Fetch from Firestore
                patientsListener = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("patients")
                    .whereEqualTo("doctorEmail", currentUser.email)
                    .addSnapshotListener((snapshot, e) -> {
                        if (e != null) {
                            // Fallback to local
                            executor.execute(() -> {
                                List<PatientRecord> localPatients = db.patientRecordDao().getPatientsForDoctor(currentUser.email);
                                if (getActivity() != null) {
                                    requireActivity().runOnUiThread(() -> {
                                        adapter.setPatients(localPatients);
                                        tvEmptyState.setVisibility(localPatients.isEmpty() ? View.VISIBLE : View.GONE);
                                    });
                                }
                            });
                            return;
                        }
                        
                        if (snapshot != null) {
                            List<PatientRecord> patients = new ArrayList<>();
                            for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                                String name = doc.getString("name");
                                String age = doc.getString("age");
                                String gender = doc.getString("gender");
                                String ethnicity = doc.getString("ethnicity");
                                String growth = doc.getString("growthStatus");
                                String notes = doc.getString("clinicalNotes");
                                String ceph = doc.getString("cephValues");
                                String pEmail = doc.getString("patientEmail");
                                
                                if (name != null) {
                                    PatientRecord pr = new PatientRecord(name, age != null ? age : "", gender != null ? gender : "", ethnicity != null ? ethnicity : "", growth != null ? growth : "", notes != null ? notes : "", ceph != null ? ceph : "", currentUser.email, pEmail != null ? pEmail : "", System.currentTimeMillis());
                                    // Hack to map Firestore doc ID if needed, but for display this is fine
                                    patients.add(pr);
                                }
                            }
                            
                            if (getActivity() != null) {
                                requireActivity().runOnUiThread(() -> {
                                    adapter.setPatients(patients);
                                    tvEmptyState.setVisibility(patients.isEmpty() ? View.VISIBLE : View.GONE);
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
        if (patientsListener != null) patientsListener.remove();
        if (executor != null) executor.shutdown();
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
                
                // Save locally
                executor.execute(() -> {
                    AppDatabase.getDatabase(requireContext()).patientRecordDao().insert(newPatient);
                    
                    // Sync to Firestore
                    java.util.Map<String, Object> patientData = new java.util.HashMap<>();
                    patientData.put("name", name);
                    patientData.put("patientEmail", email);
                    patientData.put("age", age);
                    patientData.put("gender", gender);
                    patientData.put("ethnicity", ethnicity);
                    patientData.put("growthStatus", growth);
                    patientData.put("clinicalNotes", notes);
                    patientData.put("cephValues", ceph);
                    patientData.put("doctorEmail", currentUser.email);
                    patientData.put("timestamp", System.currentTimeMillis());
                    
                    com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("patients").add(patientData)
                        .addOnSuccessListener(docRef -> {
                            loadPatients();
                        })
                        .addOnFailureListener(e -> {
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Saved locally (Sync failed)", Toast.LENGTH_SHORT).show();
                            });
                            loadPatients();
                        });
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

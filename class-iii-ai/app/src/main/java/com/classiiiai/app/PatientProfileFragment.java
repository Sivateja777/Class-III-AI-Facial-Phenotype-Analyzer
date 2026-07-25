package com.classiiiai.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.classiiiai.app.data.AppDatabase;
import com.classiiiai.app.data.PatientRecord;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PatientProfileFragment extends Fragment {

    private static final String ARG_PATIENT_ID = "patient_id";
    private int patientId;
    private ExecutorService executor;

    public static PatientProfileFragment newInstance(int patientId) {
        PatientProfileFragment fragment = new PatientProfileFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PATIENT_ID, patientId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            patientId = getArguments().getInt(ARG_PATIENT_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView ivBack = view.findViewById(R.id.ivBack);
        TextView tvProfileName = view.findViewById(R.id.tvProfileName);
        TextView tvProfileBasicInfo = view.findViewById(R.id.tvProfileBasicInfo);
        TextView tvEthnicity = view.findViewById(R.id.tvEthnicity);
        TextView tvGrowth = view.findViewById(R.id.tvGrowth);
        TextView tvNotes = view.findViewById(R.id.tvNotes);
        TextView tvCephValues = view.findViewById(R.id.tvCephValues);

        ivBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(requireContext());
            PatientRecord patient = db.patientRecordDao().getPatientById(patientId);
            
            requireActivity().runOnUiThread(() -> {
                if (patient != null) {
                    tvProfileName.setText(patient.name);
                    tvProfileBasicInfo.setText(patient.age + " yrs • " + patient.gender);
                    
                    tvEthnicity.setText(patient.ethnicity != null && !patient.ethnicity.isEmpty() ? patient.ethnicity : "Not specified");
                    tvGrowth.setText(patient.growthStatus != null && !patient.growthStatus.isEmpty() ? patient.growthStatus : "Not specified");
                    tvNotes.setText(patient.clinicalNotes != null && !patient.clinicalNotes.isEmpty() ? patient.clinicalNotes : "No clinical notes provided.");
                    tvCephValues.setText(patient.cephValues != null && !patient.cephValues.isEmpty() ? patient.cephValues : "Not provided.");
                } else {
                    Toast.makeText(requireContext(), "Error loading patient details", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
            });
        });
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executor != null) executor.shutdown();
    }
}

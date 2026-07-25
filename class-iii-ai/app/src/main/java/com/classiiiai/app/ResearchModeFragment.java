package com.classiiiai.app;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.classiiiai.app.data.AppDatabase;
import com.classiiiai.app.data.PatientRecord;
import com.classiiiai.app.data.User;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResearchModeFragment extends Fragment {

    private TextView tvTotalCases;
    private ExecutorService executor;
    private List<PatientRecord> patientsList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_research_mode, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        tvTotalCases = view.findViewById(R.id.tvTotalCases);
        executor = Executors.newSingleThreadExecutor();
        
        Button btnExportCsv = view.findViewById(R.id.btnExportCsv);
        btnExportCsv.setOnClickListener(v -> exportCsv());
        
        view.findViewById(R.id.btnRunBatch).setOnClickListener(v -> 
            Toast.makeText(requireContext(), "Batch Analysis Started...", Toast.LENGTH_SHORT).show()
        );

        loadCases();
    }

    private void loadCases() {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(requireContext());
            User user = db.userDao().getLastLoggedInUser();
            if (user != null) {
                patientsList = db.patientRecordDao().getPatientsForDoctor(user.email);
                requireActivity().runOnUiThread(() -> {
                    tvTotalCases.setText(String.valueOf(patientsList.size()));
                });
            }
        });
    }

    private void exportCsv() {
        if (patientsList == null || patientsList.isEmpty()) {
            Toast.makeText(requireContext(), "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            StringBuilder csv = new StringBuilder();
            csv.append("Patient_ID,Age,Gender,Ethnicity,Growth_Status\n"); // Anonymized

            for (PatientRecord p : patientsList) {
                String anonymizedId = "PAT-" + p.id;
                csv.append(String.format("%s,%s,%s,%s,%s\n",
                        anonymizedId, p.age, p.gender, p.ethnicity, p.growthStatus));
            }

            saveToDownloads(csv.toString());
        });
    }

    private void saveToDownloads(String csvData) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, "class_iii_research_export_" + System.currentTimeMillis() + ".csv");
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            }

            Uri uri = requireContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                OutputStream out = requireContext().getContentResolver().openOutputStream(uri);
                if (out != null) {
                    out.write(csvData.getBytes(StandardCharsets.UTF_8));
                    out.close();
                    requireActivity().runOnUiThread(() -> 
                        Toast.makeText(requireContext(), "CSV Exported to Downloads", Toast.LENGTH_LONG).show()
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            requireActivity().runOnUiThread(() -> 
                Toast.makeText(requireContext(), "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executor != null) executor.shutdown();
    }
}

package com.classiiiai.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.classiiiai.app.data.AppDatabase;
import com.classiiiai.app.data.AnalysisReport;
import com.classiiiai.app.data.User;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PatientReportsFragment extends Fragment {

    private RecyclerView rvReports;
    private LinearLayout llEmptyState;
    private ReportAdapter adapter;
    private ExecutorService executor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // We reuse the fragment_reports layout since it is identical
        return inflater.inflate(R.layout.fragment_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        rvReports = view.findViewById(R.id.rvReports);
        llEmptyState = view.findViewById(R.id.llEmptyState);

        rvReports.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ReportAdapter(new ArrayList<>());
        rvReports.setAdapter(adapter);

        executor = Executors.newSingleThreadExecutor();
        loadReports();
    }

    private void loadReports() {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(requireContext());
            User user = db.userDao().getLastLoggedInUser();
            
            if (user != null) {
                List<AnalysisReport> reports = db.analysisReportDao().getReportsForPatient(user.email);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (reports.isEmpty()) {
                            llEmptyState.setVisibility(View.VISIBLE);
                            rvReports.setVisibility(View.GONE);
                        } else {
                            llEmptyState.setVisibility(View.GONE);
                            rvReports.setVisibility(View.VISIBLE);
                            adapter.setReports(reports);
                        }
                    });
                }
            }
        });
    }
}

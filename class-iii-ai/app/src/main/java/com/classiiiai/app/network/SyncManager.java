package com.classiiiai.app.network;

import android.content.Context;
import android.util.Log;
import com.classiiiai.app.data.AnalysisReport;
import com.classiiiai.app.data.AppDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.concurrent.Executors;

public class SyncManager {
    private static final String TAG = "SyncManager";

    public static void syncReportsFromFirebase(Context context) {
        FirebaseFirestore dbFirestore = FirebaseFirestore.getInstance();
        
        dbFirestore.collection("analysis_reports")
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        AppDatabase db = AppDatabase.getDatabase(context);
                        
                        // Clear existing to avoid duplicates, as we will fully sync from Web source of truth
                        db.analysisReportDao().deleteAll();
                        
                        int count = 0;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                String patientName = document.getString("patientName");
                                String patientId = document.getString("patientId"); // acts as email
                                String diagnosis = document.getString("diagnosis");
                                Double severityObj = document.getDouble("severityScore");
                                double severityScore = severityObj != null ? severityObj : 0.0;
                                String heatmapUrl = document.getString("heatmapUrl");
                                String reportUrl = document.getString("reportUrl");
                                
                                Long timestampObj = document.getLong("timestamp");
                                long timestamp = timestampObj != null ? timestampObj : System.currentTimeMillis();

                                if (patientName != null && diagnosis != null) {
                                    AnalysisReport report = new AnalysisReport(
                                        patientName, 
                                        patientId != null ? patientId : "", 
                                        "", // frontalImageUrl
                                        diagnosis, 
                                        severityScore, 
                                        heatmapUrl != null ? heatmapUrl : "", 
                                        reportUrl != null ? reportUrl : "", 
                                        timestamp
                                    );
                                    db.analysisReportDao().insert(report);
                                    count++;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error syncing web report to Room: " + e.getMessage());
                            }
                        }
                        Log.d(TAG, "Successfully synced " + count + " web reports to local Android database.");
                    });
                } else {
                    Log.e(TAG, "Firebase sync failed: ", task.getException());
                }
            });
    }
}

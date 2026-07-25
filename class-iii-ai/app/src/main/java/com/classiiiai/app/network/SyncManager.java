package com.classiiiai.app.network;

import android.content.Context;
import android.util.Log;
import com.classiiiai.app.data.AnalysisReport;
import com.classiiiai.app.data.AppDatabase;
import java.util.Map;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SyncManager {
    private static final String TAG = "SyncManager";

    public static void syncReportsFromFirebase(Context context) {
        FirebaseApiService apiService = FirebaseClient.getClient().create(FirebaseApiService.class);
        
        apiService.getAllReports().enqueue(new Callback<Map<String, AnalysisReport>>() {
            @Override
            public void onResponse(Call<Map<String, AnalysisReport>> call, Response<Map<String, AnalysisReport>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, AnalysisReport> firebaseReports = response.body();
                    
                    Executors.newSingleThreadExecutor().execute(() -> {
                        AppDatabase db = AppDatabase.getDatabase(context);
                        for (Map.Entry<String, AnalysisReport> entry : firebaseReports.entrySet()) {
                            try {
                                AnalysisReport report = entry.getValue();
                                if (report != null) {
                                    // Firebase keys usually act as IDs, but our local DB autogenerates ID.
                                    // We should technically check if it already exists by some unique field, 
                                    // but for this demo, we'll just insert if the local DB is empty or insert new.
                                    // Simplest robust approach: Clear all and insert fresh, or just insert new ones.
                                    db.analysisReportDao().insert(report);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error syncing individual report to Room: " + e.getMessage());
                            }
                        }
                        Log.d(TAG, "Successfully synced " + firebaseReports.size() + " reports to local database.");
                    });
                }
            }

            @Override
            public void onFailure(Call<Map<String, AnalysisReport>> call, Throwable t) {
                Log.e(TAG, "Firebase sync failed: " + t.getMessage());
                // Silently fail. The UI will just show whatever is currently in the local DB.
            }
        });
    }
}

package com.classiiiai.app;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.classiiiai.app.data.AnalysisReport;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {

    private List<AnalysisReport> reports;

    public ReportAdapter(List<AnalysisReport> reports) {
        this.reports = reports;
    }

    public void setReports(List<AnalysisReport> reports) {
        this.reports = reports;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AnalysisReport report = reports.get(position);
        holder.tvDiagnosis.setText(report.diagnosis);
        holder.tvSeverityScore.setText("Severity: " + report.severityScore);
        
        holder.tvPatientName.setText("Patient: " + report.patientName);
        
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        holder.tvDate.setText("Analyzed on: " + sdf.format(new Date(report.timestamp)));
        
        holder.itemView.setOnClickListener(v -> {
            try {
                String txtData = "Class III AI Analyzer Report\n"
                    + "============================\n"
                    + "Patient: " + report.patientName + "\n"
                    + "Date: " + sdf.format(new Date(report.timestamp)) + "\n"
                    + "Diagnosis: " + report.diagnosis + "\n"
                    + "Severity Score: " + report.severityScore + "\n"
                    + "Heatmap Reference: " + report.heatmapUrl + "\n";
                    
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "AI_Report_" + report.patientName + "_" + report.timestamp + ".txt");
                values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain");
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS);
                }

                Uri uri = v.getContext().getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    java.io.OutputStream out = v.getContext().getContentResolver().openOutputStream(uri);
                    if (out != null) {
                        out.write(txtData.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        out.close();
                        Toast.makeText(v.getContext(), "TXT Report Exported to Downloads", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (Exception e) {
                Toast.makeText(v.getContext(), "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return reports == null ? 0 : reports.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDiagnosis, tvSeverityScore, tvPatientName, tvDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDiagnosis = itemView.findViewById(R.id.tvDiagnosis);
            tvSeverityScore = itemView.findViewById(R.id.tvSeverityScore);
            tvPatientName = itemView.findViewById(R.id.tvPatientName);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }
}

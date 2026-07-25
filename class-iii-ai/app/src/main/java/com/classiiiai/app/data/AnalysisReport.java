package com.classiiiai.app.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "analysis_reports")
public class AnalysisReport {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String patientName;
    public String patientEmail;
    public String imagePath;
    public String diagnosis;
    public double severityScore;
    public String heatmapUrl;
    public String reportUrl;
    public long timestamp;

    public AnalysisReport(String patientName, String patientEmail, String imagePath, String diagnosis, double severityScore, String heatmapUrl, String reportUrl, long timestamp) {
        this.patientName = patientName;
        this.patientEmail = patientEmail;
        this.imagePath = imagePath;
        this.diagnosis = diagnosis;
        this.severityScore = severityScore;
        this.heatmapUrl = heatmapUrl;
        this.reportUrl = reportUrl;
        this.timestamp = timestamp;
    }
}

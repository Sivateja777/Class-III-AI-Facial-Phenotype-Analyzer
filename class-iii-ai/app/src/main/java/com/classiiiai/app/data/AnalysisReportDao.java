package com.classiiiai.app.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface AnalysisReportDao {
    @Insert
    void insert(AnalysisReport report);

    @Delete
    void delete(AnalysisReport report);

    @Query("SELECT * FROM analysis_reports WHERE patientEmail = :email ORDER BY severityScore DESC, timestamp DESC")
    List<AnalysisReport> getReportsForPatient(String email);
    
    @Query("SELECT COUNT(*) FROM analysis_reports")
    int getTotalReports();
    
    @Query("SELECT * FROM analysis_reports")
    List<AnalysisReport> getAllReports();
    
    @Query("DELETE FROM analysis_reports")
    void deleteAll();
}

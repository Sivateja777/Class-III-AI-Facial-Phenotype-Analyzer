package com.classiiiai.app.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface PatientRecordDao {
    @Insert
    void insert(PatientRecord record);

    @Update
    void update(PatientRecord record);

    @Delete
    void delete(PatientRecord record);

    @Query("SELECT * FROM patient_records WHERE doctorEmail = :doctorEmail ORDER BY timestamp DESC")
    List<PatientRecord> getPatientsForDoctor(String doctorEmail);
    
    @Query("SELECT COUNT(*) FROM patient_records")
    int getTotalPatients();

    @Query("SELECT * FROM patient_records WHERE id = :id LIMIT 1")
    PatientRecord getPatientById(int id);

    @Query("SELECT COUNT(*) FROM patient_records WHERE doctorEmail = :doctorEmail")
    int getPatientCountForDoctor(String doctorEmail);
}

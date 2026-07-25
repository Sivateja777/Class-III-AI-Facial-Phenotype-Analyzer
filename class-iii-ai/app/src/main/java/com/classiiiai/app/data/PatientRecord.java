package com.classiiiai.app.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "patient_records")
public class PatientRecord {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String name;
    public String age;
    public String gender;
    public String ethnicity;
    public String growthStatus;
    public String clinicalNotes;
    public String cephValues;
    public String doctorEmail; // To link to the doctor who added them
    public String patientEmail; // To link to the actual patient's account
    public long timestamp;

    public PatientRecord(String name, String age, String gender, String ethnicity, String growthStatus, String clinicalNotes, String cephValues, String doctorEmail, String patientEmail, long timestamp) {
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.ethnicity = ethnicity;
        this.growthStatus = growthStatus;
        this.clinicalNotes = clinicalNotes;
        this.cephValues = cephValues;
        this.doctorEmail = doctorEmail;
        this.patientEmail = patientEmail;
        this.timestamp = timestamp;
    }
}

package com.classiiiai.app.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "appointments")
public class Appointment {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String patientName;
    public String patientEmail;
    public int age;
    public String doctorEmail;
    public String dateTime;
    public String status; // Pending, Confirmed, Completed, Cancelled
    public String reason;
    public String clinicalHistory;

    public Appointment(String patientName, String patientEmail, int age, String doctorEmail, String dateTime, String status, String reason, String clinicalHistory) {
        this.patientName = patientName;
        this.patientEmail = patientEmail;
        this.age = age;
        this.doctorEmail = doctorEmail;
        this.dateTime = dateTime;
        this.status = status;
        this.reason = reason;
        this.clinicalHistory = clinicalHistory;
    }
}

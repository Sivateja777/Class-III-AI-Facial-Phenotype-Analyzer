package com.classiiiai.app.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "users")
public class User {
    @PrimaryKey
    @NonNull
    public String email;
    
    public String displayName;
    public String profilePictureUrl;
    public String role;
    public long loginTimestamp;
    
    public boolean isProfileComplete;
    public String phoneNumber;
    public String dateOfBirth;
    public String clinicName;
    public String medicalLicenseNumber;

    public User(@NonNull String email, String displayName, String profilePictureUrl, String role, long loginTimestamp) {
        this.email = email;
        this.displayName = displayName;
        this.profilePictureUrl = profilePictureUrl;
        this.role = role;
        this.loginTimestamp = loginTimestamp;
        this.isProfileComplete = false;
        this.phoneNumber = "";
        this.dateOfBirth = "";
        this.clinicName = "";
        this.medicalLicenseNumber = "";
    }
}

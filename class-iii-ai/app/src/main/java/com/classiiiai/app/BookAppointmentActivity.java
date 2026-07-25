package com.classiiiai.app;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.classiiiai.app.data.AppDatabase;
import com.classiiiai.app.data.Appointment;
import com.classiiiai.app.data.User;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class BookAppointmentActivity extends AppCompatActivity {

    private EditText etPatientName;
    private EditText etPatientAge;
    private EditText etChiefComplaint;
    private EditText etClinicalHistory;
    private Spinner spinnerDoctors;
    private AppDatabase db;
    private User currentUser;
    private List<User> doctorsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_appointment);

        db = AppDatabase.getDatabase(this);

        etPatientName = findViewById(R.id.etPatientName);
        etPatientAge = findViewById(R.id.etPatientAge);
        etChiefComplaint = findViewById(R.id.etChiefComplaint);
        etClinicalHistory = findViewById(R.id.etClinicalHistory);
        spinnerDoctors = findViewById(R.id.spinnerDoctors);
        Button btnSubmitRequest = findViewById(R.id.btnSubmitRequest);
        ImageView btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        loadInitialData();

        btnSubmitRequest.setOnClickListener(v -> submitAppointment());
    }

    private void loadInitialData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            currentUser = db.userDao().getLastLoggedInUser();
            
            // Get all doctors
            List<User> allUsers = db.userDao().getAllUsers();
            doctorsList = new ArrayList<>();
            List<String> doctorNames = new ArrayList<>();
            
            for (User u : allUsers) {
                if ("doctor".equals(u.role)) {
                    doctorsList.add(u);
                    doctorNames.add("Dr. " + u.displayName);
                }
            }

            runOnUiThread(() -> {
                if (currentUser != null && currentUser.displayName != null) {
                    etPatientName.setText(currentUser.displayName);
                }
                
                if (doctorNames.isEmpty()) {
                    doctorNames.add("No doctors available");
                    findViewById(R.id.btnSubmitRequest).setEnabled(false);
                }
                
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, doctorNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDoctors.setAdapter(adapter);
            });
        });
    }

    private void submitAppointment() {
        String name = etPatientName.getText().toString().trim();
        String ageStr = etPatientAge.getText().toString().trim();
        String complaint = etChiefComplaint.getText().toString().trim();
        String history = etClinicalHistory.getText().toString().trim();

        if (name.isEmpty() || ageStr.isEmpty() || complaint.isEmpty()) {
            Toast.makeText(this, "Please fill in Name, Age, and Chief Complaint", Toast.LENGTH_SHORT).show();
            return;
        }

        int age = Integer.parseInt(ageStr);
        int selectedDoctorIndex = spinnerDoctors.getSelectedItemPosition();
        
        if (doctorsList == null || doctorsList.isEmpty()) return;
        
        User selectedDoctor = doctorsList.get(selectedDoctorIndex);
        String patientEmail = (currentUser != null) ? currentUser.email : "guest@example.com";
        String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

        Executors.newSingleThreadExecutor().execute(() -> {
            Appointment appt = new Appointment(
                name,
                patientEmail,
                age,
                selectedDoctor.email,
                currentDate,
                "Pending",
                complaint,
                history
            );
            db.appointmentDao().insert(appt);

            runOnUiThread(() -> {
                Toast.makeText(BookAppointmentActivity.this, "Appointment Requested Successfully!", Toast.LENGTH_LONG).show();
                finish();
            });
        });
    }
}

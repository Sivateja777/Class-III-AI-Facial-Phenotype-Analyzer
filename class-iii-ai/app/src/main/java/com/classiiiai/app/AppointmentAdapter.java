package com.classiiiai.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.classiiiai.app.data.Appointment;
import java.util.List;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {
    private List<Appointment> appointments;
    private String userRole;

    public AppointmentAdapter(List<Appointment> appointments, String userRole) {
        this.appointments = appointments;
        this.userRole = userRole;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appointment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Appointment appt = appointments.get(position);
        holder.tvDateTime.setText(appt.dateTime);
        holder.tvStatus.setText(appt.status);
        
        if ("doctor".equals(userRole)) {
            holder.tvDoctorName.setText("Patient: " + (appt.patientEmail != null ? appt.patientEmail : "Unknown"));
        } else {
            holder.tvDoctorName.setText("With: " + (appt.doctorEmail != null ? appt.doctorEmail : "AI Clinic"));
        }
        
        holder.tvReason.setText("Reason: " + appt.reason);
    }

    @Override
    public int getItemCount() {
        return appointments != null ? appointments.size() : 0;
    }

    public void updateData(List<Appointment> newAppointments) {
        this.appointments = newAppointments;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateTime, tvStatus, tvDoctorName, tvReason;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDoctorName = itemView.findViewById(R.id.tvDoctorName);
            tvReason = itemView.findViewById(R.id.tvReason);
        }
    }
}

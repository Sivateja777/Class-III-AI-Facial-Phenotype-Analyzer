package com.classiiiai.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.classiiiai.app.data.PatientRecord;
import java.util.List;

public class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.ViewHolder> {

    private List<PatientRecord> patients;
    private OnDeleteClickListener deleteClickListener;
    private OnItemClickListener itemClickListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(PatientRecord patient);
    }
    
    public interface OnItemClickListener {
        void onItemClick(PatientRecord patient);
    }

    public PatientAdapter(List<PatientRecord> patients, OnDeleteClickListener deleteListener, OnItemClickListener itemListener) {
        this.patients = patients;
        this.deleteClickListener = deleteListener;
        this.itemClickListener = itemListener;
    }

    public void setPatients(List<PatientRecord> patients) {
        this.patients = patients;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_patient, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PatientRecord patient = patients.get(position);
        holder.tvName.setText(patient.name);
        holder.tvDetails.setText(patient.age + " yrs | " + patient.gender);
        
        if (patient.name != null && !patient.name.isEmpty()) {
            holder.tvInitial.setText(String.valueOf(patient.name.charAt(0)).toUpperCase());
        } else {
            holder.tvInitial.setText("?");
        }
        
        holder.ivDelete.setOnClickListener(v -> {
            if (deleteClickListener != null) {
                deleteClickListener.onDeleteClick(patient);
            }
        });
        
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(patient);
            }
        });
    }

    @Override
    public int getItemCount() {
        return patients == null ? 0 : patients.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvInitial, tvName, tvDetails;
        ImageView ivDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitial = itemView.findViewById(R.id.tvInitial);
            tvName = itemView.findViewById(R.id.tvName);
            tvDetails = itemView.findViewById(R.id.tvDetails);
            ivDelete = itemView.findViewById(R.id.ivDelete);
        }
    }
}

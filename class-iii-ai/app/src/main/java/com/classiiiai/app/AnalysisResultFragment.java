package com.classiiiai.app;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class AnalysisResultFragment extends Fragment {

    private String patientName;
    private String diagnosis;
    private double severityScore;
    private String imageUriString;
    
    private String featuresJson;
    private String heatmapUrl;
    private String severityCategory;
    private String reportUrl;
    
    // Default angle if not provided
    private double convexityAngle = 172.5; 

    public static AnalysisResultFragment newInstance(String patientName, String diagnosis, double severityScore, String severityCategory, String imageUriString, String featuresJson, String heatmapUrl, String reportUrl) {
        AnalysisResultFragment fragment = new AnalysisResultFragment();
        Bundle args = new Bundle();
        args.putString("patientName", patientName);
        args.putString("diagnosis", diagnosis);
        args.putDouble("severityScore", severityScore);
        args.putString("severityCategory", severityCategory);
        args.putString("imageUriString", imageUriString);
        args.putString("featuresJson", featuresJson);
        args.putString("heatmapUrl", heatmapUrl);
        args.putString("reportUrl", reportUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            patientName = getArguments().getString("patientName", "Unknown");
            diagnosis = getArguments().getString("diagnosis", "Class III");
            severityScore = getArguments().getDouble("severityScore", 0.0);
            severityCategory = getArguments().getString("severityCategory", "Unknown");
            imageUriString = getArguments().getString("imageUriString", "");
            featuresJson = getArguments().getString("featuresJson", "{}");
            heatmapUrl = getArguments().getString("heatmapUrl", "");
            reportUrl = getArguments().getString("reportUrl", "");
            
            // Mock angle calculation based on severity
            if (severityScore > 80) convexityAngle = 165.2; // severe
            else if (severityScore > 50) convexityAngle = 170.1; // moderate
            else convexityAngle = 175.8; // mild/normal
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analysis_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvPatientName = view.findViewById(R.id.tvResultPatientName);
        TextView tvDiagnosis = view.findViewById(R.id.tvDiagnosis);
        TextView tvSeverity = view.findViewById(R.id.tvSeverityScore);
        TextView tvAngle = view.findViewById(R.id.tvAngle);
        TextView tvAdvancedFeatures = view.findViewById(R.id.tvAdvancedFeatures);
        TextView tvClinicalReasoning = view.findViewById(R.id.tvClinicalReasoning);
        ImageView ivResultImage = view.findViewById(R.id.ivResultImage);
        ImageView ivHeatmap = view.findViewById(R.id.ivHeatmapOverlay);
        
        tvPatientName.setText("Patient: " + patientName);
        tvDiagnosis.setText(diagnosis);
        
        // Show severity category if available, otherwise just score
        if (severityCategory != null && !severityCategory.isEmpty()) {
            tvSeverity.setText(String.format("%.1f (%s)", severityScore, severityCategory));
        } else {
            tvSeverity.setText(String.format("%.1f", severityScore));
        }
        
        String aiRecommendation = null;
        // Parse advanced features
        try {
            org.json.JSONObject features = new org.json.JSONObject(featuresJson);
            if (features.length() > 0) {
                if (features.has("convexity")) {
                    convexityAngle = features.getDouble("convexity");
                }
                tvAngle.setText(String.format("%.1f°", convexityAngle));
                
                StringBuilder sb = new StringBuilder();
                if (features.has("mdi")) sb.append(String.format("Mandibular Dominance Index (MDI): %.2f\n", features.getDouble("mdi")));
                if (features.has("mds")) sb.append(String.format("Maxillary Deficiency Score (MDS): %.2f\n", features.getDouble("mds")));
                if (features.has("vps")) sb.append(String.format("Vertical Proportion Score (VPS): %.1f%%\n", features.getDouble("vps")));
                if (features.has("stps")) sb.append(String.format("Soft Tissue Projection Score: %.1f\n", features.getDouble("stps")));
                if (features.has("chin_prominence")) sb.append(String.format("Chin Prominence: %.1f mm\n", features.getDouble("chin_prominence")));
                if (features.has("e_line")) sb.append(String.format("E-line Deviation: %.1f mm\n", features.getDouble("e_line")));
                
                tvAdvancedFeatures.setText(sb.toString().trim());
                
                if (features.has("clinical_reasoning")) {
                    tvClinicalReasoning.setText(features.getString("clinical_reasoning"));
                } else {
                    tvClinicalReasoning.setText("No clinical reasoning provided by AI.");
                }
                
                if (features.has("treatment_recommendation")) {
                    aiRecommendation = features.getString("treatment_recommendation");
                }
            } else {
                tvAngle.setText(String.format("%.1f°", convexityAngle));
                tvAdvancedFeatures.setText("Advanced metrics unavailable (Local fallback mode)");
            }
        } catch (Exception e) {
            tvAngle.setText(String.format("%.1f°", convexityAngle));
            tvAdvancedFeatures.setText("Error parsing AI metrics.");
        }
        
        TextView tvRecommendation = view.findViewById(R.id.tvRecommendation);
        Button btnBookAppointment = view.findViewById(R.id.btnBookAppointment);
        
        if (aiRecommendation != null && !aiRecommendation.isEmpty()) {
            tvRecommendation.setText("AI Recommendation: " + aiRecommendation);
            if (diagnosis.toLowerCase().contains("class i") || diagnosis.toLowerCase().contains("normal") || severityScore < 40) {
                btnBookAppointment.setVisibility(View.GONE);
            } else {
                if ("Severe".equalsIgnoreCase(severityCategory) || severityScore >= 70.0) {
                    tvRecommendation.setTextColor(android.graphics.Color.RED);
                }
                btnBookAppointment.setVisibility(View.VISIBLE);
                btnBookAppointment.setOnClickListener(v -> {
                    android.widget.Toast.makeText(requireContext(), "Redirecting to Booking Portal...", android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        } else {
            if (diagnosis.toLowerCase().contains("class i") || diagnosis.toLowerCase().contains("normal") || severityScore < 40) {
                tvRecommendation.setText("Your facial profile analysis looks healthy! No doctor appointment is needed at this time.");
                btnBookAppointment.setVisibility(View.GONE);
            } else {
                if ("Severe".equalsIgnoreCase(severityCategory) || severityScore >= 70.0) {
                    tvRecommendation.setText("🚨 SURGICAL INTERVENTION HIGHLY LIKELY 🚨\n\nWe detected a severe malocclusion that requires immediate professional evaluation. Orthognathic surgery may be indicated.");
                    tvRecommendation.setTextColor(android.graphics.Color.RED);
                } else {
                    tvRecommendation.setText("We detected a potential malocclusion that requires professional evaluation. We highly recommend booking an appointment with an orthodontist.");
                }
                btnBookAppointment.setVisibility(View.VISIBLE);
                
                btnBookAppointment.setOnClickListener(v -> {
                    android.widget.Toast.makeText(requireContext(), "Redirecting to Booking Portal...", android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        }

        if (!imageUriString.isEmpty()) {
            ivResultImage.setImageURI(Uri.parse(imageUriString));
            
            if (heatmapUrl != null && heatmapUrl.startsWith("http")) {
                java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        java.io.InputStream in = new java.net.URL(heatmapUrl).openStream();
                        android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(in);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                ivHeatmap.setImageBitmap(bmp);
                                ivHeatmap.setAlpha(0.6f); // Semi-transparent overlay
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } else {
                ivHeatmap.setVisibility(View.GONE);
            }
        }

        view.findViewById(R.id.btnBackToDashboard).setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        Button btnDownloadPdf = view.findViewById(R.id.btnDownloadPdf);
        if (reportUrl != null && !reportUrl.isEmpty()) {
            btnDownloadPdf.setVisibility(View.VISIBLE);
            btnDownloadPdf.setOnClickListener(v -> {
                String fullUrl = reportUrl.startsWith("http") ? reportUrl : "http://192.168.137.57:8000" + reportUrl;
                try {
                    android.app.DownloadManager.Request request = new android.app.DownloadManager.Request(Uri.parse(fullUrl));
                    request.setTitle("Class III AI Analysis Report");
                    request.setDescription("Downloading clinical report PDF...");
                    request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, "Patient_Analysis_Report_" + System.currentTimeMillis() + ".pdf");
                    
                    android.app.DownloadManager manager = (android.app.DownloadManager) requireContext().getSystemService(android.content.Context.DOWNLOAD_SERVICE);
                    if (manager != null) {
                        manager.enqueue(request);
                        android.widget.Toast.makeText(requireContext(), "Downloading Report...", android.widget.Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    android.widget.Toast.makeText(requireContext(), "Failed to start download.", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            btnDownloadPdf.setVisibility(View.GONE);
        }
    }
}

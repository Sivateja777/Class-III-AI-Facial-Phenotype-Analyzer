package com.classiiiai.app;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.classiiiai.app.data.AppDatabase;
import com.classiiiai.app.data.AnalysisReport;
import com.classiiiai.app.data.PatientRecord;
import com.classiiiai.app.data.User;
import com.classiiiai.app.network.AnalysisApiService;
import com.classiiiai.app.network.AnalysisResponse;
import com.classiiiai.app.network.RetrofitClient;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AnalysisFragment extends Fragment {

    private PreviewView viewFinder;
    private ImageView ivPreview;
    private ImageView ivThumbFrontal, ivThumbLateral;
    private RadioGroup rgCaptureMode;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private int lensFacing = CameraSelector.LENS_FACING_FRONT;
    private Spinner spinnerPatients;
    private java.util.List<PatientRecord> patientList;
    private ImageView ivGhostOverlay;
    private FaceOverlayView faceOverlay;
    private FaceDetector faceDetector;
    
    private Button btnCapture, btnGallery, btnRetake, btnAnalyze;
    private LinearLayout llCaptureControls;
    private TextView tvCameraGuide;
    
    private ActivityResultLauncher<String> galleryLauncher;
    private Uri frontalUri = null;
    private Uri lateralUri = null;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(requireContext(), "Camera permission is required.", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analysis, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewFinder = view.findViewById(R.id.viewFinder);
        ivPreview = view.findViewById(R.id.ivPreview);
        ivThumbFrontal = view.findViewById(R.id.ivThumbFrontal);
        ivThumbLateral = view.findViewById(R.id.ivThumbLateral);
        rgCaptureMode = view.findViewById(R.id.rgCaptureMode);
        
        btnCapture = view.findViewById(R.id.btnCapture);
        btnGallery = view.findViewById(R.id.btnGallery);
        btnRetake = view.findViewById(R.id.btnRetake);
        btnAnalyze = view.findViewById(R.id.btnAnalyze);
        llCaptureControls = view.findViewById(R.id.llCaptureControls);
        tvCameraGuide = view.findViewById(R.id.tvCameraGuide);
        spinnerPatients = view.findViewById(R.id.spinnerPatients);
        ivGhostOverlay = view.findViewById(R.id.ivGhostOverlay);
        faceOverlay = view.findViewById(R.id.faceOverlay);
        
        Button btnFlipCamera = view.findViewById(R.id.btnFlipCamera);

        cameraExecutor = Executors.newSingleThreadExecutor();
        loadPatientsIntoSpinner();

        // Setup MLKit
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .build();
        faceDetector = FaceDetection.getClient(options);

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                previewSelectedImage(uri);
            }
        });

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        btnCapture.setOnClickListener(v -> takePhoto());
        btnGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        btnRetake.setOnClickListener(v -> resetToCamera());
        btnAnalyze.setOnClickListener(v -> performAiAnalysis());
        
        btnFlipCamera.setOnClickListener(v -> {
            lensFacing = lensFacing == CameraSelector.LENS_FACING_FRONT ?
                    CameraSelector.LENS_FACING_BACK : CameraSelector.LENS_FACING_FRONT;
            startCamera();
        });
        
        rgCaptureMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbFrontal) {
                tvCameraGuide.setText("Front Face Guide\\nCenter face • Neutral expression");
                if (frontalUri != null) {
                    showPreview(frontalUri);
                } else {
                    resetToCamera();
                }
            } else {
                tvCameraGuide.setText("Lateral Profile Guide\\nRight side • Neutral expression");
                if (lateralUri != null) {
                    showPreview(lateralUri);
                } else {
                    resetToCamera();
                }
            }
        });
    }
    
    private void previewSelectedImage(Uri uri) {
        if (rgCaptureMode.getCheckedRadioButtonId() == R.id.rbFrontal) {
            frontalUri = uri;
            ivThumbFrontal.setImageURI(uri);
        } else {
            lateralUri = uri;
            ivThumbLateral.setImageURI(uri);
        }
        showPreview(uri);
        checkReadyForAnalysis();
    }
    
    private void showPreview(Uri uri) {
        ivPreview.setImageURI(uri);
        viewFinder.setVisibility(View.GONE);
        llCaptureControls.setVisibility(View.GONE);
        tvCameraGuide.setVisibility(View.GONE);
        btnGallery.setVisibility(View.GONE);
        ivPreview.setVisibility(View.VISIBLE);
    }
    
    private void checkReadyForAnalysis() {
        if (frontalUri != null && lateralUri != null) {
            btnAnalyze.setEnabled(true);
            btnAnalyze.setAlpha(1.0f);
        } else {
            btnAnalyze.setEnabled(false);
            btnAnalyze.setAlpha(0.5f);
        }
    }
    
    private void resetToCamera() {
        if (rgCaptureMode.getCheckedRadioButtonId() == R.id.rbFrontal) {
            frontalUri = null;
            ivThumbFrontal.setImageDrawable(null);
        } else {
            lateralUri = null;
            ivThumbLateral.setImageDrawable(null);
        }
        
        ivPreview.setVisibility(View.GONE);
        
        viewFinder.setVisibility(View.VISIBLE);
        llCaptureControls.setVisibility(View.VISIBLE);
        tvCameraGuide.setVisibility(View.VISIBLE);
        btnGallery.setVisibility(View.VISIBLE);
        
        checkReadyForAnalysis();
    }

    private void performAiAnalysis() {
        if (frontalUri == null || lateralUri == null) return;
        
        Toast.makeText(requireContext(), "Uploading Dual Scans & Analyzing...", Toast.LENGTH_SHORT).show();
        
        MultipartBody.Part frontalPart = prepareFilePart("frontal", frontalUri);
        MultipartBody.Part lateralPart = prepareFilePart("lateral", lateralUri);
        
        if (frontalPart == null || lateralPart == null) {
            showErrorDialog("Failed to process images for upload.");
            return;
        }
        AnalysisApiService service = RetrofitClient.getClient("http://10.0.2.2:8000/").create(AnalysisApiService.class);
        Call<AnalysisResponse> call = service.analyzeImage(frontalPart, lateralPart);
        
        call.enqueue(new Callback<AnalysisResponse>() {
            @Override
            public void onResponse(Call<AnalysisResponse> call, Response<AnalysisResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AnalysisResponse res = response.body();
                    
                    if (res.error != null && !res.error.isEmpty()) {
                        showErrorDialog(res.error);
                        return;
                    }
                    
                    
                    String featuresJson = "";
                    if (res.features != null) {
                        featuresJson = new com.google.gson.Gson().toJson(res.features);
                    }
                    final String finalFeaturesJson = featuresJson;
                    
                    cameraExecutor.execute(() -> {
                        saveAndShowResults(res.diagnosis, res.severityScore, res.severityCategory, res.heatmapUrl, res.reportUrl, finalFeaturesJson);
                    });
                } else {
                    performLocalAnalysisFallback();
                }
            }

            @Override
            public void onFailure(Call<AnalysisResponse> call, Throwable t) {
                performLocalAnalysisFallback();
            }
        });
    }

    private void performLocalAnalysisFallback() {
        requireActivity().runOnUiThread(() -> {
            Toast.makeText(requireContext(), "Backend offline. Using Local AI Engine...", Toast.LENGTH_SHORT).show();
        });

        try {
            InputImage image = InputImage.fromFilePath(requireContext(), lateralUri);
            faceDetector.process(image)
                .addOnSuccessListener(faces -> {
                    if (!faces.isEmpty()) {
                        Face face = faces.get(0);
                        
                        // Default Fallback
                        double severityScore = 30.0;
                        String diagnosis = "Class I - Normal";
                        
                        // Attempt to extract true geometric contours
                        if (face.getContour(com.google.mlkit.vision.face.FaceContour.NOSE_BRIDGE) != null &&
                            face.getContour(com.google.mlkit.vision.face.FaceContour.UPPER_LIP_TOP) != null &&
                            face.getContour(com.google.mlkit.vision.face.FaceContour.FACE) != null) {
                            
                            java.util.List<android.graphics.PointF> nose = face.getContour(com.google.mlkit.vision.face.FaceContour.NOSE_BRIDGE).getPoints();
                            java.util.List<android.graphics.PointF> lip = face.getContour(com.google.mlkit.vision.face.FaceContour.UPPER_LIP_TOP).getPoints();
                            java.util.List<android.graphics.PointF> chin = face.getContour(com.google.mlkit.vision.face.FaceContour.FACE).getPoints();
                            
                            if (!nose.isEmpty() && !lip.isEmpty() && !chin.isEmpty()) {
                                android.graphics.PointF p1 = nose.get(0);
                                android.graphics.PointF p2 = lip.get(0);
                                android.graphics.PointF p3 = chin.get(chin.size() / 2); // Bottom of chin
                                
                                // Calculate Angle
                                double angle = calculateAngle(p1, p2, p3);
                                
                                // Map Angle to Severity
                                if (angle > 175) {
                                    diagnosis = "Class III - Maxillary Deficiency";
                                    severityScore = 75.0 + ((angle - 175) * 2);
                                } else if (angle < 165) {
                                    diagnosis = "Class II - Mandibular Retrognathia";
                                    severityScore = 60.0 + ((165 - angle) * 2);
                                } else {
                                    diagnosis = "Class I - Normal";
                                    severityScore = 15.0;
                                }
                                
                                // Cap severity
                                severityScore = Math.min(100.0, Math.max(0.0, severityScore));
                            }
                        }
                        
                        final String finalDiagnosis = diagnosis;
                        final double finalSeverityScore = severityScore;
                        final String finalFeaturesJson = "{}"; // Local fallback doesn't have advanced ML features yet
                        
                        cameraExecutor.execute(() -> {
                            saveAndShowResults(finalDiagnosis, finalSeverityScore, finalSeverityScore > 50 ? "Severe" : "Mild", "local_fallback", "", finalFeaturesJson);
                        });
                    } else {
                        showErrorDialog("Local AI: No face detected in lateral scan.");
                    }
                })
                .addOnFailureListener(e -> showErrorDialog("Local AI failed: " + e.getMessage()));
        } catch (Exception e) {
            showErrorDialog("Local AI error: " + e.getMessage());
        }
    }
    
    private double calculateAngle(android.graphics.PointF p1, android.graphics.PointF p2, android.graphics.PointF p3) {
        double a = Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2);
        double b = Math.pow(p2.x - p3.x, 2) + Math.pow(p2.y - p3.y, 2);
        double c = Math.pow(p3.x - p1.x, 2) + Math.pow(p3.y - p1.y, 2);
        double radians = Math.acos((a + b - c) / Math.sqrt(4 * a * b));
        return Math.toDegrees(radians);
    }

    private void saveAndShowResults(String diagnosis, double severityScore, String severityCategory, String heatmapUrl, String reportUrl, String featuresJson) {
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        User user = db.userDao().getLastLoggedInUser();
        
        String selectedPatientName = "Unknown Patient";
        String targetPatientEmail = user != null ? user.email : "";

        if (user != null) {
            if ("doctor".equals(user.role) && spinnerPatients.getSelectedItemPosition() > 0 && patientList != null) {
                PatientRecord selectedPatient = patientList.get(spinnerPatients.getSelectedItemPosition() - 1);
                selectedPatientName = selectedPatient.name;
                // Important fix: Save under the patient's actual email if they have one
                if (selectedPatient.patientEmail != null && !selectedPatient.patientEmail.isEmpty()) {
                    targetPatientEmail = selectedPatient.patientEmail;
                }
            } else {
                selectedPatientName = user.displayName;
            }

            AnalysisReport report = new AnalysisReport(selectedPatientName, targetPatientEmail, frontalUri.toString(), diagnosis, severityScore, heatmapUrl, reportUrl, System.currentTimeMillis());
            db.analysisReportDao().insert(report);
            
            // Firebase Firestore sync for Web App parity
            com.google.firebase.firestore.FirebaseFirestore dbFirestore = com.google.firebase.firestore.FirebaseFirestore.getInstance();
            java.util.Map<String, Object> firestoreReport = new java.util.HashMap<>();
            firestoreReport.put("patientId", targetPatientEmail);
            firestoreReport.put("patientName", selectedPatientName);
            firestoreReport.put("diagnosis", diagnosis);
            firestoreReport.put("severityScore", severityScore);
            firestoreReport.put("timestamp", System.currentTimeMillis());
            if (heatmapUrl != null) firestoreReport.put("heatmapUrl", heatmapUrl);
            if (reportUrl != null) firestoreReport.put("reportUrl", reportUrl);

            dbFirestore.collection("analysis_reports").add(firestoreReport)
                .addOnSuccessListener(documentReference -> android.util.Log.d("Sync", "Successfully synced to Firestore for Web App"))
                .addOnFailureListener(e -> android.util.Log.e("Sync", "Failed to sync to Firestore: ", e));
        }
        
        String finalPatientName = selectedPatientName;
        requireActivity().runOnUiThread(() -> {
            // Launch Premium Result Dashboard
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, AnalysisResultFragment.newInstance(finalPatientName, diagnosis, severityScore, severityCategory, frontalUri.toString(), featuresJson, heatmapUrl, reportUrl))
                    .addToBackStack(null)
                    .commit();
                    
            // Reset camera state
            frontalUri = null;
            lateralUri = null;
            ivThumbFrontal.setImageDrawable(null);
            ivThumbLateral.setImageDrawable(null);
            rgCaptureMode.check(R.id.rbFrontal);
            resetToCamera(); 
        });
    }
    
    private MultipartBody.Part prepareFilePart(String partName, Uri fileUri) {
        try {
            java.io.InputStream is = requireContext().getContentResolver().openInputStream(fileUri);
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            byte[] bytes = buffer.toByteArray();
            is.close();
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), bytes);
            return MultipartBody.Part.createFormData(partName, "image.jpg", requestFile);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private void showErrorDialog(String errorMsg) {
        requireActivity().runOnUiThread(() -> {
            new AlertDialog.Builder(requireContext())
                .setTitle("Analysis Failed")
                .setMessage(errorMsg + "\\n\\nPlease retake the photo ensuring good lighting and a clear view.")
                .setPositiveButton("Retry", (dialog, which) -> dialog.dismiss())
                .show();
        });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();
                
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
                    @Override
                    @androidx.annotation.OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
                    public void analyze(@NonNull ImageProxy imageProxy) {
                        android.media.Image mediaImage = imageProxy.getImage();
                        if (mediaImage != null) {
                            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                            faceDetector.process(image)
                                    .addOnSuccessListener(faces -> {
                                        if (!faces.isEmpty()) {
                                            faceOverlay.setFace(faces.get(0), imageProxy.getWidth(), imageProxy.getHeight());
                                        } else {
                                            faceOverlay.setFace(null, 0, 0);
                                        }
                                    })
                                    .addOnCompleteListener(task -> imageProxy.close());
                        } else {
                            imageProxy.close();
                        }
                    }
                });

                androidx.camera.core.CameraSelector cameraSelector = new androidx.camera.core.CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(requireContext(), "Failed to start camera.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        File photoFile = new File(requireContext().getExternalFilesDir(null),
                new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg");

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Uri savedUri = Uri.fromFile(photoFile);
                        requireActivity().runOnUiThread(() -> previewSelectedImage(savedUri));
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(requireContext(), "Photo capture failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadPatientsIntoSpinner() {
        cameraExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(requireContext());
            User user = db.userDao().getLastLoggedInUser();
            if (user != null) {
                patientList = db.patientRecordDao().getPatientsForDoctor(user.email);
                java.util.List<String> patientNames = new java.util.ArrayList<>();
                patientNames.add("No Patient Selected"); // Default option
                
                for (PatientRecord p : patientList) {
                    patientNames.add(p.name + " (" + p.age + "y)");
                }
                
                requireActivity().runOnUiThread(() -> {
                    android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, patientNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerPatients.setAdapter(adapter);
                    
                    spinnerPatients.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                            if (position > 0) {
                                PatientRecord selected = patientList.get(position - 1);
                                loadGhostImage(selected.name);
                            } else {
                                ivGhostOverlay.setVisibility(View.GONE);
                            }
                        }

                        @Override
                        public void onNothingSelected(android.widget.AdapterView<?> parent) {
                        }
                    });
                });
            }
        });
    }

    private void loadGhostImage(String patientName) {
        cameraExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(requireContext());
            java.util.List<AnalysisReport> reports = db.analysisReportDao().getAllReports();
            
            AnalysisReport lastReport = null;
            for (AnalysisReport r : reports) {
                if (patientName.equals(r.patientName)) {
                    if (lastReport == null || r.timestamp > lastReport.timestamp) {
                        lastReport = r;
                    }
                }
            }
            
            final AnalysisReport finalReport = lastReport;
            requireActivity().runOnUiThread(() -> {
                if (finalReport != null && finalReport.imagePath != null) {
                    ivGhostOverlay.setImageURI(Uri.parse(finalReport.imagePath));
                    ivGhostOverlay.setVisibility(View.VISIBLE);
                    Toast.makeText(requireContext(), "Ghost Camera enabled for longitudinal tracking", Toast.LENGTH_SHORT).show();
                } else {
                    ivGhostOverlay.setVisibility(View.GONE);
                }
            });
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}

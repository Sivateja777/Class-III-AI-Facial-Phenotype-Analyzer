from fastapi import FastAPI, UploadFile, File, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
import cv2
import numpy as np
import mediapipe as mp
import os
import time
import json
import google.generativeai as genai
from PIL import Image
import io

from ml_pipeline import MLPipeline
from report_generator import ReportGenerator

# Configure Gemini
genai.configure(api_key="YOUR_GEMINI_API_KEY_HERE")
generation_config = {
    "temperature": 0.1,
    "response_mime_type": "application/json",
}
vision_model = genai.GenerativeModel("gemini-1.5-pro", generation_config=generation_config)

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Mount a static directory to serve heatmap images and pdfs
os.makedirs("static/heatmaps", exist_ok=True)
os.makedirs("static/reports", exist_ok=True)
app.mount("/static", StaticFiles(directory="static"), name="static")

try:
    mp_face_mesh = mp.solutions.face_mesh
    face_mesh = mp_face_mesh.FaceMesh(static_image_mode=True, max_num_faces=1, refine_landmarks=True)
except AttributeError:
    mp_face_mesh = None
    face_mesh = None

# Initialize Core AI Components
pipeline = MLPipeline("model.pkl")
report_gen = ReportGenerator(output_dir="static/reports")

@app.post("/analyze")
async def analyze_image(request: Request, frontal: UploadFile = File(...), lateral: UploadFile = File(...)):
    # Read both files
    frontal_contents = await frontal.read()
    lateral_contents = await lateral.read()
    
    frontal_nparr = np.frombuffer(frontal_contents, np.uint8)
    lateral_nparr = np.frombuffer(lateral_contents, np.uint8)
    
    frontal_img = cv2.imdecode(frontal_nparr, cv2.IMREAD_COLOR)
    lateral_img = cv2.imdecode(lateral_nparr, cv2.IMREAD_COLOR)
    
    if frontal_img is None or lateral_img is None:
        return {"error": "Invalid image files provided."}

    # Image Preprocessing (Blur Detection, BG Removal, Normalize, Resize)
    frontal_norm, front_blurry, _ = pipeline.preprocess_image(frontal_img)
    lateral_norm, lat_blurry, _ = pipeline.preprocess_image(lateral_img)
    
    if front_blurry or lat_blurry:
        return {"error": "One or both images are too blurry (Failed Laplacian Variance Test). Please retake."}
    
    # Save original (we will use lateral as the primary visual for the report)
    timestamp = int(time.time())
    original_path = f"static/heatmaps/orig_{timestamp}.jpg"
    cv2.imwrite(original_path, lateral_norm)

    # Convert to RGB for MediaPipe
    frontal_rgb = cv2.cvtColor(frontal_norm, cv2.COLOR_BGR2RGB)
    lateral_rgb = cv2.cvtColor(lateral_norm, cv2.COLOR_BGR2RGB)
    
    if face_mesh is None:
        return {"error": "MediaPipe Face Mesh failed to initialize."}

    # Process lateral image for geometric features (E-line, Convexity, etc)
    results_lateral = face_mesh.process(lateral_rgb)
    
    if not results_lateral.multi_face_landmarks:
        return {"error": "No face detected in the lateral image."}
        
    landmarks = results_lateral.multi_face_landmarks[0].landmark
    h, w, _ = lateral_img.shape
    
    # 1. Feature Engineering (using lateral landmarks)
    features = pipeline.extract_geometric_features(landmarks, w, h)
    
    # 2. ML Classification & Severity Scoring using Gemini Pro Vision
    # HYBRID ENSEMBLE METHOD: Inject deterministic MediaPipe mathematical features into the LLM prompt.
    prompt = f"""
    You are an expert orthodontist. Analyze these two patient images (Frontal and Lateral profiles).
    Determine if the patient's skeletal profile is Class I (Normal), Class II (Retrognathic Mandible/Overbite), 
    or Class III (Prognathic Mandible/Maxillary Deficiency/Underbite).
    
    IMPORTANT CLINICAL MEASUREMENTS:
    Our local Computer Vision model (MediaPipe) has already detected the facial landmarks and calculated 
    the following deterministic mathematical features for this patient. You MUST use these precise numbers 
    to guide your diagnosis instead of just eyeballing the image:
    - Convexity Depth (Negative means concave profile): {features.get('convexity_depth', 0):.2f}
    - Wits Proxy (Maxilla vs Mandible protrusion): {features.get('wits_proxy', 0):.2f}
    - Lower Face Height Ratio: {features.get('lfhr', 0):.2f}
    - E-Line Deviation: {features.get('e_line_dev', 0):.2f}
    
    Return a strict JSON format exactly like this:
    {{
      "diagnosis": "Class I - Normal" | "Class II - Mandibular Retrognathia" | "Class III - Maxillary Deficiency/Mandibular Prognathia",
      "severityScore": <float between 0 and 100>,
      "severityCategory": "Mild" | "Moderate" | "Severe",
      "clinical_reasoning": "<detailed clinical reasoning incorporating the exact measurements provided above>",
      "treatment_recommendation": "<a personalized AI recommendation for treatment based on the diagnosis and severity>"
    }}
    """
    
    try:
        img1 = Image.open(io.BytesIO(frontal_contents))
        img2 = Image.open(io.BytesIO(lateral_contents))
        response = vision_model.generate_content([prompt, img1, img2])
        result = json.loads(response.text)
        
        diagnosis = result.get("diagnosis", "Class I - Normal")
        severity_score = float(result.get("severityScore", 15.0))
        severity_category = result.get("severityCategory", "Mild")
        confidence = 99.0
        
        # Inject the LLM clinical reasoning and recommendation into the features payload for the frontend
        features["clinical_reasoning"] = result.get("clinical_reasoning", "")
        features["treatment_recommendation"] = result.get("treatment_recommendation", "")
        
    except Exception as e:
        print("Gemini API Error:", e)
        # Fallback to local Random Forest model if API fails or parsing fails
        diagnosis, severity_score, confidence, severity_category = pipeline.classify_and_score(features)
        features["treatment_recommendation"] = "We detected a potential malocclusion that requires professional evaluation. We highly recommend booking an appointment with an orthodontist."
    
    # 3. Explainable AI (Heatmap on lateral profile)
    heatmap_overlay = pipeline.generate_explainable_heatmap(lateral_norm, landmarks, features)
    heatmap_filename = f"heatmap_{timestamp}.jpg"
    heatmap_path = f"static/heatmaps/{heatmap_filename}"
    cv2.imwrite(heatmap_path, heatmap_overlay)
    
    # 4. Report Generation
    # Mocking patient ID, in a real app pass it from Android
    pdf_filename = report_gen.generate_pdf(
        patient_id="PATIENT_123",
        diagnosis=diagnosis,
        confidence=confidence,
        severity=severity_score,
        features=features,
        original_img_path=original_path,
        heatmap_img_path=heatmap_path
    )
    
    base_url = f"{request.url.scheme}://{request.url.netloc}"
    
    return {
        "severityScore": severity_score,
        "severityCategory": severity_category,
        "diagnosis": diagnosis,
        "confidence": confidence,
        "heatmapUrl": f"{base_url}/static/heatmaps/{heatmap_filename}",
        "reportUrl": f"{base_url}/static/reports/{pdf_filename}",
        "features": features
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)

import numpy as np
import cv2
import pickle
import os
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, confusion_matrix, roc_auc_score
from sklearn.model_selection import train_test_split
import mediapipe as mp

class MLPipeline:
    def __init__(self, model_path="model.pkl"):
        self.model_path = model_path
        self.model = None
        
        self.class_mapping = {
            1: "Class III - Maxillary Deficiency",
            2: "Class III - Mandibular Prognathism",
            3: "Combined Skeletal Class III",
            4: "Borderline / Pseudo Class III"
        }
        
        try:
            self.mp_selfie = mp.solutions.selfie_segmentation.SelfieSegmentation(model_selection=1)
        except Exception:
            self.mp_selfie = None
            
        try:
            self.mp_face_mesh = mp.solutions.face_mesh.FaceMesh(
                static_image_mode=True, max_num_faces=1, refine_landmarks=True)
        except Exception:
            self.mp_face_mesh = None

        self._load_or_train_model()
        
    def _load_or_train_model(self):
        """Loads the Random Forest model if exists, else trains a synthetic one."""
        if os.path.exists(self.model_path):
            try:
                with open(self.model_path, 'rb') as f:
                    self.model = pickle.load(f)
            except Exception:
                self._train_synthetic_model()
        else:
            self._train_synthetic_model()
            
    def _train_synthetic_model(self):
        """Generates synthetic baseline data for the 4 classes and trains RF."""
        data = []
        labels = []
        
        columns = ["convexity_depth", "wits_proxy", "maxilla_prominence", "mandible_prominence", "lfhr", "e_line_dev"]
        
        for _ in range(400):
            # 1: Class III - Maxillary Deficiency 
            # (Concave profile, Maxilla retruded, Mandible normal)
            data.append([
                np.random.normal(-5, 2), # convexity_depth
                np.random.normal(-6, 2), # wits_proxy
                np.random.normal(-10, 3),# maxilla_prominence (retruded)
                np.random.normal(5, 3),  # mandible_prominence (normal)
                np.random.normal(0.55, 0.02), # lfhr
                np.random.normal(2, 1)   # e_line_dev
            ])
            labels.append(1)
            
            # 2: Class III - Mandibular Prognathism
            # (Concave profile, Maxilla normal, Mandible protruded)
            data.append([
                np.random.normal(-8, 2),
                np.random.normal(-9, 2),
                np.random.normal(5, 3),  # maxilla normal
                np.random.normal(20, 3), # mandible protruded
                np.random.normal(0.58, 0.02),
                np.random.normal(-2, 1)
            ])
            labels.append(2)
            
            # 3: Combined Skeletal Class III
            # (Severe concave, Maxilla retruded, Mandible protruded)
            data.append([
                np.random.normal(-12, 3),
                np.random.normal(-15, 3),
                np.random.normal(-8, 3),  # maxilla retruded
                np.random.normal(18, 3),  # mandible protruded
                np.random.normal(0.57, 0.02),
                np.random.normal(-3, 1)
            ])
            labels.append(3)
            
            # 4: Borderline / Pseudo Class III
            # (Straight to slightly concave, wits near 0)
            data.append([
                np.random.normal(0, 2),
                np.random.normal(-1, 1),
                np.random.normal(2, 2),
                np.random.normal(6, 2),
                np.random.normal(0.52, 0.02),
                np.random.normal(0, 1)
            ])
            labels.append(4)
            
        df = pd.DataFrame(data, columns=columns)
        
        self.model = RandomForestClassifier(n_estimators=100, random_state=42)
        self.model.fit(df, labels)
        
        X_train, X_test, y_train, y_test = train_test_split(df, labels, test_size=0.2, random_state=42)
        test_model = RandomForestClassifier(n_estimators=100, random_state=42)
        test_model.fit(X_train, y_train)
        y_pred = test_model.predict(X_test)
        
        print("\n=== Anatomical Model Re-trained ===")
        print("Accuracy:", accuracy_score(y_test, y_pred))
        
        with open(self.model_path, 'wb') as f:
            pickle.dump(self.model, f)

    def preprocess_image(self, img):
        """Resizes, aligns, normalizes, removes background, and checks blur."""
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        laplacian_var = cv2.Laplacian(gray, cv2.CV_64F).var()
        is_blurry = laplacian_var < 50.0

        img_aligned = img.copy()
        h, w = img.shape[:2]
        if self.mp_face_mesh:
            img_rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
            results = self.mp_face_mesh.process(img_rgb)
            if results.multi_face_landmarks:
                landmarks = results.multi_face_landmarks[0].landmark
                left_eye = np.array([landmarks[33].x * w, landmarks[33].y * h])
                right_eye = np.array([landmarks[263].x * w, landmarks[263].y * h])
                
                if abs(right_eye[0] - left_eye[0]) > w * 0.05:
                    dy = right_eye[1] - left_eye[1]
                    dx = right_eye[0] - left_eye[0]
                    angle = np.degrees(np.arctan2(dy, dx))
                    center = (int((left_eye[0] + right_eye[0]) // 2), int((left_eye[1] + right_eye[1]) // 2))
                    M = cv2.getRotationMatrix2D(center, angle, 1.0)
                    img_aligned = cv2.warpAffine(img, M, (w, h))
                else:
                    M = np.float32([[1, 0, 0], [0, 1, 0]])
                    img_aligned = cv2.warpAffine(img, M, (w, h))
                
                results_aligned = self.mp_face_mesh.process(cv2.cvtColor(img_aligned, cv2.COLOR_BGR2RGB))
                if results_aligned.multi_face_landmarks:
                    landmarks_aligned = results_aligned.multi_face_landmarks[0].landmark
                    xs = [lm.x for lm in landmarks_aligned]
                    ys = [lm.y for lm in landmarks_aligned]
                    min_x, max_x = max(0, min(xs)), min(1, max(xs))
                    min_y, max_y = max(0, min(ys)), min(1, max(ys))
                    margin_x = (max_x - min_x) * 0.2
                    margin_y = (max_y - min_y) * 0.2
                    min_x = max(0, min_x - margin_x)
                    max_x = min(1, max_x + margin_x)
                    min_y = max(0, min_y - margin_y)
                    max_y = min(1, max_y + margin_y)
                    
                    x1, y1 = int(min_x * w), int(min_y * h)
                    x2, y2 = int(max_x * w), int(max_y * h)
                    
                    if x2 > x1 and y2 > y1:
                        img_aligned = img_aligned[y1:y2, x1:x2]

        img_resized = cv2.resize(img_aligned, (512, 512))
        
        if self.mp_selfie:
            img_rgb = cv2.cvtColor(img_resized, cv2.COLOR_BGR2RGB)
            results = self.mp_selfie.process(img_rgb)
            condition = np.stack((results.segmentation_mask,) * 3, axis=-1) > 0.1
            bg_image = np.zeros(img_resized.shape, dtype=np.uint8)
            img_resized = np.where(condition, img_resized, bg_image)
            
        lab = cv2.cvtColor(img_resized, cv2.COLOR_BGR2LAB)
        l, a, b = cv2.split(lab)
        clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8,8))
        cl = clahe.apply(l)
        limg = cv2.merge((cl,a,b))
        img_norm = cv2.cvtColor(limg, cv2.COLOR_LAB2BGR)
        
        return img_norm, is_blurry, laplacian_var

    def extract_geometric_features(self, landmarks, img_width, img_height):
        def get_pt(idx):
            return np.array([landmarks[idx].x * img_width, landmarks[idx].y * img_height])
            
        glabella = get_pt(9)
        nasion = get_pt(8)
        pronasale = get_pt(1) 
        subnasale = get_pt(2) 
        upper_lip = get_pt(13)
        lower_lip = get_pt(14)
        pogonion = get_pt(152) 
        menton = get_pt(199)
        sella_approx = get_pt(168)
        a_point = get_pt(164)
        b_point = get_pt(175)
        
        # Determine face direction (1 = facing right, -1 = facing left)
        face_dir = 1 if pronasale[0] > sella_approx[0] else -1
        
        # 1. Convexity Depth
        gp_vec = pogonion - glabella
        gp_length = np.linalg.norm(gp_vec)
        convexity_depth = 0
        if gp_length > 0:
            convexity_depth = ((subnasale[0] - glabella[0]) * gp_vec[1] - (subnasale[1] - glabella[1]) * gp_vec[0]) / gp_length
            convexity_depth *= face_dir
            
        # 2. Wits Proxy
        wits_proxy = (a_point[0] - b_point[0]) * face_dir
        
        # 3. Maxilla & Mandible Prominence
        maxilla_prominence = (a_point[0] - nasion[0]) * face_dir
        mandible_prominence = (b_point[0] - nasion[0]) * face_dir
        
        # 4. Lower Face Height Ratio
        total_face = np.linalg.norm(glabella - menton)
        lower_face = np.linalg.norm(subnasale - menton)
        lfhr = lower_face / total_face if total_face > 0 else 0.5
        
        # 5. E-Line Deviation
        e_line_vec = pogonion - pronasale
        e_line_length = np.linalg.norm(e_line_vec)
        e_line_dev = 0
        if e_line_length > 0:
            e_line_dev = ((lower_lip[0] - pronasale[0]) * e_line_vec[1] - (lower_lip[1] - pronasale[1]) * e_line_vec[0]) / e_line_length
            e_line_dev *= face_dir

        features = {
            "convexity_depth": convexity_depth,
            "wits_proxy": wits_proxy,
            "maxilla_prominence": maxilla_prominence,
            "mandible_prominence": mandible_prominence,
            "lfhr": lfhr,
            "e_line_dev": e_line_dev
        }
        return features
        
    def classify_and_score(self, features):
        columns = ["convexity_depth", "wits_proxy", "maxilla_prominence", "mandible_prominence", "lfhr", "e_line_dev"]
        input_data = pd.DataFrame([[features[k] for k in columns]], columns=columns)
        
        pred_class = self.model.predict(input_data)[0]
        diagnosis = self.class_mapping.get(pred_class, "Unknown")
        
        probs = self.model.predict_proba(input_data)[0]
        confidence = float(np.max(probs) * 100)
        
        severity_index = (max(0, -features['wits_proxy']) * 2.5) + (max(0, -features['convexity_depth']) * 2.0)
        
        if severity_index < 10:
            severity_category = "Mild"
        elif severity_index < 25:
            severity_category = "Moderate"
        else:
            severity_category = "Severe"
            
        severity_score = min(100.0, float(severity_index * 2))
            
        return diagnosis, severity_score, confidence, severity_category

    def generate_explainable_heatmap(self, image, landmarks, features):
        importances = self.model.feature_importances_
        columns = ["convexity_depth", "wits_proxy", "maxilla_prominence", "mandible_prominence", "lfhr", "e_line_dev"]
        feature_importance_map = dict(zip(columns, importances))
        
        h, w, _ = image.shape
        heatmap = np.zeros((h, w), dtype=np.uint8)
        
        maxilla_pt = (int(landmarks[164].x * w), int(landmarks[164].y * h))
        mandible_pt = (int(landmarks[175].x * w), int(landmarks[175].y * h))
        
        intensity = int(min(255, max(0, -features['wits_proxy']) * 15))
        
        cv2.circle(heatmap, maxilla_pt, radius=50, color=intensity, thickness=-1)
        cv2.circle(heatmap, mandible_pt, radius=50, color=intensity, thickness=-1)
        
        heatmap = cv2.GaussianBlur(heatmap, (151, 151), 0)
        heatmap_colored = cv2.applyColorMap(heatmap, cv2.COLORMAP_JET)
        overlay = cv2.addWeighted(image, 0.6, heatmap_colored, 0.4, 0)
        
        return overlay

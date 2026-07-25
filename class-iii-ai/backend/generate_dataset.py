import cv2
import numpy as np
import os
import random

def create_directory_structure(base_dir):
    classes = ['Class_I', 'Class_II', 'Class_III']
    for cls in classes:
        os.makedirs(os.path.join(base_dir, cls), exist_ok=True)
    return classes

def draw_face_frontal(image_size=(512, 512), bg_color=(255, 255, 255), face_color=(220, 220, 220)):
    # Create blank image
    img = np.full((image_size[1], image_size[0], 3), bg_color, dtype=np.uint8)
    
    # Generic Frontal Face (Oval)
    center = (256, 256)
    axes = (120, 160)
    angle = 0
    
    # Add random variation to simulate different patients
    axes = (axes[0] + random.randint(-10, 10), axes[1] + random.randint(-10, 10))
    
    cv2.ellipse(img, center, axes, angle, 0, 360, face_color, -1)
    
    # Draw simple facial features to give context
    # Eyes
    cv2.circle(img, (210, 220), 10, (100, 100, 100), -1)
    cv2.circle(img, (300, 220), 10, (100, 100, 100), -1)
    
    # Nose
    pts_nose = np.array([[256, 220], [240, 280], [272, 280]], np.int32)
    cv2.fillPoly(img, [pts_nose], (180, 180, 180))
    
    # Mouth
    cv2.ellipse(img, (256, 320), (30, 10), 0, 0, 180, (150, 100, 100), 3)
    
    # Add noise to make it realistic for ML
    noise = np.random.normal(0, 10, img.shape).astype(np.uint8)
    img = cv2.add(img, noise)
    
    return img

def draw_face_lateral(face_class, image_size=(512, 512), bg_color=(255, 255, 255), face_color=(220, 220, 220)):
    img = np.full((image_size[1], image_size[0], 3), bg_color, dtype=np.uint8)
    
    # Base points for lateral profile (facing left)
    forehead = [300, 100]
    nasion = [270, 200]    # Bridge of nose
    pronasale = [230, 250] # Tip of nose
    subnasale = [270, 280] # Under nose
    upper_lip = [260, 300]
    lower_lip = [265, 320]
    
    # Adjust chin (Pogonion) based on Class
    if face_class == 'Class_I':
        # Normal profile (Convexity ~165-175)
        chin = [270, 380]
        menton = [280, 400]
    elif face_class == 'Class_II':
        # Retrognathic (Recessed chin) -> pushes X coordinate to the right (since facing left)
        offset = random.randint(30, 50)
        chin = [270 + offset, 380]
        menton = [280 + offset, 400]
    else: # Class_III
        # Prognathic (Protruding chin) -> pushes X coordinate to the left
        offset = random.randint(30, 50)
        chin = [270 - offset, 380]
        menton = [280 - offset, 400]
        
    # Add random micro-variations so images aren't identical
    points = [forehead, nasion, pronasale, subnasale, upper_lip, lower_lip, chin, menton]
    for pt in points:
        pt[0] += random.randint(-5, 5)
        pt[1] += random.randint(-5, 5)
        
    # Draw polygon for head shape
    head_back = [450, 250]
    neck_back = [450, 500]
    neck_front = [320, 500]
    
    pts = np.array([forehead, nasion, pronasale, subnasale, upper_lip, lower_lip, chin, menton, neck_front, neck_back, head_back], np.int32)
    
    cv2.fillPoly(img, [pts], face_color)
    
    # Draw eye (side view)
    cv2.ellipse(img, (320, 220), (15, 8), 0, 0, 360, (100, 100, 100), -1)
    
    # Add noise
    noise = np.random.normal(0, 10, img.shape).astype(np.uint8)
    img = cv2.add(img, noise)
    
    return img

def main():
    print("Starting Synthetic Dataset Generation...")
    base_dir = os.path.join(os.getcwd(), 'dataset')
    classes = create_directory_structure(base_dir)
    
    images_per_class = 100
    total_images = 0
    
    for cls in classes:
        print(f"Generating {images_per_class} patients for {cls}...")
        cls_dir = os.path.join(base_dir, cls)
        
        for i in range(1, images_per_class + 1):
            patient_id = f"{i:03d}"
            
            # Generate Frontal
            frontal_img = draw_face_frontal()
            frontal_path = os.path.join(cls_dir, f"patient_{patient_id}_frontal.jpg")
            cv2.imwrite(frontal_path, frontal_img)
            total_images += 1
            
            # Generate Lateral
            lateral_img = draw_face_lateral(cls)
            lateral_path = os.path.join(cls_dir, f"patient_{patient_id}_lateral.jpg")
            cv2.imwrite(lateral_path, lateral_img)
            total_images += 1
            
    print(f"Successfully generated {total_images} images in '{base_dir}'.")

if __name__ == "__main__":
    main()

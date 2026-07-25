import React, { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Upload, RefreshCw } from 'lucide-react';

const Simulation = () => {
  const navigate = useNavigate();
  const [imageSrc, setImageSrc] = useState(null);
  const [morphValue, setMorphValue] = useState(0); // -10 to 10
  const canvasRef = useRef(null);
  const imgRef = useRef(null); // To store original image data

  const handleImageUpload = (e) => {
    const file = e.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (event) => {
        setImageSrc(event.target.result);
      };
      reader.readAsDataURL(file);
    }
  };

  useEffect(() => {
    if (imageSrc && canvasRef.current) {
      const canvas = canvasRef.current;
      const ctx = canvas.getContext('2d');
      const img = new Image();
      img.onload = () => {
        // Calculate aspect ratio
        const maxWidth = 500;
        const scale = Math.min(maxWidth / img.width, 1);
        canvas.width = img.width * scale;
        canvas.height = img.height * scale;
        
        ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
        
        // Save original image data for later restoration
        imgRef.current = { img, width: canvas.width, height: canvas.height };
        applyMorph(0); // Initial draw
      };
      img.src = imageSrc;
    }
  }, [imageSrc]);

  useEffect(() => {
    applyMorph(morphValue);
  }, [morphValue]);

  const applyMorph = (amount) => {
    if (!imgRef.current || !canvasRef.current) return;
    
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    const { img, width, height } = imgRef.current;
    
    // Clear canvas and draw original
    ctx.clearRect(0, 0, width, height);
    ctx.drawImage(img, 0, 0, width, height);
    
    if (amount === 0) return;
    
    // Web Canvas Pixel Manipulation to mimic MorphImageView
    const imageData = ctx.getImageData(0, 0, width, height);
    const data = imageData.data;
    const resultData = new Uint8ClampedArray(data.length);
    
    // Copy the whole array first
    for (let i = 0; i < data.length; i++) {
        resultData[i] = data[i];
    }
    
    const w = width;
    const h = height;
    
    for (let y = 0; y < h; y++) {
      const ny = y / h;
      let warpX = 0;
      
      // Focus effect on the lower half (jaw region)
      if (ny > 0.55) {
        let intensity = 0;
        if (ny < 0.75) {
          const t = (ny - 0.55) / 0.20;
          intensity = t * t * (3 - 2 * t); // Smoothstep
        } else {
          intensity = 1.0;
        }
        warpX = amount * intensity * (w / 35.0);
      }
      
      for (let x = 0; x < w; x++) {
        // Find where this pixel should pull data from
        const sourceX = Math.min(w - 1, Math.max(0, x - warpX));
        
        // Nearest neighbor interpolation for simplicity
        const srcIdx = (y * w + Math.floor(sourceX)) * 4;
        const dstIdx = (y * w + x) * 4;
        
        resultData[dstIdx] = data[srcIdx];
        resultData[dstIdx + 1] = data[srcIdx + 1];
        resultData[dstIdx + 2] = data[srcIdx + 2];
        resultData[dstIdx + 3] = data[srcIdx + 3];
      }
    }
    
    const newImageData = new ImageData(resultData, width, height);
    ctx.putImageData(newImageData, 0, 0);
  };

  return (
    <div className="container" style={{ padding: '2rem' }}>
      <button onClick={() => navigate(-1)} className="btn" style={{ backgroundColor: 'transparent', padding: '0.5rem 0', marginBottom: '2rem', color: 'var(--text-secondary)' }}>
        <ArrowLeft size={18} /> Back
      </button>

      <div className="glass-card flex-center" style={{ flexDirection: 'column', maxWidth: '800px', margin: '0 auto' }}>
        <h2 style={{ marginBottom: '1.5rem', textAlign: 'center' }}>Facial Morphing Simulator</h2>
        
        {!imageSrc ? (
          <div style={{ border: '2px dashed var(--surface-border)', padding: '4rem', borderRadius: '12px', textAlign: 'center', backgroundColor: 'rgba(0,0,0,0.2)', width: '100%', cursor: 'pointer' }}>
            <input type="file" id="simUpload" accept="image/*" style={{ display: 'none' }} onChange={handleImageUpload} />
            <label htmlFor="simUpload" style={{ cursor: 'pointer', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '1rem' }}>
              <Upload size={48} color="var(--primary)" />
              <span style={{ fontSize: '1.2rem' }}>Upload Lateral Profile to Start</span>
            </label>
          </div>
        ) : (
          <div style={{ width: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
            
            <div style={{ marginBottom: '2rem', borderRadius: '12px', overflow: 'hidden', border: '1px solid var(--surface-border)' }}>
              <canvas ref={canvasRef} style={{ display: 'block', maxWidth: '100%', height: 'auto' }}></canvas>
            </div>
            
            <div style={{ width: '100%', maxWidth: '500px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem' }}>
                <span style={{ color: 'var(--error)' }}>Setback (Class II/III Correction)</span>
                <span style={{ color: 'var(--secondary)' }}>Advancement</span>
              </div>
              
              <input 
                type="range" 
                min="-10" 
                max="10" 
                step="0.1" 
                value={morphValue} 
                onChange={(e) => setMorphValue(parseFloat(e.target.value))}
                style={{ width: '100%', marginBottom: '1rem' }}
              />
              
              <div style={{ textAlign: 'center', fontSize: '1.2rem', fontWeight: 'bold', marginBottom: '2rem', color: morphValue === 0 ? 'var(--primary)' : (morphValue > 0 ? 'var(--secondary)' : 'var(--error)') }}>
                {morphValue === 0 ? 'Neutral (0mm)' : (morphValue > 0 ? `Advancement: +${morphValue}mm` : `Setback: ${morphValue}mm`)}
              </div>
              
              <button className="btn" style={{ width: '100%', backgroundColor: 'rgba(255,255,255,0.1)' }} onClick={() => setMorphValue(0)}>
                <RefreshCw size={18} /> Reset Simulation
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default Simulation;

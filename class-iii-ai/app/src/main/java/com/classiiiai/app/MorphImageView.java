package com.classiiiai.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatImageView;

public class MorphImageView extends AppCompatImageView {

    private static final int WIDTH_BLOCKS = 20;
    private static final int HEIGHT_BLOCKS = 20;
    private static final int COUNT = (WIDTH_BLOCKS + 1) * (HEIGHT_BLOCKS + 1);

    private float[] verts = new float[COUNT * 2];
    private float[] orig = new float[COUNT * 2];
    private Bitmap sourceBitmap;

    private float morphAmount = 0; // -10 to 10

    public MorphImageView(Context context) {
        super(context);
    }

    public MorphImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        if (drawable instanceof BitmapDrawable) {
            sourceBitmap = ((BitmapDrawable) drawable).getBitmap();
            buildMesh();
        }
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        sourceBitmap = bm;
        buildMesh();
    }

    private void buildMesh() {
        if (sourceBitmap == null) return;
        
        float w = sourceBitmap.getWidth();
        float h = sourceBitmap.getHeight();
        
        int index = 0;
        for (int y = 0; y <= HEIGHT_BLOCKS; y++) {
            float fy = h * y / HEIGHT_BLOCKS;
            for (int x = 0; x <= WIDTH_BLOCKS; x++) {
                float fx = w * x / WIDTH_BLOCKS;
                orig[index * 2 + 0] = verts[index * 2 + 0] = fx;
                orig[index * 2 + 1] = verts[index * 2 + 1] = fy;
                index += 1;
            }
        }
        applyMorph();
    }

    public void setMorphAmount(float amount) {
        this.morphAmount = amount; // Negative = setback, Positive = advancement
        applyMorph();
    }

    private void applyMorph() {
        if (sourceBitmap == null) return;
        
        float w = sourceBitmap.getWidth();
        float h = sourceBitmap.getHeight();
        
        // We will warp the bottom left quadrant (assuming patient faces left).
        // For a more robust app, MLKit landmarks would dictate exactly where the jaw is.
        // For simulation purposes, we stretch the pixels horizontally in the lower 30% of the image.

        for (int i = 0; i < COUNT * 2; i += 2) {
            float ox = orig[i + 0];
            float oy = orig[i + 1];

            // Normalize coordinates
            float nx = ox / w;
            float ny = oy / h;

            float warpX = 0;
            
            // Focus effect on the lower half of the image (jaw/mandible region)
            if (ny > 0.55f) {
                float intensity = 0;
                // Smooth transition (soft tissue blend) from middle to jawline
                if (ny < 0.75f) {
                    // Ease-in curve for natural neck/lip blending
                    float t = (ny - 0.55f) / 0.20f;
                    intensity = t * t * (3 - 2 * t); // Smoothstep formula
                } else {
                    // Maximum uniform shift for the actual jawbone
                    intensity = 1.0f;
                }
                
                // Shift the entire lower row horizontally to simulate BSSO surgery block movement
                warpX = morphAmount * intensity * (w / 35f); 
            }

            verts[i + 0] = orig[i + 0] + warpX;
            verts[i + 1] = orig[i + 1];
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (sourceBitmap != null) {
            // Calculate scale and translation to center the mesh inside the ImageView
            int viewWidth = getWidth();
            int viewHeight = getHeight();
            int imgWidth = sourceBitmap.getWidth();
            int imgHeight = sourceBitmap.getHeight();
            
            float scale = Math.min((float)viewWidth/imgWidth, (float)viewHeight/imgHeight);
            float dx = (viewWidth - imgWidth * scale) / 2f;
            float dy = (viewHeight - imgHeight * scale) / 2f;
            
            canvas.save();
            canvas.translate(dx, dy);
            canvas.scale(scale, scale);
            
            canvas.drawBitmapMesh(sourceBitmap, WIDTH_BLOCKS, HEIGHT_BLOCKS, verts, 0, null, 0, null);
            
            canvas.restore();
        } else {
            super.onDraw(canvas);
        }
    }
}

package com.classiiiai.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;

import java.util.List;

public class FaceOverlayView extends View {

    private Face face;
    private Paint paintDot;
    private Paint paintLine;
    private Paint paintText;

    private int imageWidth;
    private int imageHeight;

    public FaceOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintDot = new Paint();
        paintDot.setColor(Color.GREEN);
        paintDot.setStyle(Paint.Style.FILL);
        paintDot.setAntiAlias(true);

        paintLine = new Paint();
        paintLine.setColor(Color.CYAN);
        paintLine.setStyle(Paint.Style.STROKE);
        paintLine.setStrokeWidth(4.0f);
        paintLine.setAntiAlias(true);

        paintText = new Paint();
        paintText.setColor(Color.YELLOW);
        paintText.setTextSize(48f);
        paintText.setAntiAlias(true);
    }

    public void setFace(Face face, int imageWidth, int imageHeight) {
        this.face = face;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        invalidate(); // Redraw
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (face == null) return;

        // Scale factors to map image coordinates to view coordinates
        float scaleX = (float) getWidth() / imageWidth;
        float scaleY = (float) getHeight() / imageHeight;
        
        // Since it's a front camera, we might need to mirror X
        // For simplicity, we assume MLKit coordinates map directly after scale
        // In a real app, coordinate transformation for front camera requires flipping.

        FaceContour faceContour = face.getContour(FaceContour.FACE);
        FaceContour noseContour = face.getContour(FaceContour.NOSE_BRIDGE);
        FaceContour lowerLip = face.getContour(FaceContour.LOWER_LIP_BOTTOM);

        if (faceContour != null && noseContour != null && lowerLip != null) {
            
            // Approximate Nasion (N) - top of nose bridge
            List<PointF> nosePoints = noseContour.getPoints();
            PointF nasion = null;
            if (!nosePoints.isEmpty()) {
                nasion = nosePoints.get(0);
            }

            // Approximate Pogonion (Pg) - bottom of chin (lowest point on face contour)
            List<PointF> facePoints = faceContour.getPoints();
            PointF pogonion = null;
            float maxY = 0;
            for (PointF p : facePoints) {
                if (p.y > maxY) {
                    maxY = p.y;
                    pogonion = p;
                }
            }

            // Approximate Subnasale (Sn) / Point A area - bottom of nose
            PointF subnasale = null;
            if (nosePoints.size() > 1) {
                subnasale = nosePoints.get(nosePoints.size() - 1);
            }
            
            if (nasion != null && subnasale != null && pogonion != null) {
                
                // Draw dots
                canvas.drawCircle(nasion.x * scaleX, nasion.y * scaleY, 8f, paintDot);
                canvas.drawCircle(subnasale.x * scaleX, subnasale.y * scaleY, 8f, paintDot);
                canvas.drawCircle(pogonion.x * scaleX, pogonion.y * scaleY, 8f, paintDot);

                // Draw lines connecting N - Sn - Pg
                canvas.drawLine(nasion.x * scaleX, nasion.y * scaleY, subnasale.x * scaleX, subnasale.y * scaleY, paintLine);
                canvas.drawLine(subnasale.x * scaleX, subnasale.y * scaleY, pogonion.x * scaleX, pogonion.y * scaleY, paintLine);

                // Calculate angle (N-Sn-Pg)
                double angle = calculateAngle(nasion, subnasale, pogonion);
                
                // Draw text
                canvas.drawText(String.format("Convexity: %.1f°", angle), 50, 100, paintText);
            }
        }
    }

    private double calculateAngle(PointF p1, PointF p2, PointF p3) {
        // Vector 1: p2 -> p1
        float v1x = p1.x - p2.x;
        float v1y = p1.y - p2.y;
        
        // Vector 2: p2 -> p3
        float v2x = p3.x - p2.x;
        float v2y = p3.y - p2.y;
        
        double dotProduct = v1x * v2x + v1y * v2y;
        double mag1 = Math.sqrt(v1x * v1x + v1y * v1y);
        double mag2 = Math.sqrt(v2x * v2x + v2y * v2y);
        
        double angleRad = Math.acos(dotProduct / (mag1 * mag2));
        return Math.toDegrees(angleRad);
    }
}

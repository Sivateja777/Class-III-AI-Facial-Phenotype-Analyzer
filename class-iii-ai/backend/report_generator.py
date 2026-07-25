import os
from reportlab.lib.pagesizes import letter
from reportlab.lib import colors
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Image, Table, TableStyle
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import inch
import time

class ReportGenerator:
    def __init__(self, output_dir="reports"):
        self.output_dir = output_dir
        if not os.path.exists(output_dir):
            os.makedirs(output_dir)

    def generate_pdf(self, patient_id, diagnosis, confidence, severity, features, original_img_path, heatmap_img_path):
        """Generates a clinical PDF report for the AI analysis."""
        timestamp = int(time.time())
        filename = f"{patient_id}_report_{timestamp}.pdf"
        filepath = os.path.join(self.output_dir, filename)
        
        doc = SimpleDocTemplate(filepath, pagesize=letter)
        styles = getSampleStyleSheet()
        elements = []
        
        # Custom Styles
        title_style = ParagraphStyle('Title', parent=styles['Heading1'], textColor=colors.HexColor('#0F172A'), alignment=1)
        sub_style = ParagraphStyle('Subtitle', parent=styles['Normal'], textColor=colors.HexColor('#64748B'), alignment=1)
        
        # Header
        elements.append(Paragraph("Class III Maxillofacial AI Analysis", title_style))
        elements.append(Paragraph(f"Patient ID: {patient_id} | Date: {time.strftime('%Y-%m-%d %H:%M')}", sub_style))
        elements.append(Spacer(1, 0.25 * inch))
        
        # Diagnosis Block
        elements.append(Paragraph(f"<b>Primary AI Diagnosis:</b> {diagnosis}", styles['Heading2']))
        elements.append(Paragraph(f"<b>Severity Score:</b> {severity}/100", styles['Normal']))
        elements.append(Paragraph(f"<b>AI Confidence:</b> {confidence:.1f}%", styles['Normal']))
        elements.append(Spacer(1, 0.25 * inch))
        
        # Images Table (Original vs Heatmap)
        img1 = Image(original_img_path, width=2.5*inch, height=3*inch)
        img2 = Image(heatmap_img_path, width=2.5*inch, height=3*inch)
        
        img_table = Table([[img1, img2]], colWidths=[3*inch, 3*inch])
        img_table.setStyle(TableStyle([
            ('ALIGN', (0,0), (-1,-1), 'CENTER'),
            ('VALIGN', (0,0), (-1,-1), 'MIDDLE')
        ]))
        elements.append(img_table)
        
        img_labels = Table([["Original Scan", "AI Activation Heatmap"]], colWidths=[3*inch, 3*inch])
        img_labels.setStyle(TableStyle([
            ('ALIGN', (0,0), (-1,-1), 'CENTER'),
            ('TEXTCOLOR', (0,0), (-1,-1), colors.HexColor('#64748B'))
        ]))
        elements.append(img_labels)
        elements.append(Spacer(1, 0.25 * inch))
        
        # Feature Measurements Table
        elements.append(Paragraph("Geometric AI Features", styles['Heading3']))
        data = [
            ["Metric", "Value", "Standard Norm"],
            ["Facial Convexity Angle", f"{features['convexity']:.1f}°", "~165° - 175°"],
            ["Lower Facial Height Ratio", f"{features['lfhr']:.1f}%", "~50% - 55%"],
            ["Nasolabial Angle", f"{features['nasolabial']:.1f}°", "~90° - 110°"],
            ["Mentolabial Angle", f"{features['mentolabial']:.1f}°", "~120° - 130°"],
            ["E-Line Deviation", f"{features['e_line']:.2f} px", "0 (Lips on line)"]
        ]
        
        t = Table(data, colWidths=[2.5*inch, 1.5*inch, 1.5*inch])
        t.setStyle(TableStyle([
            ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#0F172A')),
            ('TEXTCOLOR', (0, 0), (-1, 0), colors.whitesmoke),
            ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
            ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
            ('BOTTOMPADDING', (0, 0), (-1, 0), 12),
            ('BACKGROUND', (0, 1), (-1, -1), colors.HexColor('#F8FAFC')),
            ('GRID', (0,0), (-1,-1), 1, colors.HexColor('#E2E8F0'))
        ]))
        
        elements.append(t)
        
        doc.build(elements)
        return filename

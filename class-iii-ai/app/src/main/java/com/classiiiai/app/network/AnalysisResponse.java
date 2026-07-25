package com.classiiiai.app.network;

import com.google.gson.annotations.SerializedName;

public class AnalysisResponse {
    @SerializedName("severityScore")
    public double severityScore;
    
    @SerializedName("severityCategory")
    public String severityCategory;
    
    @SerializedName("diagnosis")
    public String diagnosis;
    
    @SerializedName("heatmapUrl")
    public String heatmapUrl;
    
    @SerializedName("reportUrl")
    public String reportUrl;
    
    @SerializedName("details")
    public String details;
    
    @SerializedName("features")
    public java.util.Map<String, Double> features;
    
    @SerializedName("error")
    public String error;
}

package com.classiiiai.app.network;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface AnalysisApiService {
    @Multipart
    @POST("/analyze")
    Call<AnalysisResponse> analyzeImage(@Part MultipartBody.Part frontal, @Part MultipartBody.Part lateral);
}

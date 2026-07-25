package com.classiiiai.app.network;

import com.classiiiai.app.data.User;
import com.classiiiai.app.data.AnalysisReport;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface FirebaseApiService {
    
    // Put a user at a specific path, encoding their email as the key
    @PUT("users/{emailId}.json")
    Call<User> saveUser(@Path("emailId") String emailId, @Body User user);
    
    // Get all users
    @GET("users.json")
    Call<Map<String, User>> getAllUsers();
    
    // Get a specific user
    @GET("users/{emailId}.json")
    Call<User> getUser(@Path("emailId") String emailId);

    // Push a new report (generates a unique random key)
    @POST("reports.json")
    Call<Map<String, String>> saveReport(@Body AnalysisReport report);
    
    // Get all reports
    @GET("reports.json")
    Call<Map<String, AnalysisReport>> getAllReports();
}

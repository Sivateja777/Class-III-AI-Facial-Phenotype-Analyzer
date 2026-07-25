package com.classiiiai.app.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FirebaseClient {
    private static final String BASE_URL = "https://facial-phenotype-analyze-128d5-default-rtdb.firebaseio.com/";
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}

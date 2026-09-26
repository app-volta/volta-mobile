package com.aula.volta.data.api;

import com.aula.volta.data.model.CooperativeJSON;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface CooperativeAPI {

    @GET("cooperatives/recommended")
    Call<List<CooperativeJSON>> getRecommended(@Query("occurrenceId") String occurrenceId);
}

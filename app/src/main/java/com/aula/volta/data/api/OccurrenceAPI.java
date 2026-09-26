package com.aula.volta.data.api;

import com.aula.volta.data.model.OccurrenceJSON;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface OccurrenceAPI {

    @GET("occurrences")
    Call<List<OccurrenceJSON>> getOccurrences();

    @GET("occurrences/{id}")
    Call<JsonObject> getOccurrence(@Path("id") String id);

    @POST("occurrences")
    Call<JsonObject> createOccurrence(@Body JsonObject body);

    @POST("occurrences/{id}/analysis")
    Call<JsonObject> analyzeOccurrence(@Path("id") String id, @Body JsonObject body);
}

package com.aula.volta.data.api;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ReportAPI {

    @GET("reports/summary")
    Call<JsonObject> getSummary();
}

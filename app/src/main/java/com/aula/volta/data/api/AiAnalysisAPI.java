package com.aula.volta.data.api;

import com.aula.volta.data.model.AiAnalysisJSON;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface AiAnalysisAPI {

    @POST("occurrences/{id}/analysis")
    Call<AiAnalysisJSON> analyzeOccurrence(@Path("id") String occurrenceId,
                                           @Body JsonObject body);
}

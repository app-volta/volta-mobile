package com.aula.volta.data.api;

import com.aula.volta.data.model.AiAnalysisJSON;
import com.aula.volta.data.model.incident.AiReportResponseDTO;
import com.aula.volta.data.model.incident.IncidentRequestDTO;
import com.aula.volta.data.model.incident.IncidentResponseDTO;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface AiAnalysisAPI {

    @Headers({
            "Content-Type: application/json; charset=utf-8",
            "Accept: application/json; charset=utf-8"
    })
    @POST("occurrences/{id}/analysis")
    Call<AiAnalysisJSON> analyzeOccurrence(@Path("id") String occurrenceId,
                                           @Body JsonObject body);

    @Headers({
            "Content-Type: application/json; charset=utf-8",
            "Accept: application/json; charset=utf-8"
    })
    @POST("incidents")
    Call<IncidentResponseDTO> createIncident(@Body IncidentRequestDTO request);

    @Headers({
            "Accept: application/json; charset=utf-8"
    })
    @GET("incidents/{id}/ai-report")
    Call<AiReportResponseDTO> getIncidentAiReport(@Path("id") String incidentId);

    @Headers({
            "Accept: application/json; charset=utf-8"
    })
    @GET("incidents/{id}/scoring-context")
    Call<JsonObject> getIncidentScoringContext(@Path("id") String incidentId);

    @Headers({
            "Accept: application/json; charset=utf-8"
    })
    @GET("areas/mine")
    Call<JsonArray> getMyAreas();

    @Headers({
            "Accept: application/json; charset=utf-8"
    })
    @GET("waste-types")
    Call<JsonArray> getWasteTypes();
}

package com.aula.volta.data.api;

import com.aula.volta.data.model.OccurrenceJSON;
import com.aula.volta.data.model.incident.AiReportResponseDTO;
import com.aula.volta.data.model.incident.IncidentRequestDTO;
import com.aula.volta.data.model.incident.IncidentResponseDTO;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Endpoints de gestão de incidentes/ocorrências da API VOLTA (https://api.54.210.1.34.sslip.io).
 */
public interface OccurrenceAPI {

    // --- Endpoints Oficiais do Banco de Dados (/incidents) ---

    @Headers({"Accept: application/json; charset=utf-8"})
    @GET("incidents")
    Call<List<IncidentResponseDTO>> getIncidents();

    @Headers({"Accept: application/json; charset=utf-8"})
    @GET("incidents")
    Call<List<IncidentResponseDTO>> getIncidentsByStatus(@Query("status") String status);

    @Headers({"Accept: application/json; charset=utf-8"})
    @GET("incidents/{id}")
    Call<IncidentResponseDTO> getIncidentById(@Path("id") String id);

    @Headers({
            "Content-Type: application/json; charset=utf-8",
            "Accept: application/json; charset=utf-8"
    })
    @POST("incidents")
    Call<IncidentResponseDTO> createIncident(@Body IncidentRequestDTO body);

    @PATCH("incidents/{id}/close")
    Call<Void> closeIncident(@Path("id") String id);

    @Headers({"Accept: application/json; charset=utf-8"})
    @GET("incidents/{id}/ai-report")
    Call<AiReportResponseDTO> getIncidentAiReport(@Path("id") String id);

    @Headers({"Accept: application/json; charset=utf-8"})
    @GET("incidents/{id}/scoring-context")
    Call<JsonObject> getIncidentScoringContext(@Path("id") String id);

    // --- Endpoints de compatibilidade (mock/cache legado) ---

    @GET("occurrences")
    Call<List<OccurrenceJSON>> getOccurrences();

    @GET("occurrences/{id}")
    Call<JsonObject> getOccurrence(@Path("id") String id);

    @POST("occurrences")
    Call<JsonObject> createOccurrence(@Body JsonObject body);

    @POST("occurrences/{id}/analysis")
    Call<JsonObject> analyzeOccurrence(@Path("id") String id, @Body JsonObject body);
}

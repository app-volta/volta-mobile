package com.aula.volta.data.api;

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
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface OccurrenceAPI {

    @Headers({"Accept: application/json; charset=utf-8"})
    @GET("incidents")
    Call<List<IncidentResponseDTO>> getIncidents();

    @Headers({"Accept: application/json; charset=utf-8"})
    @GET("incidents")
    Call<List<IncidentResponseDTO>> getIncidentsByStatus(@Query("status") String status);

    @Headers({"Accept: application/json; charset=utf-8"})
    @GET("incidents")
    Call<List<IncidentResponseDTO>> getIncidentsFiltered(
            @Query("status") String status,
            @Query("priority") String priority,
            @Query("areaId") String areaId
    );

    @Headers({"Accept: application/json; charset=utf-8"})
    @GET("incidents/{id}")
    Call<IncidentResponseDTO> getIncidentById(@Path("id") String id);

    @Headers({
            "Content-Type: application/json; charset=utf-8",
            "Accept: application/json; charset=utf-8"
    })
    @POST("incidents")
    Call<IncidentResponseDTO> createIncident(@Body IncidentRequestDTO body);

    @Headers({
            "Content-Type: application/json; charset=utf-8",
            "Accept: application/json; charset=utf-8"
    })
    @PUT("incidents/{id}")
    Call<IncidentResponseDTO> updateIncident(@Path("id") String id, @Body JsonObject body);

    @Headers({"Accept: application/json; charset=utf-8"})
    @PATCH("incidents/{id}/approve")
    Call<IncidentResponseDTO> approveIncident(@Path("id") String id);

    @Headers({
            "Content-Type: application/json; charset=utf-8",
            "Accept: application/json; charset=utf-8"
    })
    @PATCH("incidents/{id}/reject")
    Call<IncidentResponseDTO> rejectIncident(@Path("id") String id, @Body JsonObject body);

    @Headers({
            "Content-Type: application/json; charset=utf-8",
            "Accept: application/json; charset=utf-8"
    })
    @PATCH("incident/{id}/reject")
    Call<IncidentResponseDTO> rejectIncidentSingular(@Path("id") String id, @Body JsonObject body);

    @PATCH("incidents/{id}/close")
    Call<Void> closeIncident(@Path("id") String id);

    @Headers({"Accept: application/json; charset=utf-8"})
    @GET("incidents/{id}/ai-report")
    Call<AiReportResponseDTO> getIncidentAiReport(@Path("id") String id);

    @Headers({"Accept: application/json; charset=utf-8"})
    @GET("incidents/{id}/scoring-context")
    Call<JsonObject> getIncidentScoringContext(@Path("id") String id);
}

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

/**
 * Endpoints de Análise de Imagem por IA e Relatório de IA de Incidentes (QA: https://api.qa.54.210.1.34.sslip.io).
 *
 * Contratos mapeados:
 * 1. POST /occurrences/{id}/analysis — Análise direta de imagem (recebe photoUrl/imageUrl do Cloudinary)
 * 2. POST /incidents — Registra incidente com photoUrl (secure_url do Cloudinary), areaId, wasteTypeId, etc.
 * 3. GET /incidents/{id}/ai-report — Consulta o relatório de IA gerado para o incidente (AiReportResponseDTO)
 * 4. GET /incidents/{id}/scoring-context — Consulta o contexto de score com resumo do relatório de IA
 * 5. GET /areas/mine — Lista os setores/áreas da empresa autenticada
 * 6. GET /waste-types — Lista o catálogo de tipos de resíduo (CONAMA 275/2001 / NBR 10004)
 */
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

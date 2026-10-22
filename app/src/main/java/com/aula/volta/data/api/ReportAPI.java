package com.aula.volta.data.api;

import com.aula.volta.data.model.EsgMetricResponseDTO;
import com.aula.volta.data.model.RecyclingPercentageResponseDTO;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * Endpoints de Indicadores ESG e Relatórios PGRS da API VOLTA (https://api.54.210.1.34.sslip.io).
 */
public interface ReportAPI {

    // --- Endpoints Oficiais do Banco de Dados ---

    @Headers({"Accept: application/json; charset=utf-8"})
    @GET("esg/metrics")
    Call<List<EsgMetricResponseDTO>> getEsgMetrics();

    @Headers({"Accept: application/json; charset=utf-8"})
    @GET("esg/score")
    Call<JsonObject> getEsgScore();

    @Headers({
            "Content-Type: application/json; charset=utf-8",
            "Accept: application/json; charset=utf-8"
    })
    @POST("esg/recycling-percentage")
    Call<RecyclingPercentageResponseDTO> calculateRecyclingPercentage(@Body JsonObject body);

    // --- Endpoints de compatibilidade ---

    @GET("reports/summary")
    Call<JsonObject> getSummary();
}

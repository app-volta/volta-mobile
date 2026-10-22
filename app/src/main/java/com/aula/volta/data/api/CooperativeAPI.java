package com.aula.volta.data.api;

import com.aula.volta.data.model.CollectionRequestDTO;
import com.aula.volta.data.model.CollectionResponseDTO;
import com.aula.volta.data.model.CooperativeJSON;
import com.aula.volta.data.model.CooperativeResponseDTO;
import com.google.gson.JsonArray;
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
 * Endpoints de cooperativas e coletas da API VOLTA (https://api.54.210.1.34.sslip.io).
 */
public interface CooperativeAPI {

    // --- Endpoints Oficiais do Banco de Dados ---

    @Headers({"Accept: application/json; charset=utf-8"})
    @GET("cooperatives")
    Call<List<CooperativeResponseDTO>> getCooperatives();

    @Headers({"Accept: application/json; charset=utf-8"})
    @GET("cooperatives/{id}")
    Call<CooperativeResponseDTO> getCooperativeById(@Path("id") String id);

    @Headers({"Accept: application/json; charset=utf-8"})
    @GET("cooperatives/scoring-data")
    Call<JsonArray> getCooperativeScoringData();

    @Headers({"Accept: application/json; charset=utf-8"})
    @GET("collections")
    Call<List<CollectionResponseDTO>> getCollections();

    @Headers({
            "Content-Type: application/json; charset=utf-8",
            "Accept: application/json; charset=utf-8"
    })
    @POST("collections")
    Call<CollectionResponseDTO> requestCollection(@Body CollectionRequestDTO body);

    @Headers({
            "Content-Type: application/json; charset=utf-8",
            "Accept: application/json; charset=utf-8"
    })
    @PATCH("collections/{id}/schedule")
    Call<Void> scheduleCollection(@Path("id") String id, @Body JsonObject body);

    @Headers({
            "Content-Type: application/json; charset=utf-8",
            "Accept: application/json; charset=utf-8"
    })
    @PATCH("collections/{id}/status")
    Call<Void> updateCollectionStatus(@Path("id") String id, @Body JsonObject body);

    @Headers({
            "Content-Type: application/json; charset=utf-8",
            "Accept: application/json; charset=utf-8"
    })
    @POST("collections/{collectionId}/review")
    Call<JsonObject> reviewCollection(@Path("collectionId") String collectionId, @Body JsonObject body);

    // --- Endpoints de compatibilidade ---

    @GET("cooperatives/recommended")
    Call<List<CooperativeJSON>> getRecommended(@Query("occurrenceId") String occurrenceId);
}

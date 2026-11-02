package com.aula.volta.data.api;

import com.aula.volta.data.model.AreaResponseDTO;
import com.aula.volta.data.model.WasteTypeResponseDTO;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;

public interface CatalogAPI {

    @Headers({"Accept: application/json; charset=utf-8"})
    @GET("areas/mine")
    Call<List<AreaResponseDTO>> getMyAreas();

    @Headers({"Accept: application/json; charset=utf-8"})
    @GET("areas")
    Call<List<AreaResponseDTO>> getAllAreas();

    @Headers({"Accept: application/json; charset=utf-8"})
    @GET("waste-types")
    Call<List<WasteTypeResponseDTO>> getWasteTypes();
}

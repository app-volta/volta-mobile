package com.aula.volta.data.api;

import com.aula.volta.data.model.NotificationJSON;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface NotificationAPI {

    @GET("notifications")
    Call<List<NotificationJSON>> getNotifications();
}

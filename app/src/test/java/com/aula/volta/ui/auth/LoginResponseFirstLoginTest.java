package com.aula.volta.ui.auth;

import com.aula.volta.data.model.auth.LoginResponse;
import com.google.gson.Gson;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LoginResponseFirstLoginTest {

    private final Gson gson = new Gson();

    @Test
    public void parsesFirstLoginTrue_fromStandardBackendResponse() {
        String json = "{\"token\":\"jwt-sample-token\",\"expiresIn\":86400000,\"firstLogin\":true}";
        LoginResponse response = gson.fromJson(json, LoginResponse.class);

        assertTrue("hasToken should be true", response.hasToken());
        assertEquals("jwt-sample-token", response.getToken());
        assertEquals(Long.valueOf(86400000), response.getExpiresIn());
        assertTrue("firstLogin should be true", Boolean.TRUE.equals(response.isFirstLogin()));
    }

    @Test
    public void parsesFirstLoginFalse_whenNotFirstLogin() {
        String json = "{\"token\":\"jwt-sample-token\",\"expiresIn\":86400000,\"firstLogin\":false}";
        LoginResponse response = gson.fromJson(json, LoginResponse.class);

        assertTrue("hasToken should be true", response.hasToken());
        assertFalse("firstLogin should be false", Boolean.TRUE.equals(response.isFirstLogin()));
    }

    @Test
    public void parsesAlternateSnakeCaseField_first_login() {
        String json = "{\"token\":\"jwt-sample-token\",\"expiresIn\":86400000,\"first_login\":true}";
        LoginResponse response = gson.fromJson(json, LoginResponse.class);

        assertTrue("hasToken should be true", response.hasToken());
        assertTrue("firstLogin should be true when deserializing first_login", Boolean.TRUE.equals(response.isFirstLogin()));
    }

    @Test
    public void parsesAlternateSnakeCaseField_is_first_login() {
        String json = "{\"token\":\"jwt-sample-token\",\"expiresIn\":86400000,\"is_first_login\":true}";
        LoginResponse response = gson.fromJson(json, LoginResponse.class);

        assertTrue("hasToken should be true", response.hasToken());
        assertTrue("firstLogin should be true when deserializing is_first_login", Boolean.TRUE.equals(response.isFirstLogin()));
    }

    @Test
    public void handlesMissingFirstLogin_defaultsToNullSafely() {
        String json = "{\"token\":\"jwt-sample-token\",\"expiresIn\":86400000}";
        LoginResponse response = gson.fromJson(json, LoginResponse.class);

        assertTrue("hasToken should be true", response.hasToken());
        assertNull("firstLogin should be null when omitted", response.isFirstLogin());
        assertFalse("Boolean.TRUE.equals should be false", Boolean.TRUE.equals(response.isFirstLogin()));
    }
}

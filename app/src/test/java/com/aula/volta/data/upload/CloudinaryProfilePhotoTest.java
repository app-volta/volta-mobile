package com.aula.volta.data.upload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.aula.volta.data.model.UserResponseDTO;
import com.google.gson.Gson;

import org.junit.Test;

public class CloudinaryProfilePhotoTest {

    @Test
    public void testSanitizeEmailForPublicId() {
        assertEquals("avatar_breno_volta_com", CloudinaryUploader.sanitizeEmailForPublicId("breno@volta.com"));
        assertEquals("avatar_carlos_amaral_empresa_com_br", CloudinaryUploader.sanitizeEmailForPublicId("carlos.amaral@empresa.com.br"));
        assertEquals("avatar_guest", CloudinaryUploader.sanitizeEmailForPublicId(null));
        assertEquals("avatar_guest", CloudinaryUploader.sanitizeEmailForPublicId(""));
    }

    @Test
    public void testGetDeterministicProfileUrl() {
        String url = CloudinaryUploader.getDeterministicProfileUrl("breno@volta.com");
        assertNotNull(url);
        assertEquals("https://res.cloudinary.com/kgou1air/image/upload/Perfil/Assets/Image/avatar_breno_volta_com.jpg", url);
        assertTrue(url.startsWith("https://res.cloudinary.com/"));
        assertTrue(url.endsWith(".jpg"));
    }

    @Test
    public void testUserResponseDTOAlternatePhotoFields() {
        Gson gson = new Gson();

        // 1. photoUrl camelCase
        String json1 = "{\"id\":\"1\",\"name\":\"Breno\",\"photoUrl\":\"https://res.cloudinary.com/test1.jpg\"}";
        UserResponseDTO u1 = gson.fromJson(json1, UserResponseDTO.class);
        assertEquals("https://res.cloudinary.com/test1.jpg", u1.getPhotoUrl());

        // 2. photo_url snake_case
        String json2 = "{\"id\":\"1\",\"name\":\"Breno\",\"photo_url\":\"https://res.cloudinary.com/test2.jpg\"}";
        UserResponseDTO u2 = gson.fromJson(json2, UserResponseDTO.class);
        assertEquals("https://res.cloudinary.com/test2.jpg", u2.getPhotoUrl());

        // 3. avatarUrl
        String json3 = "{\"id\":\"1\",\"name\":\"Breno\",\"avatarUrl\":\"https://res.cloudinary.com/test3.jpg\"}";
        UserResponseDTO u3 = gson.fromJson(json3, UserResponseDTO.class);
        assertEquals("https://res.cloudinary.com/test3.jpg", u3.getPhotoUrl());

        // 4. imageUrl
        String json4 = "{\"id\":\"1\",\"name\":\"Breno\",\"imageUrl\":\"https://res.cloudinary.com/test4.jpg\"}";
        UserResponseDTO u4 = gson.fromJson(json4, UserResponseDTO.class);
        assertEquals("https://res.cloudinary.com/test4.jpg", u4.getPhotoUrl());

        // 5. foto_url
        String json5 = "{\"id\":\"1\",\"name\":\"Breno\",\"foto_url\":\"https://res.cloudinary.com/test5.jpg\"}";
        UserResponseDTO u5 = gson.fromJson(json5, UserResponseDTO.class);
        assertEquals("https://res.cloudinary.com/test5.jpg", u5.getPhotoUrl());
    }
}

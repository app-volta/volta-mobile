package com.aula.volta.data.upload;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class CloudinaryUploader {

    private static final String TAG = "CloudinaryUploader";

    public static String CLOUD_NAME = "kgou1air";

    public static String UPLOAD_PRESET = "AI-Analysis";
    public static String ASSET_FOLDER = "Ocorrencia/Assets/Images";

    public static String PROFILE_UPLOAD_PRESET = "Perfil-Photo";
    public static String PROFILE_ASSET_FOLDER = "Perfil/Assets/Image";

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .build();

    private static void logI(String tag, String msg) {
        try {
            Log.i(tag, msg);
        } catch (Throwable t) {
            System.out.println("[" + tag + "] " + msg);
        }
    }

    private static void logE(String tag, String msg) {
        try {
            Log.e(tag, msg);
        } catch (Throwable t) {
            System.err.println("[" + tag + "] " + msg);
        }
    }

    private static Handler mainHandler;

    private static void postToMain(Runnable r) {
        try {
            if (mainHandler == null && Looper.getMainLooper() != null) {
                mainHandler = new Handler(Looper.getMainLooper());
            }
        } catch (Throwable ignored) {
        }
        if (mainHandler != null) {
            mainHandler.post(r);
        } else {
            r.run();
        }
    }

    private static final Map<String, String> uploadedUrlCache = new ConcurrentHashMap<>();
    private static final Map<String, String> uploadedPublicIdCache = new ConcurrentHashMap<>();

    private static final Map<String, List<UploadCallback>> inFlightCallbacks = new HashMap<>();

    public interface BitmapCallback {
        void onBitmapLoaded(android.graphics.Bitmap bitmap);
        void onError(Exception e);
    }

    public static void fetchRemoteBitmap(String remoteUrl, BitmapCallback callback) {
        if (remoteUrl == null || remoteUrl.trim().isEmpty() || !remoteUrl.startsWith("http")) {
            if (callback != null) {
                postToMain(() -> callback.onError(new IllegalArgumentException("URL inválida: " + remoteUrl)));
            }
            return;
        }

        Request req = new Request.Builder()
                .url(remoteUrl.trim())
                .header("User-Agent", "Volta-Mobile-Android")
                .build();

        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (callback != null) {
                    postToMain(() -> callback.onError(e));
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (response.isSuccessful() && body != null) {
                        byte[] bytes = body.bytes();
                        android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        if (bmp != null) {
                            if (callback != null) {
                                postToMain(() -> callback.onBitmapLoaded(bmp));
                            }
                            return;
                        }
                    }
                    if (callback != null) {
                        postToMain(() -> callback.onError(new Exception("HTTP " + response.code() + " ao buscar bitmap: " + remoteUrl)));
                    }
                } catch (Exception e) {
                    if (callback != null) {
                        postToMain(() -> callback.onError(e));
                    }
                }
            }
        });
    }

    public interface UploadCallback {
        void onSuccess(String remoteUrl, String publicId);
        void onError(Exception exception, String fallbackLocalPath);
    }

    private CloudinaryUploader() {
    }

    public static String getCachedUrl(String localFilePath) {
        if (localFilePath == null) return null;
        String direct = uploadedUrlCache.get(localFilePath);
        if (direct != null && !direct.isEmpty()) {
            return direct;
        }
        for (Map.Entry<String, String> entry : uploadedUrlCache.entrySet()) {
            if (entry.getKey().startsWith(localFilePath)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static String sanitizeEmailForPublicId(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "avatar_guest";
        }
        return "avatar_" + email.trim().toLowerCase().replaceAll("[^a-zA-Z0-9]", "_");
    }

    public static String getDeterministicProfileUrl(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        String id = sanitizeEmailForPublicId(email);
        return "https://res.cloudinary.com/" + CLOUD_NAME + "/image/upload/" + PROFILE_ASSET_FOLDER + "/" + id + ".jpg";
    }

    public static void uploadImage(Context context, File imageFile, UploadCallback callback) {
        uploadImage(context, imageFile, UPLOAD_PRESET, ASSET_FOLDER, null, callback);
    }

    public static void uploadProfileImage(Context context, File imageFile, String userEmail, UploadCallback callback) {
        String publicId = sanitizeEmailForPublicId(userEmail);
        uploadImage(context, imageFile, PROFILE_UPLOAD_PRESET, PROFILE_ASSET_FOLDER, publicId, callback);
    }

    public static void uploadProfileImage(Context context, File imageFile, UploadCallback callback) {
        String userEmail = com.aula.volta.data.local.SessionManager.email(context);
        uploadProfileImage(context, imageFile, userEmail, callback);
    }

    public static void uploadImage(Context context, File imageFile, String uploadPreset, String assetFolder, UploadCallback callback) {
        uploadImage(context, imageFile, uploadPreset, assetFolder, null, callback);
    }

    public static void uploadImage(Context context, File imageFile, String uploadPreset, String assetFolder, String publicId, UploadCallback callback) {
        if (imageFile == null || !imageFile.exists()) {
            if (callback != null) {
                postToMain(() -> callback.onError(
                        new IllegalArgumentException("Arquivo de imagem inválido"), null));
            }
            return;
        }

        final String fileKey = imageFile.getAbsolutePath() + "_" + imageFile.lastModified() + "_" + uploadPreset
                + "_" + (publicId != null ? publicId : "");

        String fromCache = uploadedUrlCache.get(fileKey);
        if (fromCache == null || fromCache.isEmpty()) {
            fromCache = getCachedUrl(imageFile.getAbsolutePath());
        }
        final String resolvedCachedUrl = fromCache;
        if (resolvedCachedUrl != null && !resolvedCachedUrl.isEmpty()) {
            final String cachedId = uploadedPublicIdCache.getOrDefault(fileKey, "");
            if (callback != null) {
                postToMain(() -> callback.onSuccess(resolvedCachedUrl, cachedId));
            }
            return;
        }

        if (CLOUD_NAME == null || CLOUD_NAME.trim().isEmpty()
                || uploadPreset == null || uploadPreset.trim().isEmpty()) {
            if (callback != null) {
                postToMain(() -> callback.onError(
                        new IllegalStateException("Cloudinary não configurado (usando imagem local)"),
                        imageFile.getAbsolutePath()));
            }
            return;
        }

        synchronized (inFlightCallbacks) {
            List<UploadCallback> waiting = inFlightCallbacks.get(fileKey);
            if (waiting != null) {
                if (callback != null) {
                    waiting.add(callback);
                }
                return;
            }
            List<UploadCallback> newList = new ArrayList<>();
            if (callback != null) {
                newList.add(callback);
            }
            inFlightCallbacks.put(fileKey, newList);
        }

        MultipartBody.Builder form = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", uploadPreset);

        if (assetFolder != null && !assetFolder.trim().isEmpty()) {
            form.addFormDataPart("folder", assetFolder.trim());
        }
        if (publicId != null && !publicId.trim().isEmpty()) {
            form.addFormDataPart("public_id", publicId.trim());
        }

        String mediaType = imageFile.getName().toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
        RequestBody fileBody = RequestBody.create(MediaType.parse(mediaType), imageFile);
        form.addFormDataPart("file", imageFile.getName(), fileBody);

        Request request = new Request.Builder()
                .url("https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload")
                .post(form.build())
                .build();

        logI(TAG, "Iniciando upload para Cloudinary (" + uploadPreset + ") do arquivo: " + imageFile.getName());

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                logE(TAG, "Erro de rede no upload Cloudinary: " + e.getMessage());
                dispatchError(fileKey, e, imageFile.getAbsolutePath());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    String respStr = body != null ? body.string() : "";
                    if (response.isSuccessful()) {
                        JsonObject json = JsonParser.parseString(respStr).getAsJsonObject();
                        String secureUrl = json.get("secure_url").getAsString();
                        String respPublicId = json.has("public_id") ? json.get("public_id").getAsString() : "";

                        uploadedUrlCache.put(fileKey, secureUrl);
                        uploadedUrlCache.put(imageFile.getAbsolutePath(), secureUrl);
                        uploadedPublicIdCache.put(fileKey, respPublicId);
                        uploadedPublicIdCache.put(imageFile.getAbsolutePath(), respPublicId);

                        logI(TAG, "Upload concluído com sucesso no Cloudinary! secure_url: " + secureUrl);
                        dispatchSuccess(fileKey, secureUrl, respPublicId);
                    } else {
                        logE(TAG, "Cloudinary retornou HTTP " + response.code() + ": " + respStr);
                        dispatchError(fileKey, new Exception("Cloudinary retornou HTTP " + response.code() + ": " + respStr), imageFile.getAbsolutePath());
                    }
                } catch (Exception e) {
                    logE(TAG, "Erro ao processar resposta do Cloudinary: " + e.getMessage());
                    dispatchError(fileKey, e, imageFile.getAbsolutePath());
                }
            }
        });
    }

    private static void dispatchSuccess(String fileKey, String secureUrl, String publicId) {
        final List<UploadCallback> callbacks;
        synchronized (inFlightCallbacks) {
            callbacks = inFlightCallbacks.remove(fileKey);
        }
        if (callbacks != null) {
            postToMain(() -> {
                for (UploadCallback cb : callbacks) {
                    if (cb != null) {
                        cb.onSuccess(secureUrl, publicId);
                    }
                }
            });
        }
    }

    private static void dispatchError(String fileKey, Exception error, String fallbackPath) {
        final List<UploadCallback> callbacks;
        synchronized (inFlightCallbacks) {
            callbacks = inFlightCallbacks.remove(fileKey);
        }
        if (callbacks != null) {
            postToMain(() -> {
                for (UploadCallback cb : callbacks) {
                    if (cb != null) {
                        cb.onError(error, fallbackPath);
                    }
                }
            });
        }
    }
}

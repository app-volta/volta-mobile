package com.aula.volta.data.upload;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Serviço de Upload de Imagens no Cloudinary (Fase 13 - Integração em Nuvem).
 *
 * Realiza upload direto (unsigned) de fotos para a nuvem do Cloudinary,
 * retornando a secure_url HTTPS para envio à IA e ao backend.
 *
 * Possui controle de idempotência (cache por arquivo + fila in-flight) para garantir
 * que a mesma foto nunca seja enviada duas vezes ao Cloudinary.
 */
public final class CloudinaryUploader {

    // Configurações oficiais do Cloudinary do projeto VOLTA
    public static String CLOUD_NAME = "kgou1air";

    // Ocorrências / Análise de IA
    public static String UPLOAD_PRESET = "AI-Analysis";
    public static String ASSET_FOLDER = "Ocorrencia/Assets/Images";

    // Fotos de Perfil (Perfil-Photo / Perfil/Assets/Image)
    public static String PROFILE_UPLOAD_PRESET = "Perfil-Photo";
    public static String PROFILE_ASSET_FOLDER = "Perfil/Assets/Image";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Cache de URLs já enviadas (filePath -> secureUrl) para evitar upload duplicado da mesma foto
    private static final Map<String, String> uploadedUrlCache = new ConcurrentHashMap<>();
    private static final Map<String, String> uploadedPublicIdCache = new ConcurrentHashMap<>();

    // Controle de uploads em andamento (filePath -> lista de callbacks aguardando)
    private static final Map<String, List<UploadCallback>> inFlightCallbacks = new HashMap<>();

    public interface UploadCallback {
        void onSuccess(String remoteUrl, String publicId);
        void onError(Exception exception, String fallbackLocalPath);
    }

    private CloudinaryUploader() {
    }

    /**
     * Retorna a URL do Cloudinary já existente para este arquivo local, se já tiver sido enviado.
     */
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

    /**
     * Faz upload da imagem no Cloudinary em background (apenas 1 vez por arquivo).
     * Se o mesmo arquivo já estiver subindo ou já tiver subido, reaproveita o resultado.
     */
    /**
     * Faz upload da imagem de ocorrência (preset AI-Analysis, pasta Ocorrencia/Assets/Images).
     */
    public static void uploadImage(Context context, File imageFile, UploadCallback callback) {
        uploadImage(context, imageFile, UPLOAD_PRESET, ASSET_FOLDER, callback);
    }

    /**
     * Faz upload da foto de perfil (preset Perfil-Photo, pasta Perfil/Assets/Image).
     */
    public static void uploadProfileImage(Context context, File imageFile, UploadCallback callback) {
        uploadImage(context, imageFile, PROFILE_UPLOAD_PRESET, PROFILE_ASSET_FOLDER, callback);
    }

    /**
     * Faz upload da imagem no Cloudinary com preset e pasta especificados.
     */
    public static void uploadImage(Context context, File imageFile, String uploadPreset, String assetFolder, UploadCallback callback) {
        if (imageFile == null || !imageFile.exists()) {
            if (callback != null) {
                mainHandler.post(() -> callback.onError(
                        new IllegalArgumentException("Arquivo de imagem inválido"), null));
            }
            return;
        }

        final String fileKey = imageFile.getAbsolutePath() + "_" + imageFile.lastModified() + "_" + uploadPreset;

        // 1. Se esta foto já foi enviada anteriormente com este preset, reaproveita a URL
        String fromCache = uploadedUrlCache.get(fileKey);
        if (fromCache == null || fromCache.isEmpty()) {
            fromCache = getCachedUrl(imageFile.getAbsolutePath());
        }
        final String resolvedCachedUrl = fromCache;
        if (resolvedCachedUrl != null && !resolvedCachedUrl.isEmpty()) {
            final String cachedId = uploadedPublicIdCache.getOrDefault(fileKey, "");
            if (callback != null) {
                mainHandler.post(() -> callback.onSuccess(resolvedCachedUrl, cachedId));
            }
            return;
        }

        // 2. Se ainda não configurado no Cloudinary, executa fallback gracioso com arquivo local
        if (CLOUD_NAME == null || CLOUD_NAME.trim().isEmpty()
                || uploadPreset == null || uploadPreset.trim().isEmpty()) {
            if (callback != null) {
                mainHandler.post(() -> callback.onError(
                        new IllegalStateException("Cloudinary não configurado (usando imagem local)"),
                        imageFile.getAbsolutePath()));
            }
            return;
        }

        // 3. Se já existe um upload em andamento para este mesmo arquivo, apenas anexa o callback
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

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                String boundary = "===" + System.currentTimeMillis() + "===";
                String LINE_FEED = "\r\n";
                URL url = new URL("https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload");

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(30000);
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setUseCaches(false);
                conn.setRequestProperty("Connection", "close");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                OutputStream outputStream = conn.getOutputStream();
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true);

                // Campo upload_preset
                writer.append("--").append(boundary).append(LINE_FEED);
                writer.append("Content-Disposition: form-data; name=\"upload_preset\"").append(LINE_FEED);
                writer.append("Content-Type: text/plain; charset=UTF-8").append(LINE_FEED);
                writer.append(LINE_FEED).append(uploadPreset).append(LINE_FEED);

                // Campo folder
                if (assetFolder != null && !assetFolder.trim().isEmpty()) {
                    writer.append("--").append(boundary).append(LINE_FEED);
                    writer.append("Content-Disposition: form-data; name=\"folder\"").append(LINE_FEED);
                    writer.append("Content-Type: text/plain; charset=UTF-8").append(LINE_FEED);
                    writer.append(LINE_FEED).append(assetFolder).append(LINE_FEED);
                }

                // Campo file (arquivo binário)
                String fileName = imageFile.getName();
                writer.append("--").append(boundary).append(LINE_FEED);
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"").append(LINE_FEED);
                writer.append("Content-Type: image/jpeg").append(LINE_FEED);
                writer.append(LINE_FEED);
                writer.flush();

                try (FileInputStream inputStream = new FileInputStream(imageFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                }

                writer.append(LINE_FEED);
                writer.append("--").append(boundary).append("--").append(LINE_FEED);
                writer.close();

                int status = conn.getResponseCode();
                if (status == HttpURLConnection.HTTP_OK) {
                    java.util.Scanner s = new java.util.Scanner(conn.getInputStream()).useDelimiter("\\A");
                    String responseStr = s.hasNext() ? s.next() : "";
                    JsonObject json = JsonParser.parseString(responseStr).getAsJsonObject();
                    String secureUrl = json.get("secure_url").getAsString();
                    String publicId = json.has("public_id") ? json.get("public_id").getAsString() : "";

                    uploadedUrlCache.put(fileKey, secureUrl);
                    uploadedUrlCache.put(imageFile.getAbsolutePath(), secureUrl);
                    uploadedPublicIdCache.put(fileKey, publicId);
                    uploadedPublicIdCache.put(imageFile.getAbsolutePath(), publicId);

                    dispatchSuccess(fileKey, secureUrl, publicId);
                } else {
                    throw new Exception("Cloudinary retornou HTTP " + status);
                }
            } catch (Exception e) {
                dispatchError(fileKey, e, fileKey);
            } finally {
                if (conn != null) {
                    conn.disconnect();
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
            mainHandler.post(() -> {
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
            mainHandler.post(() -> {
                for (UploadCallback cb : callbacks) {
                    if (cb != null) {
                        cb.onError(error, fallbackPath);
                    }
                }
            });
        }
    }
}

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Serviço de Upload de Imagens no Cloudinary (Fase 13 - Integração em Nuvem).
 *
 * Realiza upload direto (unsigned) de fotos para a nuvem do Cloudinary,
 * retornando a secure_url HTTPS para envio à IA e ao backend.
 *
 * Caso o CLOUD_NAME ou UPLOAD_PRESET não estejam configurados, o serviço
 * aciona o fallback gracioso (retornando a URI local da foto sem travar o app).
 */
public final class CloudinaryUploader {

    // Configurações do Cloudinary (preencher assim que criar a conta gratuita)
    public static String CLOUD_NAME = ""; // Ex: "volta-cloud"
    public static String UPLOAD_PRESET = ""; // Ex: "volta_unsigned"

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface UploadCallback {
        void onSuccess(String remoteUrl, String publicId);
        void onError(Exception exception, String fallbackLocalPath);
    }

    private CloudinaryUploader() {
    }

    /**
     * Faz upload da imagem no Cloudinary em background.
     * Se não configurado, chama o fallback imediatamente.
     */
    public static void uploadImage(Context context, File imageFile, UploadCallback callback) {
        if (imageFile == null || !imageFile.exists()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Arquivo de imagem inválido"), null);
            }
            return;
        }

        // Se ainda não configurado no Cloudinary, executa fallback gracioso com arquivo local
        if (CLOUD_NAME == null || CLOUD_NAME.trim().isEmpty()
                || UPLOAD_PRESET == null || UPLOAD_PRESET.trim().isEmpty()) {
            if (callback != null) {
                mainHandler.post(() -> callback.onError(
                        new IllegalStateException("Cloudinary não configurado (usando imagem local)"),
                        imageFile.getAbsolutePath()));
            }
            return;
        }

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                String boundary = "===" + System.currentTimeMillis() + "===";
                String LINE_FEED = "\r\n";
                URL url = new URL("https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload");

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setUseCaches(false);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                OutputStream outputStream = conn.getOutputStream();
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true);

                // Campo upload_preset
                writer.append("--").append(boundary).append(LINE_FEED);
                writer.append("Content-Disposition: form-data; name=\"upload_preset\"").append(LINE_FEED);
                writer.append("Content-Type: text/plain; charset=UTF-8").append(LINE_FEED);
                writer.append(LINE_FEED).append(UPLOAD_PRESET).append(LINE_FEED);

                // Campo file (arquivo binário)
                String fileName = imageFile.getName();
                writer.append("--").append(boundary).append(LINE_FEED);
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"").append(LINE_FEED);
                writer.append("Content-Type: image/jpeg").append(LINE_FEED);
                writer.append(LINE_FEED);
                writer.flush();

                FileInputStream inputStream = new FileInputStream(imageFile);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
                inputStream.close();

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

                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onSuccess(secureUrl, publicId);
                        }
                    });
                } else {
                    throw new Exception("Cloudinary retornou HTTP " + status);
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(e, imageFile.getAbsolutePath());
                    }
                });
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }
}

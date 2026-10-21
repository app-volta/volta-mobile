package com.aula.volta.data.mock;

import android.content.Context;

import com.aula.volta.data.local.PrefsHelper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Mock removível (Fase 0). Responde path → JSON de app/src/main/assets/mock/
 * ou do cache local de ocorrências criadas na sessão.
 */
public class MockInterceptor implements Interceptor {

    private final Context context;

    public MockInterceptor(Context context) {
        this.context = context;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        String path = request.url().encodedPath();
        String method = request.method();

        // Se for GET /occurrences/{id} e existir detalhe salvo localmente (com foto/Cloudinary URL), retorna o salvo
        if (path.matches("/occurrences/[^/]+") && method.equals("GET")) {
            String id = path.substring("/occurrences/".length());
            String cached = PrefsHelper.getJson(context, "cache_occurrence_detail", id);
            if (cached != null && !cached.isEmpty()) {
                return jsonResponse(request, 200, cached);
            }
        }

        // Se for POST /occurrences, gera um ID único para cada ocorrência criada
        if (path.equals("/occurrences") && method.equals("POST")) {
            String uniqueId = String.valueOf(10000 + (System.currentTimeMillis() % 90000));
            String createdJson = "{\"id\":\"" + uniqueId + "\",\"status\":\"EM_ANALISE\"}";
            return jsonResponse(request, 200, createdJson);
        }

        String asset = assetFor(method, path);
        if (asset == null) {
            return jsonResponse(request, 404, "{\"erro\":\"mock nao encontrado\"}");
        }

        try {
            Thread.sleep(asset.equals("ai_analysis.json") ? 1200 : 300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return jsonResponse(request, 200, readAsset("mock/" + asset));
    }

    /** Mapeia método+path aos contratos REST do PLANO (item 3). */
    private String assetFor(String method, String path) {
        if (path.equals("/occurrences") && method.equals("GET")) {
            return "occurrences.json";
        }
        if (path.matches("/occurrences/[^/]+/analysis") && method.equals("POST")) {
            return "ai_analysis.json";
        }
        if (path.matches("/occurrences/[^/]+") && method.equals("GET")) {
            return "occurrence_detail.json";
        }
        if (path.equals("/occurrences") && method.equals("POST")) {
            return "occurrence_created.json";
        }
        if (path.equals("/notifications") && method.equals("GET")) {
            return "notifications.json";
        }
        if (path.equals("/cooperatives/recommended") && method.equals("GET")) {
            return "cooperatives.json";
        }
        if (path.equals("/reports/summary") && method.equals("GET")) {
            return "reports.json";
        }
        return null;
    }

    private String readAsset(String name) throws IOException {
        try (InputStream in = context.getAssets().open(name)) {
            int size = in.available();
            byte[] buffer = new byte[size];
            int read = 0;
            while (read < size) {
                int n = in.read(buffer, read, size - read);
                if (n == -1) {
                    break;
                }
                read += n;
            }
            return new String(buffer, 0, read, StandardCharsets.UTF_8);
        }
    }

    private Response jsonResponse(Request request, int code, String json) {
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message(code == 200 ? "OK" : "Mock nao encontrado")
                .body(ResponseBody.create(json, MediaType.get("application/json")))
                .build();
    }
}

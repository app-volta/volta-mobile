package com.aula.volta;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.recyclerview.widget.RecyclerView;

import com.aula.volta.data.model.Occurrence;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Adapter dos cards de ocorrência (Fase 2). Mesmo visual do Figma, 100% orientado a dados:
 * badge por prioridade, ícone/fundo por material ou miniatura real da foto (Cloudinary / local).
 * Reutilizado na HomeFragment e OccurrencesFragment.
 */
public class OccurrenceAdapter extends RecyclerView.Adapter<OccurrenceAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Occurrence occurrence);
    }

    private static final ExecutorService imageExecutor = Executors.newFixedThreadPool(3);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Bitmap> memoryCache;

    static {
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8; // 1/8 da memória disponível
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    private final List<Occurrence> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public OccurrenceAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<Occurrence> occurrences) {
        items.clear();
        if (occurrences != null) {
            items.addAll(occurrences);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_occurrence_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Occurrence occurrence = items.get(position);

        holder.title.setText(occurrence.getTitulo());
        holder.subtitle.setText(occurrence.getSetor() + " · " + occurrence.getTempoRelativo());

        applyPriorityStyle(holder);
        applyMaterialIcon(holder, occurrence);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(occurrence);
            }
        });
    }

    private void applyPriorityStyle(ViewHolder holder) {
        Occurrence occurrence = items.get(holder.getBindingAdapterPosition());
        String priority = occurrence.getPrioridade() == null ? "" : occurrence.getPrioridade();

        if (priority.equalsIgnoreCase("ALTA") || priority.equalsIgnoreCase("HIGH")) {
            holder.badge.setBackgroundResource(R.drawable.bg_badge_alta);
            holder.badge.setTextColor(holder.itemView.getContext()
                    .getColor(R.color.priority_high_text));
            holder.badge.setText(R.string.priority_alta);
        } else if (priority.equalsIgnoreCase("BAIXA") || priority.equalsIgnoreCase("LOW")) {
            holder.badge.setBackgroundResource(R.drawable.bg_badge_baixa);
            holder.badge.setTextColor(holder.itemView.getContext()
                    .getColor(R.color.priority_low_text));
            holder.badge.setText(R.string.priority_baixa);
        } else {
            holder.badge.setBackgroundResource(R.drawable.bg_badge_media);
            holder.badge.setTextColor(holder.itemView.getContext()
                    .getColor(R.color.priority_medium_text));
            holder.badge.setText(R.string.priority_media);
        }
    }

    /**
     * Exibe a miniatura real da ocorrência (local ou da nuvem Cloudinary) com cantos arredondados (r=14dp),
     * ou faz fallback para o ícone vetorial do tipo de resíduo.
     */
    private void applyVisualMediaOrMaterial(ViewHolder holder, Occurrence occurrence) {
        final float cornerRadiusPx = 14f * holder.itemView.getResources().getDisplayMetrics().density;
        String localPath = occurrence.getPhotoPath();
        String remoteUrl = occurrence.getPhotoUrl();

        // 1. Tenta carregar imagem local se existir no dispositivo
        if (localPath != null && !localPath.isEmpty()) {
            File localFile = new File(localPath);
            if (localFile.exists()) {
                Bitmap cached = memoryCache.get(localPath);
                if (cached != null) {
                    showThumbnail(holder, cached, cornerRadiusPx);
                    return;
                }
                Bitmap bmp = decodeSampledBitmap(localFile.getAbsolutePath(), 120, 120);
                if (bmp != null) {
                    memoryCache.put(localPath, bmp);
                    showThumbnail(holder, bmp, cornerRadiusPx);
                    return;
                }
            }
        }

        // 2. Tenta carregar a imagem remota do Cloudinary / API
        if (remoteUrl != null && remoteUrl.startsWith("http")) {
            Bitmap cached = memoryCache.get(remoteUrl);
            if (cached != null) {
                showThumbnail(holder, cached, cornerRadiusPx);
                return;
            }

            // Exibe ícone vetorial temporário enquanto faz o download em background
            applyMaterialFallback(holder, occurrence);

            holder.thumbnail.setTag(remoteUrl);
            imageExecutor.execute(() -> {
                try {
                    URL url = new URL(remoteUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);
                    conn.connect();
                    try (InputStream is = conn.getInputStream()) {
                        Bitmap bmp = BitmapFactory.decodeStream(is);
                        if (bmp != null) {
                            memoryCache.put(remoteUrl, bmp);
                            mainHandler.post(() -> {
                                if (remoteUrl.equals(holder.thumbnail.getTag())) {
                                    showThumbnail(holder, bmp, cornerRadiusPx);
                                }
                            });
                        }
                    }
                } catch (Exception ignored) {
                }
            });
            return;
        }

        // 3. Fallback: ícone vetorial temático do material
        applyMaterialFallback(holder, occurrence);
    }

    private void showThumbnail(ViewHolder holder, Bitmap bmp, float cornerRadiusPx) {
        RoundedBitmapDrawable rounded = RoundedBitmapDrawableFactory.create(holder.itemView.getResources(), bmp);
        rounded.setCornerRadius(cornerRadiusPx);
        holder.thumbnail.setImageDrawable(rounded);
        holder.thumbnail.setVisibility(View.VISIBLE);
        holder.icon.setVisibility(View.GONE);
        holder.iconContainer.setBackground(null);
    }

    /**
     * Exibe o icone tematico oficial do tipo de residuo (Figma 790:1706),
     * garantindo que nas ocorrencias recentes seja sempre usado o icone e nao a foto da ocorrencia.
     */
    private void applyMaterialIcon(ViewHolder holder, Occurrence occurrence) {
        if (holder.thumbnail != null) {
            holder.thumbnail.setVisibility(View.GONE);
        }
        if (holder.icon != null) {
            holder.icon.setVisibility(View.VISIBLE);
        }

        String titulo = (occurrence.getTitulo() != null ? occurrence.getTitulo() : "")
                + " " + (occurrence.getMaterial() != null ? occurrence.getMaterial() : "");
        titulo = titulo.toLowerCase(Locale.ROOT);

        if (titulo.contains("plast") || titulo.contains("filme") || titulo.contains("pet")) {
            holder.iconContainer.setBackgroundResource(R.drawable.bg_occurrence_icon_blue);
            holder.icon.setImageResource(R.drawable.ic_occurrence_waste_blue);
        } else if (titulo.contains("metal") || titulo.contains("sucata")
                || titulo.contains("ferro") || titulo.contains("alum") || titulo.contains("aco") || titulo.contains("ço")) {
            holder.iconContainer.setBackgroundResource(R.drawable.bg_occurrence_icon_purple);
            holder.icon.setImageResource(R.drawable.ic_occurrence_waste_purple);
        } else {
            holder.iconContainer.setBackgroundResource(R.drawable.bg_occurrence_icon);
            holder.icon.setImageResource(R.drawable.ic_occurrence_waste);
        }
    }

    private void applyMaterialFallback(ViewHolder holder, Occurrence occurrence) {
        applyMaterialIcon(holder, occurrence);
    }

    private static Bitmap decodeSampledBitmap(String path, int reqWidth, int reqHeight) {
        try {
            int maxDimen = Math.max(reqWidth, reqHeight);
            return com.aula.volta.util.ImageUtils.decodeFileWithExifRotation(path, maxDimen > 0 ? maxDimen : 256);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final FrameLayout iconContainer;
        final ImageView thumbnail;
        final ImageView icon;
        final TextView title;
        final TextView subtitle;
        final TextView badge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconContainer = itemView.findViewById(R.id.cardIconContainer);
            thumbnail = itemView.findViewById(R.id.cardThumbnail);
            icon = itemView.findViewById(R.id.cardIcon);
            title = itemView.findViewById(R.id.cardTitle);
            subtitle = itemView.findViewById(R.id.cardSubtitle);
            badge = itemView.findViewById(R.id.cardBadge);
        }
    }
}

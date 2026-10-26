package com.aula.volta.ui.dialog;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.aula.volta.R;
import com.aula.volta.ui.widget.ZoomableImageView;
import com.aula.volta.util.BottomSheetImmersiveHelper;
import com.aula.volta.util.ImageUtils;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Modal Imersivo de Visualização e Zoom de Imagens (Lightbox) — Design System VOLTA.
 * Exibe a foto da ocorrência em tela cheia com suporte a Pinch-to-Zoom e Double-Tap.
 */
public class ImagePreviewDialog extends DialogFragment {

    private static final String TAG = "ImagePreviewDialog";
    private static final String ARG_LOCAL_PATH = "arg_local_path";
    private static final String ARG_REMOTE_URL = "arg_remote_url";
    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_SUBTITLE = "arg_subtitle";

    private static Bitmap transientBitmap;

    private ZoomableImageView imgPreviewZoom;
    private ProgressBar loadingPreview;
    private TextView tvPreviewTitle;
    private TextView tvCardMaterial;
    private TextView tvCardLocation;
    private View cardPreviewDetails;

    public static void show(FragmentManager fm, @Nullable String localPath,
                            @Nullable String title, @Nullable String subtitle) {
        show(fm, localPath, null, title, subtitle);
    }

    public static void show(FragmentManager fm, @Nullable String localPath, @Nullable String remoteUrl,
                            @Nullable String title, @Nullable String subtitle) {
        if (fm == null || fm.isStateSaved()) {
            return;
        }
        ImagePreviewDialog dialog = new ImagePreviewDialog();
        Bundle args = new Bundle();
        args.putString(ARG_LOCAL_PATH, localPath);
        args.putString(ARG_REMOTE_URL, remoteUrl);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_SUBTITLE, subtitle);
        dialog.setArguments(args);
        dialog.show(fm, TAG);
    }

    public static void show(FragmentManager fm, @NonNull Bitmap bitmap,
                            @Nullable String title, @Nullable String subtitle) {
        if (fm == null || fm.isStateSaved()) {
            return;
        }
        transientBitmap = bitmap;
        ImagePreviewDialog dialog = new ImagePreviewDialog();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_SUBTITLE, subtitle);
        dialog.setArguments(args);
        dialog.show(fm, TAG);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            BottomSheetImmersiveHelper.applyImmersiveWindow(window);
        }
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_image_preview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imgPreviewZoom = view.findViewById(R.id.imgPreviewZoom);
        loadingPreview = view.findViewById(R.id.loadingPreview);
        tvPreviewTitle = view.findViewById(R.id.tvPreviewTitle);
        tvCardMaterial = view.findViewById(R.id.tvCardMaterial);
        tvCardLocation = view.findViewById(R.id.tvCardLocation);
        cardPreviewDetails = view.findViewById(R.id.cardPreviewDetails);

        View btnClose = view.findViewById(R.id.btnCloseImagePreview);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }

        Bundle args = getArguments();
        String localPath = args != null ? args.getString(ARG_LOCAL_PATH) : null;
        String remoteUrl = args != null ? args.getString(ARG_REMOTE_URL) : null;
        String title = args != null ? args.getString(ARG_TITLE) : null;
        String subtitle = args != null ? args.getString(ARG_SUBTITLE) : null;

        if (title != null && !title.isEmpty()) {
            tvPreviewTitle.setText(title);
            tvCardMaterial.setText(title);
        } else {
            tvPreviewTitle.setText("Foto da Ocorrência");
            tvCardMaterial.setText("Resíduo Registrado");
        }

        if (subtitle != null && !subtitle.isEmpty()) {
            tvCardLocation.setText(subtitle);
            cardPreviewDetails.setVisibility(View.VISIBLE);
        } else {
            cardPreviewDetails.setVisibility(View.GONE);
        }

        loadImage(localPath, remoteUrl);
    }

    private void loadImage(@Nullable String localPath, @Nullable String remoteUrl) {
        if (transientBitmap != null) {
            imgPreviewZoom.setImageBitmap(transientBitmap);
            transientBitmap = null;
            return;
        }

        if (localPath != null && !localPath.isEmpty()) {
            File file = new File(localPath);
            if (file.exists()) {
                Bitmap bmp = ImageUtils.decodeFileWithExifRotation(file.getAbsolutePath(), 1920);
                if (bmp != null) {
                    imgPreviewZoom.setImageBitmap(bmp);
                    return;
                }
            }
        }

        if (remoteUrl != null && remoteUrl.startsWith("http")) {
            loadingPreview.setVisibility(View.VISIBLE);
            new Thread(() -> {
                try {
                    URL url = new URL(remoteUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(12000);
                    conn.setReadTimeout(12000);
                    conn.setDoInput(true);
                    conn.connect();
                    try (InputStream is = conn.getInputStream()) {
                        Bitmap bmp = BitmapFactory.decodeStream(is);
                        if (bmp != null && isAdded()) {
                            imgPreviewZoom.post(() -> {
                                if (isAdded()) {
                                    loadingPreview.setVisibility(View.GONE);
                                    imgPreviewZoom.setImageBitmap(bmp);
                                }
                            });
                            return;
                        }
                    }
                } catch (Exception ignored) {
                }
                if (isAdded()) {
                    imgPreviewZoom.post(() -> {
                        if (isAdded()) {
                            loadingPreview.setVisibility(View.GONE);
                            imgPreviewZoom.setImageResource(R.drawable.img_caixas_papelao);
                        }
                    });
                }
            }).start();
            return;
        }

        imgPreviewZoom.setImageResource(R.drawable.img_caixas_papelao);
    }
}

package com.aula.volta.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aula.volta.R;
import com.aula.volta.util.BottomSheetImmersiveHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * BottomSheetDialogFragment para seleção da origem da foto de perfil:
 * - Tirar foto com a câmera
 * - Escolher da galeria
 * - Remover foto atual (quando aplicável)
 */
public class ProfilePhotoSheet extends BottomSheetDialogFragment {

    public interface OnPhotoOptionSelectedListener {
        void onCameraSelected();
        void onGallerySelected();
        void onRemoveSelected();
    }

    private static final String ARG_HAS_PHOTO = "arg_has_photo";
    private boolean hasPhoto = false;
    private OnPhotoOptionSelectedListener listener;

    public static ProfilePhotoSheet newInstance(boolean hasPhoto) {
        ProfilePhotoSheet sheet = new ProfilePhotoSheet();
        Bundle args = new Bundle();
        args.putBoolean(ARG_HAS_PHOTO, hasPhoto);
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnPhotoOptionSelectedListener(OnPhotoOptionSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        BottomSheetImmersiveHelper.configureBottomSheet(dialog);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_profile_photo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            hasPhoto = getArguments().getBoolean(ARG_HAS_PHOTO, false);
        }

        View btnCamera = view.findViewById(R.id.btnOptionCamera);
        View btnGallery = view.findViewById(R.id.btnOptionGallery);
        View btnRemove = view.findViewById(R.id.btnOptionRemove);
        View btnCancel = view.findViewById(R.id.btnOptionCancel);

        if (btnCamera != null) {
            btnCamera.setOnClickListener(v -> {
                dismiss();
                if (listener != null) {
                    listener.onCameraSelected();
                }
            });
        }

        if (btnGallery != null) {
            btnGallery.setOnClickListener(v -> {
                dismiss();
                if (listener != null) {
                    listener.onGallerySelected();
                }
            });
        }

        if (btnRemove != null) {
            btnRemove.setVisibility(hasPhoto ? View.VISIBLE : View.GONE);
            btnRemove.setOnClickListener(v -> {
                dismiss();
                if (listener != null) {
                    listener.onRemoveSelected();
                }
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dismiss());
        }
    }
}

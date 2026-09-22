package com.aula.volta;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

public class NewOccurrenceFragment extends Fragment {

    public NewOccurrenceFragment() {
        // Construtor vazio obrigat?rio
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_occurrence, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // _________________________________________________________________________________________
        //            NOVA OCORR?NCIA
        // _________________________________________________________________________________________

        MaterialButton btnCamera = view.findViewById(R.id.btnCaptureCamera);
        MaterialButton btnGallery = view.findViewById(R.id.btnOpenGallery);
        MaterialButton btnSend = view.findViewById(R.id.btnSendAnalysis);
    }
}

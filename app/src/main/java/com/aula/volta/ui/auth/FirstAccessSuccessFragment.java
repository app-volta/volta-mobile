package com.aula.volta.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import android.content.Context;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.aula.volta.R;
import com.aula.volta.data.local.SessionManager;

public class FirstAccessSuccessFragment extends Fragment {

    public FirstAccessSuccessFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_first_access_success, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Context appContext = requireContext().getApplicationContext();

        String fullName = getArguments() != null ? getArguments().getString("name", "") : "";
        if (fullName == null || fullName.trim().isEmpty()) {
            fullName = SessionManager.getName(appContext);
        }
        String firstName = extractFirstName(fullName);

        TextView tvTitle = view.findViewById(R.id.tvFirstAccessSuccessTitle);
        if (tvTitle != null) {
            if (!firstName.isEmpty()) {
                tvTitle.setText(getString(R.string.auth_first_access_success_title, firstName));
            } else {
                tvTitle.setText(R.string.auth_first_access_success_title_default);
            }
        }

        View btnStart = view.findViewById(R.id.btnFirstAccessStart);
        if (btnStart != null) {
            btnStart.setOnClickListener(v -> completeAndEnter(appContext));
        }

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        completeAndEnter(appContext);
                    }
                });
    }

    private void completeAndEnter(Context appContext) {
        SessionManager.setFirstLoginPending(appContext, false);
        if (isAdded() && getActivity() instanceof AuthActivity) {
            ((AuthActivity) requireActivity()).finishLogin();
        }
    }

    private String extractFirstName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : fullName;
    }
}

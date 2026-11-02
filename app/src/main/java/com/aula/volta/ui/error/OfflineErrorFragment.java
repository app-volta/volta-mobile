package com.aula.volta.ui.error;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.aula.volta.R;
import com.aula.volta.util.NetworkUtils;
import com.google.android.material.button.MaterialButton;

public class OfflineErrorFragment extends Fragment {

    public static final String ARG_ERROR_TYPE = "errorType";
    public static final String ARG_BADGE = "badge";
    public static final String ARG_TITLE = "title";
    public static final String ARG_MESSAGE = "message";
    public static final String ARG_INFO_TITLE = "infoTitle";
    public static final String ARG_INFO_BODY = "infoBody";

    public static final String TYPE_OFFLINE = "OFFLINE";
    public static final String TYPE_SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
    public static final String TYPE_SUBMISSION_FAILED = "SUBMISSION_FAILED";

    public static final String REQUEST_KEY = "error_retry_key";
    public static final String RESULT_ONLINE = "isOnline";

    private String errorType = TYPE_OFFLINE;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_offline_error, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton btnBack = view.findViewById(R.id.btnErrorBack);
        ImageView ivMascot = view.findViewById(R.id.ivErrorMascot);
        TextView tvBadge = view.findViewById(R.id.tvErrorBadge);
        TextView tvTitle = view.findViewById(R.id.tvErrorTitle);
        TextView tvMessage = view.findViewById(R.id.tvErrorMessage);
        View cardInfo = view.findViewById(R.id.cardErrorInfo);
        TextView tvInfoTitle = view.findViewById(R.id.tvErrorInfoTitle);
        TextView tvInfoBody = view.findViewById(R.id.tvErrorInfoBody);
        MaterialButton btnRetry = view.findViewById(R.id.btnErrorRetry);
        MaterialButton btnSecondary = view.findViewById(R.id.btnErrorSecondary);

        Bundle args = getArguments();
        if (args != null) {
            errorType = args.getString(ARG_ERROR_TYPE, TYPE_OFFLINE);
            String customBadge = args.getString(ARG_BADGE);
            String customTitle = args.getString(ARG_TITLE);
            String customMessage = args.getString(ARG_MESSAGE);
            String customInfoTitle = args.getString(ARG_INFO_TITLE);
            String customInfoBody = args.getString(ARG_INFO_BODY);

            if (customBadge != null && !customBadge.isEmpty()) {
                tvBadge.setText(customBadge);
            }
            if (customTitle != null && !customTitle.isEmpty()) {
                tvTitle.setText(customTitle);
            }
            if (customMessage != null && !customMessage.isEmpty()) {
                tvMessage.setText(customMessage);
            }
            if (customInfoTitle != null && !customInfoTitle.isEmpty()) {
                tvInfoTitle.setText(customInfoTitle);
            }
            if (customInfoBody != null && !customInfoBody.isEmpty()) {
                tvInfoBody.setText(customInfoBody);
            }
        }

        if (TYPE_SERVICE_UNAVAILABLE.equalsIgnoreCase(errorType)) {
            ivMascot.setImageResource(R.drawable.img_mascot_service_error);
            if (args == null || args.getString(ARG_BADGE) == null) {
                tvBadge.setText("Fora do ar");
            }
            if (args == null || args.getString(ARG_TITLE) == null) {
                tvTitle.setText("O VOLTA não respondeu");
            }
            if (args == null || args.getString(ARG_MESSAGE) == null) {
                tvMessage.setText("Não é nada com você: o serviço está fora do ar ou demorou demais pra responder. Tente de novo em alguns minutos.");
            }
            if (args == null || args.getString(ARG_INFO_TITLE) == null) {
                tvInfoTitle.setText("Instabilidade no servidor");
                tvInfoBody.setText("Nossos serviços estão em manutenção ou com alto tempo de resposta. Seus dados continuam salvos.");
            }
        } else if (TYPE_SUBMISSION_FAILED.equalsIgnoreCase(errorType)) {
            ivMascot.setImageResource(R.drawable.img_mascot_offline);
            if (args == null || args.getString(ARG_BADGE) == null) {
                tvBadge.setText("Você está offline");
            }
            if (args == null || args.getString(ARG_TITLE) == null) {
                tvTitle.setText("Não conseguimos enviar");
            }
            if (args == null || args.getString(ARG_MESSAGE) == null) {
                tvMessage.setText("Sua foto e os dados continuam aqui. Confira a conexão e tente de novo.");
            }
            if (args == null || args.getString(ARG_INFO_TITLE) == null) {
                tvInfoTitle.setText("Registro retido com segurança");
                tvInfoBody.setText("Nenhum dado digitado foi perdido. Quando a conexão for restabelecida, a ocorrência será enviada à IA.");
            }
        } else {

            ivMascot.setImageResource(R.drawable.img_mascot_offline);
        }

        btnBack.setOnClickListener(v -> navigateBack(view));
        btnSecondary.setOnClickListener(v -> navigateBack(view));

        btnRetry.setOnClickListener(v -> {
            boolean online = NetworkUtils.isOnline(requireContext());
            if (online) {
                Toast.makeText(requireContext(), "Conexão restabelecida!", Toast.LENGTH_SHORT).show();
                Bundle res = new Bundle();
                res.putBoolean(RESULT_ONLINE, true);
                res.putString(ARG_ERROR_TYPE, errorType);
                getParentFragmentManager().setFragmentResult(REQUEST_KEY, res);
                navigateBack(view);
            } else {
                Toast.makeText(requireContext(), "Ainda sem conexão. Verifique o Wi-Fi ou dados móveis.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateBack(View v) {
        try {
            if (!Navigation.findNavController(v).popBackStack()) {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            }
        } catch (Exception e) {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        }
    }
}

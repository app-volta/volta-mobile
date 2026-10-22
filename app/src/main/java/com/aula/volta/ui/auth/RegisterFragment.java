package com.aula.volta.ui.auth;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.aula.volta.R;
import com.aula.volta.data.local.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Cadastro — Figma 790:1007.
 * 4 cards com chips coloridos, seleção de setor industrial,
 * aceite de termos e validações completas (mínimo de 8 caracteres de senha).
 */
public class RegisterFragment extends Fragment {

    private String selectedSector = "";
    private boolean passwordVisible = false;

    public RegisterFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Restaura estado em caso de rotação de tela
        TextView tvSector = view.findViewById(R.id.tvRegisterSector);
        if (savedInstanceState != null) {
            selectedSector = savedInstanceState.getString("selected_sector", "");
            if (!selectedSector.isEmpty() && tvSector != null) {
                tvSector.setText(selectedSector);
                tvSector.setTextColor(requireContext().getColor(R.color.vf_text_primary));
            }
            passwordVisible = savedInstanceState.getBoolean("password_visible", false);
        }

        // Botão Fechar circular superior direito e link Rodapé "Entrar"
        View.OnClickListener goBack = v -> {
            if (!Navigation.findNavController(v).popBackStack(R.id.auth_login, false)) {
                Navigation.findNavController(v).navigateUp();
            }
        };
        View btnClose = view.findViewById(R.id.btnCloseRegister);
        if (btnClose != null) {
            btnClose.setOnClickListener(goBack);
        }
        View btnGoLogin = view.findViewById(R.id.btnGoLogin);
        if (btnGoLogin != null) {
            btnGoLogin.setOnClickListener(goBack);
        }

        // Card Setor e Diálogo de Seleção
        View cardSector = view.findViewById(R.id.cardRegisterSector);
        View.OnClickListener openSectorDialog = v -> {
            String[] sectors = getResources().getStringArray(R.array.occurrence_sector_options);
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.auth_register_sector_hint)
                    .setItems(sectors, (dialog, which) -> {
                        selectedSector = sectors[which];
                        if (tvSector != null) {
                            tvSector.setText(selectedSector);
                            tvSector.setTextColor(requireContext().getColor(R.color.vf_text_primary));
                        }
                    })
                    .show();
        };
        if (cardSector != null) {
            cardSector.setOnClickListener(openSectorDialog);
        }

        // Toggle Visibilidade de Senha (preservando Typeface/Fonte)
        EditText etPassword = view.findViewById(R.id.etRegisterPassword);
        ImageView btnToggle = view.findViewById(R.id.btnToggleRegisterPassword);
        if (etPassword != null && btnToggle != null) {
            applyPasswordVisibility(etPassword, btnToggle, passwordVisible);
            btnToggle.setOnClickListener(v -> {
                passwordVisible = !passwordVisible;
                applyPasswordVisibility(etPassword, btnToggle, passwordVisible);
            });
        }

        // Checkbox e Texto dos Termos
        CheckBox cbTerms = view.findViewById(R.id.cbTerms);
        TextView tvTerms = view.findViewById(R.id.tvTerms);
        View rowTerms = view.findViewById(R.id.rowTerms);
        if (tvTerms != null) {
            tvTerms.setText(Html.fromHtml(getString(R.string.auth_register_terms), Html.FROM_HTML_MODE_LEGACY));
        }
        if (rowTerms != null) {
            rowTerms.setOnClickListener(v -> {
                if (cbTerms != null) {
                    cbTerms.setChecked(!cbTerms.isChecked());
                }
            });
        }

        // Submissão do Cadastro com Validações
        view.findViewById(R.id.btnCreateAccount).setOnClickListener(v -> {
            String name = textOf(view, R.id.etRegisterName).trim();
            String email = textOf(view, R.id.etRegisterEmail).trim();
            String password = textOf(view, R.id.etRegisterPassword);

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), R.string.auth_name_hint, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!AuthValidator.isValidEmail(email)) {
                Toast.makeText(requireContext(), R.string.auth_invalid, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!AuthValidator.isValidSector(selectedSector)) {
                Toast.makeText(requireContext(), R.string.auth_register_sector_required, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!AuthValidator.isValidPassword(password)) {
                Toast.makeText(requireContext(), R.string.auth_register_pw_min, Toast.LENGTH_SHORT).show();
                return;
            }
            if (cbTerms != null && !cbTerms.isChecked()) {
                Toast.makeText(requireContext(), R.string.auth_register_terms_required, Toast.LENGTH_SHORT).show();
                return;
            }

            // Persiste o setor localmente na sessão
            SessionManager.saveSector(requireContext(), selectedSector);

            Bundle args = new Bundle();
            args.putString("email", email);
            args.putString("name", name);
            args.putString("sector", selectedSector);
            args.putString("password", password);
            Navigation.findNavController(v).navigate(R.id.auth_confirm, args);
        });
    }

    private void applyPasswordVisibility(EditText etPassword, ImageView btnToggle, boolean visible) {
        Typeface tf = etPassword.getTypeface();
        etPassword.setInputType(visible
                ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etPassword.setTypeface(tf);
        etPassword.setSelection(etPassword.getText().length());
        btnToggle.setColorFilter(requireContext().getColor(
                visible ? R.color.login_btn_text_green : R.color.vf_text_secondary));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("selected_sector", selectedSector);
        outState.putBoolean("password_visible", passwordVisible);
    }

    private String textOf(View root, int id) {
        EditText et = root.findViewById(id);
        return et == null ? "" : et.getText().toString();
    }
}

package com.aula.volta.ui.auth;

import com.aula.volta.R;

/**
 * Validador e lógica de regras de negócio para o fluxo de autenticação (Figma 790:954).
 */
public final class AuthValidator {

    private AuthValidator() {
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 8;
    }

    public static boolean isValidEmail(String email) {
        return email != null && !email.trim().isEmpty() && email.contains("@");
    }

    public static boolean isValidSector(String sector) {
        return sector != null && !sector.trim().isEmpty();
    }

    public static boolean isPasswordMatch(String first, String second) {
        return isValidPassword(first) && first.equals(second);
    }

    /**
     * Medidor de força de senha:
     * 0 caracteres: Nível 0 (vazio)
     * 1..3 caracteres: Nível 1 (Fraca)
     * 4..5 caracteres: Nível 2 (Média)
     * 6..7 caracteres: Nível 3 (Boa)
     * 8+ caracteres: Nível 4 (Forte)
     */
    public static int calculateStrengthLevel(int length) {
        if (length >= 8) {
            return 4;
        } else if (length >= 6) {
            return 3;
        } else if (length >= 4) {
            return 2;
        } else {
            return length == 0 ? 0 : 1;
        }
    }

    public static int getStrengthLabelRes(int level) {
        switch (level) {
            case 4:
                return R.string.auth_pw_strong;
            case 3:
                return R.string.auth_pw_good;
            case 2:
                return R.string.auth_pw_fair;
            case 1:
            default:
                return R.string.auth_pw_weak;
        }
    }

    public static int getStrengthColorRes(int level) {
        switch (level) {
            case 4:
                return R.color.status_aprovada_text;
            case 3:
                return R.color.login_btn_text_green;
            case 2:
                return R.color.priority_medium_text;
            case 1:
            default:
                return R.color.priority_high_text;
        }
    }
}

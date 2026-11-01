package com.aula.volta.ui.auth;

import com.aula.volta.R;

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

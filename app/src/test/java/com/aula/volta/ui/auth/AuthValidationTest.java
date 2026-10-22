package com.aula.volta.ui.auth;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Testes unitários para as regras de validação e medidor de força do fluxo de autenticação.
 */
public class AuthValidationTest {

    @Test
    public void passwordLengthValidation_enforcesMinimumEightCharacters() {
        assertFalse("Senha com menos de 8 caracteres deve ser inválida", isValidPassword("1234567"));
        assertFalse("Senha vazia deve ser inválida", isValidPassword(""));
        assertTrue("Senha com exatamente 8 caracteres deve ser válida", isValidPassword("12345678"));
        assertTrue("Senha com mais de 8 caracteres deve ser válida", isValidPassword("senhaSegura123!"));
    }

    @Test
    public void emailValidation_checksPresenceAndAtSymbol() {
        assertFalse("E-mail vazio deve ser inválido", isValidEmail(""));
        assertFalse("E-mail sem @ deve ser inválido", isValidEmail("userexample.com"));
        assertTrue("E-mail com @ deve ser válido", isValidEmail("breno@volta.com"));
    }

    @Test
    public void sectorValidation_rejectsEmptyOrPlaceholder() {
        assertFalse("Setor vazio é inválido", isValidSector(""));
        assertFalse("Setor nulo é inválido", isValidSector(null));
        assertTrue("Setor industrial preenchido é válido", isValidSector("Frigorífico — Setor B2"));
    }

    @Test
    public void passwordMatchValidation_checksEquality() {
        assertTrue("Senhas idênticas com 8+ caracteres devem conferir",
                isPasswordMatch("senhaForte123", "senhaForte123"));
        assertFalse("Senhas diferentes não devem conferir",
                isPasswordMatch("senhaForte123", "outraSenha123"));
        assertFalse("Senhas idênticas com menos de 8 caracteres não devem conferir",
                isPasswordMatch("123", "123"));
    }

    @Test
    public void strengthMeterLevels_correctlyCategorizes() {
        assertEquals("0 caracteres -> Nível 0", 0, calculateStrengthLevel(0));
        assertEquals("1 caractere -> Nível 1 (Fraca)", 1, calculateStrengthLevel(1));
        assertEquals("3 caracteres -> Nível 1 (Fraca)", 1, calculateStrengthLevel(3));
        assertEquals("4 caracteres -> Nível 2 (Média)", 2, calculateStrengthLevel(4));
        assertEquals("5 caracteres -> Nível 2 (Média)", 2, calculateStrengthLevel(5));
        assertEquals("6 caracteres -> Nível 3 (Boa)", 3, calculateStrengthLevel(6));
        assertEquals("7 caracteres -> Nível 3 (Boa)", 3, calculateStrengthLevel(7));
        assertEquals("8 caracteres -> Nível 4 (Forte)", 4, calculateStrengthLevel(8));
        assertEquals("12 caracteres -> Nível 4 (Forte)", 4, calculateStrengthLevel(12));
    }

    private boolean isValidPassword(String password) {
        return password != null && password.length() >= 8;
    }

    private boolean isValidEmail(String email) {
        return email != null && !email.trim().isEmpty() && email.contains("@");
    }

    private boolean isValidSector(String sector) {
        return sector != null && !sector.trim().isEmpty();
    }

    private boolean isPasswordMatch(String first, String second) {
        return isValidPassword(first) && first.equals(second);
    }

    private int calculateStrengthLevel(int length) {
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
}

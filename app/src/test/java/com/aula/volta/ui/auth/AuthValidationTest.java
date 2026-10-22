package com.aula.volta.ui.auth;

import com.aula.volta.R;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Testes unitários para as regras de validação e medidor de força do fluxo de autenticação.
 * Valida diretamente a classe de produção AuthValidator.
 */
public class AuthValidationTest {

    @Test
    public void passwordLengthValidation_enforcesMinimumEightCharacters() {
        assertFalse("Senha com menos de 8 caracteres deve ser inválida",
                AuthValidator.isValidPassword("1234567"));
        assertFalse("Senha vazia deve ser inválida",
                AuthValidator.isValidPassword(""));
        assertFalse("Senha nula deve ser inválida",
                AuthValidator.isValidPassword(null));
        assertTrue("Senha com exatamente 8 caracteres deve ser válida",
                AuthValidator.isValidPassword("12345678"));
        assertTrue("Senha com mais de 8 caracteres deve ser válida",
                AuthValidator.isValidPassword("senhaSegura123!"));
    }

    @Test
    public void emailValidation_checksPresenceAndAtSymbol() {
        assertFalse("E-mail vazio deve ser inválido",
                AuthValidator.isValidEmail(""));
        assertFalse("E-mail nulo deve ser inválido",
                AuthValidator.isValidEmail(null));
        assertFalse("E-mail sem @ deve ser inválido",
                AuthValidator.isValidEmail("userexample.com"));
        assertTrue("E-mail com @ deve ser válido",
                AuthValidator.isValidEmail("breno@volta.com"));
    }

    @Test
    public void sectorValidation_rejectsEmptyOrPlaceholder() {
        assertFalse("Setor vazio é inválido",
                AuthValidator.isValidSector(""));
        assertFalse("Setor com espaços é inválido",
                AuthValidator.isValidSector("   "));
        assertFalse("Setor nulo é inválido",
                AuthValidator.isValidSector(null));
        assertTrue("Setor industrial preenchido é válido",
                AuthValidator.isValidSector("Frigorífico — Setor B2"));
    }

    @Test
    public void passwordMatchValidation_checksEquality() {
        assertTrue("Senhas idênticas com 8+ caracteres devem conferir",
                AuthValidator.isPasswordMatch("senhaForte123", "senhaForte123"));
        assertFalse("Senhas diferentes não devem conferir",
                AuthValidator.isPasswordMatch("senhaForte123", "outraSenha123"));
        assertFalse("Senhas idênticas com menos de 8 caracteres não devem conferir",
                AuthValidator.isPasswordMatch("123", "123"));
        assertFalse("Primeira senha nula não deve conferir",
                AuthValidator.isPasswordMatch(null, "senhaForte123"));
    }

    @Test
    public void strengthMeterLevels_correctlyCategorizes() {
        assertEquals("0 caracteres -> Nível 0", 0, AuthValidator.calculateStrengthLevel(0));
        assertEquals("1 caractere -> Nível 1 (Fraca)", 1, AuthValidator.calculateStrengthLevel(1));
        assertEquals("3 caracteres -> Nível 1 (Fraca)", 1, AuthValidator.calculateStrengthLevel(3));
        assertEquals("4 caracteres -> Nível 2 (Média)", 2, AuthValidator.calculateStrengthLevel(4));
        assertEquals("5 caracteres -> Nível 2 (Média)", 2, AuthValidator.calculateStrengthLevel(5));
        assertEquals("6 caracteres -> Nível 3 (Boa)", 3, AuthValidator.calculateStrengthLevel(6));
        assertEquals("7 caracteres -> Nível 3 (Boa)", 3, AuthValidator.calculateStrengthLevel(7));
        assertEquals("8 caracteres -> Nível 4 (Forte)", 4, AuthValidator.calculateStrengthLevel(8));
        assertEquals("12 caracteres -> Nível 4 (Forte)", 4, AuthValidator.calculateStrengthLevel(12));
    }

    @Test
    public void strengthLabelsAndColors_mappedCorrectly() {
        assertEquals(R.string.auth_pw_weak, AuthValidator.getStrengthLabelRes(1));
        assertEquals(R.string.auth_pw_fair, AuthValidator.getStrengthLabelRes(2));
        assertEquals(R.string.auth_pw_good, AuthValidator.getStrengthLabelRes(3));
        assertEquals(R.string.auth_pw_strong, AuthValidator.getStrengthLabelRes(4));

        assertEquals(R.color.priority_high_text, AuthValidator.getStrengthColorRes(1));
        assertEquals(R.color.priority_medium_text, AuthValidator.getStrengthColorRes(2));
        assertEquals(R.color.login_btn_text_green, AuthValidator.getStrengthColorRes(3));
        assertEquals(R.color.status_aprovada_text, AuthValidator.getStrengthColorRes(4));
    }
}

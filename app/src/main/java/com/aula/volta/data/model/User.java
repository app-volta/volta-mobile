package com.aula.volta.data.model;

/**
 * Modelo de identidade (Fase 0). Telas de login vêm na Fase 12;
 * o modelo existe desde a fundação para ocorrências/history o referenciarem.
 */
public class User {

    private String id;
    private String nome;
    private String email;
    private String perfil;
    private String empresa;
    private String unidade;
    private String permissoes;

    public User() {
    }

    public User(String id, String nome, String email, String perfil,
                String empresa, String unidade, String permissoes) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.perfil = perfil;
        this.empresa = empresa;
        this.unidade = unidade;
        this.permissoes = permissoes;
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getPerfil() {
        return perfil;
    }

    public String getEmpresa() {
        return empresa;
    }

    public String getUnidade() {
        return unidade;
    }

    public String getPermissoes() {
        return permissoes;
    }
}

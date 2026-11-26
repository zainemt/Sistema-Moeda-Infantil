package com.example.moeda_estudantil.model;

import com.example.moeda_estudantil.exception.RegraNegocioException;
import com.example.moeda_estudantil.model.enums.PerfilUsuario;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "alunos")
@Getter
@Setter
public class Aluno extends Usuario {

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String cpf;

    @Column(nullable = false)
    private String rg;

    @Column(nullable = false)
    private Double saldo;

    @Embedded
    private Endereco endereco;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instituicao_id")
    private Instituicao instituicao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id")
    private Curso curso;

    public Aluno() {
        super();
    }

    public Aluno(Long id, String email, String senha, PerfilUsuario perfil, LocalDateTime dataCriacao,
                 String nome, String cpf, String rg, Double saldo, Endereco endereco,
                 Instituicao instituicao, Curso curso) {
        super(id, email, senha, perfil, dataCriacao);
        this.nome = nome;
        this.cpf = cpf;
        this.rg = rg;
        this.saldo = saldo;
        this.endereco = endereco;
        this.instituicao = instituicao;
        this.curso = curso;
    }

    // ── Regras de saldo (GRASP: Especialista na Informação) ─────────────────

    /**
     * Adiciona moedas ao saldo. A quantidade deve ser maior que zero.
     */
    public void creditar(double quantidade) {
        validarQuantidade(quantidade);
        this.saldo = saldoAtual() + quantidade;
    }

    /**
     * Retira moedas do saldo, recusando a operação quando o saldo não é suficiente.
     */
    public void debitar(double quantidade) {
        validarQuantidade(quantidade);
        if (!possuiSaldo(quantidade)) {
            throw new RegraNegocioException(
                    "Saldo insuficiente. Saldo atual: " + saldoAtual() +
                    " moedas. Valor solicitado: " + quantidade + " moedas.");
        }
        this.saldo = saldoAtual() - quantidade;
    }

    /**
     * Indica se o saldo cobre a quantidade informada.
     */
    public boolean possuiSaldo(double quantidade) {
        return saldoAtual() >= quantidade;
    }

    private double saldoAtual() {
        return this.saldo != null ? this.saldo : 0.0;
    }

    private static void validarQuantidade(double quantidade) {
        if (quantidade <= 0) {
            throw new RegraNegocioException("A quantidade de moedas deve ser maior que zero.");
        }
    }
}

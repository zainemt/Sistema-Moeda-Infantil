package com.example.moeda_estudantil.model;

import com.example.moeda_estudantil.exception.RegraNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de unidade pura das regras de saldo atribuídas às entidades
 * (GRASP: Especialista na Informação). Não usam mocks nem Spring.
 */
@DisplayName("Regras de saldo nas entidades Aluno e Professor")
class SaldoEntidadesTest {

    private Aluno alunoComSaldo(double saldo) {
        Aluno aluno = new Aluno();
        aluno.setSaldo(saldo);
        return aluno;
    }

    private Professor professorComSaldo(double saldo) {
        Professor professor = new Professor();
        professor.setSaldo(saldo);
        return professor;
    }

    @Test
    @DisplayName("Aluno: creditar soma a quantidade ao saldo")
    void alunoCreditarSomaAoSaldo() {
        Aluno aluno = alunoComSaldo(100.0);

        aluno.creditar(50.0);

        assertThat(aluno.getSaldo()).isEqualTo(150.0);
    }

    @Test
    @DisplayName("Aluno: debitar com saldo suficiente subtrai a quantidade")
    void alunoDebitarComSaldoSuficiente() {
        Aluno aluno = alunoComSaldo(500.0);

        aluno.debitar(200.0);

        assertThat(aluno.getSaldo()).isEqualTo(300.0);
    }

    @Test
    @DisplayName("Aluno: debitar acima do saldo lança exceção e não altera o saldo")
    void alunoDebitarSemSaldoLancaExcecao() {
        Aluno aluno = alunoComSaldo(100.0);

        assertThatThrownBy(() -> aluno.debitar(200.0))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Saldo insuficiente");
        assertThat(aluno.getSaldo()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Professor: debitar acima do saldo lança exceção; saldo exato é permitido")
    void professorDebitarRespeitaSaldo() {
        Professor professor = professorComSaldo(1000.0);

        assertThatThrownBy(() -> professor.debitar(1000.01))
                .isInstanceOf(RegraNegocioException.class);
        assertThat(professor.possuiSaldo(1000.0)).isTrue();

        professor.debitar(1000.0);

        assertThat(professor.getSaldo()).isZero();
    }

    @Test
    @DisplayName("Quantidade zero ou negativa é recusada em creditar e debitar")
    void quantidadeInvalidaERecusada() {
        Aluno aluno = alunoComSaldo(100.0);
        Professor professor = professorComSaldo(100.0);

        assertThatThrownBy(() -> aluno.creditar(0)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> aluno.debitar(-10)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> professor.creditar(-1)).isInstanceOf(RegraNegocioException.class);
        assertThat(aluno.getSaldo()).isEqualTo(100.0);
        assertThat(professor.getSaldo()).isEqualTo(100.0);
    }
}

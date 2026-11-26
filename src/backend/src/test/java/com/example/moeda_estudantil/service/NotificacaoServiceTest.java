package com.example.moeda_estudantil.service;

import com.example.moeda_estudantil.model.Aluno;
import com.example.moeda_estudantil.model.Cupom;
import com.example.moeda_estudantil.model.EmpresaParceira;
import com.example.moeda_estudantil.model.Professor;
import com.example.moeda_estudantil.model.Vantagem;
import com.example.moeda_estudantil.model.enums.PerfilUsuario;
import com.example.moeda_estudantil.model.enums.StatusCupom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacaoService - e-mails de envio de moedas e de resgate")
class NotificacaoServiceTest {

    @Mock
    private EmailService emailService;

    private NotificacaoService notificacaoService;

    private Aluno aluno;
    private EmpresaParceira empresa;
    private Vantagem vantagem;
    private Cupom cupom;
    private UUID codigo;

    @BeforeEach
    void setUp() {
        notificacaoService = new NotificacaoService(emailService);

        aluno = new Aluno(10L, "aluno@test.com", "senha", PerfilUsuario.ALUNO, LocalDateTime.now(),
                "João Teste", "111.222.333-44", "MG-1", 500.0, null, null, null);
        empresa = new EmpresaParceira(1L, "empresa@parceiro.com", "senha", PerfilUsuario.EMPRESA,
                LocalDateTime.now(), "Cantina Central", "00.000.001/0001-01");
        vantagem = Vantagem.builder().id(5L).titulo("Desconto 20%").custoMoedas(200.0).empresa(empresa).build();
        codigo = UUID.randomUUID();
        cupom = Cupom.builder().id(100L).codigo(codigo).status(StatusCupom.GERADO)
                .aluno(aluno).vantagem(vantagem).build();
    }

    @Test
    @DisplayName("Resgate: envia exatamente um e-mail ao aluno e um à empresa, com o mesmo código")
    void resgateEnviaUmEmailParaCadaDestinatario() {
        notificacaoService.notificarResgate(cupom);

        verify(emailService).enviarCupomAluno("aluno@test.com", codigo.toString(), "Desconto 20%", "Cantina Central");
        verify(emailService).enviarAvisoResgateEmpresa("empresa@parceiro.com", codigo.toString(),
                "Desconto 20%", "João Teste", "***.222.333-**");
        verifyNoMoreInteractions(emailService);
    }

    @Test
    @DisplayName("Resgate: sem e-mail da empresa, só o aluno é notificado")
    void resgateSemEmailDaEmpresaNotificaSoOAluno() {
        empresa.setEmail("  ");

        notificacaoService.notificarResgate(cupom);

        verify(emailService).enviarCupomAluno(anyString(), anyString(), anyString(), anyString());
        verifyNoMoreInteractions(emailService);
    }

    @Test
    @DisplayName("Envio de moedas: avisa o aluno com valor, professor e motivo")
    void recebimentoDeMoedasAvisaOAluno() {
        Professor professor = new Professor(2L, "prof@test.com", "senha", PerfilUsuario.PROFESSOR,
                LocalDateTime.now(), "Prof. Ana", "99999999999", 1000.0, null, null);

        notificacaoService.notificarRecebimentoMoedas(aluno, professor, 50.0, "Participação em aula");

        verify(emailService).enviarAvisoRecebimentoMoedas("aluno@test.com", 50.0, "Prof. Ana", "Participação em aula");
        verifyNoMoreInteractions(emailService);
    }

    @Test
    @DisplayName("Falha no envio é registrada em log e não é propagada")
    void falhaNoEnvioNaoPropaga() {
        doThrow(new RuntimeException("SMTP fora do ar"))
                .when(emailService).enviarCupomAluno(any(), any(), any(), any());

        assertThatCode(() -> notificacaoService.notificarResgate(cupom)).doesNotThrowAnyException();
        verify(emailService, never()).enviarAvisoRecebimentoMoedas(any(), anyDouble(), any(), any());
    }

    @Test
    @DisplayName("Com transação ativa, o envio só acontece depois do commit")
    void comTransacaoAtivaEnviaSoAposCommit() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            notificacaoService.notificarResgate(cupom);
            verifyNoInteractions(emailService);

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);

            verify(emailService).enviarCupomAluno(any(), any(), any(), any());
            verify(emailService).enviarAvisoResgateEmpresa(any(), any(), any(), any(), any());
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}

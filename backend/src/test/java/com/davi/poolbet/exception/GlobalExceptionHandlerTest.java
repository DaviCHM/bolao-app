package com.davi.poolbet.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;

/**
 * Mapeamentos do handler que nao sao exercitados pelo teste de integracao (dificeis de
 * provocar via HTTP): conflito de concorrencia retryavel e o fallback generico.
 */
class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	@DisplayName("Conflito de lock (otimista/pessimista) vira 409, nao 500")
	void conflitoDeConcorrenciaVira409() {
		var response = handler.handleConcurrencyConflict(
				new OptimisticLockingFailureException("versao do saldo mudou"));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getBody().message()).contains("tente novamente");
	}

	@Test
	@DisplayName("Violacao de constraint do banco vira 409 com mensagem generica")
	void violacaoDeIntegridadeVira409() {
		var response = handler.handleDataIntegrityViolation(
				new DataIntegrityViolationException("Duplicate entry 'x' for key 'grupos.nome'"));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getBody().message()).doesNotContain("Duplicate entry");
		assertThat(response.getBody().message()).contains("Tente novamente");
	}

	@Test
	@DisplayName("Excecao nao mapeada vira 500 com mensagem generica (sem vazar detalhes)")
	void excecaoNaoMapeadaVira500() {
		var response = handler.handleGeneric(new IllegalStateException("detalhe interno"));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody().message()).doesNotContain("detalhe interno");
	}
}

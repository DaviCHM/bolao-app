package com.davi.poolbet.exception;

import java.util.stream.Collectors;

import com.davi.poolbet.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traducao central de excecoes para respostas HTTP com corpo padronizado
 * ({@link ErrorResponse}: timestamp, status, error, message).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(MarketNotOpenException.class)
	public ResponseEntity<ErrorResponse> handleMarketNotOpen(MarketNotOpenException ex) {
		return build(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(DuplicateResourceException.class)
	public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex) {
		return build(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(InsufficientBalanceException.class)
	public ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException ex) {
		return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
		return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
	}

	@ExceptionHandler(ForbiddenException.class)
	public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
		return build(HttpStatus.FORBIDDEN, ex.getMessage());
	}

	@ExceptionHandler({ InvalidBetException.class, IllegalArgumentException.class })
	public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	/** Falha de Bean Validation nos @Valid @RequestBody: agrega os erros de campo. */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return build(HttpStatus.BAD_REQUEST, message.isEmpty() ? "requisicao invalida" : message);
	}

	/** JSON malformado ou valor de enum invalido no corpo. */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
		return build(HttpStatus.BAD_REQUEST, "corpo da requisicao invalido ou malformado");
	}

	/** Tipo invalido em parametro (ex.: ?status=XPTO nao e um MarketStatus). */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		return build(HttpStatus.BAD_REQUEST, "valor invalido para o parametro '" + ex.getName() + "'");
	}

	/**
	 * Conflito de concorrencia retryavel: lock otimista do saldo do usuario (apostas
	 * simultaneas em mercados diferentes) ou falha de aquisicao de lock/deadlock no banco.
	 * Nada ficou inconsistente (a transacao sofreu rollback); o cliente pode tentar de novo.
	 */
	@ExceptionHandler(ConcurrencyFailureException.class)
	public ResponseEntity<ErrorResponse> handleConcurrencyConflict(ConcurrencyFailureException ex) {
		return build(HttpStatus.CONFLICT,
				"operacao conflitou com outra em andamento; tente novamente");
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
		log.error("Erro nao mapeado ao atender requisicao", ex);
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "erro interno inesperado");
	}

	private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
		ErrorResponse body = ErrorResponse.of(status.value(), status.getReasonPhrase(), message);
		return ResponseEntity.status(status).body(body);
	}
}

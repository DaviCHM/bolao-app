package com.davi.poolbet.config;

import com.davi.poolbet.exception.UnauthorizedException;
import com.davi.poolbet.model.Grupo;
import com.davi.poolbet.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Exige {@code Authorization: Bearer <token>} em toda a API (exceto /api/auth/**, ver
 * {@link WebConfig}) e expoe o id do grupo autenticado como atributo {@code grupoId} da
 * requisicao — os controllers o recebem via {@code @RequestAttribute}. A excecao lancada
 * aqui e traduzida para 401 pelo GlobalExceptionHandler.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

	public static final String GRUPO_ID_ATTRIBUTE = "grupoId";

	private final AuthService authService;

	public AuthInterceptor(AuthService authService) {
		this.authService = authService;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		String authorization = request.getHeader("Authorization");
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			throw new UnauthorizedException("Autenticacao necessaria: envie o header Authorization Bearer");
		}
		Grupo grupo = authService.authenticate(authorization.substring("Bearer ".length()));
		request.setAttribute(GRUPO_ID_ATTRIBUTE, grupo.getId());
		return true;
	}
}

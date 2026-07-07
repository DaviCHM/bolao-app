package com.davi.poolbet.controller;

import com.davi.poolbet.dto.AuthRequest;
import com.davi.poolbet.dto.AuthResponse;
import com.davi.poolbet.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Unico grupo de rotas fora da autenticacao (ver {@link com.davi.poolbet.config.AuthInterceptor}). */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest request) {
		AuthResponse body = AuthResponse.from(authService.register(request.nome(), request.senha()));
		return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody AuthRequest request) {
		return AuthResponse.from(authService.login(request.nome(), request.senha()));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(
			@RequestHeader(name = "Authorization", required = false) String authorization) {
		if (authorization != null && authorization.startsWith("Bearer ")) {
			authService.logout(authorization.substring("Bearer ".length()));
		}
		return ResponseEntity.noContent().build();
	}
}

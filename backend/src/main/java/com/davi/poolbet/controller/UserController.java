package com.davi.poolbet.controller;

import java.util.List;

import com.davi.poolbet.dto.CreateUserRequest;
import com.davi.poolbet.dto.UserResponse;
import com.davi.poolbet.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Participantes do grupo autenticado ({@code grupoId} vem do AuthInterceptor). */
@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping
	public ResponseEntity<UserResponse> create(
			@RequestAttribute("grupoId") Long grupoId,
			@Valid @RequestBody CreateUserRequest request) {
		UserResponse body = UserResponse.from(userService.createUser(grupoId, request.nome()));
		return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

	/** Lista os participantes do grupo (ordem alfabetica). Base do seletor de usuario do frontend. */
	@GetMapping
	public List<UserResponse> list(@RequestAttribute("grupoId") Long grupoId) {
		return userService.listUsers(grupoId).stream().map(UserResponse::from).toList();
	}

	@GetMapping("/{id}")
	public UserResponse get(@RequestAttribute("grupoId") Long grupoId, @PathVariable Long id) {
		return UserResponse.from(userService.getUser(grupoId, id));
	}
}

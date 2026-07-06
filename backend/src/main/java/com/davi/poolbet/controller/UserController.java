package com.davi.poolbet.controller;

import com.davi.poolbet.dto.CreateUserRequest;
import com.davi.poolbet.dto.UserResponse;
import com.davi.poolbet.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping
	public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
		UserResponse body = UserResponse.from(userService.createUser(request.nome()));
		return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

	@GetMapping("/{id}")
	public UserResponse get(@PathVariable Long id) {
		return UserResponse.from(userService.getUser(id));
	}
}

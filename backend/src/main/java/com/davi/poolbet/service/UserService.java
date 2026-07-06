package com.davi.poolbet.service;

import java.math.BigDecimal;
import java.util.List;

import com.davi.poolbet.exception.DuplicateResourceException;
import com.davi.poolbet.exception.ResourceNotFoundException;
import com.davi.poolbet.model.User;
import com.davi.poolbet.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestao de usuarios. Cada novo usuario comeca com um saldo ficticio padrao.
 */
@Service
public class UserService {

	/** Saldo inicial "fake money" de cada novo usuario. */
	static final BigDecimal SALDO_INICIAL = new BigDecimal("1000.00");

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Transactional
	public User createUser(String nome) {
		if (userRepository.existsByNome(nome)) {
			throw new DuplicateResourceException("Ja existe um usuario com o nome '" + nome + "'");
		}
		return userRepository.save(new User(nome, SALDO_INICIAL));
	}

	/** Lista todos os usuarios em ordem alfabetica (base do seletor "quem sou eu" do frontend). */
	@Transactional(readOnly = true)
	public List<User> listUsers() {
		return userRepository.findAllByOrderByNomeAsc();
	}

	@Transactional(readOnly = true)
	public User getUser(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Usuario " + id + " nao encontrado"));
	}
}

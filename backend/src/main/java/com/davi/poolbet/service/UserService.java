package com.davi.poolbet.service;

import java.math.BigDecimal;
import java.util.List;

import com.davi.poolbet.exception.DuplicateResourceException;
import com.davi.poolbet.exception.ResourceNotFoundException;
import com.davi.poolbet.model.Grupo;
import com.davi.poolbet.model.User;
import com.davi.poolbet.repository.GrupoRepository;
import com.davi.poolbet.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestao dos participantes de um grupo. Todo acesso e escopado pelo grupo autenticado:
 * um participante de outro grupo e indistinguivel de inexistente (404).
 */
@Service
public class UserService {

	/** Saldo inicial de cada novo participante. */
	static final BigDecimal SALDO_INICIAL = new BigDecimal("1000.00");

	private final UserRepository userRepository;
	private final GrupoRepository grupoRepository;

	public UserService(UserRepository userRepository, GrupoRepository grupoRepository) {
		this.userRepository = userRepository;
		this.grupoRepository = grupoRepository;
	}

	@Transactional
	public User createUser(Long grupoId, String nome) {
		if (userRepository.existsByGrupoIdAndNome(grupoId, nome)) {
			throw new DuplicateResourceException(
					"Ja existe um participante com o nome '" + nome + "' neste grupo");
		}
		Grupo grupo = grupoRepository.findById(grupoId)
				.orElseThrow(() -> new ResourceNotFoundException("Grupo " + grupoId + " nao encontrado"));
		return userRepository.save(new User(grupo, nome, SALDO_INICIAL));
	}

	/** Lista os participantes do grupo em ordem alfabetica (base do seletor "quem sou eu"). */
	@Transactional(readOnly = true)
	public List<User> listUsers(Long grupoId) {
		return userRepository.findByGrupoIdOrderByNomeAsc(grupoId);
	}

	@Transactional(readOnly = true)
	public User getUser(Long grupoId, Long id) {
		return userRepository.findById(id)
				.filter(user -> user.getGrupo().getId().equals(grupoId))
				.orElseThrow(() -> new ResourceNotFoundException("Usuario " + id + " nao encontrado"));
	}
}

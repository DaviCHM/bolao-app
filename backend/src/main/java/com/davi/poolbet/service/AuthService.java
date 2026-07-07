package com.davi.poolbet.service;

import java.security.SecureRandom;
import java.util.HexFormat;

import com.davi.poolbet.exception.DuplicateResourceException;
import com.davi.poolbet.exception.UnauthorizedException;
import com.davi.poolbet.model.Grupo;
import com.davi.poolbet.model.Sessao;
import com.davi.poolbet.repository.GrupoRepository;
import com.davi.poolbet.repository.SessaoRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autenticacao por grupo: um cadastro por turma de amigos (nome + senha compartilhada).
 * A senha e armazenada como hash BCrypt; o login emite um token de sessao aleatorio
 * (256 bits, hex) persistido em banco e enviado pelo cliente como Bearer token.
 */
@Service
public class AuthService {

	private final GrupoRepository grupoRepository;
	private final SessaoRepository sessaoRepository;
	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
	private final SecureRandom random = new SecureRandom();

	public AuthService(GrupoRepository grupoRepository, SessaoRepository sessaoRepository) {
		this.grupoRepository = grupoRepository;
		this.sessaoRepository = sessaoRepository;
	}

	/** Cria um grupo novo e ja abre uma sessao para ele. */
	@Transactional
	public Sessao register(String nome, String senha) {
		if (grupoRepository.existsByNome(nome)) {
			throw new DuplicateResourceException("Ja existe um grupo com o nome '" + nome + "'");
		}
		Grupo grupo = grupoRepository.save(new Grupo(nome, encoder.encode(senha)));
		return newSession(grupo);
	}

	/**
	 * Autentica o grupo e abre uma sessao nova.
	 * A mensagem de erro nao distingue "grupo inexistente" de "senha errada" de proposito.
	 */
	@Transactional
	public Sessao login(String nome, String senha) {
		Grupo grupo = grupoRepository.findByNome(nome)
				.filter(g -> encoder.matches(senha, g.getSenhaHash()))
				.orElseThrow(() -> new UnauthorizedException("Nome do grupo ou senha incorretos"));
		return newSession(grupo);
	}

	/** Resolve o token de sessao para o grupo autenticado; 401 se invalido. */
	@Transactional(readOnly = true)
	public Grupo authenticate(String token) {
		return sessaoRepository.findById(token)
				.map(Sessao::getGrupo)
				.orElseThrow(() -> new UnauthorizedException("Sessao invalida ou expirada; faca login novamente"));
	}

	/** Encerra a sessao (logout). Token desconhecido e no-op. */
	@Transactional
	public void logout(String token) {
		sessaoRepository.deleteById(token);
	}

	private Sessao newSession(Grupo grupo) {
		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		return sessaoRepository.save(new Sessao(HexFormat.of().formatHex(bytes), grupo));
	}
}

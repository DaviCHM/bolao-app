package com.davi.poolbet.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

import com.davi.poolbet.exception.DuplicateResourceException;
import com.davi.poolbet.exception.UnauthorizedException;
import com.davi.poolbet.model.Grupo;
import com.davi.poolbet.model.Sessao;
import com.davi.poolbet.repository.GrupoRepository;
import com.davi.poolbet.repository.SessaoRepository;
import org.springframework.beans.factory.annotation.Value;
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
	private final Duration sessaoTtl;
	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
	private final SecureRandom random = new SecureRandom();

	public AuthService(GrupoRepository grupoRepository, SessaoRepository sessaoRepository,
			@Value("${bolao.sessao.ttl-dias:30}") long sessaoTtlDias) {
		this.grupoRepository = grupoRepository;
		this.sessaoRepository = sessaoRepository;
		this.sessaoTtl = Duration.ofDays(sessaoTtlDias);
	}

	/** Cria um grupo novo e ja abre uma sessao para ele. */
	@Transactional
	public Sessao register(String nome, String senha) {
		String nomeNormalizado = normalizarNome(nome);
		if (grupoRepository.existsByNome(nomeNormalizado)) {
			throw new DuplicateResourceException("Ja existe um grupo com o nome '" + nomeNormalizado + "'");
		}
		Grupo grupo = grupoRepository.save(new Grupo(nomeNormalizado, encoder.encode(senha)));
		return newSession(grupo);
	}

	/**
	 * Autentica o grupo e abre uma sessao nova.
	 * A mensagem de erro nao distingue "grupo inexistente" de "senha errada" de proposito.
	 */
	@Transactional
	public Sessao login(String nome, String senha) {
		Grupo grupo = grupoRepository.findByNome(normalizarNome(nome))
				.filter(g -> encoder.matches(senha, g.getSenhaHash()))
				.orElseThrow(() -> new UnauthorizedException("Nome do grupo ou senha incorretos"));
		return newSession(grupo);
	}

	/**
	 * Resolve o token de sessao para o grupo autenticado; 401 se invalido.
	 * Sessoes mais velhas que o TTL configurado ({@code bolao.sessao.ttl-dias})
	 * sao removidas e tratadas como token invalido.
	 */
	@Transactional
	public Grupo authenticate(String token) {
		Sessao sessao = sessaoRepository.findById(token)
				.orElseThrow(AuthService::sessaoInvalida);
		if (sessao.getCriadoEm().plus(sessaoTtl).isBefore(Instant.now())) {
			sessaoRepository.delete(sessao);
			throw sessaoInvalida();
		}
		return sessao.getGrupo();
	}

	/** Encerra a sessao (logout). Token desconhecido e no-op. */
	@Transactional
	public void logout(String token) {
		sessaoRepository.deleteById(token);
	}

	/** Remove espacos nas pontas e colapsa espacos internos multiplos (case preservado). */
	private static String normalizarNome(String nome) {
		return nome == null ? null : nome.trim().replaceAll("\\s+", " ");
	}

	private static UnauthorizedException sessaoInvalida() {
		return new UnauthorizedException("Sessao invalida ou expirada; faca login novamente");
	}

	private Sessao newSession(Grupo grupo) {
		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		return sessaoRepository.save(new Sessao(HexFormat.of().formatHex(bytes), grupo));
	}
}

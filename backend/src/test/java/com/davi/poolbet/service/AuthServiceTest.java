package com.davi.poolbet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import com.davi.poolbet.exception.UnauthorizedException;
import com.davi.poolbet.model.Grupo;
import com.davi.poolbet.model.Sessao;
import com.davi.poolbet.repository.GrupoRepository;
import com.davi.poolbet.repository.SessaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Testes da autenticacao por grupo: expiracao de sessao por TTL e normalizacao
 * de espacos no nome do grupo em cadastro e login.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final long TTL_DIAS = 30;

	@Mock
	private GrupoRepository grupoRepository;

	@Mock
	private SessaoRepository sessaoRepository;

	private AuthService authService;

	private Grupo grupo;

	@BeforeEach
	void setUp() {
		authService = new AuthService(grupoRepository, sessaoRepository, TTL_DIAS);
		grupo = new Grupo("Amigos do Fla", "$2a$10$hash");
	}

	@Test
	@DisplayName("Sessao dentro do TTL autentica normalmente")
	void sessaoDentroDoTtlAutentica() {
		Sessao sessao = new Sessao("token-valido", grupo);
		when(sessaoRepository.findById("token-valido")).thenReturn(Optional.of(sessao));

		assertThat(authService.authenticate("token-valido")).isSameAs(grupo);
	}

	@Test
	@DisplayName("Sessao mais velha que o TTL e deletada e vira 401")
	void sessaoExpiradaEDeletadaEVira401() {
		Sessao sessao = new Sessao("token-velho", grupo);
		ReflectionTestUtils.setField(sessao, "criadoEm",
				Instant.now().minus(TTL_DIAS + 1, ChronoUnit.DAYS));
		when(sessaoRepository.findById("token-velho")).thenReturn(Optional.of(sessao));

		assertThatThrownBy(() -> authService.authenticate("token-velho"))
				.isInstanceOf(UnauthorizedException.class)
				.hasMessageContaining("expirada");

		verify(sessaoRepository).delete(sessao);
	}

	@Test
	@DisplayName("Token desconhecido vira 401")
	void tokenDesconhecidoVira401() {
		when(sessaoRepository.findById("nao-existe")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.authenticate("nao-existe"))
				.isInstanceOf(UnauthorizedException.class);
	}

	@Test
	@DisplayName("Cadastro normaliza espacos do nome (trim e colapso) sem mudar case")
	void cadastroNormalizaEspacosDoNome() {
		when(grupoRepository.existsByNome("Amigos do Fla")).thenReturn(false);
		when(grupoRepository.save(any(Grupo.class))).thenAnswer(inv -> inv.getArgument(0));
		when(sessaoRepository.save(any(Sessao.class))).thenAnswer(inv -> inv.getArgument(0));

		authService.register("  Amigos   do  Fla ", "senha123");

		ArgumentCaptor<Grupo> captor = ArgumentCaptor.forClass(Grupo.class);
		verify(grupoRepository).save(captor.capture());
		assertThat(captor.getValue().getNome()).isEqualTo("Amigos do Fla");
	}

	@Test
	@DisplayName("Login busca o grupo pelo nome normalizado")
	void loginBuscaPeloNomeNormalizado() {
		when(grupoRepository.findByNome("Amigos do Fla")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login("  Amigos   do  Fla ", "qualquer"))
				.isInstanceOf(UnauthorizedException.class);

		verify(grupoRepository).findByNome("Amigos do Fla");
	}
}

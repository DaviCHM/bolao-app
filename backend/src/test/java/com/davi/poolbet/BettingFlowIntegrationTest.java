package com.davi.poolbet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Teste de integracao ponta-a-ponta (controllers + services + JPA em H2) do fluxo
 * registrar grupo -> criar participantes -> apostar -> resolver, mais caminhos de erro
 * chave e o isolamento entre grupos. Sem Jackson: o JSON de requisicao e montado como
 * texto e a resposta e lida via JsonPath.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BettingFlowIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	/** Registra um grupo novo e devolve o token de sessao. */
	private String registrarGrupo(String nome) throws Exception {
		MvcResult res = mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nome\":\"" + nome + "\",\"senha\":\"segredo123\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andReturn();
		return stringAt(res, "$.token");
	}

	private long createUser(String token, String nome) throws Exception {
		MvcResult res = mockMvc.perform(post("/api/users")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nome\":\"" + nome + "\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.saldo").value(1000.00))
				.andReturn();
		return longAt(res, "$.id");
	}

	private long createMarket(String token, String pergunta, long criadorId) throws Exception {
		String body = "{\"pergunta\":\"" + pergunta + "\",\"opcaoA\":\"Sim\",\"opcaoB\":\"Nao\","
				+ "\"criadorId\":" + criadorId + "}";
		MvcResult res = mockMvc.perform(post("/api/markets")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andReturn();
		return longAt(res, "$.id");
	}

	private void bet(String token, long marketId, long userId, String opcao, String valor) throws Exception {
		String body = "{\"usuarioId\":" + userId + ",\"opcao\":\"" + opcao + "\",\"valor\":" + valor + "}";
		mockMvc.perform(post("/api/markets/" + marketId + "/bets")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated());
	}

	private double saldoOf(String token, long userId) throws Exception {
		MvcResult res = mockMvc.perform(get("/api/users/" + userId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andReturn();
		return numAt(res, "$.saldo");
	}

	@Test
	void fluxoCompleto_criarApostarResolver() throws Exception {
		String token = registrarGrupo("turma-fluxo");
		long alice = createUser(token, "alice");
		long bob = createUser(token, "bob");
		long carol = createUser(token, "carol");
		long market = createMarket(token, "Vai chover amanha?", carol);

		// total_A = 100 (alice 10 + bob 90), total_B = 900 (carol), pool = 1000.
		bet(token, market, alice, "A", "10");
		bet(token, market, bob, "A", "90");
		bet(token, market, carol, "B", "900");

		// Metricas derivadas: odd A = 1000/100 = 10x; prob A = 0.1.
		mockMvc.perform(get("/api/markets/" + market).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mercado.pool").value(1000.00))
				.andExpect(jsonPath("$.mercado.oddA").value(10.00))
				.andExpect(jsonPath("$.mercado.probA").value(0.1))
				.andExpect(jsonPath("$.apostas.length()").value(3));

		// Resolve: A vence (solicitante = criador carol).
		MvcResult resolved = mockMvc.perform(post("/api/markets/" + market + "/resolve")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"resultado\":\"A\",\"solicitanteId\":" + carol + "}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mercado.status").value("FECHADO"))
				.andExpect(jsonPath("$.mercado.resultado").value("A"))
				.andReturn();

		// Invariante: soma dos payouts == pool.
		List<Object> payouts = JsonPath.read(resolved.getResponse().getContentAsString(),
				"$.apostas[*].payout");
		double somaPayouts = payouts.stream().mapToDouble(p -> ((Number) p).doubleValue()).sum();
		assertThat(somaPayouts).isEqualTo(1000.00);

		// Saldos: alice 1000-10+100=1090; bob 1000-90+900=1810; carol 1000-900+0=100.
		assertThat(saldoOf(token, alice)).isEqualTo(1090.00);
		assertThat(saldoOf(token, bob)).isEqualTo(1810.00);
		assertThat(saldoOf(token, carol)).isEqualTo(100.00);
	}

	@Test
	void semToken_retorna401() throws Exception {
		mockMvc.perform(get("/api/markets"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void loginComSenhaErrada_retorna401() throws Exception {
		registrarGrupo("turma-senha");
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nome\":\"turma-senha\",\"senha\":\"errada999\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void grupoNaoVeMercadoDeOutroGrupo_retorna404() throws Exception {
		String tokenA = registrarGrupo("turma-a");
		String tokenB = registrarGrupo("turma-b");
		long dono = createUser(tokenA, "dono-isolado");
		long market = createMarket(tokenA, "Mercado do grupo A", dono);

		// O grupo B nao enxerga o mercado (404, nao 403: existencia nao vaza).
		mockMvc.perform(get("/api/markets/" + market).header("Authorization", "Bearer " + tokenB))
				.andExpect(status().isNotFound());
		// Nem o participante.
		mockMvc.perform(get("/api/users/" + dono).header("Authorization", "Bearer " + tokenB))
				.andExpect(status().isNotFound());
		// E a listagem do grupo B vem vazia.
		mockMvc.perform(get("/api/markets").header("Authorization", "Bearer " + tokenB))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void resolverDuasVezes_retorna409() throws Exception {
		String token = registrarGrupo("turma-409");
		long dono = createUser(token, "dono1");
		long apostador = createUser(token, "apostador1");
		long market = createMarket(token, "Mercado A", dono);
		bet(token, market, apostador, "A", "50");

		mockMvc.perform(post("/api/markets/" + market + "/resolve")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"resultado\":\"A\",\"solicitanteId\":" + dono + "}"))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/markets/" + market + "/resolve")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"resultado\":\"B\",\"solicitanteId\":" + dono + "}"))
				.andExpect(status().isConflict());
	}

	@Test
	void resolverNaoSendoCriador_retorna403() throws Exception {
		String token = registrarGrupo("turma-403");
		long dono = createUser(token, "dono2");
		long intruso = createUser(token, "intruso2");
		long market = createMarket(token, "Mercado B", dono);

		mockMvc.perform(post("/api/markets/" + market + "/resolve")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"resultado\":\"A\",\"solicitanteId\":" + intruso + "}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void apostarAcimaDoSaldo_retorna422() throws Exception {
		String token = registrarGrupo("turma-422");
		long dono = createUser(token, "dono3");
		long market = createMarket(token, "Mercado C", dono);

		mockMvc.perform(post("/api/markets/" + market + "/bets")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"usuarioId\":" + dono + ",\"opcao\":\"A\",\"valor\":5000}"))
				.andExpect(status().is(422));
	}

	@Test
	void apostarComValorNegativo_retorna400() throws Exception {
		String token = registrarGrupo("turma-400");
		long dono = createUser(token, "dono4");
		long market = createMarket(token, "Mercado D", dono);

		mockMvc.perform(post("/api/markets/" + market + "/bets")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"usuarioId\":" + dono + ",\"opcao\":\"A\",\"valor\":-10}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void listarUsuarios_retornaTodosEmOrdemAlfabetica() throws Exception {
		String token = registrarGrupo("turma-lista");
		createUser(token, "zilda-lista");
		createUser(token, "ana-lista");

		MvcResult res = mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andReturn();

		List<String> nomes = JsonPath.read(res.getResponse().getContentAsString(), "$[*].nome");
		assertThat(nomes).containsExactly("ana-lista", "zilda-lista");
	}

	@Test
	void nomeDuplicadoNoMesmoGrupo_retorna409_masOutroGrupoPodeUsar() throws Exception {
		String tokenA = registrarGrupo("turma-dup-a");
		String tokenB = registrarGrupo("turma-dup-b");
		createUser(tokenA, "repetido");

		mockMvc.perform(post("/api/users")
						.header("Authorization", "Bearer " + tokenA)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nome\":\"repetido\"}"))
				.andExpect(status().isConflict());

		// O mesmo nome e valido em outro grupo (unicidade por grupo, nao global).
		createUser(tokenB, "repetido");
	}

	private long longAt(MvcResult res, String path) throws Exception {
		return ((Number) JsonPath.read(res.getResponse().getContentAsString(), path)).longValue();
	}

	private double numAt(MvcResult res, String path) throws Exception {
		return ((Number) JsonPath.read(res.getResponse().getContentAsString(), path)).doubleValue();
	}

	private String stringAt(MvcResult res, String path) throws Exception {
		return JsonPath.read(res.getResponse().getContentAsString(), path);
	}
}

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
 * criar -> apostar -> resolver, mais caminhos de erro chave. Sem Jackson: o JSON de
 * requisicao e montado como texto e a resposta e lida via JsonPath.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BettingFlowIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	private long createUser(String nome) throws Exception {
		MvcResult res = mockMvc.perform(post("/api/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nome\":\"" + nome + "\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.saldo").value(1000.00))
				.andReturn();
		return longAt(res, "$.id");
	}

	private long createMarket(String pergunta, long criadorId) throws Exception {
		String body = "{\"pergunta\":\"" + pergunta + "\",\"opcaoA\":\"Sim\",\"opcaoB\":\"Nao\","
				+ "\"criadorId\":" + criadorId + "}";
		MvcResult res = mockMvc.perform(post("/api/markets")
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andReturn();
		return longAt(res, "$.id");
	}

	private void bet(long marketId, long userId, String opcao, String valor) throws Exception {
		String body = "{\"usuarioId\":" + userId + ",\"opcao\":\"" + opcao + "\",\"valor\":" + valor + "}";
		mockMvc.perform(post("/api/markets/" + marketId + "/bets")
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated());
	}

	private double saldoOf(long userId) throws Exception {
		MvcResult res = mockMvc.perform(get("/api/users/" + userId))
				.andExpect(status().isOk())
				.andReturn();
		return numAt(res, "$.saldo");
	}

	@Test
	void fluxoCompleto_criarApostarResolver() throws Exception {
		long alice = createUser("alice");
		long bob = createUser("bob");
		long carol = createUser("carol");
		long market = createMarket("Vai chover amanha?", carol);

		// total_A = 100 (alice 10 + bob 90), total_B = 900 (carol), pool = 1000.
		bet(market, alice, "A", "10");
		bet(market, bob, "A", "90");
		bet(market, carol, "B", "900");

		// Metricas derivadas: odd A = 1000/100 = 10x; prob A = 0.1.
		mockMvc.perform(get("/api/markets/" + market))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mercado.pool").value(1000.00))
				.andExpect(jsonPath("$.mercado.oddA").value(10.00))
				.andExpect(jsonPath("$.mercado.probA").value(0.1))
				.andExpect(jsonPath("$.apostas.length()").value(3));

		// Resolve: A vence (solicitante = criador carol).
		MvcResult resolved = mockMvc.perform(post("/api/markets/" + market + "/resolve")
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
		assertThat(saldoOf(alice)).isEqualTo(1090.00);
		assertThat(saldoOf(bob)).isEqualTo(1810.00);
		assertThat(saldoOf(carol)).isEqualTo(100.00);
	}

	@Test
	void resolverDuasVezes_retorna409() throws Exception {
		long dono = createUser("dono1");
		long apostador = createUser("apostador1");
		long market = createMarket("Mercado A", dono);
		bet(market, apostador, "A", "50");

		mockMvc.perform(post("/api/markets/" + market + "/resolve")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"resultado\":\"A\",\"solicitanteId\":" + dono + "}"))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/markets/" + market + "/resolve")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"resultado\":\"B\",\"solicitanteId\":" + dono + "}"))
				.andExpect(status().isConflict());
	}

	@Test
	void resolverNaoSendoCriador_retorna403() throws Exception {
		long dono = createUser("dono2");
		long intruso = createUser("intruso2");
		long market = createMarket("Mercado B", dono);

		mockMvc.perform(post("/api/markets/" + market + "/resolve")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"resultado\":\"A\",\"solicitanteId\":" + intruso + "}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void apostarAcimaDoSaldo_retorna422() throws Exception {
		long dono = createUser("dono3");
		long market = createMarket("Mercado C", dono);

		mockMvc.perform(post("/api/markets/" + market + "/bets")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"usuarioId\":" + dono + ",\"opcao\":\"A\",\"valor\":5000}"))
				.andExpect(status().is(422));
	}

	@Test
	void apostarComValorNegativo_retorna400() throws Exception {
		long dono = createUser("dono4");
		long market = createMarket("Mercado D", dono);

		mockMvc.perform(post("/api/markets/" + market + "/bets")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"usuarioId\":" + dono + ",\"opcao\":\"A\",\"valor\":-10}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void listarUsuarios_retornaTodosEmOrdemAlfabetica() throws Exception {
		createUser("zilda-lista");
		createUser("ana-lista");

		MvcResult res = mockMvc.perform(get("/api/users"))
				.andExpect(status().isOk())
				.andReturn();

		List<String> nomes = JsonPath.read(res.getResponse().getContentAsString(), "$[*].nome");
		assertThat(nomes).contains("ana-lista", "zilda-lista");
		assertThat(nomes.indexOf("ana-lista")).isLessThan(nomes.indexOf("zilda-lista"));
	}

	@Test
	void nomeDuplicado_retorna409() throws Exception {
		createUser("repetido");
		mockMvc.perform(post("/api/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nome\":\"repetido\"}"))
				.andExpect(status().isConflict());
	}

	private long longAt(MvcResult res, String path) throws Exception {
		return ((Number) JsonPath.read(res.getResponse().getContentAsString(), path)).longValue();
	}

	private double numAt(MvcResult res, String path) throws Exception {
		return ((Number) JsonPath.read(res.getResponse().getContentAsString(), path)).doubleValue();
	}
}

package it.esercitazione.liveauction.producer.prodotto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class CatalogoIntegrationTests {
    private static final String PASSWORD = "TestPassword123!";

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void adminManagesCatalogAndVisitorsSeeOnlyActiveProducts() throws Exception {
        String suffisso = UUID.randomUUID().toString().substring(0, 8);
        String adminToken = token("admin_" + suffisso, true);
        String userToken = token("user_" + suffisso, false);
        String slug = "test-" + suffisso;

        String categoriaBody = json.writeValueAsString(new Categoria("Test " + suffisso, slug, null));
        mvc.perform(post("/api/v1/admin/categorie")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON).content(categoriaBody))
                .andExpect(status().isForbidden());
        long categoriaId = json.readTree(mvc.perform(post("/api/v1/admin/categorie")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(categoriaBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attiva").value(true))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(post("/api/v1/admin/categorie")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(categoriaBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORIA_GIA_ESISTENTE"));
        mvc.perform(get("/api/v1/categorie"))
                .andExpect(status().isOk());

        String sku = "tst-" + suffisso;
        String prodottoBody = json.writeValueAsString(new NuovoProdotto(categoriaId, sku,
                "Laptop " + suffisso, "Prodotto di prova", new BigDecimal("1299.90"), true, 3));
        JsonNode creato = json.readTree(mvc.perform(post("/api/v1/admin/prodotti")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(prodottoBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value(sku.toUpperCase()))
                .andExpect(jsonPath("$.quantitaBloccata").value(0))
                .andExpect(jsonPath("$.versione").value(0))
                .andReturn().getResponse().getContentAsString());
        long prodottoId = creato.get("id").asLong();
        mvc.perform(post("/api/v1/admin/prodotti")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(prodottoBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SKU_GIA_UTILIZZATO"));

        mvc.perform(get("/api/v1/prodotti")
                        .param("query", suffisso).param("categoria", slug).param("astabile", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(prodottoId))
                .andExpect(jsonPath("$.content[0].asteProgrammate").value(0));
        mvc.perform(get("/api/v1/prodotti").param("size", "101"))
                .andExpect(status().isBadRequest());

        String modifica = json.writeValueAsString(new ModificaProdotto(categoriaId, sku,
                "Laptop " + suffisso, null, null, false, 5, false, 0L));
        mvc.perform(put("/api/v1/admin/prodotti/{id}", prodottoId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(modifica))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantitaDisponibile").value(5))
                .andExpect(jsonPath("$.attivo").value(false))
                .andExpect(jsonPath("$.versione").value(1));
        mvc.perform(put("/api/v1/admin/prodotti/{id}", prodottoId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(modifica))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSIONE_NON_AGGIORNATA"));

        mvc.perform(get("/api/v1/prodotti/{id}", prodottoId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RISORSA_NON_TROVATA"));
        mvc.perform(get("/api/v1/admin/prodotti/{id}", prodottoId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria.slug").value(slug));
    }

    private String token(String username, boolean admin) throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new Registration(username, username + "@example.com", PASSWORD))))
                .andExpect(status().isCreated());
        if (admin) {
            jdbc.update("UPDATE utenti SET ruolo = 'ADMIN' WHERE username = ?", username);
        }
        return json.readTree(mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new Login(username, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("accessToken").asText();
    }

    private record Registration(String username, String email, String password) {
    }

    private record Login(String username, String password) {
    }

    private record Categoria(String nome, String slug, Boolean attiva) {
    }

    private record NuovoProdotto(Long categoriaId, String sku, String nome, String descrizione,
                                 BigDecimal prezzoFisso, Boolean astabile, Integer quantitaDisponibile) {
    }

    private record ModificaProdotto(Long categoriaId, String sku, String nome, String descrizione,
                                    BigDecimal prezzoFisso, Boolean astabile, Integer quantitaDisponibile,
                                    Boolean attivo, Long versione) {
    }
}

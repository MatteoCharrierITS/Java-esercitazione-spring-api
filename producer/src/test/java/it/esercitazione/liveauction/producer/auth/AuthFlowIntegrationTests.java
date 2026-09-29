package it.esercitazione.liveauction.producer.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class AuthFlowIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void registrationCreatesWalletAndLoginEnforcesRoles() throws Exception {
        String username = "test_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "TestPassword123!";
        String body = json.writeValueAsString(new Registration(username,
                username + "@example.com", password));

        String registrationBody = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.ruolo").value("USER"))
                .andReturn().getResponse().getContentAsString();
        long userId = json.readTree(registrationBody).get("id").asLong();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM portafogli WHERE utente_id = ?", Integer.class, userId)).isEqualTo(1);
        String hash = jdbc.queryForObject(
                "SELECT password_hash FROM utenti WHERE id = ?", String.class, userId);
        assertThat(hash).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, hash)).isTrue();

        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new Login(username, "wrong-password"))))
                .andExpect(status().isUnauthorized());

        String loginBody = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new Login(username, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        JsonNode login = json.readTree(loginBody);
        String token = login.get("accessToken").asText();
        assertThat(token).isNotBlank();

        mvc.perform(post("/api/v1/admin/aste")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/aste"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/v1/me/portafoglio")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        jdbc.update("UPDATE utenti SET attivo = FALSE WHERE id = ?", userId);
        mvc.perform(get("/api/v1/me/portafoglio")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        String adminLoginBody = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new Login("admin_demo", "Demo123!"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String adminToken = json.readTree(adminLoginBody).get("accessToken").asText();
        // L'endpoint amministrativo non è ancora implementato: 404 prova che il ruolo è passato.
        mvc.perform(post("/api/v1/admin/aste")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    private record Registration(String username, String email, String password) {}
    private record Login(String username, String password) {}
}

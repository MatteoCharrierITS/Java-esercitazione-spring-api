package it.esercitazione.liveauction.producer.monitoring;

import it.esercitazione.liveauction.producer.auth.AuthController;
import it.esercitazione.liveauction.producer.auth.repos.UtenteRepository;
import it.esercitazione.liveauction.producer.auth.services.LoginService;
import it.esercitazione.liveauction.producer.auth.services.RegistrazioneService;
import it.esercitazione.liveauction.producer.config.OpenApiConfig;
import it.esercitazione.liveauction.producer.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = ProbeIntegrationTests.TestApplication.class)
@AutoConfigureMockMvc
class ProbeIntegrationTests {
    @Configuration
    @EnableAutoConfiguration(excludeName = {
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
            "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
    })
    @Import({ProbeController.class, SecurityConfig.class, OpenApiConfig.class, AuthController.class})
    static class TestApplication {
    }

    @Autowired MockMvc mvc;
    @Autowired ApplicationContext context;
    @MockitoBean(name = "dbHealthContributor") HealthIndicator database;
    @MockitoBean UtenteRepository utenti;
    @MockitoBean JdbcTemplate jdbc;
    @MockitoBean LoginService loginService;
    @MockitoBean RegistrazioneService registrazioneService;

    @BeforeEach
    void available() {
        when(database.getHealth(anyBoolean())).thenReturn(Health.up().build());
        AvailabilityChangeEvent.publish(context, LivenessState.CORRECT);
        AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
    }

    @Test
    void probesArePublicWhenOperational() throws Exception {
        mvc.perform(get("/api/v1/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        verifyNoInteractions(database);
        mvc.perform(get("/api/v1/ready")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        verify(database).getHealth(anyBoolean());
    }

    @Test
    void databaseFailureAffectsReadinessOnlyAndHidesDetails() throws Exception {
        when(database.getHealth(anyBoolean())).thenReturn(Health.down().withDetail("error", "private connection details").build());
        mvc.perform(get("/api/v1/health")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/ready"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("SERVIZIO_NON_PRONTO"))
                .andExpect(jsonPath("$.statusService").value("DOWN"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void refusingTrafficAffectsReadinessEvenWhenDatabaseIsUp() throws Exception {
        AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC);
        mvc.perform(get("/api/v1/ready")).andExpect(status().isServiceUnavailable());
        mvc.perform(get("/api/v1/health")).andExpect(status().isOk());
    }

    @Test
    void brokenLivenessReturnsServiceUnavailable() throws Exception {
        AvailabilityChangeEvent.publish(context, LivenessState.BROKEN);
        mvc.perform(get("/api/v1/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVIZIO_NON_ATTIVO"));
    }

    @Test
    void swaggerIsPublicAndDescribesBearerAuthentication() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/health'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/ready'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/ready'].get.responses['503']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/logout'].post.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.security[0]").doesNotExist());
        mvc.perform(post("/api/v1/auth/logout")).andExpect(status().isUnauthorized());
    }
}

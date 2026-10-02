package it.esercitazione.liveauction.producer.monitoring;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Monitoring", description = "Stato del servizio e disponibilità delle dipendenze")
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "Controllo superato",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProbeResponse.class))),
        @ApiResponse(responseCode = "503", description = "Servizio non disponibile",
                content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
})
public class ProbeController {
    private final HealthEndpoint healthEndpoint;

    @GetMapping("/health")
    @Operation(summary = "Verifica che il servizio sia attivo", description = "Controlla la liveness senza interrogare il database.")
    public ResponseEntity<?> health() {
        return probe("liveness", "SERVIZIO_NON_ATTIVO", "Il servizio non è attivo");
    }

    @GetMapping("/ready")
    @Operation(summary = "Verifica che il servizio sia operativo", description = "Controlla la readiness applicativa e la connessione al database; restituisce 503 se non pronto.")
    public ResponseEntity<?> ready() {
        return probe("readiness", "SERVIZIO_NON_PRONTO", "Il servizio non è pronto a ricevere richieste");
    }

    private ResponseEntity<?> probe(String group, String code, String detail) {
        HealthComponent health = healthEndpoint.healthForPath(group);
        if (health != null && Status.UP.equals(health.getStatus())) {
            return ResponseEntity.ok(new ProbeResponse("UP"));
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, detail);
        problem.setTitle("Servizio non disponibile");
        problem.setProperty("code", code);
        problem.setProperty("statusService", health == null ? "UNKNOWN" : health.getStatus().getCode());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }
}

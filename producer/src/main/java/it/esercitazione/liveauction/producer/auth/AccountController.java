package it.esercitazione.liveauction.producer.auth;

import it.esercitazione.liveauction.producer.auth.services.EliminazioneUtenteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class AccountController {
    private final EliminazioneUtenteService eliminazioneUtenteService;

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void elimina(JwtAuthenticationToken authentication) {
        eliminazioneUtenteService.elimina(Long.parseLong(authentication.getToken().getSubject()));
    }
}

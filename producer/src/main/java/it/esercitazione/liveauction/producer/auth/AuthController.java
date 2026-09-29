package it.esercitazione.liveauction.producer.auth;

import it.esercitazione.liveauction.producer.auth.requests.LoginRequest;
import it.esercitazione.liveauction.producer.auth.requests.RefreshRequest;
import it.esercitazione.liveauction.producer.auth.requests.RegistrazioneRequest;
import it.esercitazione.liveauction.producer.auth.responses.LoginResponse;
import it.esercitazione.liveauction.producer.auth.responses.RegistrazioneResponse;
import it.esercitazione.liveauction.producer.auth.services.LoginService;
import it.esercitazione.liveauction.producer.auth.services.RegistrazioneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final RegistrazioneService registrazioneService;
    private final LoginService loginService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrazioneResponse registra(@Valid @RequestBody RegistrazioneRequest request) {
        return registrazioneService.registra(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return loginService.login(request);
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return loginService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(JwtAuthenticationToken authentication) {
        UUID sessionId = UUID.fromString(authentication.getToken().getClaimAsString("sid"));
        long userId = Long.parseLong(authentication.getToken().getSubject());
        loginService.logout(sessionId, userId);
    }
}

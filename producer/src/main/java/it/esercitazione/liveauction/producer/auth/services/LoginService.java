package it.esercitazione.liveauction.producer.auth.services;

import it.esercitazione.liveauction.producer.auth.models.Utente;
import it.esercitazione.liveauction.producer.auth.repos.UtenteRepository;
import it.esercitazione.liveauction.producer.auth.requests.LoginRequest;
import it.esercitazione.liveauction.producer.auth.responses.LoginResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
public class LoginService {
    private final UtenteRepository utenti;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final Duration tokenTtl;

    public LoginService(UtenteRepository utenti, PasswordEncoder passwordEncoder,
                        JwtEncoder jwtEncoder, @Value("${app.auth.token-ttl}") Duration tokenTtl) {
        this.utenti = utenti;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.tokenTtl = tokenTtl;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        Utente utente = utenti.findByUsername(username)
                .orElseThrow(LoginService::credenzialiNonValide);
        if (!utente.isAttivo() || !passwordEncoder.matches(request.password(), utente.getPasswordHash())) {
            throw credenzialiNonValide();
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plus(tokenTtl);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("liveauction-producer")
                .subject(utente.getId().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new LoginResponse(token, "Bearer", expiresAt,
                utente.getId(), utente.getUsername(), utente.getRuolo());
    }

    private static ResponseStatusException credenzialiNonValide() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenziali non valide");
    }
}

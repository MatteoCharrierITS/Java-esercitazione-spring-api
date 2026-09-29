package it.esercitazione.liveauction.producer.auth.services;

import it.esercitazione.liveauction.producer.auth.models.Ruolo;
import it.esercitazione.liveauction.producer.auth.models.Utente;
import it.esercitazione.liveauction.producer.auth.repos.UtenteRepository;
import it.esercitazione.liveauction.producer.auth.requests.RegistrazioneRequest;
import it.esercitazione.liveauction.producer.auth.responses.RegistrazioneResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RegistrazioneService {

    private final UtenteRepository utenteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public RegistrazioneResponse registra(RegistrazioneRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (username.length() < 3 || request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username o password non validi");
        }

        if (utenteRepository.existsByUsername(username)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Username già utilizzato");
        }

        if (utenteRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Email già utilizzata");
        }

        var newUser = new Utente();

        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPasswordHash(passwordEncoder.encode(request.password()));
        newUser.setRuolo(Ruolo.USER);
        newUser.setAttivo(true);

        Utente salvato;
        try {
            salvato = utenteRepository.saveAndFlush(newUser);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Username o email già utilizzati", exception);
        }

        // Il saldo iniziale è zero: non serve un movimento di ledger.
        jdbcTemplate.update("INSERT INTO portafogli (utente_id) VALUES (?)", salvato.getId());

        return new RegistrazioneResponse(
                salvato.getId(),
                salvato.getUsername(),
                salvato.getEmail(),
                salvato.getRuolo()
        );
    }

}

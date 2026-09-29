package it.esercitazione.liveauction.producer.auth.services;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EliminazioneUtenteService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void elimina(long userId) {
        String anonymousId = UUID.randomUUID().toString();
        String passwordHash = passwordEncoder.encode(UUID.randomUUID().toString());
        jdbc.update("""
                UPDATE utenti
                SET attivo = FALSE,
                    username = ?,
                    email = ?,
                    password_hash = ?
                WHERE id = ? AND attivo = TRUE
                """, "deleted_" + anonymousId, "deleted_" + anonymousId + "@example.invalid",
                passwordHash, userId);
        jdbc.update("""
                UPDATE auth_sessions SET revoked_at = CURRENT_TIMESTAMP
                WHERE utente_id = ? AND revoked_at IS NULL
                """, userId);
    }
}

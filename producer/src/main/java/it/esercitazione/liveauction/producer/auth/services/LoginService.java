package it.esercitazione.liveauction.producer.auth.services;

import it.esercitazione.liveauction.producer.auth.models.Utente;
import it.esercitazione.liveauction.producer.auth.repos.UtenteRepository;
import it.esercitazione.liveauction.producer.auth.requests.LoginRequest;
import it.esercitazione.liveauction.producer.auth.responses.LoginResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class LoginService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UtenteRepository utenti;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JdbcTemplate jdbc;
    private final Duration tokenTtl;
    private final Duration refreshTtl;

    public LoginService(UtenteRepository utenti, PasswordEncoder passwordEncoder,
                        JwtEncoder jwtEncoder, JdbcTemplate jdbc,
                        @Value("${app.auth.token-ttl}") Duration tokenTtl,
                        @Value("${app.auth.refresh-ttl}") Duration refreshTtl) {
        this.utenti = utenti;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.jdbc = jdbc;
        this.tokenTtl = tokenTtl;
        this.refreshTtl = refreshTtl;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        Utente utente = utenti.findByUsername(username)
                .orElseThrow(LoginService::credenzialiNonValide);
        if (!utente.isAttivo() || !passwordEncoder.matches(request.password(), utente.getPasswordHash())) {
            throw credenzialiNonValide();
        }

        Instant now = Instant.now();
        Instant refreshExpiresAt = now.plus(refreshTtl);
        UUID sessionId = UUID.randomUUID();
        String refreshToken = newRefreshToken();
        jdbc.update("INSERT INTO auth_sessions (id, utente_id, refresh_token_hash, expires_at) VALUES (?, ?, ?, ?)",
                sessionId, utente.getId(), hash(refreshToken), Timestamp.from(refreshExpiresAt));
        return response(utente, sessionId, refreshToken, refreshExpiresAt, now);
    }

    @Transactional
    public LoginResponse refresh(String refreshToken) {
        String tokenHash = hash(refreshToken);
        List<Session> sessions = jdbc.query("""
                SELECT id, utente_id, expires_at FROM auth_sessions
                WHERE refresh_token_hash = ? AND revoked_at IS NULL FOR UPDATE
                """, (rs, row) -> new Session(rs.getObject("id", UUID.class),
                rs.getLong("utente_id"), rs.getTimestamp("expires_at").toInstant()), tokenHash);
        if (sessions.isEmpty()) {
            throw tokenNonValido();
        }
        Session session = sessions.getFirst();
        Instant now = Instant.now();
        if (!now.isBefore(session.expiresAt())) {
            throw tokenNonValido();
        }
        Utente utente = utenti.findById(session.userId())
                .filter(Utente::isAttivo)
                .orElseThrow(LoginService::tokenNonValido);
        String nextToken = newRefreshToken();
        jdbc.update("UPDATE auth_sessions SET refresh_token_hash = ? WHERE id = ?",
                hash(nextToken), session.id());
        return response(utente, session.id(), nextToken, session.expiresAt(), now);
    }

    @Transactional
    public void logout(UUID sessionId, long userId) {
        jdbc.update("UPDATE auth_sessions SET revoked_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND utente_id = ? AND revoked_at IS NULL", sessionId, userId);
    }

    private LoginResponse response(Utente utente, UUID sessionId, String refreshToken,
                                   Instant refreshExpiresAt, Instant now) {
        Instant expiresAt = now.plus(tokenTtl);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("liveauction-producer")
                .subject(utente.getId().toString())
                .claim("sid", sessionId.toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new LoginResponse(accessToken, "Bearer", expiresAt, refreshToken,
                refreshExpiresAt, utente.getId(), utente.getUsername(), utente.getRuolo());
    }

    private static String newRefreshToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 non disponibile", exception);
        }
    }

    private static ResponseStatusException credenzialiNonValide() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenziali non valide");
    }

    private static ResponseStatusException tokenNonValido() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token non valido");
    }

    private record Session(UUID id, long userId, Instant expiresAt) {}
}

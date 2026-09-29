package it.esercitazione.liveauction.producer.auth.responses;

import it.esercitazione.liveauction.producer.auth.models.Ruolo;

import java.time.Instant;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        String refreshToken,
        Instant refreshExpiresAt,
        Long userId,
        String username,
        Ruolo ruolo
) {
}

package it.esercitazione.liveauction.producer.auth.responses;

import it.esercitazione.liveauction.producer.auth.models.Ruolo;

public record RegistrazioneResponse (
        Long id,
        String username,
        String email,
        Ruolo ruolo
) {
}

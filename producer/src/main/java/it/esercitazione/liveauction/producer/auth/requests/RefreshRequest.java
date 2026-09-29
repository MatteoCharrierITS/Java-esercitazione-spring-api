package it.esercitazione.liveauction.producer.auth.requests;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank String refreshToken) {
}

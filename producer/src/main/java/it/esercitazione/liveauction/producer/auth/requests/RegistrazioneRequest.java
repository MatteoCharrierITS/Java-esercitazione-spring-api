package it.esercitazione.liveauction.producer.auth.requests;

import jakarta.validation.constraints.*;

public record RegistrazioneRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Email @Size(max = 200) String email,
        @NotBlank @Size(min = 8, max = 72) String password
) {
}

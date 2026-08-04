package com.carolinysilva.taskflow_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "E-mail é obrigatório")
        @Size(max = 255, message = "E-mail deve ter no máximo 255 caracteres")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Size(max = 72, message = "Senha deve ter no máximo 72 caracteres")
        String password
) {
}

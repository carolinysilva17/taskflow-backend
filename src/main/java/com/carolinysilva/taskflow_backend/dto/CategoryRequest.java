package com.carolinysilva.taskflow_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CategoryRequest(

        @NotBlank(message = "Nome é obrigatório")
        String name,

        @NotBlank(message = "Cor é obrigatória")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Cor deve estar no formato hexadecimal, ex: #4CAF50")
        String color
) {
}

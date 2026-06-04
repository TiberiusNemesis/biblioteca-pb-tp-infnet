package br.edu.infnet.biblioteca.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BookRequest(
        @NotBlank(message = "title must not be blank")
        @Size(max = 200)
        String title,

        @NotBlank(message = "author must not be blank")
        @Size(max = 120)
        String author,

        @NotBlank(message = "isbn must not be blank")
        @Pattern(regexp = "[0-9Xx\\-]{10,20}", message = "isbn has invalid format")
        String isbn,

        @Min(value = 1450, message = "publishedYear must be >= 1450")
        @Max(value = 2100, message = "publishedYear is unrealistically far in the future")
        int publishedYear
) {
}

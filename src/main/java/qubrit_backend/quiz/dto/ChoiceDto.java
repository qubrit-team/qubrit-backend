package qubrit_backend.quiz.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChoiceDto(Long id, String content, Boolean correct) {
}

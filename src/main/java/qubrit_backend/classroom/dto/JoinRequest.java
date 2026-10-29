package qubrit_backend.classroom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "참여 코드로 학급 참여 요청")
public record JoinRequest(@Schema(example = "K7M2QX4P") @NotBlank String code) {
}

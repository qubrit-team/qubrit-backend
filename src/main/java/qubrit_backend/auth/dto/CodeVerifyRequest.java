package qubrit_backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CodeVerifyRequest(
		@NotBlank @Email String email,
		@Pattern(regexp = "^\\d{6}$") String code) {
}

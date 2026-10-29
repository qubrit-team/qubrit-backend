package qubrit_backend.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

// 모든 오류를 {"message": "..."} 형태로 통일
@RestControllerAdvice
public class ApiExceptionHandler {

	public record ErrorResponse(String message) {
	}

	@ExceptionHandler(ResponseStatusException.class)
	ResponseEntity<ErrorResponse> handleStatus(ResponseStatusException e) {
		return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(e.getReason()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ErrorResponse("입력한 정보를 다시 확인해 주세요."));
	}
}

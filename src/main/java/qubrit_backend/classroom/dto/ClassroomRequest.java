package qubrit_backend.classroom.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "학급 만들기 요청. subject 또는 subjects 중 하나는 반드시 있어야 합니다.")
public record ClassroomRequest(
		@Schema(description = "학급 이름 (1~50자)", example = "미림마고 2-2") @NotBlank @Size(max = 50) String name,
		@Schema(description = "학급 분야 한 개 (1~30자). subjects를 보내면 생략 가능", example = "파이썬") @Size(max = 30) String subject,
		@Schema(description = "학교 이름", example = "미림마이스터고등학교") @Size(max = 50) String school,
		@Schema(description = "학년 (1~3)", example = "2") @Min(1) @Max(3) Integer grade,
		@Schema(description = "반", example = "3") @Min(1) @Max(30) Integer classNo,
		@Schema(description = "과목 복수 선택", example = "[\"파이썬\", \"자료구조\"]") @Size(max = 6) List<@NotBlank @Size(max = 30) String> subjects,
		@Schema(description = "참여 방식. APPROVAL=방장이 승인한 사람만, CODE=초대 코드만 있으면 바로 (기본 APPROVAL)", example = "APPROVAL") @Pattern(regexp = "APPROVAL|CODE") String joinMode,
		@Schema(description = "멤버도 기출을 올릴 수 있는지 (기본 true)", example = "true") Boolean memberUploadAllowed) {
}

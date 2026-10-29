package qubrit_backend.school;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "학교 검색 결과")
public record SchoolDto(Long id, String name, @Schema(description = "지역", example = "서울 구로구") String region) {
}

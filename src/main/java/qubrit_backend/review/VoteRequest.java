package qubrit_backend.review;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "문제 평가. UP은 좋아요, DOWN은 신고(이유 권장)")
public record VoteRequest(@NotNull VoteType vote, VoteReason reason) {
}

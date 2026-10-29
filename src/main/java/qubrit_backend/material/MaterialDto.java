package qubrit_backend.material;

import java.time.Instant;

public record MaterialDto(Long id, String title, String subject, String fileName, String contentType, long size,
		String uploaderNickname, boolean mine, Instant createdAt) {
}

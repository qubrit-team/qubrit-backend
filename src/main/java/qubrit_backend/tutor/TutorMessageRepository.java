package qubrit_backend.tutor;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TutorMessageRepository extends JpaRepository<TutorMessage, Long> {

	Optional<TutorMessage> findByUserIdAndKindAndMessageDate(Long userId, TutorKind kind, LocalDate date);
}

package qubrit_backend.subject;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// 과목이 하나도 없을 때만 기본 과목을 채운다
@Component
public class SubjectSeeder implements ApplicationRunner {

	private static final List<String> DEFAULTS = List.of("국어", "수학", "영어", "사회", "과학", "한국사");

	private final SubjectRepository subjectRepository;

	public SubjectSeeder(SubjectRepository subjectRepository) {
		this.subjectRepository = subjectRepository;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (subjectRepository.count() > 0) {
			return;
		}
		for (int i = 0; i < DEFAULTS.size(); i++) {
			subjectRepository.save(new Subject(DEFAULTS.get(i), i));
		}
	}
}

package qubrit_backend.classroom;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import qubrit_backend.classroom.domain.ClassroomSubject;
import qubrit_backend.classroom.domain.ClassroomSubjectRepository;

// 학급 과목이 하나도 없을 때만 기본 과목을 채운다
@Component
public class ClassroomSubjectSeeder implements ApplicationRunner {

	private static final List<String> DEFAULTS = List.of("파이썬", "자료구조", "알고리즘", "데이터베이스", "웹 프로그래밍", "자격증");

	private final ClassroomSubjectRepository repository;

	public ClassroomSubjectSeeder(ClassroomSubjectRepository repository) {
		this.repository = repository;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (repository.count() > 0) {
			return;
		}
		for (int i = 0; i < DEFAULTS.size(); i++) {
			repository.save(new ClassroomSubject(DEFAULTS.get(i), i));
		}
	}
}

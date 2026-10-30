package qubrit_backend.school;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// 학교가 하나도 없을 때만 임시 학교 목록을 채운다 (실제 학교 데이터 연동 전까지 사용)
@Component
public class SchoolSeeder implements ApplicationRunner {

	private static final List<School> SAMPLES = List.of(
			new School("미림마이스터고등학교", "서울 구로구"),
			new School("미림여자정보과학고등학교", "서울 구로구"),
			new School("대덕소프트웨어마이스터고등학교", "대전 유성구"),
			new School("대구소프트웨어마이스터고등학교", "대구 달성군"),
			new School("광주소프트웨어마이스터고등학교", "광주 광산구"),
			new School("부산소프트웨어마이스터고등학교", "부산 강서구"),
			new School("선린인터넷고등학교", "서울 용산구"),
			new School("한국디지털미디어고등학교", "서울 영등포구"),
			new School("서울고등학교", "서울 서초구"),
			new School("서울과학고등학교", "서울 종로구"),
			new School("미림중학교", "서울 구로구"),
			new School("미림초등학교", "서울 구로구"));

	private final SchoolRepository repository;

	public SchoolSeeder(SchoolRepository repository) {
		this.repository = repository;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (repository.count() == 0) {
			repository.saveAll(SAMPLES);
		}
	}
}

package qubrit_backend.classroom;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import qubrit_backend.auth.domain.User;
import qubrit_backend.auth.domain.UserRepository;
import qubrit_backend.classroom.domain.*;
import qubrit_backend.quiz.domain.*;
import qubrit_backend.subject.Subject;
import qubrit_backend.subject.SubjectRepository;

/**
 * 학급 화면 확인용 예시 데이터. qubrit.sample-data.email 에 가입한 계정의 이메일을 적었을 때만,
 * 그리고 예시 데이터가 아직 없을 때만 한 번 넣는다 (예시 계정 이메일은 @qubrit.sample).
 */
@Component
public class ClassroomSampleData implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(ClassroomSampleData.class);
	private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
	private static final String SAMPLE_DOMAIN = "@qubrit.sample";
	private static final int SAMPLE_STUDENTS = 27;

	private final String targetEmail;
	private final UserRepository userRepository;
	private final SubjectRepository subjectRepository;
	private final ClassroomRepository classroomRepository;
	private final ClassroomMemberRepository memberRepository;
	private final ClassroomInviteRepository inviteRepository;
	private final ClassroomSetRepository classroomSetRepository;
	private final ClassroomAssignmentRepository assignmentRepository;
	private final QuestionSetRepository setRepository;
	private final PasswordEncoder passwordEncoder;
	private final TransactionTemplate tx;

	public ClassroomSampleData(@Value("${qubrit.sample-data.email:}") String targetEmail,
			UserRepository userRepository, SubjectRepository subjectRepository,
			ClassroomRepository classroomRepository, ClassroomMemberRepository memberRepository,
			ClassroomInviteRepository inviteRepository, ClassroomSetRepository classroomSetRepository,
			ClassroomAssignmentRepository assignmentRepository, QuestionSetRepository setRepository,
			PasswordEncoder passwordEncoder, TransactionTemplate tx) {
		this.targetEmail = targetEmail.trim();
		this.userRepository = userRepository;
		this.subjectRepository = subjectRepository;
		this.classroomRepository = classroomRepository;
		this.memberRepository = memberRepository;
		this.inviteRepository = inviteRepository;
		this.classroomSetRepository = classroomSetRepository;
		this.assignmentRepository = assignmentRepository;
		this.setRepository = setRepository;
		this.passwordEncoder = passwordEncoder;
		this.tx = tx;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (targetEmail.isEmpty()) {
			return;
		}
		User me = userRepository.findByEmail(targetEmail).orElse(null);
		if (me == null) {
			log.warn("학급 예시 데이터: {} 계정이 없어 건너뜁니다. 먼저 가입해 주세요.", targetEmail);
			return;
		}
		if (userRepository.existsByEmail("yumin" + SAMPLE_DOMAIN)) {
			return;
		}
		tx.executeWithoutResult(status -> seed(me));
		log.info("학급 예시 데이터를 넣었습니다. ({})", targetEmail);
	}

	private void seed(User me) {
		User yumin = sampleUser("yumin", "이유민");
		List<User> students = new ArrayList<>();
		for (int i = 1; i <= SAMPLE_STUDENTS; i++) {
			students.add(sampleUser("student" + i, "학생" + i));
		}
		String python = "파이썬";
		String algorithm = "알고리즘";
		String certificate = "자격증";
		Instant now = Instant.now();

		// 목록은 최근에 만든 순이므로 화면 맨 아래 학급부터 만든다
		Classroom study = room(yumin, certificate, "정보처리기능사 스터디", me, students.subList(0, 4));
		share(study, yumin, certificate, 2, now.minus(30, ChronoUnit.DAYS));

		Classroom algo = room(yumin, algorithm, "2학년 알고리즘 심화반", me, students.subList(0, 13));
		share(algo, yumin, algorithm, 3, now.minus(20, ChronoUnit.DAYS));
		LocalDate dueDay = LocalDate.now(ZONE).plusDays(2);
		assignmentRepository.save(new ClassroomAssignment(algo.getId(), "그래프 탐색 과제",
				dueDay.atTime(23, 59).atZone(ZONE).toInstant()));

		Classroom mine = room(me, python, "미림마고 2학년 2반", null, students);
		share(mine, yumin, python, 3, now.minus(30, ChronoUnit.DAYS));
		share(mine, yumin, python, 2, now.minus(1, ChronoUnit.DAYS));

		Classroom invited = room(yumin, python, "미림마고 2-2 파이썬 스터디", null, students.subList(0, 11));
		share(invited, yumin, python, 18, now.minus(10, ChronoUnit.DAYS));
		inviteRepository.save(new ClassroomInvite(invited.getId(), yumin.getId(), me.getId()));
	}

	/** 방장(owner)과 멤버로 학급을 만든다. extra가 있으면 그 사람도, members가 있으면 그들도 멤버로 넣는다. */
	private Classroom room(User owner, String subject, String name, User extra, List<User> members) {
		Classroom room = classroomRepository.save(new Classroom(owner.getId(), subject, name, code()));
		memberRepository.save(new ClassroomMember(room, owner.getId()));
		if (extra != null) {
			memberRepository.save(new ClassroomMember(room, extra.getId()));
		}
		for (User m : members) {
			memberRepository.save(new ClassroomMember(room, m.getId()));
		}
		return room;
	}

	/** 학급에 공유된 기출 count개 (문제 없는 비공개 세트로 만든다) */
	private void share(Classroom room, User owner, String subject, int count, Instant sharedAt) {
		for (int i = 1; i <= count; i++) {
			QuestionSet set = setRepository.save(new QuestionSet(owner.getId(), subjectOf(subject),
					"[예시] " + room.getName() + " 기출 " + UUID.randomUUID().toString().substring(0, 4), null, code(),
					Visibility.PRIVATE, SetSource.MANUAL, SetStatus.PUBLISHED));
			classroomSetRepository.save(new ClassroomSet(room.getId(), set.getId(), sharedAt));
		}
	}

	private User sampleUser(String id, String nickname) {
		return userRepository.save(new User(id + SAMPLE_DOMAIN, passwordEncoder.encode(UUID.randomUUID().toString()),
				nickname, List.of()));
	}

	/** 문제 세트는 등록된 과목이 필요하므로 학급 분야와 같은 이름이 없으면 첫 과목을 쓴다 */
	private Subject subjectOf(String name) {
		return subjectRepository.findByName(name)
				.orElseGet(() -> subjectRepository.findAllByOrderBySortOrderAsc().get(0));
	}

	private String code() {
		return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
	}
}

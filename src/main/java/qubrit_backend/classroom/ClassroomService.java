package qubrit_backend.classroom;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import qubrit_backend.auth.domain.User;
import qubrit_backend.auth.domain.UserRepository;
import qubrit_backend.classroom.domain.*;
import qubrit_backend.classroom.dto.ClassroomRequest;
import qubrit_backend.classroom.dto.ClassroomSummary;
import qubrit_backend.classroom.dto.InviteDto;

@Service
public class ClassroomService {

	// 헷갈리는 글자(0/O, 1/I)는 뺀 참여 코드 문자
	private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final int CODE_LENGTH = 8;
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
	private static final int NEW_SET_DAYS = 7;

	private final ClassroomRepository classroomRepository;
	private final ClassroomMemberRepository memberRepository;
	private final ClassroomInviteRepository inviteRepository;
	private final ClassroomSetRepository classroomSetRepository;
	private final ClassroomAssignmentRepository assignmentRepository;
	private final UserRepository userRepository;

	public ClassroomService(ClassroomRepository classroomRepository, ClassroomMemberRepository memberRepository,
			ClassroomInviteRepository inviteRepository, ClassroomSetRepository classroomSetRepository,
			ClassroomAssignmentRepository assignmentRepository, UserRepository userRepository) {
		this.classroomRepository = classroomRepository;
		this.memberRepository = memberRepository;
		this.inviteRepository = inviteRepository;
		this.classroomSetRepository = classroomSetRepository;
		this.assignmentRepository = assignmentRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public List<ClassroomSummary> listMine(Long userId) {
		return summaries(classroomRepository.findJoinedBy(userId), userId);
	}

	/** 학급을 만들면 만든 사람이 방장이자 첫 멤버가 된다. */
	@Transactional
	public ClassroomSummary create(Long userId, ClassroomRequest req) {
		List<String> subjects = req.subjects() == null ? List.of()
				: req.subjects().stream().map(String::trim).filter(v -> !v.isEmpty()).distinct().toList();
		String main = !subjects.isEmpty() ? subjects.get(0) : req.subject() == null ? "" : req.subject().trim();
		if (main.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "과목을 하나 이상 골라 주세요.");
		}
		Classroom room = new Classroom(userId, main, req.name().trim(), newJoinCode());
		room.applySettings(req.school() == null ? null : req.school().trim(), req.grade(), req.classNo(),
				String.join(",", subjects), req.joinMode() == null ? "APPROVAL" : req.joinMode(),
				req.memberUploadAllowed() == null || req.memberUploadAllowed());
		room = classroomRepository.save(room);
		memberRepository.save(new ClassroomMember(room, userId));
		return summaries(List.of(room), userId).get(0);
	}

	@Transactional
	public ClassroomSummary join(Long userId, String code) {
		Classroom room = classroomRepository.findByJoinCode(code.trim().toUpperCase())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "참여 코드를 찾을 수 없습니다."));
		return addMember(room, userId);
	}

	/** 나에게 온 초대 (대기 중인 것만, 최근 순) */
	@Transactional(readOnly = true)
	public List<InviteDto> listInvites(Long userId) {
		List<ClassroomInvite> invites = inviteRepository
				.findByInviteeIdAndStatusOrderByCreatedAtDesc(userId, InviteStatus.PENDING);
		if (invites.isEmpty()) {
			return List.of();
		}
		Map<Long, Classroom> rooms = classroomRepository
				.findAllById(invites.stream().map(ClassroomInvite::getClassroomId).toList()).stream()
				.collect(Collectors.toMap(Classroom::getId, Function.identity()));
		Map<Long, String> inviters = userRepository
				.findAllById(invites.stream().map(ClassroomInvite::getInviterId).toList()).stream()
				.collect(Collectors.toMap(User::getId, User::getNickname));
		List<Long> roomIds = List.copyOf(rooms.keySet());
		Map<Long, Integer> members = counts(memberRepository.countByClassrooms(roomIds));
		Map<Long, Integer> sets = counts(classroomSetRepository.countByClassrooms(roomIds));
		return invites.stream()
				.filter(i -> rooms.containsKey(i.getClassroomId()))
				.map(i -> new InviteDto(i.getId(), inviters.getOrDefault(i.getInviterId(), "알 수 없음"),
						i.getClassroomId(), rooms.get(i.getClassroomId()).getName(),
						members.getOrDefault(i.getClassroomId(), 0), sets.getOrDefault(i.getClassroomId(), 0)))
				.toList();
	}

	@Transactional
	public ClassroomSummary acceptInvite(Long userId, Long inviteId) {
		ClassroomInvite invite = pendingInvite(userId, inviteId);
		Classroom room = classroomRepository.findById(invite.getClassroomId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "학급을 찾을 수 없습니다."));
		invite.respond(InviteStatus.ACCEPTED);
		return memberRepository.existsByClassroomIdAndUserId(room.getId(), userId)
				? summaries(List.of(room), userId).get(0)
				: addMember(room, userId);
	}

	@Transactional
	public void declineInvite(Long userId, Long inviteId) {
		pendingInvite(userId, inviteId).respond(InviteStatus.DECLINED);
	}

	private ClassroomInvite pendingInvite(Long userId, Long inviteId) {
		ClassroomInvite invite = inviteRepository.findById(inviteId)
				.filter(i -> i.getInviteeId().equals(userId))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "초대를 찾을 수 없습니다."));
		if (invite.getStatus() != InviteStatus.PENDING) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 처리한 초대입니다.");
		}
		return invite;
	}

	private ClassroomSummary addMember(Classroom room, Long userId) {
		if (memberRepository.existsByClassroomIdAndUserId(room.getId(), userId)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 참여 중인 학급입니다.");
		}
		memberRepository.save(new ClassroomMember(room, userId));
		return summaries(List.of(room), userId).get(0);
	}

	private List<ClassroomSummary> summaries(List<Classroom> rooms, Long userId) {
		if (rooms.isEmpty()) {
			return List.of();
		}
		List<Long> ids = rooms.stream().map(Classroom::getId).toList();
		Instant now = Instant.now();
		Map<Long, Integer> members = counts(memberRepository.countByClassrooms(ids));
		Map<Long, Integer> recentSets = counts(
				classroomSetRepository.countSharedSince(ids, now.minus(NEW_SET_DAYS, ChronoUnit.DAYS)));
		Map<Long, Integer> dday = new HashMap<>();
		LocalDate today = LocalDate.now(ZONE);
		for (Object[] row : assignmentRepository.nextDueByClassrooms(ids, now)) {
			LocalDate due = ((Instant) row[1]).atZone(ZONE).toLocalDate();
			dday.put((Long) row[0], (int) ChronoUnit.DAYS.between(today, due));
		}
		return rooms.stream().map(c -> {
			boolean owner = c.isOwner(userId);
			return new ClassroomSummary(c.getId(), c.getName(), c.getSubject(),
					members.getOrDefault(c.getId(), 0), owner, owner ? c.getJoinCode() : null,
					recentSets.getOrDefault(c.getId(), 0), dday.get(c.getId()));
		}).toList();
	}

	private static Map<Long, Integer> counts(List<Object[]> rows) {
		return rows.stream().collect(Collectors.toMap(row -> (Long) row[0], row -> ((Long) row[1]).intValue()));
	}

	private String newJoinCode() {
		while (true) {
			StringBuilder sb = new StringBuilder(CODE_LENGTH);
			for (int i = 0; i < CODE_LENGTH; i++) {
				sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
			}
			String code = sb.toString();
			if (!classroomRepository.existsByJoinCode(code)) {
				return code;
			}
		}
	}
}

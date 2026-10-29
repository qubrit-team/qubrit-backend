package qubrit_backend.classroom.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassroomMemberRepository extends JpaRepository<ClassroomMember, Long> {

	boolean existsByClassroomIdAndUserId(Long classroomId, Long userId);

	/** 학급별 멤버 수: [학급 id, 멤버 수] */
	@Query("select m.classroom.id, count(m) from ClassroomMember m where m.classroom.id in :ids group by m.classroom.id")
	List<Object[]> countByClassrooms(@Param("ids") List<Long> ids);
}

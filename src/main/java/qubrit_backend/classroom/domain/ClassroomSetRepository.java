package qubrit_backend.classroom.domain;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassroomSetRepository extends JpaRepository<ClassroomSet, Long> {

	/** 학급별 공유된 기출 수: [학급 id, 개수] */
	@Query("select s.classroomId, count(s) from ClassroomSet s where s.classroomId in :ids group by s.classroomId")
	List<Object[]> countByClassrooms(@Param("ids") List<Long> ids);

	/** 학급별로 since 이후에 공유된 기출 수: [학급 id, 개수] */
	@Query("select s.classroomId, count(s) from ClassroomSet s where s.classroomId in :ids and s.sharedAt >= :since group by s.classroomId")
	List<Object[]> countSharedSince(@Param("ids") List<Long> ids, @Param("since") Instant since);
}

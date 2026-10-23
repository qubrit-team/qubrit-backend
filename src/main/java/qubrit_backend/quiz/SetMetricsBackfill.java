package qubrit_backend.quiz;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// 서버를 켤 때 모든 세트의 지표를 최신 풀이/평가 기준으로 맞춘다
@Component
public class SetMetricsBackfill implements ApplicationRunner {

	private final SetMetricsService metrics;

	public SetMetricsBackfill(SetMetricsService metrics) {
		this.metrics = metrics;
	}

	@Override
	public void run(ApplicationArguments args) {
		metrics.refreshAll();
	}
}

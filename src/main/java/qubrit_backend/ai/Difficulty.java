package qubrit_backend.ai;

public enum Difficulty {
	EASY("쉬움: 핵심 개념을 그대로 묻는 수준"),
	MEDIUM("보통: 개념을 이해하고 적용해야 풀 수 있는 수준"),
	HARD("어려움: 여러 개념을 종합하거나 함정이 있는 수준");

	private final String hint;

	Difficulty(String hint) { this.hint = hint; }

	public String hint() { return hint; }
}

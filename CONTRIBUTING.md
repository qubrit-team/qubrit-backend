## Branch

- `main`: **직접 push 금지**, PR로만 병합
- 작업 시 `main`에서 새 브랜치를 생성해서 작업

### Branch Naming

| 종류      | 형식             | 예시                  |
| --------- | ---------------- | --------------------- |
| 기능 개발 | `feature/기능명` | `feature/user-signup` |
| 버그 수정 | `fix/버그명`     | `fix/login-error`     |

## Commit Message

| 태그           | 용도          |
| -------------- | ------------- |
| `[Feat]::`     | 새 기능 추가  |
| `[Fix]::`      | 버그 수정     |
| `[Docs]::`     | 문서 수정     |
| `[Refactor]::` | 코드 리팩토링 |
| `[Chore]::`    | 기타 수정     |

```
[Feat]:: 문제 생성 API 구현
[Fix]:: 로그인 오류 수정
```

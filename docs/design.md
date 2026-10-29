# Qubrit 백엔드 설계 (0단계)

해커톤 목표: 직접 출제 · AI 출제(Gemini) · 문제 품질 검증/추천 · 과외쌤 AI · 학습 자료실.
캘린더·푸시·AI 면접은 이번 범위 밖이라 마지막 절에 확장 지점만 적는다.

## 1. 공통 규칙

- 패키지: 기능별로 `qubrit_backend.<기능>` 아래 `domain / dto / (Controller, Service)` (기존 `auth`와 동일).
- 인증: `Authorization: Bearer {accessToken}`. `permitAll`로 열지 않은 모든 API는 로그인 필요. 컨트롤러에서 `@AuthenticationPrincipal Long userId`.
- 오류: `{"message": "..."}` (`ApiExceptionHandler`). 상태코드 400 입력오류 / 401 미로그인 / 403 권한없음 / 404 없음 / 409 충돌.
- 목록: `?page=0&size=20` → `{ "items": [...], "page": 0, "size": 20, "totalItems": 134 }`.
- 시간: ISO-8601 문자열(UTC). ID는 `Long`.
- 스키마: `ddl-auto=update` (해커톤용). 마이그레이션 도구는 쓰지 않는다.
- 과목: `subjects` 테이블이 기준. 기존 `user_subjects`(과목 이름 문자열)는 이번엔 유지하고, 새 테이블들은 `subject_id`를 쓴다.

## 2. ERD

```
users (기존) ── id, email, password, nickname, [+] role(STUDENT|TEACHER), [+] weekly_goal(기본 15)   ※ 추가 컬럼은 필요해지는 단계(4단계 등)에서 기본값과 함께 넣는다
user_subjects (기존) ── user_id, subject(이름)

subjects ── id, name(unique), sort_order

question_sets ── id, owner_id→users, subject_id→subjects, title, description,
                 share_code(unique, 6자), visibility(PUBLIC|LINK|PRIVATE),
                 source(MANUAL|AI), material_id→materials(null),
                 status(DRAFT|PUBLISHED), question_count, created_at

questions ── id, set_id→question_sets, position, type(MCQ|SHORT), content,
             explanation, answer_text(SHORT 정답), difficulty(1~5, null),
             quality_status(PENDING|PASS|WARN|FAIL), created_at
question_choices ── id, question_id→questions, position, content, is_correct

attempts ── id, user_id→users, set_id→question_sets,
            status(IN_PROGRESS|DONE), score, total, started_at, finished_at
attempt_answers ── id, attempt_id→attempts, question_id→questions,
                   selected_choice_id(null), answer_text(null),
                   is_correct, ai_feedback(null), answered_at
                   unique(attempt_id, question_id)

materials ── id, uploader_id→users, subject_id→subjects, title,
             file_name, content_type, size, storage_path, extracted_text(null), created_at

question_votes ── id, question_id→questions, user_id→users, vote(UP|DOWN),
                  reason(null: WRONG_ANSWER|AMBIGUOUS|TOO_EASY|TOO_HARD|OTHER), created_at
                  unique(question_id, user_id)
question_reviews ── id, question_id→questions, verdict(PASS|WARN|FAIL),
                    issues(text, JSON 배열), reviewed_at     -- AI 검증 결과

wrong_notes ── id, user_id→users, question_id→questions, resolved(bool),
               wrong_count, last_wrong_at         unique(user_id, question_id)
tutor_messages ── id, user_id→users, kind(DAILY|ANALYSIS), date, content(JSON), created_at
                  unique(user_id, kind, date)      -- Gemini 응답 캐시(호출 비용/지연 절감)
```

관계 요약: `users 1─N question_sets 1─N questions 1─N question_choices`,
`users 1─N attempts N─1 question_sets`, `attempts 1─N attempt_answers N─1 questions`,
`materials 1─N question_sets(source=AI)`.

설계 메모
- **스트릭/주간 목표**는 별도 테이블 없이 `attempts`·`attempt_answers`에서 날짜별로 계산한다.
- **오답 노트**는 풀이 제출 때 `wrong_notes`를 갱신한다(틀리면 생성/`wrong_count++`, 다시 맞히면 `resolved=true`).
- **`quality_status`**는 `question_reviews`(AI)와 `question_votes`(사용자)로 갱신하는 요약값이다. 풀이 화면에서 FAIL은 숨긴다.
- **객관식 정답**은 `question_choices.is_correct`가 기준(정답 1개). 풀이 응답에는 정답을 제출 전까지 내려주지 않는다.

## 3. API 명세

표기: 🔓 = 로그인 불필요, 나머지는 로그인 필요.

### 3-1. 인증 (구현됨)
| | 경로 | 설명 |
|---|---|---|
| POST 🔓 | `/api/auth/signup` | 회원가입 |
| GET 🔓 | `/api/auth/check-email?email=` | 이메일 중복확인 |
| POST 🔓 | `/api/auth/login` | 로그인 → `{accessToken, email, nickname}` |
| GET | `/api/auth/me` | 내 정보 |

### 3-2. 과목 (0단계에서 구현)
| | 경로 | 응답 |
|---|---|---|
| GET 🔓 | `/api/subjects` | `["국어","수학","영어","사회","과학","한국사"]` (이름 문자열 배열, `sort_order` 순) |

> 프론트 홈 화면이 문자열 배열을 기대하므로 이 형태를 유지한다. `subjectId`가 필요해지면 `/api/subjects/detail`에서 `[{id,name}]`을 제공한다.

### 3-3. 문제 세트 (1단계, 구현 완료)
| | 경로 | 설명 |
|---|---|---|
| POST | `/api/sets` | 세트 생성(문제 포함 가능) |
| GET | `/api/sets?subject=국어&scope=mine\|public&q=&page=` | 목록 (`scope=recommended`는 3단계) |
| GET | `/api/sets/{id}` | 상세(문제 목록, 정답 제외) |
| GET | `/api/sets/code/{shareCode}` | 공유 코드로 조회 |
| PUT | `/api/sets/{id}` | 수정(소유자만) |
| DELETE | `/api/sets/{id}` | 삭제(소유자만) |
| POST | `/api/sets/{id}/questions` | 문제 추가 |
| PUT | `/api/questions/{id}` | 문제 수정 |
| DELETE | `/api/questions/{id}` | 문제 삭제 |

문제 생성 요청 예:
```json
{ "type": "MCQ", "content": "다음 중 ...", "explanation": "...",
  "choices": [{"content":"A","correct":false},{"content":"B","correct":true}] }
{ "type": "SHORT", "content": "...", "answerText": "정답", "explanation": "..." }
```

### 3-4. 풀이 (1단계, 구현 완료)

구현된 동작 메모
- 세트 상세: 출제자가 아니면 `explanation`, `answerText`, `choices[].correct`, `qualityStatus`를 내려주지 않는다.
- 공개 범위: `PUBLIC`(목록 노출) / `LINK`(코드·ID로만) / `PRIVATE`(출제자만). 열람 불가 세트는 404.
- 공유 코드: 6자(헷갈리는 0/O/1/I 제외), 조회 시 대소문자 무관.
- 답 제출은 문제당 1번(재제출 409). 객관식은 선택지 ID로 채점, 주관식은 공백·대소문자를 무시한 문자열 비교(2단계에서 AI 채점으로 교체).
- 틀리면 `wrong_notes`에 기록, 같은 문제를 이후 맞히면 `resolved`.
- 세트/문제 삭제 시 해당 풀이 기록과 오답 노트도 함께 삭제된다.

| | 경로 | 설명 |
|---|---|---|
| POST | `/api/sets/{id}/attempts` | 풀이 시작(진행 중인 게 있으면 그걸 반환) → `{attemptId}` |
| PUT | `/api/attempts/{id}/answers/{questionId}` | 답 제출 `{selectedChoiceId}` 또는 `{answerText}` → `{correct, explanation, correctAnswer, aiFeedback}` (주관식은 2단계에서 AI 채점, 1단계는 문자열 비교) |
| POST | `/api/attempts/{id}/finish` | 종료 → `{score,total}` |
| GET | `/api/attempts/{id}` | 결과/진행 상태 |
| GET | `/api/me/attempts?status=` | 내 풀이 기록, `IN_PROGRESS`는 홈의 "이어서 풀기" |

### 3-5. 학습 자료실 · AI 출제 (2단계, 구현 완료)

구현된 동작 메모
- 업로드: pdf, txt, md, png, jpg(jpeg), 최대 20MB. 형식은 확장자로 판단하고 저장 이름은 UUID. 파일은 `storage.dir`(기본 `uploads/`, git 제외)에 저장.
- 자료는 로그인한 모두가 열람/다운로드(과목 단위 공개), 삭제는 올린 사람만.
- AI 출제: txt/md는 본문을 프롬프트에 넣고(최대 3만 자), pdf/이미지는 Gemini에 파일째 첨부. 결과는 항상 `DRAFT` 세트(`source=AI`, `material_id` 연결)로 저장되어 출제자만 볼 수 있고, 검토·수정 후 `/api/sets/{id}/publish`로 발행.
- 형식에 어긋난 AI 문제(객관식 정답 ≠ 1개 등)는 건너뛰고, 하나도 못 쓰면 502.
- 주관식 채점: 정답과 정확히 일치하면 AI 없이 정답, 아니면 Gemini가 출제자 정답 기준으로 판단하고 `aiFeedback`을 돌려준다. AI를 쓸 수 없으면 오답 처리.
- 키 미설정이면 AI 출제는 503, 호출 실패/응답 오류는 502.
- 설정: `GEMINI_API_KEY`(환경변수), `gemini.model`(기본 `gemini-2.5-flash`), `gemini.base-url`.
- 실제 Gemini와의 호출은 가짜 서버로만 검증했다. 키 연결 후 실제 응답으로 한 번 확인할 것.

| | 경로 | 설명 |
|---|---|---|
| POST | `/api/materials` (multipart: `file`, `title`, `subject`) | 업로드 |
| GET | `/api/materials?subject=&q=&page=` | 목록 |
| GET | `/api/materials/{id}` | 상세 |
| GET | `/api/materials/{id}/download` | 원본 다운로드 |
| DELETE | `/api/materials/{id}` | 삭제(업로더만) |
| POST | `/api/materials/{id}/generate` `{count, types:["MCQ","SHORT"], difficulty}` | 자료 기반 AI 출제 → **DRAFT 세트 생성**, 사용자가 검토 후 발행 |
| POST | `/api/ai/generate` `{subject, topic, count, types}` | 자료 없이 주제만으로 출제 보조 |
| POST | `/api/sets/{id}/publish` | DRAFT → PUBLISHED |

### 3-6. 품질 검증 · 추천 (3단계, 구현 완료)

구현된 동작 메모
- 경로 변경/추가: 평가 `POST /api/questions/{id}/votes`, 품질 조회 `GET /api/questions/{id}/quality`, AI 검증 `POST /api/questions/{id}/review`(출제자만), 이력 `GET /api/questions/{id}/reviews`(출제자만), 세트 전체 검증 `POST /api/sets/{id}/review`(출제자만, 문제 수만큼 Gemini 호출), 추천 `GET /api/recommendations?subject=&size=`.
- 평가: 1인 1표(다시 보내면 변경), 내가 만든 문제는 불가, 발행된 세트만. 좋아요에는 이유를 저장하지 않는다.
- 품질 상태(`qualityStatus`)는 AI 검증과 평가를 합쳐 계산한다 (`QualityService.decide`).
  - FAIL: AI가 FAIL, 또는 👎 5개 이상이면서 👍의 2배 초과
  - WARN: AI가 WARN, 또는 👎 3개 이상이면서 👍보다 많음
  - PASS: AI가 PASS, 또는 👍 3개 이상이면서 👎보다 많음
  - 그 외 PENDING
- FAIL 문제는 풀이자에게 숨겨지고(세트 상세·문제 수·풀이 total에서 제외, 답 제출 404), 출제자는 계속 볼 수 있다. 상태/AI 검증 결과(`aiReview.issues`)는 출제자에게만 내려간다.
- 문제를 수정하면 이전 평가와 AI 검증은 삭제되고 PENDING으로 돌아간다.
- 추천: 규칙 기반 점수 = 내 과목 오답률(최대 +3, 3문제 미만이면 0.5로 가정) + 관심 과목(+1) + 다른 사용자 평가(±0.9) + 최신도(최대 +0.3). 내가 만들었거나 이미 푼 세트, 평가가 크게 나쁜(순 👎 3 이상) 세트는 제외. 응답의 `reason`에 이유가 담긴다.
- AI 검증은 가짜 Gemini 서버로만 검증했다. 키 연결 후 실제 판정 품질을 확인할 것.

| | 경로 | 설명 |
|---|---|---|
| POST | `/api/questions/{id}/votes` `{vote, reason}` | 좋아요/신고(1인 1표, 재요청 시 갱신) |
| POST | `/api/questions/{id}/review` | AI 검증 실행 → `{verdict, issues[]}` (발행 시 자동 실행도 가능) |
| GET | `/api/questions/{id}/reviews` | 검증 결과 조회 |
| GET | `/api/recommendations?subject=` | 사용자 맞춤 세트 추천(규칙 기반: 오답 비율 높은 과목/약점 우선) |

### 3-7. 과외쌤 · 오답 노트 · 홈 (4단계, 구현 완료)

구현된 동작 메모
- `GET /api/home` 응답: `{nickname, tutor:{headline,cheer,focusSubject,focusCount,ai}, weeklyGoal:{done,goal}, streak, wrongNoteCount, inProgress:[AttemptSummary]}`. 화면의 "수아님, 오늘은 …" 문구는 `nickname + "님, " + tutor.headline`.
- 스트릭: 하루(KST)에 답을 한 번이라도 제출한 날의 연속 일수. 오늘 아직 안 풀었어도 어제까지의 연속은 유지. 주간 진행: 이번 주(월~일, KST)에 제출한 답 개수 / `users.weekly_goal`(기본 15, `PUT /api/me/goal`).
- 과외쌤(`/api/tutor/today`, `/api/tutor/analysis`): 풀이 기록 요약을 Gemini에 넘겨 사용자·종류·날짜별로 하루 한 번만 생성해 `tutor_messages`에 캐시. 풀이 기록이 없는 신규 사용자는 AI를 부르지 않는다. AI를 쓸 수 없거나 실패하면 `ai=false`인 규칙 기반 문구로 대체하고(홈은 절대 실패하지 않음), 실패 후 60초간은 AI 호출을 건너뛴다. 분석의 과목별 정답률은 항상 DB 기준 최신값.
- 오답 노트: `GET /api/wrong-notes?subject=&resolved=false`, `POST /api/wrong-notes/retry {subject?, size?}`. 복습은 해결 못 한 오답을 복사한 비공개 세트(`review_set=true`, 목록·추천에 안 나옴)를 만들고 풀이를 바로 시작한다. 복습에서 맞히면 원본 오답 노트가 해결 처리되고, 또 틀리면 `wrongCount`가 오른다.
- 스키마 주의: Postgres는 enum 컬럼에 check 제약을 만들고 `ddl-auto=update`는 이를 갱신하지 않는다. 그래서 기존 enum에 값을 추가하는 대신 새 컬럼(`review_set`)을 썼다. 앞으로도 기존 enum에 값을 추가하지 말 것(추가하려면 DB의 제약을 직접 수정해야 한다).

| | 경로 | 설명 |
|---|---|---|
| GET | `/api/home` | 홈 한 번에: `{nickname, tutorMessage, weeklyGoal:{done,goal}, streak, inProgress:[...]}` |
| GET | `/api/tutor/today` | 오늘의 추천 + 응원 메시지(하루 1회 생성·캐시) |
| GET | `/api/tutor/analysis` | 과목/유형별 정답률, 자주 틀리는 부분(Gemini 요약) |
| GET | `/api/wrong-notes?subject=&resolved=` | 오답 노트 |
| POST | `/api/wrong-notes/retry` | 오답만 모아 새 풀이(attempt) 생성 |
| PUT | `/api/me/goal` `{weeklyGoal}` | 주간 목표 변경 |

## 4. Gemini 연동 방침

- `ai/GeminiClient`(인터페이스 + 구현) 한 곳에서만 외부 호출. 서비스는 인터페이스에만 의존 → 목(mock)으로 교체 가능.
- 설정: `gemini.api-key`, `gemini.model`, 타임아웃 설정을 `application.properties`에 추가(예시 파일에도 키 이름만).
- 출력은 **JSON 스키마 강제**(structured output)로 받고, 파싱 실패 시 1회 재시도 후 `502`로 응답.
- 용도별 프롬프트 4종: ① 자료 기반 출제 ② 주관식 채점 ③ 문제 품질 검증 ④ 과외쌤 분석/추천.
- 자료는 텍스트 추출 후 길이 제한(자르기)해서 전달. PDF 직접 전달 여부는 2단계 시작 때 결정.
- 호출이 느리므로 프론트는 로딩 상태를 가정하고, 과외쌤 결과는 `tutor_messages`에 캐시한다.

## 5. 단계별 패키지 계획

```
subject/   (0단계)  Subject, SubjectRepository, SubjectController, SubjectSeeder
quiz/      (1단계)  QuestionSet, Question, QuestionChoice, Attempt, AttemptAnswer, WrongNote
material/  (2단계)  Material, 저장소(파일), 텍스트 추출
ai/        (2단계)  GeminiClient, 프롬프트, 응답 DTO
review/    (3단계)  QuestionVote, QuestionReview, 추천
tutor/     (4단계)  TutorMessage, 분석/추천 서비스, HomeController
```

## 6. 이번 범위 밖 (확장 지점)

- **시험 일정 캘린더·푸시**: `exams(id, class_id, title, date, ...)`와 알림 테이블이 필요하고, 반(class) 개념이 먼저 있어야 한다.
- **반/과목별 자료실**: 지금 자료실은 과목 단위 전체 공개. 반 개념(`class_groups`, 멤버)은 이후 추가하고 `materials.class_id`(null 허용)를 붙이면 된다.
- **AI 면접**: 음성 인식(STT)과 면접 질문 세트는 `question_sets`에 `type=INTERVIEW`를 두는 방식으로 연결 가능(사용자 질문 공유 요건).
- **교사 역할**: `users.role`만 미리 두고, 권한 분기는 일정 기능이 생길 때 적용.

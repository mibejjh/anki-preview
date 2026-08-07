# Anki 당일 복습 카드 미리보기 앱 — 구현 계획

## 1. 문제 정의

AnkiDroid로 영어 단어장을 공부할 때, **플래시카드 학습(뒤집기 시험)** 전에 그날 등장할 카드 목록을 미리 큰 글씨로 출력해 외우고 싶다.

- AnkiDroid 내장 카드 탐색기는 글자가 작고 인쇄 기능이 없다.
- 주 사용 환경은 **Android**.
- AnkiDroid의 **3rd party API**를 이용해 앱을 만들고 싶다.

**목표**: "오늘 예정된 카드"를 읽기 좋은 큰 글씨 목록으로 보여주는 Android 앱.

---

## 2. 기술 조사 결과 (핵심)

AnkiDroid는 **ContentProvider 기반 API**를 제공한다. 검색한 결과, 이 API는 단순 카드 추가(intent)만이 아니라 **읽기(조회) 기능**도 갖추고 있어 계획이 매우 유리하다.

| 항목 | 확인 내용 |
|------|-----------|
| 조회 URI | `content://com.ichi2.anki.flashcards/cards` |
| 검색 문법 | Anki 브라우저 문법 그대로 지원 → `is:due`, `is:new`, `is:review`, `is:learn`, `deck:`, `tag:` 등 |
| 카드 필드 | `question`, `answer`, `question_simple`, `answer_simple`, `type`(0신규/1학습/2복습/3재학습), `queue`, `due`, `deck_id`, `note_id`, `card_name` |
| 덱 정보 | `content://.../decks` → `DECK_COUNTS`(신규/학습/복습 개수 JSON), `OPTIONS`(일일 한도 JSON) |
| 노트/노트타입 | `notes`, `models` URI로 필드·탬플릿 조회 가능 |
| 권한 | `com.ichi2.anki.permission.READ_WRITE_DATABASE` (normal 권한, manifest 병합) |
| 전제조건 | AnkiDroid 설정에서 **"AnkiDroid API 사용"** 활성화 필요, API 30+는 `<queries>` 패키지 선언 필요 |

### 핵심 이점
- **`DECK_COUNTS` 가 오늘 스케줄러가 계산한 신규/학습/복습 개수를 정확히 제공** → 스케줄링 로직(SRS, fuzz, 일일 한도)을 직접 재현할 필요가 없다.
- `is:due` + `is:new` 결합으로 "오늘 카드" 목록을 정확히 구성 가능.

> 참고: AnkiDroid의 `collection.anki2`(SQLite) 직접 읽기는 scoped storage 제약과 스케줄러 재현 문제가 있어 **비채택**. ContentProvider가 더 안전하고 공식적이다.

---

## 3. 아키텍처 설계

```
┌─────────────────────────────────────────────┐
│              Android 앱 (Kotlin)             │
│                                             │
│  [UI] Jetpack Compose + Material 3          │
│    ├─ 오늘 카드 목록 (덱별 그룹, 큰 글씨)     │
│    └─ 설정 / 인쇄 / TTS / AnkiDroid 실행     │
│                                             │
│  [Data] ContentProvider 래퍼 계층            │
│    ├─ AnkiRepository (ContentResolver 호출)  │
│    ├─ 오늘 카드 산출 로직                    │
│    └─ DTO (Card, Deck, TodayPlan)           │
│                                             │
│  [출력]                                       │
│    ├─ 텍스트/PDF 공유 (인쇄)                 │
│    ├─ TTS 발음 (영어 단어 읽기)               │
│    └─ AnkiDroid 학습 실행 (intent)           │
└─────────────────────────────────────────────┘
                │ ContentResolver
                ▼
┌─────────────────────────────────────────────┐
│   AnkiDroid ContentProvider (3rd party API)  │
│   content://com.ichi2.anki.flashcards/...    │
└─────────────────────────────────────────────┘
```

**기술 스택**
- 언어: Kotlin
- UI: Jetpack Compose + Material 3
- 최소 SDK: 26 (Android 8.0+), 대상 SDK: 최신
- 의존성: Coroutines, (선택) ViewModel, TTS는 시스템 API
- AnkiDroid 연동: `com.github.ankidroid:Anki-Android:api-v1.1.0` 아티팩트 또는 상수 직접 정의

---

## 4. 데이터 흐름 (오늘 카드 산출)

```
1. 권한 확인 → AnkiDroid 설치/API 활성화 확인
2. decks 조회 → DECK_COUNTS 로 덱별 [신규N, 학습L, 복습R] 획득
3. 카드 조회:
   ├─ is:due            → 오늘 예정 복습+학습 카드 (정확, 한도 불필요)
   └─ deck:.. is:new      → 신규 카드 전부, DECK_COUNTS["new"] 개수만큼 선취
4. 노트 필드 결합 → question/answer 와 단어/뜻 매핑
5. 덱별 그룹 + 신규/학습/복습 배지로 랜더링
```

**검증 지표**: 산출 카드 수 합계 == DECK_COUNTS 합계 (자동 일치 검사로 정확성 보장).

---

## 5. UI/UX 설계

- **홈 화면(오늘 카드)**: 덱별로 접이식 그룹, 카드당 "단어(굵게/크게) + 뜻", 신규/학습/복습 색상 배지.
- **글자 크기 조절**: 슬라이더로 폰트 배율 조정 (복습용 크게, 참고용 작게).
- **발음(TTS)**: 카드 탭 또는 버튼으로 영어 단어 발음 재생.
- **인쇄/PDF**: 목록을 텍스트/PDF로 내보내기 → Android 공유 시트(프린터, 클라우드 등).
- **테마**: 다크/라이트, 대비 높은 글씨.
- **네비게이션**: 오늘 / 전체덱 / 설정.
- **AnkiDroid 연동 버튼**: "학습 시작" → `com.ichi2.anki.ACTION_SHOW_CARDS` intent로 AnkiDroid 학습 화면 직접 실행 (앙키에서 플래시카드 시험).

---

## 6. 개발 단계 (Phase)

### Phase 0 — 환경 구축 & 스파이크
- [ ] Android 프로젝트 생성, Gradle 설정
- [ ] AnkiDroid API 활성화 확인, ContentProvider 연결 스파이크(샘플 덱 조회)
- [ ] `DECK_COUNTS` 정확성 검증 (실기기)

### Phase 1 — 데이터 계층
- [ ] ContentProvider 상수/DTO 정의
- [ ] `AnkiRepository`: 덱·카드·노트 조회, 오늘 카드 산출 로직
- [ ] 단위 테스트 (검색 문법, 카운트 일치)

### Phase 2 — UI
- [ ] Compose 화면: 덱 그룹 목록, 배지, 폰트 슬라이더
- [ ] 신규/학습/복습 필터, 덱 선택
- [ ] TTS 발음, 다크모드

### Phase 3 — 출력 & 연동
- [ ] 텍스트/PDF 내보내기 + 공유 시트
- [ ] AnkiDroid 학습 실행 intent
- [ ] 설정 화면 (일일 한도 표시, 정렬)

### Phase 4 — 안정화
- [ ] 빈 상태/API 미활성/미설치 안내
- [ ] 성능 최적화 (커서·메모리), 오류 처리
- [ ] Play Store(또는 APK) 배포 준비

---

## 7. 리스크 및 대안

| 리스크 | 대응 |
|--------|------|
| AnkiDroid API 비활성화 시 접근 불가 | 설정 유도 안내, 미설치 감지 |
| 신규 일일 한도와 `is:new` 결과 불일치 | `DECK_COUNTS["new"]`로 개수 제한 |
| 한 카드가 복습+학습 동시 검색 | `is:due` 단일 검색으로 중복 제거 |
| API 버전별 필드 차이 | 버전 대응, 상수 저버전 fallback |
| (대안) PC Anki | AnkiConnect(HTTP)로 동일 기능 확장 가능 |

---

## 8. 다음 행동

1. **Phase 0 스파이크**부터 진행해 ContentProvider 실제 동작 검증
2. 실기기에서 DECK_COUNTS 와 is:due 결과 일치 확인 후 앱 본격 구현
# AGENTS.md

이 프로젝트에서 작업하는 모든 에이전트(및 sub agent)는 아래 규칙을 반드시 준수한다.

## 1. 커밋 규칙

- 모든 구현(파일 생성/수정/삭제)이 끝나면 반드시 `commit`을 수행한다.
- 커밋 메시지는 변경 내용을 명확히 설명하는 형식으로 작성한다.
  - 예: `feat: ContentProvider 조회 레포지토리 추가`, `fix: 오늘 카드 산출 로직 수정`
  - Conventional Commits 접두어(`feat`, `fix`, `docs`, `refactor`, `test`, `chore`) 사용 권장.
- 커밋은 논리적 단위로 분리한다 (여러 무관한 변경을 한 커밋에 섞지 않는다).

## 2. worktree 병렬 작업 규칙

- 모든 sub agent는 **자신만의 git worktree**를 사용하여 작업한다.
- 서로 다른 sub agent가 같은 작업 공간(파일)을 건드려 충돌하는 것을 방지한다.
- 브랜치 분리 원칙:
  - `main`(또는 통합 브랜치)에는 직접 커밋하지 않는다.
  - 각 작업은 별도 feature 브랜치 + 별도 worktree에서 진행한다.
  - 작업 완료 후 브랜치를 통합 브랜치로 병합(merge/PR)한다.
- worktree 생성 예시:
  ```
  git worktree add ../anki-preview-<feature> -b feature/<feature>
  ```

## 3. 테스트 규칙

- 테스트는 **Android 에뮬레이터**에서 진행한다.
- 에뮬레이터 기반 동작 검증을 우선으로 하며, 필요 시 기기 반영 테스트를 추가한다.
- 테스트 결과(통과/실패, 확인된 동작)는 커밋 메시지나 작업 기록에 남긴다.

## 작업 흐름 요약

1. 작업 전: 전용 worktree + feature 브랜치 생성
2. 구현 중: 논리 단위로 커밋 수행
3. 검증: Android 에뮬레이터에서 테스트
4. 완료 후: 통합 브랜치로 병합
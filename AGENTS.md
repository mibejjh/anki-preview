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

### herdr pane 기반 업무 위임

- sub agent는 **herdr의 pane 제어 기능**을 활용하여 별도의 pane에서 `pi`를 **foreground**로 실행해 업무를 위임한다.
- 이 방식으로 각 작업의 진행 상황을 동일 세션에서 실시간으로 확인한다.
- herdr 사용 전 `HERDR_ENV=1` 여부를 확인한다 (`echo $HERDR_ENV`).
- 기본 흐름:
  1. `herdr pane list` 로 현재 pane/workspace/tab id 확인
  2. 전용 worktree(2번 규칙)에서 새 pane을 분할(split)한 뒤 `pi`를 foreground로 실행
     ```
     NEW_PANE=$(herdr pane split <현재pane> --direction right --no-focus | python3 -c 'import sys,json; print(json.load(sys.stdin)["result"]["pane"]["pane_id"])')
     herdr pane run "$NEW_PANE" "cd ../anki-preview-<feature> && pi"
     ```
  3. `herdr wait output "$NEW_PANE" ...` 로 특정 출력/완료를 기다리며 진행 상황 확인
  4. `herdr pane read "$NEW_PANE" --source recent --lines N` 로 결과 출력 확인
  5. 작업 완료 확인 후 `herdr pane close "$NEW_PANE"` 로 정리
- pane id는 세션이 닫히며 압축될 수 있으므로 매번 `pane list`/split 응답에서 다시 읽어 사용한다.

## 3. 테스트 규칙

- 테스트는 **Android 에뮬레이터**에서 진행한다.
- 에뮬레이터 기반 동작 검증을 우선으로 하며, 필요 시 기기 반영 테스트를 추가한다.
- 테스트 결과(통과/실패, 확인된 동작)는 커밋 메시지나 작업 기록에 남긴다.

## 작업 흐름 요약

1. 작업 전: 전용 worktree + feature 브랜치 생성
2. 위임: herdr pane을 분할해 별도 pane에서 `pi`를 foreground로 실행해 업무 위임 (진행 상황 실시간 확인)
3. 구현 중: 논리 단위로 커밋 수행
4. 검증: Android 에뮬레이터에서 테스트
5. 완료 후: 작업 결과 확인·정리(herdr pane close), 통합 브랜치로 병합
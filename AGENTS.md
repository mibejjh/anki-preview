# AGENTS.md

이 프로젝트에서 작업하는 모든 에이전트(및 sub agent)는 아래 규칙을 반드시 준수한다.

## 1. 커밋 규칙

- 모든 구현(파일 생성/수정/삭제)이 끝나면 반드시 `commit`을 수행한다.
- 커밋 메시지는 변경 내용을 명확히 설명하는 형식으로 작성한다.
  - 예: `feat: ContentProvider 조회 레포지토리 추가`, `fix: 오늘 카드 산출 로직 수정`
  - Conventional Commits 접두어(`feat`, `fix`, `docs`, `refactor`, `test`, `chore`) 사용 권장.
- 커밋은 논리적 단위로 분리한다 (여러 무관한 변경을 한 커밋에 섞지 않는다).

## 2. subagent 위임 및 worktree 격리 규칙

- 업무 위임은 **pi-subagents 스킬(subagent 도구)** 기반으로 수행한다. (herdr pane을 직접 제어하지 않는다)
- 모든 sub agent는 **자신만의 git worktree**에서 작업하여 서로 충돌하지 않도록 한다.
  - `subagent` 호출 시 `worktree: true`로 관리형 격리를 사용한다 (브랜치/핸드오프 자동 처리).
- 브랜치 분리 원칙:
  - `main`(또는 통합 브랜치)에는 직접 커밋하지 않는다.
  - 각 작업은 별도 feature 브랜치 + 별도 worktree에서 진행한다.
  - 작업 완료 후 부모가 브랜치를 통합 브랜치로 병합(merge/PR)한다. sub agent는 병합하지 않는다.
- 병렬 위임 기본 패턴(workflowScript):
  ```
  subagent({
    workflowScript: `
      const results = await runs.all([
        { key: "data-layer", agent: "worker", task: "...", worktree: true },
        { key: "ui", agent: "worker", task: "...", worktree: true }
      ]);
      return results.map(r => ({ key: r.key, handoff: r.artifactPaths }));
    `
  })
  ```
- 주의 사항:
  - 각 child는 **자기 worktree에만** 쓰고, 충돌하는 공유 파일(`build.gradle.kts`, `AndroidManifest.xml`, 계약 DTO)을 다른 child와 동시에 수정하지 않는다.
  - 병렬 시작 전 계약(공통 DTO/인터페이스)이 `main`에 확정되어 있어야 한다.
  - 부모는 항상 오케스트레이터로 남고, 병합·발행·결정은 부모가 수행한다.

## 3. 테스트 규칙

- 테스트는 **Android 에뮬레이터**에서 진행한다.
- 에뮬레이터 기반 동작 검증을 우선으로 하며, 필요 시 기기 반영 테스트를 추가한다.
- 테스트 결과(통과/실패, 확인된 동작)는 커밋 메시지나 작업 기록에 남긴다.

## 작업 흐름 요약

1. 작업 전: 계약(DTO/인터페이스)이 main에 확정되어 있는지 확인
2. 위임: `subagent` + `worktree: true`로 병렬 위임 (자동 브랜치/핸드오프)
3. 구현 중: 각 sub agent가 논리 단위로 커밋 수행
4. 검증: Android 에뮬레이터에서 테스트
5. 완료 후: 부모가 각 worktree의 결과를 통합 브랜치로 병합
# code-reviewer 핸드오프
- **판정**: **APPROVED** (MAJOR 0 · MINOR 4 — 전부 주석/문언)
- **로그**: `doc/20260811/20260811_075919_code-reviewer.md` ⚠️ **`20260810`이 아니다** — 리뷰가 날짜 경계를 넘겼다. 배치 02 로그는 두 디렉터리에 걸쳐 있다.
- **상세**: `pipeline/review.md`

## 다음 단계가 반드시 알아야 할 것

1. **`assertSame`(`AppBridgeHostTest.kt:36`)은 존치 판정이다 — 삭제하지 마라.** 프로덕션 변조로 직접 실험해 **독립 반증 가능**을 증명했다: setter가 값을 감싸면(`field = { value.invoke() }`) `assertEquals`는 초록인데 **`assertSame`만 RED**가 된다. V1 위반 아니고 지표 게이밍도 아니다.
2. **단, developer가 든 근거는 거짓이었다.** *"커스텀 getter를 끼워도 `requestFinish` 기반 단정은 초록"* → 실험 결과 **`assertEquals`가 먼저 FAIL**(`expected:<1> but was:<0>`)하고 `assertSame`은 도달조차 못 한다. **Kotlin은 커스텀 접근자가 생기면 클래스 내부의 프로퍼티 읽기도 getter를 경유한다** — backing field 직접 접근은 기본 접근자일 때만의 최적화다. 이 사실을 모르면 다음 배치가 같은 오판을 반복한다.
3. **커버리지 `clean` 재측정 완료 — developer 보고와 불일치 0건.** LINE 102/102, BRANCH 20/20, CLASS 15/15, INSTR 626/655(95.57%), METHOD 49/59(83.05%), CXTY 59/69(85.51%), 테스트 **50 / 실패 0 / skipped 0**. **6개 지표 하락 0건.** `30 actionable tasks: 30 executed`로 UP-TO-DATE 회피 확인.
4. **본문 무변경이 바이트 단위로 증명됐다.** 원본(`git show HEAD`) 구간 vs 새 파일 `diff` — `AppBridgeHost` 본문 **0줄**, `AppContainer` KDoc+본문 **0줄**. **`@Volatile` 살아 있고(`:19`) `?.`도 원형(`:23`), `!!` 0건.** `MainActivity.kt` 5줄은 전부 `/* */` 내부 주석.
5. **C-1·C-2 이행됨.** import 8개/11개가 plan 목록과 1:1 일치. developer 원복도 완전 — 착수 시 md5 `e6bcc47f…`가 보고 해시와 일치. **내 검증 변조 2회도 전부 원복**(md5 동일 · `git status` 불변 · 변조 잔존 0건).

## MINOR 4건 (재작업 루프 불필요 — 6단계에서 흡수 가능)

| # | 위치 | 수정 방향 |
|:--|:--|:--|
| 1 | `AppBridgeHostTest.kt:33-35` | 주석 **3번째 줄이 사실과 다르다**(위 2번). 실제 반증 경로로 교체: *"setter가 값을 감싸거나 정규화하면 `requestFinish`는 여전히 1회 호출돼 위 단정은 초록이지만, 이 단정이 RED가 된다."* |
| 2 | `MainActivity.kt:122-123` | `:119`가 `/**`가 아닌 **`/*`**이고 함수 본문 안이라 KDoc 링크 `[...]`가 해석되지 않는다. FQN도 도식을 2줄로 늘렸다 → `` `AppBridgeHost` ``로 되돌려 한 줄 유지 |
| 3 | `AppContainer.kt:15-21` | 분리 후 **소유자 맥락 소실**(`App`이 `by lazy`로 소유, `App.kt:49`). plan Step 2에 지시가 없었으므로 **developer 판단은 정당** — 권고 |
| 4 | `AppBridgeHost.kt:10-15` | plan Step 1의 "3줄 이내"에 대해 실제 **6줄**. 기록만 |

## qa(6단계) 인계 사항

- **`assertSame`은 두고 MINOR-1의 주석만 고쳐라.** 테스트 수정 권한은 qa에 있다.
- **케이스 추가 권한 유지**(evaluator 주의 ③). getter를 현실적 경로로 태우려면 **등록 → 해제(null) → 재등록** 전이가 적합하다. 다만 **102/102·METHOD 4/4는 이미 충족**이니 커버리지를 이유로 추가하지 마라(T5).
- **`onFinishRequested`의 getter는 프로덕션에서 아무도 읽지 않는다.** 이 사실을 모르고 "getter도 실사용된다"는 전제로 케이스를 짜면 안 된다.
- 테스트 클래스 KDoc의 T1(보장/미보장)은 **이미 충족**돼 있다(`:10-16`). 중복 보강 불필요.
- **프로덕션 코드를 수정하지 마라** — MINOR-2·3·4는 프로덕션 주석이라 qa 범위 밖이다. 7단계 이후 또는 후속 배치로 넘긴다.

## 미해결 / 이월

- **계획 문언 오기 2건 확인 — 둘 다 developer 주장이 맞다.** ① **G2-c**: 필터가 `'App' in n`이라 `AppInfoRepository` 계열 3줄이 **베이스라인에서도** 나온다(총 4줄) → *"한 줄만"*은 처음부터 통과 불가. ② **G2-b**: "169줄"→실제 **168줄**, "90줄 내외"→실제 **81줄**. **`plan.md`는 3단계 APPROVED 확정 문서라 사후 편집하지 않았다.** → **다음 배치 planner는 이 스니펫을 복사하지 말고 필터를 정확한 클래스 경로 대조로 바꿔라.**
- **F-1**(`inputs.dir`) 미개입(`grep` 0건) · **`interface BridgeHost`**(`BridgeDispatcher.kt:59`) 무변경 · **`private set`** 범위 밖 — 전부 후속 후보.
- **신규 관측**: 훅의 `allowed_globs` `*`가 `/`를 **넘는 것으로 보인다**(`…/geckoviewtest/*AppBridgeHost.kt`가 `bridge/AppBridgeHost.kt`를 허용, 차단 0건). Ant PatternSet(R-01)과 **반대**라 다음 배치가 화이트리스트를 좁게 쓸 때 오탐 원인이 될 수 있다.
- **화이트리스트 이탈 0건. 범위 확대 0건. 제외 목록 11 → 10(좁아짐).**

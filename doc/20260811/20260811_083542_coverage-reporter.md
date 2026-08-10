# coverage-reporter 작업 로그
- **일시**: 2026-08-11 08:35:42
- **작업**: 배치 02(`AppBridgeHost` → `bridge/` 이동 + 제외 해제 + 테스트 추가, `AppContainer` 파일 분리) 최종 게이트 — 커버리지 회귀 판정
- **입력**: `pipeline/handoff/1~6-*.md`, `pipeline/qa-report.md` §4·§6, `pipeline/plan.md` §3.2·§3.3, `pipeline/impact-report.json`, 규칙 `architecture.md`·`verification-honesty.md`·`scope-guard.md`·`work-logging.md`
- **산출물**:
  - `pipeline/coverage-report.md` (신규)
  - `pipeline/impact-report.json` (`enforce` 필드만 `true` → `false`)
  - `pipeline/handoff/7-coverage-reporter.md` (신규)
  - 본 로그

## 수행 내용

### 1. 현재 수치 재측정 (`clean` 부착)

```
JAVA_HOME=…/jbr-17.0.14/… ./gradlew clean jvmCoverageReport --rerun-tasks
→ BUILD SUCCESSFUL / 30 actionable tasks: 30 executed
```

planner의 R-P3(옛 위치 `AppBridgeHost.class` 잔존 시 수치가 흔들림)에 따라 `clean`을 붙였고,
`30 executed`로 UP-TO-DATE 회피를 확인했다. XML을 파이썬으로 직접 파싱해 6개 지표와 클래스별·메서드별 값을 뽑았다.

**LINE 102/102 · BRANCH 20/20 · CLASS 15/15 · INSTR 626/655 · METHOD 49/59 · CXTY 59/69 · 테스트 51/실패0/skip0.**

### 2. 베이스라인도 직접 재산출 (V7)

인계된 베이스라인 숫자를 옮겨 적지 않았다. `git archive HEAD | tar -x`로 배치 01 종료 시점을
스크래치패드에 독립 전개하고(`local.properties`만 복사) 같은 명령을 실행했다.
**작업 트리는 건드리지 않았다.** 전개본에서 제외 항목 11개 · `App.kt`의 `^class ` 3개를 확인해
베이스라인 시점이 맞음을 검증한 뒤 측정: **98/98 · 18/18 · 14/14 · 607/636 · 45/55 · 54/64 · 테스트 48.**

인계 보고와 **불일치 0건.** 6개 지표 전부 하락 없음(3개는 상승, 전부 분자·분모가 같은 값만큼 증가).
분모 증분이 `AppBridgeHost` 단일 클래스 규모와 정확히 일치해 출처가 하나로 특정된다.

### 3. 제외 범위 확대 여부 — 기계 대조

`git show HEAD:app/build.gradle.kts`의 목록과 현재 목록을 파싱해 집합 비교했다.
**추가 0건 · 삭제 1건(`AppBridgeHost*.class`) · 진부분집합 True.** 확대가 아니라 축소다.

남은 제외의 정당성도 원문으로 확인했다. `AppContainer`는 `Application`(`:22`)·`packageManager`(`:42·45·48`)·
`Dispatchers.Main.immediate`(`:33`) 의존이 실재해 빌드 스크립트의 기준("JVM 실행 자체가 불가능한가")을 충족.
`AppBridgeHost`는 안드로이드 타입 grep **0건**으로 기준 미달 → 해제가 옳다.

### 4. 글롭 의미론 판별 실험 (V8)

R-01("Ant `*`는 `/`를 넘지 않는다")은 이 배치 설계의 근거인데, **현재 실행만으로는 검증되지 않는다** —
`AppBridgeHost`는 패키지 이동과 패턴 삭제가 동시에 일어나 변수가 둘이다.
그래서 스크래치패드 베이스라인 전개본에 `com/example/geckoviewtest/*BridgeDispatcher*.class` 한 줄만 주입해
단일 변수로 격리했다(대상은 하위 패키지의 `bridge/BridgeDispatcher.class`).

**결과: LINE 98/98 불변, `BridgeDispatcher` 2개 항목 분모 잔존 → `*`는 `/`를 넘지 않는다. R-01 참.**
실험 패턴은 원복 확인.

반대편은 hook 소스에서 확인했다 — `guard-impact-scope.py:52`가 `fnmatch.fnmatch(rel, g)`를 쓰고,
실행 결과 `*AppBridgeHost.kt` 글롭이 `bridge/`는 물론 `gecko/deep/`까지 ALLOW한다.
**두 의미론 공존이 사실로 확정** → F-6으로 등록.

### 5. METHOD 4/4 게이밍 여부 — 직접 변조 실험

인계된 결론을 옮겨 적지 않고 실험했다. 실제 트리를 건드리지 않기 위해
`rsync`로 작업 트리를 스크래치패드에 복사해 **복사본에서만** 감싸는 setter를 주입했다.
복사 시점 md5가 `e6bcc47f9e415da11358b252ddc573da`로 3개 단계 보고 해시와 일치해
**원본이 복원된 상태임도 함께 확인**됐다.

**결과: 51건 중 정확히 1건만 FAIL — `AppBridgeHostTest.kt:38`(`assertSame`).**
같은 케이스의 `:33`(`assertEquals`)은 초록 통과. → `assertSame`은 독립 반증 가능. **게이밍 아님.**

## 결정 사항

- **판정 PASS.** `architecture.md`의 두 조항을 각각 판정했다 — (a) 변경 클래스 100% ≥ 70%, (b) 6개 지표 하락 0건.
  LINE·BRANCH만 보지 않고 6개 전부 대조했다(developer가 "getter 태우기 전 INSTR·METHOD·CXTY 3개 하락"을 보고했기 때문).
- **테스트 50 → 51인데 지표 불변을 이상 징후로 처리하지 않았다.** qa §4의 근거(추가 케이스가 이미 덮인 라인만 통과,
  케이스 근거는 커버리지가 아니라 1회성 가드 회귀 공백)를 확인했고, **지표가 안 움직인다는 사실 자체가
  수치 목적이 아님의 증거**로 리포트에 적었다.
- **모든 변조 실험을 스크래치패드 복사본에서 수행했다.** 리드 지시("프로덕션·테스트 코드를 수정하지 마라")와
  V7("직접 재측정하라")이 충돌하는 지점을 이렇게 해소했다 — 실제 트리 무변경을 유지하면서 독립 검증을 달성.
- **`enforce`만 `false`로 내렸다.** 42줄·필드 7개·`risk_notes` 11건·`allowed_globs` 4건 전부 원형 확인.
- **아카이브는 판단만 하고 실행하지 않았다.** 리드 지시가 "판단만 하고 명시하면 오케스트레이터가 처리"를
  허용했고, 아카이브는 되돌리기 번거로운 이동이라 확인 후 처리가 안전하다고 봤다.

## 이슈 / 리스크

- **F-4가 또 비용을 발생시켰다.** 베이스라인이 산문에만 있어 이번에도 `git archive`로 직접 재산출해야 했다.
  `coverage-baseline.json`이 있었으면 대조 한 줄로 끝났다. 배치가 쌓일수록 반복 비용이 커진다.
- **F-6(글롭 의미론 공존)이 다음 배치의 실질 위험이다.** 화이트리스트가 의도보다 넓게 동작하므로
  "좁게 잡았다"고 믿고 설계하면 오탐한다.
- **F-7**: `onFinishRequested`의 getter를 프로덕션이 안 읽는다. 게이밍은 아니지만
  근본 해법은 단정 추가가 아니라 노출 축소(`private set`)다. public API 변경이라 범위 밖.
- 이월 항목(MINOR-2·3·4, F-1, `interface BridgeHost`, `private set`, 계획 문언 오기 2건)은
  **결함으로 올리지 않고** 후속 과제 표에만 기록했다.
- 범위 확대 0건. 커버리지를 올리려 범위를 넓히지 않았다(V9) — 미달 지표가 없어 해당 사항도 없었다.

## 판정

**PASS**

- (a) 변경 클래스 `bridge/AppBridgeHost` 라인 커버리지 **4/4 = 100%** ≥ 70%
- (b) 전체 커버리지 **6개 지표 하락 0건** (LINE·BRANCH·CLASS 유지 / INSTR·METHOD·CXTY 상승)
- 제외 범위 **11 → 10, 진부분집합** — 확대 아님
- **F-2 CLOSED**
- 테스트 **51 / 실패 0 / skipped 0**, 인계 보고와 **불일치 없음**
- `enforce: false` 처리 완료 → **파이프라인 종료**

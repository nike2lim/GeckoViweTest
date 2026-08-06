# coverage-reporter 작업 로그

- **일시**: 2026-08-06 13:14:51
- **작업**: 파이프라인 7단계(최종 게이트) — JaCoCo 커버리지 재측정 및 베이스라인 회귀 판정. **PASS**
- **입력**:
  - `pipeline/plan.md` rev.2 §7.1/§7.2/§7.3, R-11
  - `pipeline/review.md` rev.1 §9-8 + rev.2 §467-5 (베이스라인 출처)
  - `pipeline/requirements.md` §4 (커버리지 조항)
  - `pipeline/impact-report.json`
  - `app/build.gradle.kts`, `app/src/main/java/**`, `app/src/test/**`, `app/src/main/assets/messaging/**`
  - 규칙: `architecture.md`, `verification-honesty.md`, `scope-guard.md`, `work-logging.md`
  - 스킬: `android-pipeline:coverage-report`
- **산출물**:
  - `pipeline/coverage-report.md` (신규)
  - `pipeline/impact-report.json` — `enforce` 필드만 `true` → `false`
  - `doc/20260806/20260806_131451_coverage-reporter.md` (본 로그)

## 수행 내용

### 0. 선행 실패 후 재개
직전 실행은 macOS TCC로 `~/Documents` 전체가 `Operation not permitted`가 되어 산출물 없이 종료했다. 이번 실행 시작 시 파일 시스템을 **직접 확인**했다(오케스트레이터 보고를 그대로 믿지 말라는 지시). `pipeline/coverage-report.md` 부재, `doc/20260806/`에 qa 로그만 존재, `impact-report.json` 13,371 B / `enforce: true` — 보고와 일치했고 중복 작업 위험 없이 처음부터 진행했다.

### 1. 캐시 배제 재측정 (V7)
```
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew clean jvmCoverageReport --rerun-tasks
→ BUILD SUCCESSFUL / 30 actionable tasks: 30 executed
```
`testDebugUnitTest`·`jvmCoverageReport` 모두 UP-TO-DATE 아님을 태스크 로그로 확인. `.exec` 1개 / `.ec` **0개**(계측 미집계, 베이스라인과 동일 조건).

**실측**: LINE 98/98 = 100.00% · BRANCH 18/18 = 100.00% · CLASS 14/14 · INSTRUCTION 607/636 = 95.44% · METHOD 45/55 = 81.82% · 테스트 48건 / 실패 0 / skip 0 / `@Ignore` 0.

**QA 보고값과 불일치 없음.** 인계값은 인용하지 않고 XML을 python3로 직접 파싱했다.

### 2. LINE 100% vs INSTRUCTION 95.44% 규명
미커버 10개 메서드를 전수 확인한 결과 **전부 테스트에서 호출되지 않은 Kotlin 프로퍼티 게터**(`getId`·`getOk`·`getUrl` 등, 각 3 instruction). 생성자와 라인 번호를 공유하는 합성 접근자라 LINE에 잡히지 않는다. 결함 아님. 판정 기준은 architecture.md가 명시한 LINE이므로 게이트는 LINE으로 판정하되, "100% ≠ 전부 실행됨"임을 리포트 §2.2에 남겼다.

### 3. `coverageExclusions` 축자 대조 — 확대 2건 발견 후 판정
실제 목록 11개 vs plan §7.2 목록 9개. 차이는 `AppContainer*`·`AppBridgeHost*`.

둘 다 **`App.kt` 안에 선언된 최상위 클래스**(App.kt:100, :158)라 `App.class`/`App$*.class` 패턴에 안 걸린다. plan **§7.3의 경계는 파일 단위("`App.kt`")**이므로, 이 2건은 경계를 넓힌 것이 아니라 **§7.2의 코드를 §7.3의 표에 맞춘 것**이다 → 확대 아님.

**단 `AppBridgeHost`는 유보.** `build.gradle.kts`가 스스로 밝힌 제외 기준은 "JVM 테스트로 실행 자체가 불가능한가"인데, 이 클래스는 안드로이드 타입을 하나도 참조하지 않는 순수 Kotlin(널 가능 람다 + `requestFinish()`)이다. **원칙이 아니라 파일 위치로 제외됐다.** 후속 과제 F-2.

### 4. 분모 밖 코드 실측 (추정 금지, V5)
`app/build.gradle.kts`를 수정하지 않고 수치를 얻기 위해, Gradle 캐시의 **JaCoCo core 0.8.15 `Analyzer` API를 직접 호출하는 소형 Java 프로브**를 스크래치패드에 작성해 기존 `.exec`에 대해 측정했다.

| 클래스 | LINE | BRANCH |
|:--|--:|--:|
| `NativeBridgeHandler`(+`onMessage$1`) | **0/19** | 0/6 |
| `AppContainer` | 0/20 | 0/2 |
| `AppBridgeHost` | 0/4 | 0/2 |
| `App` | 0/11 | 0/0 |

`AppBridgeHost` 포함 시 LINE 98/102 = 96.08%, 둘 다 포함 시 80.33% — **어느 경우도 70% 상회**.

### 5. 베이스라인 확정과 회귀 판정
`pipeline/coverage-baseline.json`은 **부재**(find 전수 확인) → 스킬 §4상 "베이스라인 없음 → PASS".

그러나 "비교 대상 없음"으로 끝내지 않았다. `review.md` rev.2 §467-5가 **동일 방법(`clean`+`--rerun-tasks`)으로 측정한 값을 명시적으로 인계**하고 있었다: LINE 98/98 · BRANCH 16/18 · CLASS 14. 이것을 회귀 기준으로 삼았다.

| 지표 | 베이스라인 | 재측정 | 판정 |
|:--|:--|:--|:--|
| LINE | 98/98 = 100.0% | 98/98 = 100.0% | 동일 |
| BRANCH | 16/18 = 88.9% | 18/18 = 100.0% | 개선 |
| CLASS | 14 | 14 | 동일 |

**분모가 한 칸도 안 움직였다**(LINE total 98, BRANCH total 18) → 모집단 동일, like-for-like 재계산 불필요, 파일 소실로 회귀를 가릴 여지 없음. BRANCH 상승분은 `BridgeProtocol.kt:67 require(request.name.isNotBlank())` 분기이고, 이를 커버하는 테스트(`BridgeProtocolTest.kt:59`)가 실재함을 확인 — **분모 조작이 아니라 테스트 추가**로 특정했다.

### 6. F-1(자산 UP-TO-DATE) 재확인
QA 인계를 옮겨 적지 않고 3가지로 확인: ① 테스트가 `File("src/main/assets/messaging/...")`로 소스 자산을 직접 읽음(`ExtensionManifestTest.kt:33`, `BridgeWireContractTest.kt:146`) ② `build.gradle.kts`에 `inputs.` 선언 및 `testOptions` 블록 **없음** ③ 무변경 재실행 시 `testDebugUnitTest UP-TO-DATE` 실측. → 함정 실재 확인.

## 결정 사항

1. **`NativeBridgeHandler` 0%를 FAIL로 올리지 않고 구조적 예외로 처리했다.**
   축자 적용하면 변경 클래스가 0% < 70%로 미달이다. 이 사실을 리포트에 먼저 적은 뒤, 스킬 §4 판정표의 "(구조적 예외 아님)" 단서에 근거해 세 조건을 검증했다: ① **사유 실재** — `android.util.Log`를 `onMessage` 진입 직후 호출하는데 `testOptions.unitTests.returnDefaultValues`가 설정돼 있지 않음을 확인, JVM에서 `Stub!` 예외로 즉사한다(V3) ② **사전 승인** — plan §7.3이 명시 지정, evaluator가 3단계에서 APPROVED ③ **전제 유지** — 실측 19라인으로 "20줄 내외" 계획값 내이고, 추가 3줄은 업무 로직이 아니라 취소 재전파다.
   대안(FAIL로 qa 반려)은 V9·plan R-11 위반이다. 이 클래스는 테스트를 늘려서 올릴 수 있는 대상이 아니라 원리적으로 JVM 실행이 불가하므로, 반려해도 qa가 할 수 있는 일이 없고 범위만 넓어진다.

2. **그러나 보상 통제 미실행을 부채로 등록했다(F-3).**
   plan §7.3은 이 클래스의 검증 수단을 "Step 2 게이트 + **AC-003-2 역주입 테스트**"로 지정했는데 QA가 AC-003-2를 "관측하지 않았다"로 남겼다. 예외의 정당성은 성립하나 **떠받치기로 한 통제가 이번 배치에서 실행되지 않았다.** 이를 적지 않으면 "0% 코드가 무검증 통과"한 사실이 리포트에서 사라진다. 만료 조건을 "다음에 `NativeBridgeHandler`를 수정하는 배치"로 못박고, 예외를 넓히지 않도록 "새 판정 로직을 이 클래스나 `onMessage` 람다에 넣는 것은 이 예외로 정당화되지 않는다"를 명시했다.

3. **자산을 수정해 UP-TO-DATE 함정을 재현하는 실험은 하지 않았다.**
   구조적 사실 3가지로 결론이 충분히 성립하는 반면, `background.js` mtime 혼선이 이미 두 번(code-reviewer·qa) 발생했고 **버전 관리가 없어 원복 보증이 없다.** 재현의 이득보다 상태 오염 위험이 크다고 판단했다.

4. **`coverage-baseline.json`을 생성하지 않고 F-4로 넘겼다.**
   산출물을 `coverage-report.md`·`enforce`·작업 로그로 한정하라는 지시가 있었고 베이스라인 파일 신설은 그 범위 밖이다. 대신 확립된 수치를 리포트 §6.4에 남겼다.

5. **빌드 스크립트를 수정해 `AppBridgeHost`를 분모에 넣어보지 않았다.**
   프로덕션·빌드 파일 수정 금지 지시를 지키면서 수치를 얻기 위해 JaCoCo Analyzer 프로브를 별도로 돌렸다(§4). 커버리지를 올리려고 범위를 넓히지 않았다(V9).

## 이슈 / 리스크

1. **[F-1 / developer] 자산 UP-TO-DATE 함정** — `testDebugUnitTest`에 `inputs.dir("src/main/assets/messaging")` 미선언. 자산만 깨지면 계약 테스트 15건이 조용히 건너뛰어져 **초록이 유지된다.** V8의 교과서적 거짓 그린. 3가지 구조적 사실로 실증. 내 측정은 `--rerun-tasks`로 회피했다.
2. **[F-2 / developer+evaluator] `AppBridgeHost` 제외 근거 불일치** — 순수 Kotlin 4라인/2분기가 파일 위치만으로 분모 밖. 게이트 영향 없음(포함해도 96.08%)이나 제외 기준의 일관성 문제.
3. **[F-3 / qa] AC-003-2 미관측** — `NativeBridgeHandler` 구조적 예외의 보상 통제. 만료: 해당 클래스를 다음에 수정하는 배치.
4. **[F-4] `coverage-baseline.json` 부재** — 베이스라인이 산문(review.md·coverage-report.md)에만 존재해 취약.
5. **[F-5] 버전 관리 없음** — `git status` → `fatal: not a git repository`. **QA의 "`build.gradle.kts` 변경 없음을 diff로 확인 가능"이라는 주장은 이 저장소에서 성립하지 않는다.** diff 기반 검증 수단이 원천적으로 없어, plan 원문과의 축자 대조로 대체했다. `background.js` 내용 동일성(QA 주장 #6)도 **기준 사본이 없어 독립 검증하지 못했다** — 재현하지 못했으므로 인용하지 않았고, JS는 JaCoCo에 0 기여라 판정에는 영향 없다.
6. **환경** — 이번 실행 중 파일 접근 차단 재발 없음. 직전 실행의 TCC 차단은 해소됐다.

## 판정

**PASS**

- 분모 안 14개 클래스 전부 LINE 100%, **70% 미달 0건**
- 동일 방법·동일 모집단 베이스라인 대비 **LINE 동일 · BRANCH +2 개선 · 회귀 0건**, 상승분 출처를 테스트 1건으로 특정(분모 조작 아님)
- 제외 목록이 plan §7.3 경계 안임을 축자 확인 — **제외 확대 없음**
- 테스트 48건 전부 통과, skip·`@Ignore` 0건 — 초록으로 덮은 결함 없음(V6)
- 변경 클래스 `NativeBridgeHandler` 0%는 검증된 구조적 예외로 처리, 보상 통제 부채(F-3) 등록

**되돌려 보낼 단계 없음.** F-1~F-5는 차단 사유가 아니며 다음 배치로 넘긴다(V9).

**처리**: `pipeline/impact-report.json`의 `enforce`를 `true` → `false`로 내렸다(scope-guard.md 규칙 4). JSON 유효성과 8개 최상위 키 보존을 확인했고 파일 크기 13,371 → 13,372 B(+1 byte)로 **해당 필드 외 변경 없음**을 검증했다. 리포트는 보존한다. **파이프라인 종료.**

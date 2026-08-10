# 계획 검증 (3단계 게이트)

- 대상: `pipeline/plan.md` (초안, 2026-08-10 15:05:44)
- 판정 일시: 2026-08-10 15:22:26
- 평가자: evaluator (배치 02)

---

## 판정: **APPROVED (조건부)**

조건 2건은 모두 developer가 착수 중에 검증 가능하며, **계획의 구조·결정·게이트 설계를 바꾸지 않는다.** planner 재작성(REJECTED)은 필요 없다.

| # | 조건 | 검증 방법 |
|:--|:--|:--|
| **C-1** | **G2-a의 "컴파일 경고에 unused import 0건"을 근거로 쓰지 말 것.** 대신 `git diff app/src/main/java/com/example/geckoviewtest/App.kt`의 import 블록을 plan §4 Step 2가 명시한 삭제/잔존 목록과 1:1 대조한다. | Kotlin 2.3.21은 미사용 import에 경고를 내지 않는다(내가 실측 — 아래 V1-b). 이 조건대로 하면 diff가 그 자리를 대신하고 실제로 깨질 수 있다. |
| **C-2** | **G4-a(V2 RED 확인)를 케이스 2에도 적용할 것.** 계획의 예시(`requestFinish()` 호출 주석 처리)는 케이스 1만 RED로 만든다. 케이스 2(null 분기)의 RED는 `onFinishRequested?.invoke()`를 `!!`로 바꿔 NPE가 나는 것으로 증명한다. **원복 기록 포함.** | 두 케이스 각각의 RED 로그. 이 조건은 R-P6(`?.`→`!!`)과 R-P7(null 분기 누락)을 같은 동작으로 동시에 방어한다. |

---

## 0. 재측정 (V7) — 인계값을 그대로 인용하지 않았다

planner가 §0에서 보고한 값을 **전부 이 프로젝트에서 직접 다시 실행**했다.

| 항목 | planner 보고값 | 내 재측정 | 결과 |
|:--|:--|:--|:--|
| 베이스라인 LINE | 98/98 = 100.00% | **98/98 = 100.00%** | 일치 |
| 베이스라인 BRANCH | 18/18 | **18/18** | 일치 |
| 베이스라인 CLASS | 14/14 | **14/14** (`<class>` 원소는 28개) | 일치 |
| 베이스라인 INSTRUCTION | 607/636 | **607/636** | 일치 |
| 베이스라인 METHOD | 45/55 | **45/55** | 일치 |
| 베이스라인 COMPLEXITY | 54/64 | **54/64** | 일치 |
| `AppBridgeHost` 규모 | LINE 4 / BRANCH 2 / METHOD 4 / INSTR 19 / CXTY 5 | **LINE 4 / BRANCH 2 / METHOD 4 / INSTR 19 / CXTY 5 / CLASS 1** | 일치 |
| `AppContainer` 규모 | LINE 20 / BRANCH 2 / METHOD 7 | **LINE 20 / BRANCH 2 / METHOD 7** | 일치 |
| 테스트 건수 | 48 | **48, skipped 0, failures 0** | 일치 |
| `coverageExclusions` | 11개 항목, `build.gradle.kts:118-130`, `AppBridgeHost*.class`는 122행 | **동일** | 일치 |
| 참조 지점 | `MainActivity.kt:129·201` 코드(import 없음), `:70·122·125` 주석 | **동일. 전수 grep으로 다른 참조 0건 확인** | 일치 |
| JAVA_HOME 두 경로 모두 빌드 성공 | JBR 17.0.14 / Studio JBR 21.0.10 둘 다 성공·수치 동일 | **둘 다 BUILD SUCCESSFUL, 30 tasks executed, 수치 6개 지표 전부 동일** | 일치 |
| 기본 JVM | Java 8 → 설정 단계 실패 | **1.8.0_333** | 일치 |
| 실기기 | SM-G981N 연결됨 | **`R3CN60L0QMT / SM_G981N` device** | 일치 |
| git 상태 | 커밋 2개, 미추적은 `doc/`·`pipeline/` | **`1870cdf`/`405763d`, 동일** | 일치 |

**측정 방법**

- 베이스라인: `JAVA_HOME=.../jbr-17.0.14/... ./gradlew clean jvmCoverageReport --rerun-tasks` 실행 후 `jvmCoverageReport.xml` 직접 파싱. **JDK 21로 한 번 더 실행해 6개 지표가 모두 동일함을 확인**(planner의 주장 ④ 검증).
- 클래스별 규모: 스크래치패드에 별도 Gradle 프로젝트를 만들어 **제외 목록을 전부 무시한 `JacocoReport`**를 등록하고, 실제 프로젝트의 `tmp/kotlin-classes/debug`와 `testDebugUnitTest.exec`를 직접 겨냥해 측정했다. planner의 JaCoCo core 직접 호출과 다른 경로인데 **값이 완전히 일치**한다.

### 불일치 항목

**실질적 불일치 없음.** 다음 2건은 기록해 두되 판정에 영향이 없다.

1. **`App.kt` 총 라인 수** — 계획은 "현재 169줄"(§4 G2-b)이라고 적었으나 `wc -l`은 **168**이다(파일 끝 빈 줄 처리 차이). G2-b의 "90줄 내외로 줄어든다"는 판정에는 영향이 없다.
2. **R-P3(낡은 `.class` 오염)는 내가 재현하지 않았다.** 재현하려면 실제로 클래스를 옮겨야 하는데 evaluator는 코드를 고치지 않는다. **판정에는 영향이 없다** — `clean` 추가는 무조건적으로 안전한 완화책이라(틀린 수치를 만들 수 없고 빌드 시간만 늘린다) 전제가 과했더라도 손해가 없다. 그리고 `clean jvmCoverageReport --rerun-tasks` 조합이 베이스라인을 **두 JDK에서 각각 재현**함을 내가 확인했다.

---

## 1. E-1 ~ E-5 판정

### E-1 — `AppBridgeHost` → `com.example.geckoviewtest.bridge` : **APPROVED**

`architecture.md`의 레이어 조항은 **패키지 이름이 아니라 의존 방향**을 구속한다. 대조 결과:

- **새로 생기는 의존 간선이 0개다.** `AppBridgeHost`의 유일한 의존 대상인 `interface BridgeHost`가 `bridge/BridgeDispatcher.kt:59`에 있어, 이동 후 **같은 패키지가 되므로 import 자체가 사라진다**(계획 §2 D-01, 원문 확인).
- **역방향 간선이 생기지 않는다.** `bridge/`·`data/` 패키지의 프로젝트 내부 import를 전수 확인한 결과 `bridge` → `gecko`, `bridge` → `data` 뿐이고 **루트 패키지를 참조하는 곳이 0건**이다. 이동 후에도 `bridge`는 루트를 모른다. 순환 없음.
- **"도메인 로직은 프레임워크 비의존 순수 Kotlin" 조항에 대해 개선이다.** `AppBridgeHost`는 안드로이드 타입을 하나도 쓰지 않는데(원문 확인: `App.kt:158-167`) 현재 안드로이드 진입점만 모인 루트 패키지에 있다.
- UI→ViewModel→Repository 방향은 이 배치가 건드리지 않는다(`MainViewModel` 미수정).

`bridge`가 레이어 목록에 명시돼 있지 않은 것은 **위배 사유가 아니다.** `architecture.md`는 열거된 4개 레이어만 존재하라고 요구하지 않으며, 이미 `bridge`·`data`·`gecko`가 공존하는 기존 구조를 이 배치가 바꾸지 않는다.

### E-2 — `AppContainer` 루트 패키지 유지 : **APPROVED** (단서는 §1-E2c)

리드가 요청한 두 가지를 나눠 판정한다.

#### (a) planner의 논증이 성립하는가 — **부분적으로.** 단, 결론은 지지된다.

D-02의 근거 3(`di/`로 옮기면 제외 패턴이 깨져 80.33%로 회귀 / 얻는 이득 0)을 검증했다.

- **산술은 맞다.** 98 / (98 + 4[`AppBridgeHost`] + 20[`AppContainer`]) = 98/122 = **80.33%**. 내가 측정한 두 클래스 규모와 정확히 일치한다.
- **글롭 의미론도 맞다.** 아래 §3(V8)에서 실행 확인했다.
- **그러나 "회귀한다"는 조건부다.** `di/`로 옮기면 Step 3에서 제외 패턴 한 줄을 `com/example/geckoviewtest/di/AppContainer*.class`로 **고치면 그만이고**, 그 편집 비용은 이미 계획된 "한 줄 삭제"와 동급이다. 게다가 계획 자신의 G2-c가 이 실수를 **관측으로 잡는다.** 즉 이것은 `di/` 안(案)의 내재적 비용이 아니라 **"잊었을 때"의 실패 모드**다.
- **planner도 이 점을 스스로 인정했다** — §2 D-02 원문: *"이것은 결정의 주된 이유가 아니라 1·2를 지지하는 확인 사항이다. 아키텍처적 이득이 있었다면 패턴은 고치면 그만이다."* **정직한 격하이고, 이 격하 덕분에 논증 전체는 무너지지 않는다.**

따라서 **근거 3은 사실상 0점으로 두고, 근거 1·2가 결정을 지탱하는지**만 보면 된다. 지탱한다:

- **근거 1이 강하다.** 이 저장소는 `App.kt:96-98` KDoc에서 *"Hilt 같은 DI 프레임워크를 쓰지 않은 이유: 화면 1개에 주입 대상 5개뿐이라 손익분기점 아래"*라고 **이미 문서화된 판단**을 갖고 있다. 클래스 1개짜리 `di/` 패키지는 그 판단과 어긋난다.
- **근거 2도 성립한다.** `App.kt:59`에서 `App`이 `by lazy`로 소유하는 유일 인스턴스이고, 같은 패키지면 `AppContainer(this)`가 import 없이 읽힌다(원문 확인).
- **D-03의 기준이 일관된다.** 같은 질문("무엇에 소속되는가")에 답이 갈린 것이지 규칙이 둘인 것이 아니다. `AppBridgeHost`는 정확히 한 하위 도메인에 속하고, `AppContainer`는 여러 패키지를 가로질러 조립하는 것이 역할이라 어느 하위 패키지에도 속하지 않는다. **판정 기준으로 받아들일 만하다.**

#### (b) 파일 분리만으로 사용자 요청을 충족하는가 — **충족한다고 본다. 임의로 좁힌 것이 아니다.**

사용자 원문: *"**App Class에 있는** AppBridgeHost, AppContainer는 **다른 곳으로** 옮기는게 나을거 같은데? **전역으로 설정해 놓으니** 낭비되는거 같아."*

- 불만의 **지시 대상이 "App Class에 있는"**이다. 그에 대응하는 "다른 곳"의 가장 직접적인 독법은 **App.kt 밖**이며, 같은 패키지의 별도 파일은 그 독법을 충족한다.
- `impact-report.json`의 확정 task 문언도 **"별도 파일로 분리한다"**이고, "파일 위치와 패키지"는 *바꿔도 되는 것의 범위*를 생명주기와 구분해 정한 것이지 두 클래스 모두 패키지를 바꾸라는 요구가 아니다.
- 후반부 "전역으로 설정해 놓으니 낭비"는 **필요 없이 전역인 것**을 겨눈다. `AppContainer`는 정의상 앱 전역 조립 지점이라 여기에 해당하지 않는다. 반면 `AppBridgeHost`는 해당하고, **그쪽은 실제로 패키지가 바뀐다.** 계획이 사용자 문언의 두 부분에 각각 대응하고 있다.

#### (c) 남는 단서 — 오케스트레이터 판단용 (developer를 막지 않는다)

리드의 확정 해석 *"파일 위치(및 패키지)"* + *"두 클래스 모두 이동 대상이다"*를 **"두 클래스 모두 패키지를 바꾼다"**로 읽는 것도 문언상 불가능하지는 않다. 나는 위 근거로 그 독법을 택하지 않았으나, **이것이 유일한 독법이라고 주장하지는 않는다.**

**전환 비용을 미리 계산해 둔다** — 사용자가 나중에 `di/`를 원하면:
`AppContainer.kt` 파일 이동 + `package` 선언 1줄 + `build.gradle.kts` 제외 패턴 1줄 + `App.kt`/`MainActivity.kt:70` 주석. **파이프라인 재실행 없이 소규모 후속 배치로 처리 가능하다.**
따라서 **지금 developer를 세울 이유가 없다.** 확인이 필요하다고 판단되면 오케스트레이터가 4단계와 **병렬로** 사용자에게 물으면 된다.

### E-3 — Step 4 테스트를 developer가 4단계에서 쓰는 것 : **APPROVED — 역할 위반이 아니다**

**규칙 문서에 금지 조항이 없다.** 대조 결과:

- `architecture.md`: *"신규/변경 비즈니스 로직은 단위 테스트 필수"* — **누가** 쓰는지 규정하지 않는다.
- `scope-guard.md` 규칙 3: 테스트 경로는 impact-analyzer가 **처음부터** `allowed_globs`에 넣는다 → 파이프라인 전반에 열린 경로라는 뜻이다.
- 에이전트 정의: qa는 *"테스트 코드만 작성하며 **프로덕션 코드는 수정하지 않는다**"*로 **qa 쪽에만** 제약이 걸려 있다. developer 쪽에 대칭되는 "테스트를 쓰지 않는다"는 제약은 **없다.**

즉 역할 분담은 **기본 분업이지 금지가 아니다.**

**대안(qa가 6단계에 쓰고, code-reviewer가 96.08%를 회귀로 보지 않게 한다)이 더 나은지 — 아니다. 계획 쪽이 낫다.**

대안은 **5단계 게이트에게 자기 측정치를 무시하라고 가르치는 구조**다. `verification-honesty.md`가 V3에서 경고하는 *"이건 원래 빨간 거야"라며 무시하게 된다*가 정확히 이 상태이고, 한번 그렇게 훈련되면 그 구간에서 **진짜 하락**이 와도 구분할 수 없다. 게이트를 약화시키는 대가로 얻는 것이 "형식적 역할 경계"뿐이다.

계획 쪽은 반대로 **4단계 종료 시점에 이미 102/102**라 어느 게이트도 무시할 신호가 없다. 또한 R-04의 짝(제외 해제 ↔ 테스트)은 **한 변경의 두 반쪽**이다. 이를 단계로 쪼개면 "빌드 스크립트는 이 클래스를 커버리지로 강제하는데 테스트는 없는" 상태가 커밋에 남는다.

**단, qa의 역할이 비어서는 안 된다.** 아래 주의 ③을 참조.

### E-4 — `AppContainer` 구조적 예외 유지 : **APPROVED — 제외 범위 확대가 아니다**

- **제외 목록은 넓어지지 않고 좁아진다.** 항목 수 **11 → 10**(삭제 1건, 추가 0건). `AppContainer*.class` 패턴은 **문언이 바뀌지 않고**, 패키지도 안 바뀌므로 계속 같은 것을 가리킨다.
- **빌드 스크립트가 스스로 밝힌 기준을 충족한다.** 기준 원문(`build.gradle.kts:112`): *"제외 기준은 '테스트하기 귀찮아서'가 아니라 **JVM 테스트로 실행 자체가 불가능한가**"*. `AppContainer`의 의존을 소스에서 확인했다 — `Application`(`App.kt:100`), `app.packageManager`(`:120`), `Dispatchers.Main.immediate`(`:111`). 마지막 것은 `Dispatchers.setMain` 없이는 **생성자 실행 자체가 실패**한다. 기준 충족이다.
- 반대로 `AppBridgeHost`는 안드로이드 타입 0개이므로 **기준 미달**이고, 제외를 푸는 것이 옳다(D-04). 두 판정이 같은 기준의 양쪽이다.
- 계획이 "안 고쳤으니 괜찮다"로 넘기지 않고 **G2-c/G3-b/G3-c로 관측 확인**하도록 한 것이 적절하다.

### E-5 — 주석 갱신 범위 (Step 5) : **APPROVED**

- **전수 grep으로 누락이 없음을 확인했다.** `AppBridgeHost` 언급은 `MainActivity.kt:122·125`(주석), `App.kt:136`(`AppContainer` 본문 코드), `App.kt:158`(선언) **4곳이 전부**다. Step 1·2가 뒤의 둘을, Step 5가 앞의 둘을 처리한다. **빠진 곳 없음.**
- `AppContainer` 언급은 `MainActivity.kt:70`(주석), `App.kt:26`(KDoc 링크 `[AppContainer]`), `:59`, `:100`. **D-02로 패키지가 그대로이므로 `[AppContainer]` KDoc 링크가 계속 해석되고 `:70` 주석도 참으로 남는다.** 계획이 `:70`을 고치지 않기로 한 것이 옳다.
- `comment-style.md` 규칙 5("코드와 어긋난 주석")에 대해: `:122`·`:125`는 이동 후에도 **명제로서는 참**이지만 독자가 클래스를 찾지 못한다. 계획의 처방(`bridge.AppBridgeHost`로 한정하거나 KDoc 링크로 전환)이 **충분하다.**
- **테스트 코드에 `AppBridgeHost` 참조가 0건**임도 확인했다(`FakeBridgeHost`는 `interface BridgeHost`의 별도 구현이라 무관). 이동으로 기존 테스트가 깨지지 않는다.

---

## 2. `architecture.md` 조항별 대조

| 조항 | 판정 | 근거 |
|:--|:--|:--|
| UI→ViewModel→Repository→DataSource 단방향 | **PASS** | `MainViewModel`·Repository 미수정. E-1에서 새 간선 0개·역방향 0건 확인 |
| UI에서 Repository 직접 호출 금지 | **PASS** | 해당 변경 없음 |
| ViewModel에 Context/View/Compose 주입 금지 | **PASS** | `MainViewModel` 미수정 |
| 도메인 로직 = 프레임워크 비의존 순수 Kotlin | **PASS (개선)** | 순수 Kotlin인 `AppBridgeHost`가 안드로이드 진입점 패키지 → 도메인 패키지로 |
| UiState/StateFlow, 일회성 이벤트 분리 | **PASS** | 해당 없음 |
| 코루틴 스코프 / `GlobalScope`·`runBlocking` 금지 | **PASS** | `applicationScope`·`SupervisorJob` 구조 무변경(본문 무변경 계약 R-P6) |
| Dispatcher 주입 | **PASS** | `AppContainer` 본문 무변경 |
| 새 라이브러리는 evaluator 승인 | **PASS — 승인 요청 0건** | `libs.versions.toml`은 화이트리스트 밖이고 §8이 "추가 없음"을 명시. Step 4는 기존 JUnit만 사용 |
| 버전 카탈로그에서만 버전 관리 | **PASS** | 미수정 |
| `!!` 남용 / 빈 catch / 하드코딩 문자열 | **PASS** | 본문 무변경. **C-2가 `?.`→`!!` 변질을 능동적으로 검출하게 만든다** |
| public API 시그니처 변경은 영향 파일이 전부 impact-report에 | **PASS** | `AppBridgeHost` FQCN 변경 → 영향 파일 `App.kt`·`AppContainer.kt`·`MainActivity.kt` 전부 화이트리스트 안(§3에서 실행 대조) |
| 빌드 설정 변경은 리스크 항목 명시 | **PASS** | `coverageExclusions` 1줄 삭제가 유일. Step 3 및 R-P2에 명시. AGP·Kotlin·minSdk·`ndk.abiFilters`·`dependencies` **불변** |
| 신규/변경 로직 단위 테스트 필수 | **PASS** | Step 4 (E-3 참조) |
| **변경 클래스 라인 커버리지 70% 이상** | **PASS** | 아래 |
| **전체 커버리지 베이스라인 대비 하락 금지** | **PASS** | 아래 |

### 커버리지 두 조항 — 목표치와 경로 검증

**(a) 변경 클래스 70% 이상**

| 클래스 | 분모 | 목표 | 판정 |
|:--|:--|:--|:--|
| `AppBridgeHost` | **안** (신규 진입) | 4/4 = 100% | **PASS** (≥70%) |
| `AppContainer` | 밖 (구조적 예외, E-4에서 정당성 확인) | — | 해당 없음 |
| `App` / `MainActivity` | 밖 (기존 예외) | — | 해당 없음 |

**(b) 베이스라인 대비 하락 금지 — 경로가 성립한다**

내가 직접 측정한 값으로 §3.2 표를 전부 재계산했다. **6개 지표 전부 일치하고, 하락 지표 0건이다.**

| 지표 | 베이스라인(내 실측) | + `AppBridgeHost`(내 실측) | 목표 | 방향 |
|:--|:--|:--|:--|:--|
| LINE | 98/98 | +4/+4 | **102/102 = 100.00%** | 동일 |
| BRANCH | 18/18 | +2/+2 | **20/20 = 100.00%** | 동일 |
| CLASS | 14/14 | +1/+1 | **15/15 = 100.00%** | 동일 |
| INSTRUCTION | 607/636 | +19/+19 | 626/655 = 95.57% | 상승 |
| METHOD | 45/55 | +4/+4 | 49/59 = 83.05% | 상승 |
| COMPLEXITY | 54/64 | +5/+5 | 59/69 = 85.51% | 상승 |

- **상승 3건이 분모 조작이 아님을 확인했다** — 분자·분모가 **같은 값**만큼 늘어난 결과이고, 그 값이 `AppBridgeHost` 단일 클래스의 실측 규모와 정확히 일치한다.
- **CLASS 14 → 15의 근거도 확인했다.** XML의 `<class>` 원소는 28개인데 CLASS 카운터는 14다(내가 직접 파싱). 코드 없는 인터페이스·`Companion`·`$$serializer`가 기여하지 않기 때문이며, `AppBridgeHost`는 실코드가 있어 **CLASS 1**을 기여한다(내 측정에서 `CLASS 0/1` 확인).
- **§3.1의 짝 논증이 성립한다.** 제외 해제만 하면 98/102 = 96.08%로 (b) 위반, 테스트만 하면 분모 밖이라 F-2 미해결. **둘 다 해야 100.00%가 유지된다.** 결론이 옳다.
- **중간 상태 96.08%(§3.4)는 회귀가 아니다.** 4단계 종료 시점에는 발생하지 않으며(Step 4가 4단계 안), 그래도 중간 커밋에서 관측될 수 있다는 경고가 적절히 명시돼 있다.

---

## 3. 검증 정직성 (`verification-honesty.md`)

### V1 — 대리 신호 / 게이트 독립성

**전반적으로 PASS.** 계획이 §4 서두에서 *"빌드 성공은 이 배치에서 대리 신호이므로 어느 Step에서도 단독 근거로 쓰지 않는다"*를 명시하고, 실제로 모든 `G*-a`를 "필요조건일 뿐"으로 격하했다. 이는 R-06(타입 추론이라 `MainActivity`를 안 고쳐도 컴파일 통과)에 대한 정확한 대응이다. 나도 `MainActivity.kt:129·201`에 import가 없음을 원문에서 확인했다.

**게이트별로 "실제로 깨질 수 있는가"를 하나씩 확인했다:**

| 게이트 | 깨질 수 있는가 | 확인 방법 |
|:--|:--|:--|
| G1-c / G2-b (`grep -c '^class ' App.kt`) | **YES** | 현재 값이 **3**임을 실행 확인. Step 1 후 2, Step 2 후 1로 결정적으로 변한다 |
| G1-d (`bridge/AppBridgeHost`가 분모 진입, 98/102) | **YES** | §3 V8 실행으로 `bridge/AppBridgeHost.class`가 제외되지 **않음**을 확인 + LINE 4 실측 |
| G2-c (`AppContainer`가 분모에 **없음**) | **YES** | 같은 실행에서 루트 `AppContainer.class`가 계속 제외됨을 확인 |
| G2-d / G3-b / G5-b (수치 불변) | **YES** | 움직이면 즉시 드러나는 등식 대조 |
| G3-c (제외 항목 11 → 10) | **YES** | 현재 11개 실행 확인 |
| G4-b (`AppBridgeHost` 4/4·2/2·4/4) | **YES** | 총계 4·2·4가 내 실측과 일치 — 미달이면 반드시 깨진다 |
| G4-d (테스트 48 → 50, skip 0) | **YES** | 현재 48·skipped 0 실행 확인 |
| G6-b (`#result`에 `1.0.0`) | **YES** | "응답이 왔다"가 아니라 **값**으로 판정. `versionName = "1.0.0"`(`build.gradle.kts:31`) 확인 |
| G6-d (`dumpsys` grep 결과 0) | **YES** | 화면이 아니라 프로세스 상태로 판정 |
| **G2-a 후단 ("unused import 경고 0건")** | **NO — 항상 참** | **아래 V1-b** |

#### V1-b (지적) — G2-a의 "unused import 0건"은 공허한 조건이다

**실측**: 스크래치패드에 Kotlin **2.3.21**(이 프로젝트와 같은 버전) JVM 프로젝트를 만들어 명백히 사용하지 않는 import(`java.util.concurrent.ConcurrentHashMap`)를 넣고 `compileKotlin --rerun-tasks`를 실행했다 → **경고 0건, BUILD SUCCESSFUL.** 미사용 import 경고는 IDE 인스펙션이지 컴파일러 진단이 아니다.

즉 이 조건은 미사용 import가 있든 없든 **항상 참**이고, `verification-honesty.md` V1의 *"하나가 사실상 항상 참이면 그 조건은 없는 것과 같다"*에 해당한다.

**REJECTED로 가지 않는 이유**: 이 조건은 (i) "필요조건일 뿐"로 이미 격하된 `G*-a`의 **후단 부속절**이고, (ii) Step 2의 실질 판정은 G2-b·G2-c·G2-d **세 개의 독립 게이트**가 지고 있으며 그 셋은 위 표대로 전부 실제로 깨진다. (iii) 놓칠 수 있는 것은 남은 미사용 import(MINOR 수준)뿐이고, 계획이 **삭제할 import와 남길 import를 §4 Step 2에 이름까지 열거**해 두어 `git diff` 대조가 컴파일러 경고보다 **더 강한 검사**로 이미 가능하다. → **조건 C-1로 전환하면 해소된다.**

### V2 — 새 검증이 실패를 잡는지 증명

**PASS, 단 C-2로 보강.** G4-a가 의도적 RED + 원복 기록을 요구한 것은 적절하다. 다만 제시된 예시(`requestFinish()` 호출 주석 처리)는 **케이스 1만** RED로 만든다. 케이스 2(null 분기)는 "예외 없이 반환한다"는 성질상 약한 단정이라 **RED 증명이 특히 필요하다.** → **C-2.**

### V3 — 환경이 신호를 만들 수 있는가

**PASS.** Step 6이 환경 확인을 **먼저** 했다. 나도 `adb devices -l`로 `R3CN60L0QMT / SM_G981N` 연결을 재확인했다. `abiFilters = arm64-v8a`와 일치하고, 도즈 대비 "검증 전 화면을 켠다"도 명시돼 있다. GeckoView 네이티브 엔진이 필요해 JVM으로는 원리적으로 관측 불가라는 판단도 옳다.

### V4 — 환경 한정 테스트 이중 장치

**해당 없음.** Step 4의 테스트는 안드로이드 타입 0개의 순수 JVM이라 어디서나 돈다. **환경 한정 표식을 붙이지 않는 것이 옳다**(붙이면 V4가 경고하는 가장 발견하기 어려운 거짓 그린이 된다). 계획이 Robolectric·coroutines-test를 "불필요"로 판단한 것도 이 맥락에서 정확하다.

### V5 — 비결정성

**PASS.** G6-b·G6-d를 **연속 3회** 수행하고 "3회 중 N회"로 보고하며, 1회라도 실패하면 통과가 아니라고 못박았다. 관측 실패 시 남길 진단(원문 문자열·`logcat` 200줄·`dumpsys`·경과 시간)도 사전에 지정돼 있다.

### V6 — 알려진 결함을 초록으로 덮지 않는가

**PASS.** G4-d가 `@Ignore`·skip **0건**을 명시적으로 요구한다.

### V7 — 인계값 재측정

**PASS.** planner가 §0에 재측정 표를 두고 **1차 출처(배치 01 문서)를 인용하지 않고 직접 실행한 XML에서 읽었음**을 밝혔다. 내 재측정에서 **불일치 0건**이다(§0).

### V8 — 도구가 조용히 건너뛰는 곳 : **글롭 의미론 두 개를 내가 직접 실행 확인했다**

이것이 이 배치 설계의 근거이므로 재실행했다.

**① Gradle Ant PatternSet (`coverageExclusions`)** — 스크래치패드에 독립 Gradle 프로젝트를 만들어 `app/build.gradle.kts`의 **제외 목록 11개 원문을 그대로** 복사하고, 가짜 `.class` 트리에 `fileTree(...) { exclude(coverageExclusions) }`를 적용했다.

```
제외 후 남는(= 분모에 들어가는) 파일
  IN  com/example/geckoviewtest/MainViewModel.class
  IN  com/example/geckoviewtest/bridge/AppBridgeHost.class      ← 이동 후 분모 진입
  IN  com/example/geckoviewtest/bridge/BridgeDispatcher.class
  IN  com/example/geckoviewtest/data/AppInfoRepositoryImpl.class
  IN  com/example/geckoviewtest/di/AppContainer.class           ← di/ 였다면 분모 진입
```
- 루트의 `AppContainer.class`·`AppBridgeHost.class`·`App.class`·`App$Companion.class`·`MainActivity.class`·`gecko/**`·`bridge/NativeBridgeHandler.class` → **전부 제외됨**
- **`*`가 `/`를 넘지 않음이 확인됐다.** R-01 참.

**② Python `fnmatch` (scope-guard hook)** — `guard-impact-scope.py`를 읽어 `os.path.relpath` + `fnmatch.fnmatch` 사용을 확인한 뒤 같은 함수로 실행:

```
.../geckoviewtest/AppContainer.kt          → *AppContainer.kt   매칭
.../geckoviewtest/bridge/AppBridgeHost.kt  → *AppBridgeHost.kt  매칭   ← / 를 넘는다
.../geckoviewtest/di/AppContainer.kt       → *AppContainer.kt   매칭   ← / 를 넘는다
app/src/test/.../AppBridgeHostTest.kt      → app/src/test/**    매칭
```

**두 의미론이 서로 다르다는 이 배치의 전제는 참이다.** 그리고 그 차이의 방향이 계획에 유리하다 — hook은 관대해 이동을 막지 않고, Ant는 엄격해 이동한 클래스를 분모에 넣는다.

**③ 계측 테스트 미집계** — 해당 없음. Step 4는 JVM 단위 테스트다.

### V9 — 범위 밖 결함을 넓히지 않는가

**PASS.** §7에 **행동 규약**이 있고 내용이 구체적이다: G1-d가 깨지면 *"범위를 넓혀 원인을 좇지 말고 Step 1에서 중단하고 planner에게 반환한다"*, G4-c 미달 시 *"테스트를 늘려 억지로 맞추기 전에 어느 라인이 미커버인지 XML에서 특정"*. 후자는 **수치를 맞추려 테스트를 부풀리는 것을 선제적으로 금지**한 것이라 특히 적절하다.

---

## 4. 범위 (`scope-guard.md` · V9)

### 화이트리스트 대조 — **직접 실행함. 이탈 0건.**

계획 §10의 6개 경로를 hook과 동일한 `fnmatch`로 재실행했다.

| Step | 경로 | 매칭 근거 | 판정 |
|:--|:--|:--|:--|
| 1 | `.../geckoviewtest/bridge/AppBridgeHost.kt` | glob `.../geckoviewtest/*AppBridgeHost.kt` | **통과** |
| 1,2 | `.../geckoviewtest/App.kt` | `allowed_files` | **통과** |
| 2 | `.../geckoviewtest/AppContainer.kt` | glob `.../geckoviewtest/*AppContainer.kt` | **통과** |
| 3 | `app/build.gradle.kts` | `allowed_files` | **통과** |
| 4 | `app/src/test/java/.../bridge/AppBridgeHostTest.kt` | glob `app/src/test/**` | **통과** |
| 5 | `.../geckoviewtest/MainActivity.kt` | `allowed_files` | **통과** |

**impact-analyzer 재실행 불필요.** planner의 주장과 일치한다.

또한 R-10이 경고한 두 함정을 계획이 실제로 회피했음을 확인했다 — (a) 파일명 = 클래스명 유지, (b) `bridge/BridgeDispatcher.kt`에 병합하지 않음(병합했다면 이 배치가 없애려는 "한 파일에 여러 클래스"를 재생산했을 것이다).

### 범위 확대 없음 — 4건 개별 확인

| 확인 항목 | 결과 |
|:--|:--|
| **F-1(assets `inputs.dir` 미선언)을 끌어들였는가** | **아니다.** §8이 이월로 명시했고, **Step 3 변경 내용에 "`inputs.dir` 추가 금지"를 못박아 두었다.** `build.gradle.kts`가 열려 있어 물리적으로 가능한 상황에서 명시적 금지를 넣은 것은 적절한 처리다 |
| **스코프 변경(Application → Activity)을 계획했는가** | **아니다.** §1·§8에서 `GeckoRuntime` 프로세스당 1회 제약을 근거로 "재론 금지"로 확정. 본문 무변경 계약(R-P6)이 이를 이중으로 보장한다 |
| **`interface BridgeHost`를 `BridgeDispatcher.kt`에서 분리했는가** | **아니다.** §8에서 후속 배치 **후보로만** 기록. 화이트리스트에도 없다. 내가 grep으로 확인한 결과 이 배치는 `BridgeDispatcher.kt`를 **읽지도 고치지도 않는다** |
| **새 라이브러리 / `libs.versions.toml`** | **0건.** §8이 명시. Step 4는 기존 JUnit만 사용하며, `AppBridgeHost`가 안드로이드 타입 0개라는 내 실측이 이 판단을 뒷받침한다 |
| **빌드 설정 변경 범위** | `coverageExclusions` **1줄 삭제 + 주석 추가**뿐. G3-a가 `dependencies`·`ndk.abiFilters`·다른 제외 패턴이 변하면 실패하도록 못박았다 |

---

## 5. 체크리스트 요약 (`plan-evaluation` A/B/C/D)

### A. 정합성
- [x] 모든 대상 파일이 화이트리스트 안 — **직접 실행 대조, 이탈 0건**
- [x] `risk_notes` R-01~R-11이 계획에 반영됨 — R-01→R-P2, R-03/04→§3.1, R-06→R-P1, R-08→R-P4, R-09→R-P8, R-10→§10, R-11→§8. **신규 R-P3·R-P5·R-P6·R-P7 추가**
- [x] 모든 Step에 검증 방법 있음 (Step당 3~5개)

### B. 아키텍처 — §2 전 조항 PASS

### C. 실행 가능성
- [x] **중간에 빌드가 깨진 채 방치되는 구간 없음** — Step 1이 `App.kt`에 `bridge.AppBridgeHost` import를 **함께** 추가하므로 Step 1 종료 시점에 컴파일 가능. Step 2는 같은 패키지 내 파일 분리라 순서상 안전
- [x] **기존 코드를 실제로 읽고 세운 계획** — 인용을 전수 대조했다: `App.kt:93-149`(`AppContainer`), `App.kt:151-167`(`AppBridgeHost`), `App.kt:59`, `build.gradle.kts:118-130`·`:121`·`:122`, `MainActivity.kt:70·122·125·129·201`, `bridge/BridgeDispatcher.kt:59`, `BridgeProtocol`의 `UNKNOWN_FUNCTION`(`:98`), `index.html`의 `브리지 상태:`·`(JS 미실행)`·버튼 3개·`#result`, `versionName = "1.0.0"` — **전부 원문과 일치. 존재하지 않는 클래스·상수·문자열을 전제한 곳 0건**
- [x] 테스트 경로가 화이트리스트 안(`app/src/test/**`)
- [x] 인용한 사실이 실제로 맞음 — §0 재측정 불일치 0건

### D. 검증 정직성
- [x] V1 — 게이트 독립성 (G2-a 후단 1건은 **C-1**로 교정)
- [x] V2 — RED 증명 절차 있음 (**C-2**로 보강)
- [x] V3·V4 — 환경 확인 선행, 환경 한정 표식 불필요 판단 정확
- [x] V5 — 3회 반복 + "N회 중 M회" 보고
- [x] V6 — skip·`@Ignore` 0건 요구
- [x] V7 — 재측정 표 (내 검증에서 불일치 0건)
- [x] V8 — 글롭 의미론 2종 (내가 직접 실행 확인)
- [x] V9 — 행동 규약 §7
- [x] 자동화 불가 항목(Step 6 실기기)을 수동으로 남긴다고 명시

---

## 6. developer에게 넘기는 주의 사항

1. **조건 C-1·C-2를 반드시 반영할 것.** 위 표 참조. 계획 본문은 바꾸지 말고 **로그에 조건 이행을 기록**한다.
2. **"빌드 성공"을 참조 갱신의 근거로 쓰지 마라.** `MainActivity.kt:129·201`은 타입 추론이라 import가 없다(내가 원문 재확인). 판정은 커버리지 XML 분모 목록과 `git diff`로 한다.
3. **qa(6단계)의 역할을 비워 두지 마라.** Step 4를 developer가 쓰더라도 qa는 인수 검토자이지 승인 도장이 아니다. **qa는 케이스를 추가할 권한을 유지한다** — 특히 `onFinishRequested`를 등록 → 해제 → 재등록하는 전이 케이스는 `MainActivity`의 실제 수명주기(`onCreate` 등록 / `onDestroy` null)와 대응하는데 계획의 2케이스에는 없다. **필수는 아니다**(102/102에 불필요) — 넓히라는 뜻이 아니라 qa가 판단할 여지를 남기라는 뜻이다.
4. **커버리지 측정에 `clean`을 빠짐없이 붙여라.** R-P3는 내가 재현하지 않았으나 `clean` 추가는 손해가 없고, `clean jvmCoverageReport --rerun-tasks`가 베이스라인을 **두 JDK에서 각각 재현**함을 내가 확인했다.
5. **JAVA_HOME은 JBR 17로 고정.** JDK 21도 동작하고 수치도 동일함을 내가 재확인했으므로, 21로 잘못 잡혔다고 해서 그것이 실패 원인일 수는 없다 — **다른 원인을 찾아라.**
6. **Step 1·2의 "본문 무변경"은 계약이다.** `@Volatile` 누락과 `?.`→`!!`는 컴파일도 테스트도 통과하므로 `git diff`로만 잡힌다. **C-2가 후자를 능동적으로 검출하게 만든다.**
7. **G1-d가 깨지면 §7 행동 규약대로 즉시 반환하라.** 내 실행 확인상 깨질 이유가 없지만(Ant 패턴이 `bridge/AppBridgeHost.class`를 제외하지 않음을 직접 봤다), 깨진다면 이 배치의 전제가 틀린 것이므로 원인을 좇아 범위를 넓히지 말 것.
8. **`App.kt`는 168줄이다**(계획의 "169줄"은 파일 끝 빈 줄 처리 차이). G2-b 판정에 영향 없다.
9. **F-1을 건드리지 마라.** `build.gradle.kts`가 열려 있어 물리적으로 가능하지만 범위 밖이다. G3-a가 이를 검출한다.

---

## 7. 판정 근거 요약

- `architecture.md` **전 조항 PASS.** 커버리지 두 조항 모두 충족하며, **목표치 102/102 = 100.00%의 산술을 내가 독립적으로 측정한 값으로 재계산해 확인**했다(하락 지표 0건).
- E-1~E-5 **전부 APPROVED.** 그중 계획을 바꿀 수 있었던 E-2·E-3은 각각 §1-E2·§1-E3에 판정 근거를 상세히 적었다.
- **재측정 불일치 0건**(실질). 이 파이프라인에서 인계값 검증으로 여러 건이 잡혔던 것과 달리 이번 계획의 수치는 전부 맞았다.
- 지적 1건(**V1-b**, G2-a 후단의 공허한 조건)은 계획의 구조를 바꾸지 않고 **조건 C-1**로 해소된다. Step 2의 실질 판정은 독립적으로 깨지는 게이트 3개가 지고 있어 **거짓 그린 위험이 없다.**
- 보강 1건(**C-2**)은 V2의 요구를 null 분기까지 확장하는 것으로, R-P6·R-P7을 동시에 방어한다.

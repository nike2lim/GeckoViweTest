# 커버리지 리포트 — 파이프라인 7단계 (최종 게이트)

- **일시**: 2026-08-06 13:14:51
- **측정자**: coverage-reporter
- **판정**: **PASS**
- **`enforce` 처리**: `pipeline/impact-report.json` → `false` (scope-guard.md 규칙 4)

---

## 1. 측정 조건 — 캐시 배제

```
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew clean jvmCoverageReport --rerun-tasks
→ BUILD SUCCESSFUL in 20s / 30 actionable tasks: 30 executed
```

| 항목 | 확인 결과 |
|:--|:--|
| `:app:clean` | 실행됨 |
| `:app:testDebugUnitTest` | **실행됨** (UP-TO-DATE 아님) |
| `:app:jvmCoverageReport` | **실행됨** (UP-TO-DATE 아님) |
| 리포트 XML | `app/build/reports/jacoco/jvmCoverageReport/jvmCoverageReport.xml` (29,975 B, 13:10 생성) |
| `.exec` | `app/build/outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec` **1개** |
| `.ec` (계측) | **0개** — 계측 테스트 미집계 확인(V8). 베이스라인과 동일 조건 |

인계값을 옮겨 적지 않고 전량 재측정했다(V7).

---

## 2. 전체 커버리지 (실측)

| 지표 | covered | missed | total | 비율 |
|:--|--:|--:|--:|--:|
| **LINE** | 98 | 0 | 98 | **100.00%** |
| **BRANCH** | 18 | 0 | 18 | **100.00%** |
| **CLASS** | 14 | 0 | 14 | **100.00%** |
| INSTRUCTION | 607 | 29 | 636 | 95.44% |
| METHOD | 45 | 10 | 55 | 81.82% |
| COMPLEXITY | 54 | 10 | 64 | 84.38% |

**테스트**: 48건 / 실패 0 / 오류 0 / skip 0 / `@Ignore` 0건

| 스위트 | 건수 |
|:--|--:|
| `MainViewModelTest` | 12 |
| `BridgeProtocolTest` | 11 |
| `ExtensionManifestTest` | 8 |
| `BridgeDispatcherTest` | 7 |
| `BridgeWireContractTest` | 7 |
| `AppInfoRepositoryTest` | 3 |
| **합계** | **48** |

### 2.1 QA 보고값과의 대조 — **불일치 없음**

| 항목 | QA 보고 | 재측정 | 판정 |
|:--|:--|:--|:--|
| LINE | 98/98 = 100.0% | 98/98 = 100.0% | 일치 |
| BRANCH | 18/18 = 100.0% | 18/18 = 100.0% | 일치 |
| CLASS | 14/14 | 14/14 | 일치 |
| 테스트 건수 | 48 | 48 | 일치 |
| 실패 / skip / `@Ignore` | 0 / 0 / 0 | 0 / 0 / 0 | 일치 |
| 신규 스위트 | `BridgeWireContractTest`(7) · `ExtensionManifestTest`(8) | 동일 | 일치 |

**QA가 보고한 수치는 전부 재현됐다.** 다만 아래 §2.2는 QA 리포트에 없던 것으로, 불일치가 아니라 **"100%"의 해상도에 대한 보충**이다.

### 2.2 LINE 100%인데 INSTRUCTION 95.44% — 정체 규명

LINE·BRANCH가 100%인데 INSTRUCTION에 29개, METHOD에 10개가 미커버로 남는다. 전수 확인한 결과 **10개 전부가 테스트에서 호출되지 않은 Kotlin 프로퍼티 게터**다.

| 클래스 | 미커버 메서드 |
|:--|:--|
| `BridgeResponse` | `getId` `getType` `getOk` `getValue` `getError` (5) |
| `BridgeError` | `getCode` `getMessage` (2) |
| `MainUiEvent$Navigate` | `getUrl` (1) |
| `BridgeRequest` | `getPayload` (1) |
| `BridgeProtocol` | `getJson` (1) |

각 3 instruction(`ALOAD`/`GETFIELD`/`ARETURN`)이고, 생성자 라인과 라인 번호를 공유하는 합성 접근자라 **LINE 카운터에는 미커버로 잡히지 않는다.** JaCoCo의 알려진 특성이며 결함이 아니다.

**판정 기준은 architecture.md가 명시한 "라인 커버리지"이므로 게이트 판정은 LINE으로 한다.** 다만 **LINE 100%가 "모든 코드가 실행됐다"를 뜻하지는 않는다** — 이 절을 남기는 이유가 그것이다.

---

## 3. 클래스 단위 커버리지 (70% 조항 판정)

**실행 가능 라인이 있는 14개 클래스 — 전부 100%, 70% 미달 0건.**

| 클래스 | LINE | BRANCH |
|:--|--:|--:|
| `bridge/BridgeProtocol` | 20/20 (100%) | 12/12 |
| `MainViewModel` | 25/25 (100%) | – |
| `bridge/BridgeDispatcher$handle$2` | 10/10 (100%) | 4/4 |
| `bridge/BridgeResponse` | 8/8 (100%) | – |
| `bridge/BridgeRequest` | 7/7 (100%) | – |
| `MainUiState` | 6/6 (100%) | – |
| `bridge/BridgeDispatcher` | 6/6 (100%) | – |
| `bridge/BridgeError` | 4/4 (100%) | – |
| `MainViewModel$Factory` | 4/4 (100%) | – |
| `data/AppInfoRepositoryImpl` | 3/3 (100%) | 2/2 |
| `MainViewModel$emit$1` | 2/2 (100%) | – |
| `MainUiEvent$Navigate` | 1/1 (100%) | – |
| `bridge/BridgeResult$Success` | 1/1 (100%) | – |
| `bridge/BridgeResult$Failure` | 1/1 (100%) | – |

리포트에는 `<class>` 항목이 **28개** 있으나 나머지 14개는 **실행 가능 라인이 0개**다(인터페이스 `BridgeHost`·`AppInfoRepository`, sealed 마커 `MainUiEvent`·`BridgeResult`, enum `ErrorCode`, 생성된 `$$serializer`·`$Companion`). JaCoCo의 CLASS 카운터가 이들을 세지 않으므로 `CLASS 14/14`가 성립한다. **미달이 아니다.**

plan §7.1 목표 대비:

| 클래스 | 목표 | 실측 | 판정 |
|:--|:--|:--|:--|
| `BridgeProtocol` | ≥90% | 100% | 충족 |
| `BridgeDispatcher` | ≥90% | 100% | 충족 |
| `AppInfoRepositoryImpl` | 100% | 100% | 충족 |
| `MainViewModel` | ≥85% | 100% | 충족 |
| `MainUiState` | ≥80% | 100% | 충족 |

evaluator가 §0.3에서 실증한 "순수 Kotlin 레이어 JVM LINE 100%" 경로가 그대로 성립했다. plan R-11이 경고한 설계 오염은 관측되지 않았다.

---

## 4. `coverageExclusions` 제외 범위 타당성 — **타당 (경계 확대 아님), 단 1건 유보**

### 4.1 plan §7.2 코드와의 축자 대조

`app/build.gradle.kts`의 실제 목록은 **11개**, plan §7.2에 적힌 목록은 **9개**다. **차이 2건:**

```
+ "com/example/geckoviewtest/AppContainer*.class"
+ "com/example/geckoviewtest/AppBridgeHost*.class"
```

나머지 9개는 plan §7.2와 **완전 일치**한다(`App.class`, `App$*.class`, `MainActivity*.class`, `gecko/**`, `bridge/NativeBridgeHandler*.class`, `**/BuildConfig.*`, `**/R.class`, `**/R$*.class`, `**/databinding/**`).

> **QA의 "diff로 확인 가능하다"는 주장은 이 저장소에서 성립하지 않는다.** `git status` 결과 **이 프로젝트는 git 저장소가 아니다**(`fatal: not a git repository`). 버전 관리가 없어 diff를 뜰 대상이 없다. 따라서 diff가 아니라 **plan §7.2/§7.3 원문과의 축자 대조**로 검증했다. — QA가 `build.gradle.kts`를 건드리지 않았다는 것과 목록이 plan과 일치하는가는 **별개 질문**이며, 위 2건은 QA가 아니라 4단계(developer)에서 들어온 것이다.

### 4.2 추가된 2건의 판정

`AppContainer`와 `AppBridgeHost`는 **둘 다 `App.kt` 안에 선언된 최상위 클래스**다(App.kt:100, App.kt:158). 최상위 클래스라 각각 `AppContainer.class`·`AppBridgeHost.class`로 컴파일되므로 `App.class`/`App$*.class` 패턴에 **걸리지 않는다.**

plan **§7.3의 제외 경계는 파일 단위로 서술**돼 있다 — 표의 첫 행이 클래스가 아니라 **"`App.kt`"**다. 즉 §7.3은 App.kt 전체를 분모 밖에 두기로 했는데, §7.2의 코드 목록이 그 의도를 다 담지 못했다(형제 최상위 클래스 누락). 추가된 2건은 **§7.3의 경계를 넓힌 것이 아니라 §7.2의 코드를 §7.3의 표에 맞춘 것**이다.

**→ 제외 범위 확대 아님. 승인된 경계 안이다.**

### 4.3 유보 1건 — `AppBridgeHost`는 원칙이 아니라 파일 위치로 제외됐다

`app/build.gradle.kts`가 스스로 밝힌 제외 기준은 **"테스트하기 귀찮아서가 아니라 JVM 테스트로 실행 자체가 불가능한가"**다. 두 클래스를 이 기준으로 다시 보면:

| 클래스 | 실측 | Android 의존 | 기준 충족? |
|:--|:--|:--|:--|
| `AppContainer` | LINE 0/20, BRANCH 0/2 | `Application`, `packageManager`, `Dispatchers.Main.immediate` | **충족** — JVM 실행 불가 |
| `AppBridgeHost` | LINE 0/4, BRANCH 0/2 | **없음 (순수 Kotlin)** | **미충족** |

`AppBridgeHost`는 `BridgeHost` 구현체로, `@Volatile` 널 가능 람다 필드 하나와 `requestFinish() { onFinishRequested?.invoke() }`가 전부다. **안드로이드 타입을 하나도 참조하지 않으며 JVM에서 즉시 테스트 가능하다.** 지금은 "App.kt 안에 있다"는 이유만으로 분모 밖에 있다.

**영향 실측** (JaCoCo core 0.8.15 Analyzer로 기존 `.exec`에 대해 직접 측정 — 추정치 아님):

| 시나리오 | LINE | BRANCH |
|:--|:--|:--|
| 현재 | 98/98 = 100.00% | 18/18 = 100.00% |
| `AppBridgeHost` 포함 시 | 98/102 = **96.08%** | 18/20 = **90.00%** |
| `AppBridgeHost`+`AppContainer` 포함 시 | 98/122 = **80.33%** | 18/22 = 81.82% |

**어느 경우에도 70% 게이트를 넘는다.** 따라서 FAIL 사유가 아니라 **후속 과제(F-2)**로 등록한다. 이 배치의 변경 클래스도 아니다.

---

## 5. 분모 밖 변경 코드 — `NativeBridgeHandler`에 70% 조항을 어떻게 적용했나

### 5.1 사실 확인

이번 배치의 **유일한 프로덕션 로직 변경**은 `NativeBridgeHandler.kt:52-56`이다:

```kotlin
} catch (e: CancellationException) {
    throw e
}
```

이 클래스는 `coverageExclusions`에 있어 분모 밖이다. **분모에 넣었을 때의 실측값**(JaCoCo Analyzer 직접 측정):

| 클래스 | LINE | BRANCH |
|:--|--:|--:|
| `NativeBridgeHandler` | **0/6** | 0/0 |
| `NativeBridgeHandler$onMessage$1` | **0/13** | 0/6 |
| `NativeBridgeHandler$Companion` | 0/0 | 0/0 |
| **합계** | **0/19 = 0.00%** | 0/6 |

**즉 architecture.md "변경된 클래스 라인 커버리지 70% 이상"을 축자 적용하면 0% < 70%로 미달이다.** 이 사실을 숨기지 않고 먼저 적는다.

### 5.2 적용 판정 — **구조적 예외로 처리 (FAIL 아님)**

`coverage-report` 스킬 §4의 판정표는 미달 조항에 **"(구조적 예외 아님)"** 단서를 달고 있다. 즉 구조적 예외는 인정된 카브아웃이며, 이 클래스가 거기 해당하는지를 판정했다.

**① 예외 사유가 실재하는가 — 검증함 (추측 아님)**

`NativeBridgeHandler`는 JVM 단위 테스트에서 **원리적으로 실행 불가**다:

- `android.util.Log`를 import하고 `onMessage` 진입 직후 `Log.d(...)`를 호출한다(:42). `app/build.gradle.kts`에 **`testOptions.unitTests.returnDefaultValues` 설정이 없음을 확인**했으므로, JVM 테스트에서 이 호출은 `RuntimeException("Stub!")`로 즉시 터진다.
- `WebExtension.MessageDelegate`를 구현하고 `GeckoResult<Any>`를 반환한다 — GeckoView 타입은 실기기 없이 로드되지 않는다.

V3("환경이 신호를 만들 수 있는지 먼저 확인한다") 기준으로, 이 클래스를 분모에 넣으면 **영원히 0%로 남아 수치만 왜곡**한다. `build.gradle.kts` 주석의 제외 기준을 정확히 충족한다.

**② 사전 승인된 경계인가 — 그렇다**

plan **§7.3이 이 클래스를 명시적으로 제외 대상으로 지정**했고("`bridge/NativeBridgeHandler.kt` | `WebExtension.MessageDelegate` 어댑터. **로직을 두지 않기로 했으므로** 20줄 내외"), evaluator가 3단계에서 이 계획을 APPROVED했다. coverage-reporter가 사후에 임의로 만든 예외가 아니다.

**③ 설계 전제가 지켜졌는가 — 그렇다**

§7.3의 전제는 "이 클래스에 **업무 로직을 두지 않는다**"였다. 실측 19라인으로 "20줄 내외"라는 계획값 안에 있고, 추가된 3줄은 새로운 업무 로직이 아니라 **취소 신호를 삼키지 않고 재전파하는 예외 처리**다. 로직을 분모 밖으로 도피시킨 흔적은 없다 — 오히려 `catch (Exception)`이 `CancellationException`까지 잡아 취소를 삼키던 것을 바로잡은 변경이다.

**→ 세 조건이 모두 성립하므로 구조적 예외로 인정하고, 이 미달을 FAIL로 올리지 않는다.**

### 5.3 그러나 — 보상 통제가 미실행이다 (부채로 등록)

**예외를 인정하되 조용히 넘기지는 않는다.** plan §7.3은 이 클래스의 검증 수단을 **"Step 2 게이트(3단 왕복) + AC-003-2 역주입 테스트"**로 지정했다. 그런데 **QA는 AC-003-2를 "통과"가 아니라 "관측하지 않았다"로 남겼다**(프로덕션 파일 일시 수정 + 재빌드가 필요한데 "프로덕션 코드를 수정하지 마라" 지시를 우선함).

즉 **구조적 예외의 정당성은 성립하지만, 그 예외를 떠받치기로 한 보상 통제가 이번 배치에서 실행되지 않았다.** 이것을 적지 않으면 "0% 코드가 아무 검증 없이 통과"한 것이 리포트에서 사라진다.

**부채 등록 (F-3)** — 만료 조건: **다음에 `NativeBridgeHandler`를 수정하는 배치**에서 AC-003-2를 반드시 관측할 것. 그때까지 이 예외는 "정당하되 미검증" 상태다.

또한 이 예외는 **좁게** 유지한다 — 새 판정 로직을 `NativeBridgeHandler`나 `onMessage` 람다 안에 넣는 것은 이 예외로 정당화되지 않는다. 로직은 `BridgeDispatcher`/`BridgeProtocol`(분모 안, 100%)에 둔다.

---

## 6. 베이스라인 회귀 판정 — **회귀 없음**

### 6.1 비교 대상이 무엇인지 먼저 확정한다

`pipeline/coverage-baseline.json`은 **존재하지 않는다**(`find` 전수 확인). 스킬 §4에 따르면 "베이스라인 없음 → PASS (초기 베이스라인 생성으로 기록)"이다.

**그러나 "비교 대상이 없다"고 적고 끝내지 않았다.** 파이프라인 내부에 **동일 방법으로 측정된 베이스라인이 명시적으로 인계돼 있다**:

> `pipeline/review.md` rev.1 §9-8 → rev.2에서 재확인(§467-5):
> "**[유지·재확인] 커버리지 베이스라인**: LINE **98/98 = 100.0%**, BRANCH **16/18 = 88.9%**, CLASS **14**. `clean` + `--rerun-tasks`로 재측정한 값이다. coverage-reporter(7단계)는 이 값을 회귀 기준으로 쓰면 된다."

측정 방법(`clean` + `--rerun-tasks`)과 태스크(`jvmCoverageReport`)가 내 측정과 **동일**하므로 like-for-like 비교가 성립한다. 이것을 회귀 기준으로 삼는다.

### 6.2 비교

| 지표 | 베이스라인 (review.md rev.2) | **재측정** | 판정 |
|:--|:--|:--|:--|
| LINE covered / total | 98 / 98 = 100.0% | **98 / 98 = 100.0%** | **동일 — 회귀 없음** |
| BRANCH covered / total | 16 / 18 = 88.9% | **18 / 18 = 100.0%** | **+2 covered — 개선** |
| CLASS | 14 | **14** | **동일** |

### 6.3 "회귀 없음"의 근거 — 대리 신호가 아닌 이유

단순히 "숫자가 안 떨어졌다"가 아니라, **떨어질 수 있었는데 안 떨어졌음**을 세 가지로 확인했다:

**① 모집단이 동일하다 (분모 불변)**
LINE total 98 → 98, BRANCH total 18 → 18. **분모가 한 칸도 움직이지 않았다.** 따라서 "파일이 빠져서 평균이 유지된" 경우가 아니며, 모집단 변화로 회귀를 가릴 여지 자체가 없다. like-for-like 재계산이 불필요하다.

**② 파일 단위 `covered` 하락 0건 (스킬 §4의 최우선 불변식)**
백분율보다 우선하는 불변식이다. LINE covered가 98에서 유지됐고 분모도 동일하므로, 어떤 파일도 covered 라인을 잃지 않았다. 리포트의 sourcefile 6개(`MainUiState.kt` 7/7, `MainViewModel.kt` 31/31, `AppInfoRepositoryImpl.kt` 3/3, `AppInfoRepository.kt` 0/0, `BridgeDispatcher.kt` 16/16, `BridgeProtocol.kt` 41/41) 전부 미커버 0이다.

**③ 상승분의 출처를 특정했다 (분모 조작이 아님)**
BRANCH 16→18의 원인을 추적한 결과, **`BridgeProtocol.kt:67`의 `require(request.name.isNotBlank())`** 분기다. 이를 커버하는 테스트 `name이 비어 있으면 실패로 처리한다`(`BridgeProtocolTest.kt:59`)가 **실재함을 확인**했다.

- 이것은 **테스트 추가**로 인한 상승이지 프로덕션 코드 변경이나 제외 확대가 아니다.
- 근거: `BridgeProtocol`의 LINE total이 20으로 불변이고 BRANCH total도 12로 불변인 채 covered만 올랐다. **분모를 건드리지 않고 covered만 올린 것 = 진짜 검증이 늘었다는 증거.**
- QA 주장("도달 불가능한 생성 코드가 아니라 실제로 탈 수 있는 경로")이 재현됐다.

**④ 유일한 프로덕션 변경이 수치를 움직이지 않은 것이 정합적이다**
스킬 §4에는 "프로덕션 변경 0건 배치인데 수치가 움직이면 FAIL"이라는 조항이 있다. 이 배치는 프로덕션 변경 0건이 **아니다**(`NativeBridgeHandler`). 그리고 그 변경은 **분모 밖 클래스**이므로 원리적으로 수치를 움직일 수 없다. **LINE이 정확히 불변인 것은 이 사실과 정합적**이며, 반대로 LINE이 움직였다면 그것이 이상 신호였을 것이다.

### 6.4 이번 수치를 향후 베이스라인으로 확립

**신규 프로젝트이므로 이번 값을 공식 베이스라인으로 확립한다.**

```
LINE   98/98  = 100.00%   (primary)
BRANCH 18/18  = 100.00%
CLASS  14/14  = 100.00%
클래스 14개 전부 100% (70% 미달 0건)
측정: clean + --rerun-tasks, .exec 1 / .ec 0
```

`no_relaxation` 준수: 베이스라인은 **상향만** 했다(BRANCH 88.9% → 100.0%). 기준을 낮추거나 exclusion을 넓혀 수치를 맞춘 것은 없다 — §4.1에서 제외 목록이 plan 경계 안임을 축자 확인했다.

**단, `pipeline/coverage-baseline.json` 파일 자체는 생성하지 않았다.** 이번 단계의 산출물은 `coverage-report.md`·`enforce` 필드·작업 로그로 한정하라는 지시가 있었고, 베이스라인 파일 신설은 그 범위 밖이다. **후속 과제 F-4**로 넘긴다 — 현재 베이스라인이 산문(review.md·이 리포트)에만 존재하는 것은 취약하다.

---

## 7. 커버리지에 잡히지 않는 영역 — 100%의 사각

**LINE 100%는 JaCoCo 분모에 들어간 것에 한정된 수치다.** 액면 그대로 "전부 검증됨"으로 읽으면 안 되므로 분모 밖 영역을 명시한다.

| 영역 | 실측 규모 | 왜 분모 밖인가 | 무엇으로 검증되는가 |
|:--|:--|:--|:--|
| `MainActivity` + 내부 클래스 | class 파일 15개 | View 바인딩·세션 소유 | 실기기 게이트 (Step 6·7) |
| `gecko/**` | 3개 파일 | GeckoView 콜백, 실기기 없이 실행 불가 | 실기기 게이트 (Step 3·6·7) |
| `NativeBridgeHandler` | **0/19 라인** | `android.util.Log` + GeckoView 타입 | Step 2 게이트 + **AC-003-2(미관측 — F-3)** |
| `App` / `AppContainer` | 0/11, 0/20 라인 | `GeckoRuntime`·`PackageManager`·`Dispatchers.Main` | Step 1 게이트 |
| `AppBridgeHost` | 0/4 라인 | **파일 위치로만 제외 (F-2)** | — |
| **`assets/messaging/**` JS 전량** | 7개 파일 | **JaCoCo 대상 자체가 아님** | `BridgeWireContractTest`(7) + `ExtensionManifestTest`(8) = 계약·불변식 고정 |

**JS 자산 관련 주의**: 신규 테스트 15건이 JS/JSON을 **텍스트로 대조**해 계약을 고정하지만 **JS 실행 자체는 커버리지에 잡히지 않는다.** 이것을 "JS가 검증됐다"로 읽으면 대리 신호다(V1). 실제 동작은 실기기 게이트가 담당한다.

**계측 테스트 0건은 테스트 부실이 아니다.** `.ec` 0개를 실측 확인했고, GeckoView는 실기기 없이 실행조차 되지 않는다. 계측을 늘려도 이 수치는 오르지 않는다(V8). plan §7의 전제 그대로다.

**QA의 "LINE 미상승은 정상" 주장 — 판정: 성립한다.** 추가 17건 중 15건이 JS/JSON 자산 대조이고, 이들은 Kotlin 바이트코드를 한 줄도 실행하지 않으므로 LINE 분모·분자 어디에도 기여할 수 없다. 실제로 LINE total이 98로 불변인 것이 이를 뒷받침한다. **테스트 부실로 오판하지 않는다.**

---

## 8. 후속 과제

| ID | 담당 | 내용 | 근거 / 심각도 |
|:--|:--|:--|:--|
| **F-1** | developer | `:app:testDebugUnitTest`에 **`inputs.dir("src/main/assets/messaging")` 선언** | **실측 재현함** (아래) — 방치 시 거짓 그린 |
| **F-2** | developer + evaluator | `AppBridgeHost`(4라인/2분기, 순수 Kotlin)를 별도 파일로 분리해 분모에 넣거나, 구조적 예외로 명시 등록 | §4.3. 게이트 영향 없음(포함해도 96.08%) |
| **F-3** | qa | **AC-003-2 역주입 테스트 관측** — `NativeBridgeHandler` 구조적 예외의 보상 통제 | §5.3. **만료: 다음에 `NativeBridgeHandler`를 수정하는 배치** |
| **F-4** | — | `pipeline/coverage-baseline.json` 신설 (스키마: `primary_gate`, `regression_rule`, `per_file`, `structural_exceptions`) | §6.4. 베이스라인이 산문에만 존재 |
| **F-5** | — | **버전 관리 도입** — 이 프로젝트는 git 저장소가 아니다 | §4.1·§9. diff 기반 검증이 원천 불가 |

### F-1 상세 — 자산 UP-TO-DATE 함정 (직접 확인함)

QA의 인계를 그대로 옮기지 않고 재확인했다.

**확인 1 — 테스트가 소스 자산 경로를 직접 읽는다:**
```
ExtensionManifestTest.kt:33   File("src/main/assets/messaging/manifest.json").readText()
BridgeWireContractTest.kt:146 val MESSAGING_DIR = File("src/main/assets/messaging")
```

**확인 2 — 그 경로가 태스크 입력으로 선언돼 있지 않다:** `app/build.gradle.kts` 전문에 `inputs.` 선언이 **없다**. `testOptions` 블록도 없어 `includeAndroidResources`도 꺼져 있다. 즉 Gradle은 이 자산을 `testDebugUnitTest`의 입력으로 인지하지 못한다.

**확인 3 — 태스크가 실제로 캐시된다:** 변경 없이 재실행하니 `> Task :app:testDebugUnitTest **UP-TO-DATE**` / `28 actionable tasks: 28 up-to-date`.

**→ 세 사실을 합치면: 자산만 수정하면 Gradle이 재실행할 이유를 찾지 못해 계약 테스트 15건이 조용히 건너뛰어진다.** 자산이 깨져도 초록이 남는 **잠재적 거짓 그린**이며, V8이 경고하는 "도구가 조용히 건너뛰는 곳"의 교과서적 사례다.

> 자산을 실제로 수정해 재현하는 실험은 **하지 않았다.** `background.js`의 mtime만 바뀌고 내용은 같은 혼선이 이 파이프라인에서 이미 두 번(code-reviewer·qa) 발생했고, 버전 관리가 없어 원복 보증이 없기 때문이다. 위 세 가지 구조적 사실로 결론은 충분히 성립한다.

**내 측정은 이 함정의 영향을 받지 않았다** — `clean` + `--rerun-tasks`로 강제 실행했다.

---

## 9. 검증의 한계 (명시)

정직하게 남긴다.

1. **`background.js` 내용 동일성은 독립 검증하지 못했다.** QA는 "mtime만 갱신, 내용은 sha256 동일"이라고 보고했으나, **이 프로젝트는 git 저장소가 아니고 백업본도 없어 대조할 기준 사본이 존재하지 않는다.** 따라서 이 주장은 **재현하지 못했고, 인용하지도 않는다.** 다만 판정에 영향은 없다 — JS는 JaCoCo에 0 기여이고, 계약은 `BridgeWireContractTest`·`ExtensionManifestTest` 15건이 고정하며 전부 통과했다. (→ F-5)
2. **"변경된 클래스" 식별은 mtime이 아니라 내용·인계 문서로 판정했다.** `NativeBridgeHandler.kt:52-56`의 `catch (CancellationException)` 블록을 **소스에서 직접 확인**했다. mtime 기반 집계는 사용하지 않았다.
3. **실기기 게이트는 이 단계의 범위가 아니다.** 분모 밖 영역(§7)의 실제 동작은 앞 단계들의 실기기 검증에 의존하며, 커버리지 수치가 그것을 대신하지 않는다.

---

## 10. 판정

### **PASS**

| 스킬 §4 판정 조건 | 해당 여부 |
|:--|:--|
| 베이스라인 없음 → PASS (초기 베이스라인 기록) | 해당 (`coverage-baseline.json` 부재) |
| `per_file`에서 `line_covered` 하락 → FAIL | **해당 없음** — 하락 0건, 분모 불변 |
| `primary_gate` 하락이 모집단 변화로 설명 안 됨 → FAIL | **해당 없음** — 하락 자체가 없음(LINE 100.0% 유지) |
| 변경 클래스 라인 커버리지 70% 미만 (구조적 예외 아님) → FAIL | **구조적 예외로 처리** (§5.2) — 사유 검증 + 사전 승인 + 전제 유지. 부채 F-3 등록 |
| 프로덕션 변경 0건인데 수치 이동 → FAIL | **해당 없음** — 프로덕션 변경 1건이 존재하고, 그것이 분모 밖이라 LINE 불변인 것이 정합적 |

**근거 요약**
- 분모 안 14개 클래스 **전부 LINE 100%**, 70% 미달 **0건**
- 동일 방법·동일 모집단 베이스라인(review.md rev.2) 대비 **LINE 동일, BRANCH +2 개선, 회귀 0건**
- 상승분의 출처를 테스트 1건으로 특정 — **분모 조작 아님**
- 제외 목록이 plan §7.3 경계 **안**임을 축자 확인 — **제외 확대 없음**
- 테스트 48건 전부 통과, skip·`@Ignore` 0건 — **초록으로 덮은 결함 없음**(V6)

**미달·범위 확대가 아니므로 되돌려 보낼 단계 없음.** 후속 과제 F-1~F-5는 차단 사유가 아니며 다음 배치 요구사항으로 넘긴다(V9).

**이월 항목은 결함으로 올리지 않았다**: MINOR-1(`ensureBuiltIn` 실패 시 미처리 코루틴 예외), MINOR-8(프로브 배지 release 게이팅), `PermissionDelegate` 미구현(requirements §6 스코프 아웃), MINOR-10·11(주석, 비차단).

### 처리
- `pipeline/impact-report.json` → **`enforce: false`** (scope-guard.md 규칙 4 "파이프라인 종료 시 `enforce: false`로 내리고 리포트는 보존한다"). **다른 필드는 변경하지 않았다.**
- **파이프라인 종료.**

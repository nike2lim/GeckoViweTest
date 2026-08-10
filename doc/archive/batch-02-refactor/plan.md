# 작업 계획: App.kt에서 AppContainer·AppBridgeHost 분리

- 작성 일시: 2026-08-10 15:05:44
- 재작성 여부: 초안
- 근거: `pipeline/impact-report.json` (task: "App.kt에 뭉쳐 있는 AppContainer·AppBridgeHost를 별도 파일로 분리한다. Application 수명 스코프는 유지(GeckoRuntime의 프로세스당 1회 제약)하며 바꾸는 것은 파일 위치와 패키지뿐이다.")
- 선행 핸드오프: `pipeline/handoff/1-impact-analyzer.md`

---

## 0. 이 계획이 재확인한 인계값 (V7)

앞 단계 수치를 옮겨 적지 않고 전부 이 프로젝트에서 직접 재측정했다. 측정 절차는 §9에 있다.

| 항목 | 인계값 | 내 재측정 | 결과 |
|:--|:--|:--|:--|
| 베이스라인 LINE | 98/98 = 100.00% | **98/98 = 100.00%** | 일치 |
| 베이스라인 BRANCH | 18/18 = 100.00% | **18/18 = 100.00%** | 일치 |
| 베이스라인 CLASS | 14 | **14/14 = 100.00%** | 일치 |
| `AppBridgeHost` 규모 | 4라인 / 2분기 | **LINE 4 / BRANCH 2 / METHOD 4 / INSTR 19 / CXTY 5** | 일치 (+ 미보고 3개 지표 확보) |
| `AppContainer` 규모 | (미보고) | **LINE 20 / BRANCH 2 / METHOD 7** | 신규 측정 |
| `coverageExclusions` 원문 | 11개 항목 | **11개 항목, `app/build.gradle.kts:118-130`** | 일치 |
| 참조 지점 | `MainActivity.kt:129·201` (import 없음) | **일치. 70·122·125행은 주석** | 일치 |
| 기본 JAVA_HOME | Java 8 | **1.8.0_333 — `./gradlew` 설정 단계 실패** | 일치 |
| 화이트리스트 글롭 동작 | fnmatch `*`가 `/`를 넘는다 | **4개 후보 경로 전부 매칭 확인** | 일치 |
| git 상태 | 저장소임, 커밋 2개, 클린 | **`1870cdf`/`405763d`, 미추적은 `doc/`·`pipeline/`뿐** | 일치 |

베이스라인 수치의 1차 출처는 `doc/archive/batch-01-geckoview/coverage-report.md` §2이지만, **위 값은 인용이 아니라 내가 방금 실행한 `jvmCoverageReport`의 XML에서 읽은 것**이다.

---

## 1. 목표 — 완료 시 관찰 가능한 결과

1. `App.kt`에 최상위 클래스가 **`App` 하나만** 남는다.
2. `app/src/main/java/com/example/geckoviewtest/AppContainer.kt`와 `app/src/main/java/com/example/geckoviewtest/bridge/AppBridgeHost.kt`가 새로 존재한다.
3. `jvmCoverageReport.xml`의 분모에 **`com/example/geckoviewtest/bridge/AppBridgeHost`가 있고**, `AppContainer`는 **없다**.
4. 전체 커버리지가 **LINE 102/102 = 100.00%, BRANCH 20/20 = 100.00%**로, 베이스라인(98/98, 18/18) 대비 **하락 0**.
5. 실기기(SM-G981N)에서 브리지 왕복(`getVersionName`)과 `appFinish`가 이전과 동일하게 동작한다.
6. 후속 과제 **F-2가 닫힌다** — `AppBridgeHost`는 더 이상 "파일 위치 때문에" 커버리지 분모 밖에 있지 않다.

**스코프(Application 수명)는 바뀌지 않는다.** `GeckoRuntime`의 프로세스당 1회 제약 때문이며, 사용자가 이미 동의한 확정 사항이다. 이 계획은 파일 위치와 패키지만 바꾼다.

---

## 2. 확정 결정 — 어디로 옮기는가

리드가 요구한 ①의 답이다. 선택지를 나열하지 않고 하나씩 확정한다.

### D-01. `AppBridgeHost` → `com.example.geckoviewtest.bridge` (`bridge/AppBridgeHost.kt`)

**근거**

1. **구현체를 자기 포트 옆에 둔다.** `AppBridgeHost`가 구현하는 `interface BridgeHost`는 `bridge/BridgeDispatcher.kt:59`에 선언돼 있다. 이 클래스는 안드로이드 타입을 하나도 참조하지 않고(실측: `Application`·`Context`·`PackageManager` 등 0건), 의존 대상이 `BridgeHost` 하나뿐이다. 루트 패키지에 남길 이유가 **하나도 없다** — 루트 패키지의 나머지(`App`, `MainActivity`, `MainViewModel`, `MainUiState`)는 전부 안드로이드 진입점 계열이고 이 클래스만 이질적이다.
2. **사용자 요청의 문언에 직접 대응한다.** "전역으로 설정해 놓으니 낭비되는 것 같다"의 핵심은 *앱 전역 물건이 아닌 것이 앱 전역 자리에 있다*는 위화감이다. 수명은 못 바꾸지만(D-00), **소속은 바꿀 수 있고 그것이 위화감의 실체다.**
3. **실패 모드가 안전한 쪽이다.** 하위 패키지로 옮기면 `coverageExclusions`의 `com/example/geckoviewtest/AppBridgeHost*.class` 패턴이 더 이상 맞지 않아(R-01, Ant PatternSet의 `*`는 `/`를 넘지 않음) **제외 줄을 지우는 것을 잊어도 분모에 들어온다.** 같은 패키지에 두면 반대다 — 줄 삭제를 잊는 순간 F-2가 조용히 미해결로 남고 아무도 눈치채지 못한다. **의도한 결과가 실수에도 살아남는 배치를 고른다.**

**이름은 `AppBridgeHost` 그대로 둔다.** `bridge` 패키지에서 `App` 접두사가 어색해 보일 수 있으나 (a) 이 클래스가 "앱 스코프 구현체"라는 사실은 이름이 담아야 할 정보이고, (b) 개명은 화이트리스트 글롭(`*AppBridgeHost.kt`)을 벗어나며(R-10), (c) `MainActivity` 주석 2곳과 배치 01 문서 전체의 용어가 흔들린다. **개명은 이 배치의 요청이 아니다.**

### D-02. `AppContainer` → 같은 패키지, `AppContainer.kt` (`di/` 하위 패키지 **아님**)

**근거**

1. **클래스 1개짜리 `di/` 패키지는 과잉 구조화다.** 이 앱은 화면 1개·주입 대상 5개이고, `App.kt`의 KDoc이 "Hilt를 쓰지 않은 이유: 손익분기점 아래"라고 이미 밝히고 있다. 프레임워크를 안 쓰기로 한 판단과 프레임워크용 패키지 관례를 도입하는 것은 서로 어긋난다.
2. **소유자 옆에 둔다.** `AppContainer`는 `App`이 `by lazy`로 생성해 소유하는 유일한 인스턴스다(`App.kt:59`). 둘을 같은 패키지에 붙여 두면 `AppContainer(this)`가 import 없이 읽히고, KDoc이 주장하는 "무엇이 무엇에 의존하는가가 그대로 보인다"가 유지된다.
3. **(보조 근거) 조용히 깨지는 표면을 늘리지 않는다.** `di/`로 옮기면 제외 패턴을 `com/example/geckoviewtest/di/AppContainer*.class`로 **반드시** 고쳐야 하고, 잊으면 20라인이 0%로 분모에 들어와 LINE 98/122 = 80.33%로 회귀한다. **얻는 것이 없는 위험이다** — `AppContainer`는 어디에 있든 `Application`·`packageManager`·`Dispatchers.Main.immediate` 의존 때문에 분모 밖이다.
   이것은 결정의 *주된* 이유가 아니라 1·2를 지지하는 확인 사항이다. 아키텍처적 이득이 있었다면 패턴은 고치면 그만이다.

### D-03. 두 선택이 서로 달라도 되는가 — **된다. 다른 것이 옳다.**

판단 기준은 하나다: **"이 클래스는 무엇에 소속되는가."** 두 클래스는 답이 다르다.

| | `AppBridgeHost` | `AppContainer` |
|:--|:--|:--|
| 의존 대상 | `bridge.BridgeHost` 하나 | `Application`, `bridge/*`, `data/*` 전부 |
| 안드로이드 타입 | **0개** | `Application`·`PackageManager`·`Build` |
| 존재 이유 | 브리지의 호스트 포트 구현 | 앱 전역 객체 조립 |
| 소속 | **`bridge` 하위 도메인** | **앱 루트 (조립 지점)** |

`AppContainer`는 여러 패키지를 가로질러 조립하는 것이 역할이라 어느 하위 패키지에도 속하지 않는다 — 루트가 정확한 자리다. `AppBridgeHost`는 정확히 한 하위 도메인에만 속한다. **같은 규칙을 적용해서 답이 갈린 것이지, 규칙이 두 개인 것이 아니다.**

### D-04. `AppBridgeHost`의 커버리지 제외를 **푼다** (F-2 해소)

`build.gradle.kts`가 스스로 밝힌 제외 기준은 "테스트하기 귀찮아서가 아니라 **JVM 테스트로 실행 자체가 불가능한가**"다. `AppBridgeHost`는 안드로이드 타입 0개의 순수 Kotlin이라 **기준에 미달한다.** 제외 줄을 삭제한다.

### D-05. `AppContainer`의 커버리지 제외는 **유지한다**

`Application`·`app.packageManager`·`Dispatchers.Main.immediate`에 실재 의존하므로 위 기준을 **충족한다.** D-02로 패키지가 그대로이므로 `com/example/geckoviewtest/AppContainer*.class` 패턴은 **문언 수정 없이 계속 맞는다** — 다만 그 사실을 Step 3에서 **관측으로 확인**한다(§4 G3-b). "안 고쳤으니 괜찮다"는 근거가 아니다.

---

## 3. 커버리지 조항 두 개를 동시에 만족시키는 방법 (②)

`architecture.md` §테스트는 두 가지를 **함께** 요구한다.
- (a) 변경된 클래스 라인 커버리지 **70% 이상**
- (b) 전체 커버리지 **베이스라인 대비 하락 금지**

(b)가 이 배치의 실질적 제약이다. 베이스라인이 100.00%라 **하락 금지 = 100.00% 유지**이고, 분모를 넓히는 순간 테스트 없이는 반드시 위반이다.

### 3.1 짝 (R-04) — 제외 해제와 테스트는 같은 배치에서

| 작업 | 단독 시행 시 | 짝으로 시행 시 |
|:--|:--|:--|
| `AppBridgeHost` 제외 해제만 | LINE 98/102 = **96.08%** → (b) 위반, 7단계 FAIL | — |
| `AppBridgeHostTest`만 | 분모 밖이라 수치 불변, F-2 미해결 | — |
| **둘 다** | — | LINE **102/102 = 100.00%** → (a)(b) 동시 충족 |

### 3.2 예상 최종 수치 (실측 기반 산술)

베이스라인 + `AppBridgeHost`(LINE 4 / BRANCH 2 / METHOD 4 / INSTR 19 / CXTY 5, **전부 내가 JaCoCo core 0.8.15로 직접 측정**)를 100% 덮었을 때:

| 지표 | 베이스라인 (실측) | 목표 (예상) | 방향 |
|:--|:--|:--|:--|
| **LINE** | 98/98 = 100.00% | **102/102 = 100.00%** | 동일 |
| **BRANCH** | 18/18 = 100.00% | **20/20 = 100.00%** | 동일 |
| **CLASS** | 14/14 = 100.00% | **15/15 = 100.00%** | 동일 |
| INSTRUCTION | 607/636 = 95.44% | 626/655 = **95.57%** | 상승 |
| METHOD | 45/55 = 81.82% | 49/59 = **83.05%** | 상승 |
| COMPLEXITY | 54/64 = 84.38% | 59/69 = **85.51%** | 상승 |

**하락하는 지표가 하나도 없다.** 상승 3건은 전부 "분자·분모가 같은 값만큼 늘어난" 결과이므로 분모 조작이 아니며, 출처는 `AppBridgeHostTest` 단 하나로 특정된다.

> `CLASS` 총계가 14인 이유(XML의 `<class>` 원소는 28개): 코드가 없는 인터페이스·`Companion`·`$$serializer` 14개는 CLASS 카운터에 기여하지 않는다. **직접 확인함.** `AppBridgeHost`는 실코드가 있으므로 +1 되어 15가 된다. `bridge/BridgeHost`(인터페이스)는 지금도 0 기여이고 변화 없다.

### 3.3 (a) 조항 — 변경 클래스별 판정

| 클래스 | 이 배치의 변경 | 분모 | 라인 커버리지 목표 |
|:--|:--|:--|:--|
| `AppBridgeHost` | 패키지 이동 | **안 (신규 진입)** | **4/4 = 100%** ≥ 70% ✅ |
| `AppContainer` | 파일 분리(패키지 불변) | 밖 (구조적 예외, D-05) | 해당 없음 — 사유는 D-05 |
| `App` | 클래스 2개 제거 + import 정리 | 밖 (기존 예외) | 해당 없음 |
| `MainActivity` | **주석만** | 밖 (기존 예외) | 해당 없음 |

### 3.4 중간 상태 경고 — 4단계와 6단계 사이에는 96.08%가 나온다 (**중요**)

Step 1이 끝난 시점부터 Step 4가 끝나기 전까지 커버리지는 **LINE 98/102 = 96.08%, BRANCH 18/20 = 90.00%, CLASS 14/15 = 93.33%**다. 이것은 **계획된 중간 상태이지 회귀가 아니다.**

- 이 상태를 없애기 위해 **Step 4(테스트)를 developer가 4단계 안에서 함께 수행한다**(§6 E-3에서 evaluator 판정 요청).
- 그럼에도 5단계 code-reviewer가 중간 커밋에서 96.08%를 관측할 수 있다. **이 수치 하나만으로 CHANGES_REQUESTED를 내지 말 것** — 판정 기준은 4단계 종료 시점의 수치다.
- **최종 판정은 7단계에서만 한다.**

---

## 4. 작업 순서

각 Step의 검증 게이트는 **AND로 결합되고 각 조건이 독립적으로 깨질 수 있어야** 한다(V1). "빌드 성공"은 이 배치에서 대리 신호이므로(R-06: `MainActivity.kt:129·201`은 타입 추론이라 import가 없어 패키지를 바꿔도 손대지 않고 컴파일된다) **어느 Step에서도 단독 근거로 쓰지 않는다.**

### 공통 실행 규약

```bash
export JAVA_HOME=/Users/appdevloperteam/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home
# 커버리지 측정은 반드시:
./gradlew clean jvmCoverageReport --rerun-tasks
```

`clean`은 선택이 아니다 — §5 R-P3 참조.

커버리지 분모 확인은 **리포트 HTML을 눈으로 보지 말고** 다음으로 한다(기계 판정):

```bash
python3 - <<'EOF'
import xml.etree.ElementTree as ET
r = ET.parse('app/build/reports/jacoco/jvmCoverageReport/jvmCoverageReport.xml').getroot()
names = sorted(c.get('name') for p in r.findall('package') for c in p.findall('class'))
print('총 클래스 원소:', len(names))
for n in names:
    if 'App' in n: print('  [App계열]', n)
print({c.get('type'): (c.get('covered'), c.get('missed')) for c in r.findall('counter')})
EOF
```

---

### Step 1 — `AppBridgeHost`를 `bridge` 패키지로 옮긴다

- **대상 파일**
  - 신규: `app/src/main/java/com/example/geckoviewtest/bridge/AppBridgeHost.kt`
  - 수정: `app/src/main/java/com/example/geckoviewtest/App.kt`
- **변경 내용**
  - `App.kt:151-167`의 `AppBridgeHost` 클래스를 **KDoc 포함 그대로** 새 파일로 옮긴다. 본문 로직은 **한 글자도 바꾸지 않는다.**
  - 새 파일 선두: `package com.example.geckoviewtest.bridge`. `BridgeHost`가 같은 패키지가 되므로 **import 불필요**하고, `App.kt`에서 `import com.example.geckoviewtest.bridge.BridgeHost`는 **삭제**된다.
  - 새 파일 KDoc에 **한 문장을 덧붙인다**: 왜 `bridge` 패키지에 있으면서 수명은 앱 스코프인지(D-01 근거 3줄 이내). `comment-style.md` 규칙 4(non-obvious한 WHY)에 해당한다.
  - `App.kt`(=`AppContainer`가 아직 여기 있음)에 `import com.example.geckoviewtest.bridge.AppBridgeHost`를 **추가**한다.
- **검증 게이트 (전부 만족해야 통과)**
  - **G1-a** `./gradlew clean assembleDebug` 성공 — *필요조건일 뿐 충분조건 아님(V1)*
  - **G1-b (독립)** `git diff --stat`이 `App.kt`만 수정으로, `git status`가 `bridge/AppBridgeHost.kt`를 신규로 보고한다. **`git diff app/src/main/java/com/example/geckoviewtest/App.kt`에서 삭제된 줄 = 이동한 클래스 본문 + `BridgeHost` import뿐임을 육안 대조.** 로직 변경이 섞이면 여기서 드러난다.
  - **G1-c (독립)** `grep -c '^class ' App.kt` → **2** (`App`, `AppContainer`). 1이나 3이면 실패.
  - **G1-d (독립 · R-01 현장 확인)** 커버리지를 측정하면 분모에 **`com/example/geckoviewtest/bridge/AppBridgeHost`가 나타나고 LINE 0/4**, 전체가 **98/102 = 96.08%**로 떨어진다.
    → 이것이 나타나지 **않으면** Ant 패턴이 `/`를 넘었다는 뜻이고 §2 D-01의 전제(R-01)가 이 프로젝트에서 거짓이다. **그 경우 즉시 중단하고 planner에게 반환한다**(§7 행동 규약).
    → 96.08%는 **이 Step에서는 기대값**이다. 회귀로 보고하지 말 것.

### Step 2 — `AppContainer`를 같은 패키지의 별도 파일로 분리한다

- **대상 파일**
  - 신규: `app/src/main/java/com/example/geckoviewtest/AppContainer.kt`
  - 수정: `app/src/main/java/com/example/geckoviewtest/App.kt`
- **변경 내용**
  - `App.kt:93-149`의 `AppContainer` 클래스를 **KDoc 포함 그대로** 새 파일로 옮긴다. 본문 로직 변경 **0건.**
  - 새 파일 선두: `package com.example.geckoviewtest` (**변경 없음 — 이것이 D-02다**).
  - 새 파일이 필요로 하는 import를 옮긴다: `android.app.Application`, `android.content.pm.PackageManager`, `android.os.Build`, `bridge.AppBridgeHost`, `bridge.BridgeDispatcher`, `bridge.NativeBridgeHandler`, `data.AppInfoRepository`, `data.AppInfoRepositoryImpl`, `kotlinx.coroutines.CoroutineScope`, `kotlinx.coroutines.Dispatchers`, `kotlinx.coroutines.SupervisorJob`.
  - `App.kt`에서 위 import들을 **삭제**한다. `App.kt`에 남아야 하는 것: `Application`, `bridge.BridgeProtocol`, `gecko.await`, `Deferred`, `async`, `GeckoRuntime`, `GeckoRuntimeSettings`, `WebExtension`. (`Dispatchers`는 `App`이 쓰지 않으므로 삭제된다.)
  - `App.kt:59`의 `AppContainer(this)`는 같은 패키지라 **import가 필요 없다** — 그대로 둔다.
- **검증 게이트**
  - **G2-a** `./gradlew clean assembleDebug` 성공 + **컴파일 경고에 unused import 0건**
  - **G2-b (독립)** `grep -c '^class ' App.kt` → **1**. `App.kt` 총 라인 수가 90줄 내외로 줄어든다(현재 169줄).
  - **G2-c (독립)** 커버리지 분모에 **`AppContainer`가 여전히 없다.** 위 파이썬 스니펫의 `[App계열]` 출력이 `bridge/AppBridgeHost` **한 줄만**이어야 한다.
    → `com/example/geckoviewtest/AppContainer`가 출력되면 **패키지가 의도치 않게 바뀌었거나 제외 패턴이 깨진 것**이다. 전체가 98/122 = 80.33%로 떨어져 즉시 드러난다.
  - **G2-d (독립)** 전체 수치가 Step 1과 **바이트 단위로 동일**(98/102, 18/20, 14/15). `AppContainer` 분리는 수치를 움직일 이유가 없다 — 움직이면 무언가 잘못됐다.

### Step 3 — `coverageExclusions`를 의도에 맞게 고친다

- **대상 파일**: `app/build.gradle.kts`
- **변경 내용**
  - `app/build.gradle.kts:122`의 `"com/example/geckoviewtest/AppBridgeHost*.class"` **한 줄을 삭제**한다. (Step 1 이후 이 줄은 아무것도 매칭하지 않는 죽은 패턴이다 — 빌드 스크립트가 사실과 다른 말을 하는 상태를 남기지 않는다.)
  - `app/build.gradle.kts:121`의 `"com/example/geckoviewtest/AppContainer*.class"`는 **그대로 둔다** (D-05).
  - 목록 위 주석 블록(109-117행)에 **2~3줄을 추가**한다: `AppBridgeHost`는 안드로이드 타입 0개라 제외 기준에 미달해 분모에 넣었고(F-2 해소), `AppContainer`는 `Application`·`packageManager`·`Dispatchers.Main.immediate` 의존으로 기준을 충족해 남긴다는 것. **다음 사람이 "왜 형제 클래스인데 하나만 제외인가"를 묻지 않게 하는 것이 목적이다.**
  - **`inputs.dir` 추가 금지** — F-1은 이 배치 범위 밖이다(§8).
- **검증 게이트**
  - **G3-a (독립)** `git diff app/build.gradle.kts`가 **삭제 1줄 + 주석 추가만**을 보여준다. 다른 줄(특히 다른 제외 패턴, `dependencies`, `ndk.abiFilters`)이 변경되면 실패.
  - **G3-b (독립)** 커버리지 수치가 Step 2와 **완전히 동일**(98/102 = 96.08%). 죽은 패턴을 지운 것이므로 수치가 움직이면 안 된다. **움직였다면 그 줄이 죽지 않았다는 뜻**이고 §2 D-01의 전제가 틀린 것이다.
  - **G3-c (독립)** 제외 목록 항목 수가 11 → **10**.

### Step 4 — `AppBridgeHostTest`를 추가해 짝을 맞춘다 (R-04)

- **대상 파일**: `app/src/test/java/com/example/geckoviewtest/bridge/AppBridgeHostTest.kt` (신규)
- **담당**: developer (4단계). qa(6단계)가 인수·보강한다. → §6 E-3
- **변경 내용 — 계약 수준**
  - 순수 JUnit. `coroutines-test`·Turbine·Robolectric **불필요**(안드로이드 타입 0개). **새 라이브러리 추가 없음.**
  - 최소 2개 케이스로 4라인/2분기를 전부 덮는다:
    1. **콜백이 등록된 경우** — `onFinishRequested`에 람다를 넣고 `requestFinish()` 호출 → 람다가 **정확히 1회** 실행된다.
    2. **콜백이 없는 경우** — 등록하지 않은(또는 `null`로 되돌린) 상태에서 `requestFinish()` 호출 → **예외 없이 반환하고 아무 일도 일어나지 않는다.** 이것이 `?.`의 두 번째 분기다.
  - `?.`의 null 분기를 덮는 케이스가 이 스위트의 **존재 이유**다. `MainActivity.onDestroy`가 등록을 `null`로 되돌리므로(`MainActivity.kt:201`) **실제로 도달하는 경로**이며, 여기서 NPE가 나면 화면이 사라진 뒤 브리지 호출이 앱을 죽인다.
  - KDoc은 `comment-style.md` T1을 따른다 — **무엇을 보장하고 무엇은 보장하지 않는가.** 미보장 범위를 반드시 적는다: *"이 스위트는 `requestFinish`가 콜백을 부르는 것까지만 보장한다. 그 콜백이 실제로 Activity를 종료시키는지는 `MainActivity`의 배선이고 실기기 게이트(Step 6)의 몫이다."*
  - T5(과잉 금지)를 지킨다 — 케이스가 2개뿐이므로 **클래스 KDoc 1개 + 케이스 2에 "왜 이 케이스가 필요한가" 한 줄이면 충분**하다.
- **검증 게이트**
  - **G4-a (독립 · V2)** 새 테스트를 **의도적으로 깨뜨려 RED를 확인**하고 로그에 남긴다. 예: `requestFinish()`의 호출을 잠시 주석 처리 → 케이스 1이 FAIL. **원복도 함께 기록한다.** 이 기록이 없으면 "항상 초록인 장식"과 구분할 수 없다.
  - **G4-b (독립)** 커버리지 XML에서 `com/example/geckoviewtest/bridge/AppBridgeHost`가 **LINE 4/4, BRANCH 2/2, METHOD 4/4**. 셋 중 하나라도 미달이면 실패.
  - **G4-c (독립)** 전체 **LINE 102/102 = 100.00%, BRANCH 20/20 = 100.00%, CLASS 15/15**. §3.2 표와 대조.
  - **G4-d (독립)** 테스트 총 건수 48 → **50**. 기존 48건 전부 통과, skip·`@Ignore` **0건**.

### Step 5 — `MainActivity` 주석을 새 위치에 맞춘다

- **대상 파일**: `app/src/main/java/com/example/geckoviewtest/MainActivity.kt` (**주석만**)
- **변경 내용**
  - `MainActivity.kt:122`의 흐름 도식 `... → BridgeDispatcher → AppBridgeHost`와 `:125`의 "`AppBridgeHost`는 앱 스코프라 Activity보다 오래 산다"는 **여전히 참이지만**, 클래스가 다른 패키지로 갔으므로 독자가 찾지 못한다. `AppBridgeHost`를 `bridge.AppBridgeHost`로 명시하거나 KDoc 링크(`[com.example.geckoviewtest.bridge.AppBridgeHost]`)로 바꾼다.
  - `MainActivity.kt:70`의 "`AppContainer`가 아니라 여기서 직접 넘긴다"는 `AppContainer`가 같은 패키지에 그대로 있으므로 **수정하지 않는다.**
  - **코드는 한 줄도 바꾸지 않는다.** `:129`·`:201`의 `container.bridgeHost` 참조는 타입 추론이라 그대로 동작한다(R-06) — **고칠 것이 없다는 사실 자체를 확인하는 것**이 이 Step의 절반이다.
- **검증 게이트**
  - **G5-a (독립)** `git diff app/src/main/java/com/example/geckoviewtest/MainActivity.kt`의 변경 줄이 **전부 주석/KDoc**이다. `+`/`-` 중 하나라도 실행 코드면 실패.
  - **G5-b (독립)** 커버리지 수치가 Step 4와 **완전히 동일**. 주석만 바꿨는데 수치가 움직이면 코드가 섞인 것이다(android-planning §"주석만 바꾸는 배치": **상승도 이상 신호**).
  - **G5-c (독립)** 테스트 건수 50 유지.

### Step 6 — 실기기 검증: 리팩터링이 배선을 끊지 않았음을 보인다

- **환경 (V3 — 신호 발생 가능성 먼저 확인함)**: `adb devices`로 **SM-G981N(R3CN60L0QMT) 연결 확인 완료.** `abiFilters = arm64-v8a`와 일치하고 배치 01이 검증한 것과 같은 기기다. 이 검증은 **실기기 전용**이다 — GeckoView 네이티브 엔진이 필요해 JVM 테스트로는 원리적으로 관측 불가능하다. **검증 전 화면을 켠다**(도즈 상태면 Activity가 뜨지 못해 앱 결함이 아닌 실패가 난다).
- **절차**: `./gradlew installDebug` → 앱 실행 → 내장 확장 페이지(`assets/messaging/index.html`)에서 버튼 3개를 순서대로 누른다.
- **검증 게이트 — 모두 만족해야 통과**
  - **G6-a (독립 · JS 생존)** 페이지의 `브리지 상태:` 값이 초기 문자열 **`(JS 미실행)`이 아닌 값**으로 바뀌어 있다. 그대로면 JS가 죽은 것이고 나머지 관측이 전부 무의미해진다.
  - **G6-b (독립 · 브리지 왕복 성공 경로)** `getVersionName 호출` → `#result`에 **`1.0.0`이 표시된다**. 이것이 page JS → background.js → `NativeBridgeHandler` → `BridgeDispatcher` → `AppInfoRepository` 왕복 전체의 증거다. "응답이 왔다"가 아니라 **값이 맞는지**로 판정한다(V1).
  - **G6-c (독립 · 오류 경로)** `없는 함수 호출` → `UNKNOWN_FUNCTION` 계열 오류가 표시되고 **앱이 죽지 않는다**. 성공 경로만 보면 오류 경로가 크래시로 바뀐 회귀를 놓친다.
  - **G6-d (독립 · 이 배치의 핵심 · `AppBridgeHost` 경로)** `appFinish 호출` → **앱이 실제로 종료된다.** 판정은 화면이 아니라 `adb shell dumpsys activity activities | grep -c com.example.geckoviewtest`가 **0**이 되는 것으로 한다(배치 01이 쓴 것과 같은 기준). 이 경로가 유일하게 `AppBridgeHost.requestFinish()`를 통과하므로, **이동 중 `onFinishRequested` 배선이 끊겼다면 여기서만 드러난다.**
  - **G6-e (독립 · 늦게 오는 오류 · V1)** G6-b 직후 **최소 5초** `adb logcat`을 계속 지켜보고 `AndroidRuntime`/`GeckoSession` 오류가 **없음**을 확인한다. 성공 판정 직후 끊으면 뒤늦은 예외를 놓친다.
- **비결정성 (V5)**: G6-b·G6-d는 **연속 3회** 수행하고 **"3회 중 N회"** 형태로 보고한다. 1회라도 실패하면 통과가 아니다 — "재현이 안 된다"를 결함 부재로 해석하지 않는다.
- **관측 실패 시 남길 진단**: `#result`의 마지막 문자열 원문, `adb logcat -d | tail -200`, `dumpsys` grep 결과, 경과 시간.

---

## 5. 리스크 지점

| # | 리스크 | 발생 조건 | 완화 / 롤백 |
|:--|:--|:--|:--|
| **R-P1** | "빌드 성공"을 "참조를 다 고쳤다"의 근거로 오용 | `MainActivity.kt:129·201`이 타입 추론이라 패키지가 바뀌어도 import 없이 컴파일된다(R-06) | 모든 Step에서 빌드 성공을 **G*-a(필요조건)로만** 쓰고, 판정은 `git diff` 대조와 커버리지 분모 관측으로 한다. code-reviewer도 diff로 대조(R-09로 가능해짐) |
| **R-P2** | 제외 패턴이 **조용히** 깨져 게이트 수치가 의도와 무관하게 정해진다 | Ant PatternSet의 `*`가 `/`를 넘지 않음(R-01, impact-analyzer가 스크래치패드에서 실측) | 이 계획은 **패턴에 의존하지 않고 결과를 관측한다.** G1-d·G2-c·G3-b가 각각 "무엇이 분모에 있고 없는지"를 XML에서 직접 읽는다. 패턴이 예상과 다르게 동작하면 세 게이트 중 하나가 반드시 깨진다 |
| **R-P3** | **낡은 클래스 파일이 분모를 오염시킨다** | `AppBridgeHost.class`가 `tmp/kotlin-classes/debug/com/example/geckoviewtest/`(옛 위치)에 남은 채 `bridge/`에도 생기면, 옛 위치 것은 제외되고 새 위치 것은 포함돼 **둘 다 집계되거나 수치가 오락가락한다.** `--rerun-tasks`는 낡은 산출물을 지우지 않는다 | **모든 커버리지 측정에 `clean`을 붙인다** (`./gradlew clean jvmCoverageReport --rerun-tasks`). 배치 01의 coverage-reporter도 `clean`을 썼다. G1-d의 "총 클래스 원소 수"가 예상 밖으로 늘면 이 오염을 의심한다 |
| **R-P4** | `testDebugUnitTest`가 캐시돼 테스트가 조용히 안 돈다 | F-1(assets `inputs.dir` 미선언)이 남아 있고 태스크 자체가 캐시된다(R-08) | `--rerun-tasks` 필수. 로그에서 `:app:testDebugUnitTest`가 **UP-TO-DATE가 아님**을 확인한 뒤에만 수치를 인용한다 |
| **R-P5** | 5단계 code-reviewer가 중간 상태(96.08%)를 회귀로 오판해 반려 | Step 4가 4단계 안에서 완료되지 않거나, 중간 커밋을 대상으로 리뷰 | §3.4를 handoff에 명시. Step 4를 developer가 4단계 안에서 수행(E-3). **커버리지 최종 판정은 7단계에서만** |
| **R-P6** | 이동 중 로직이 섞여 들어간다 | KDoc을 다듬다가 `@Volatile` 누락, `?.`를 `!!`로 바꾸는 등 | Step 1·2는 **본문 무변경**이 계약이다. G1-b·G2-a의 diff 육안 대조로 확인. `@Volatile` 제거는 컴파일도 테스트도 통과하므로 **diff로만 잡힌다** |
| **R-P7** | `AppBridgeHostTest`가 null 분기를 빠뜨려 BRANCH 19/20이 된다 | 케이스 1만 작성 | G4-b가 `BRANCH 2/2`를 명시적으로 요구. 1/2면 즉시 실패 |
| **R-P8** | 롤백 필요 시 되돌릴 수 없다 | — | **해소됨.** 이 프로젝트는 git 저장소이고 작업 트리가 클린하다(직접 확인: 커밋 `1870cdf`, 미추적은 `doc/`·`pipeline/`뿐). `git checkout -- .`로 완전 원복 가능하며, Step 단위 커밋을 권장한다 |

---

## 6. Evaluator(3단계)에게 요청하는 판정 항목

| # | 항목 | 요청 |
|:--|:--|:--|
| **E-1** | **D-01** — `AppBridgeHost`를 `bridge` 하위 패키지로 옮기는 것 | `architecture.md`의 레이어 규칙(UI→ViewModel→Repository→DataSource)에 `bridge`는 명시적으로 없다. 순수 Kotlin 포트 구현체를 포트 옆에 두는 것이 원칙 위배가 아닌지 판정 바람 |
| **E-2** | **D-02** — `AppContainer`를 루트 패키지에 유지하는 것 | 사용자 요청이 "다른 곳으로 옮겨라"인데 **패키지는 그대로**이고 파일만 분리된다. 이것이 요청 충족인지(리드가 확정한 해석 "파일 위치**및** 패키지"에서 "및"이 선택적인지) 판정 바람. REJECTED면 `di/` 이동 + `com/example/geckoviewtest/di/AppContainer*.class` 패턴 수정으로 전환한다 |
| **E-3** | **Step 4의 담당** — developer가 4단계에서 테스트를 쓰는 것 | 파이프라인 역할 분담상 테스트는 qa(6단계)의 몫이나, R-04는 "제외 해제와 테스트는 **같은 배치에서 짝**"을 요구하고 §3.4의 중간 상태를 없애려면 4단계여야 한다. 화이트리스트에는 `app/src/test/**`가 있어 물리적으로 가능하다. **역할 위반인지 판정 바람.** REJECTED면 Step 4를 6단계로 옮기고 §3.4 경고를 강화한다 |
| **E-4** | **`AppContainer` 구조적 예외 유지** (D-05) | 커버리지 분모 밖 유지가 "제외 범위 확대"가 아닌지 확인. 근거는 `build.gradle.kts`가 스스로 밝힌 기준("JVM 실행 자체가 불가능한가")이며, `Application`·`packageManager`·`Dispatchers.Main.immediate` 의존을 소스에서 확인했다 |
| **E-5** | **주석 갱신 범위** (Step 5) | `MainActivity` 주석만 고치고 코드는 안 고치는 것이 `comment-style.md`의 "코드와 어긋난 주석" 방지에 충분한지 |

---

## 7. 계획대로 했는데 목표가 달성되지 않을 때의 행동 규약 (V9)

- **G1-d가 깨지면**(= `bridge/AppBridgeHost`가 분모에 안 나타남): §2 D-01의 전제인 R-01이 이 프로젝트에서 거짓이다. **범위를 넓혀 원인을 좇지 말고** Step 1에서 중단하고 planner에게 반환한다. Step 3에서 제외 줄을 지우면 되므로 회복은 쉽지만, **전제가 틀렸다는 사실 자체를 기록해야** 한다.
- **G2-c가 깨지면**(= `AppContainer`가 분모에 들어옴): 패키지가 의도치 않게 바뀌었거나 낡은 클래스 파일 오염(R-P3)이다. `clean` 후 재측정 → 그래도 나오면 Step 2를 되돌리고 반환.
- **G4-c가 100.00%에 못 미치면**: 테스트를 늘려 억지로 맞추기 전에 **어느 라인이 미커버인지 XML에서 특정**하고 로그에 적는다. `AppBridgeHost` 4라인 밖의 수치가 움직였다면 이 배치가 만든 문제가 아니므로 넓히지 말고 보고한다.
- **G6-d가 실패하면**(= `appFinish`로 앱이 안 죽음): 리팩터링이 `onFinishRequested` 배선을 끊었다는 뜻으로, **이 배치의 유일한 진짜 회귀 시나리오**다. `git diff`로 Step 1·2의 본문 무변경 계약을 먼저 대조한다(`@Volatile` 누락, `?.` 변경 등).
- **어느 경우든 FAIL 반환 시 남길 것**: 깨진 게이트 번호, 관측된 원시 수치, `git diff --stat`, `logcat` 마지막 200줄.

---

## 8. 계획에서 제외한 것 (스코프 아웃)

| 항목 | 이유 |
|:--|:--|
| **F-1 — `testDebugUnitTest`에 `inputs.dir("src/main/assets/messaging")` 선언** | `build.gradle.kts`가 화이트리스트에 있어 **물리적으로는 고칠 수 있으나 이번 배치 범위 밖**(V9, 리드 지시 ⑤). 이 배치는 assets를 건드리지 않아 함정이 발현하지 않는다. `--rerun-tasks`로 회피한다. **후속 배치로 이월.** |
| **스코프 변경 (Application → Activity)** | `GeckoRuntime`이 프로세스당 1회 제약. 사용자가 이미 동의한 확정 사항이며 **재론 금지**. |
| **`AppBridgeHost` 개명** | 화이트리스트 글롭(`*AppBridgeHost.kt`)을 벗어나고(R-10), 요청 사항이 아니다. |
| **`interface BridgeHost`를 `BridgeDispatcher.kt`에서 분리** | "한 파일에 여러 클래스"라는 같은 문제이지만 **impact-analyzer가 이동 대상이 아니라고 명시**했다(핸드오프 §참조 지점). 화이트리스트에도 없다. 넓히지 않는다. **후속 배치 후보로 기록.** |
| **`gradle/libs.versions.toml`·새 라이브러리 추가** | **필요 없다.** Step 4의 테스트는 이미 있는 JUnit만 쓴다(안드로이드 타입 0개라 Robolectric·coroutines-test 불필요). 승인 요청 항목 **없음**. |
| **F-3 (AC-003-2 역주입 테스트)** | 만료 조건인 `NativeBridgeHandler` 수정이 이 배치에 없다. **이월.** |
| **F-4 (`coverage-baseline.json` 신설)** | 화이트리스트 밖(`pipeline/`은 항상 허용이나 impact-report에 작업으로 정의되지 않음). 이 배치는 베이스라인을 매번 재측정하는 방식을 유지한다. **이월.** |
| **`AndroidManifest.xml`** | `android:name=".App"`이 가리키는 `App`은 같은 패키지·같은 이름으로 남는다. **수정 불필요**(R-11). |

---

## 9. 빌드 환경 — JAVA_HOME 확정

리드가 지적한 불일치(impact-analyzer의 JBR 17 vs 배치 01의 Studio JDK 21)를 **실제 빌드 2회로 해소했다.**

| JAVA_HOME | 버전 | `clean jvmCoverageReport --rerun-tasks` | 결과 수치 |
|:--|:--|:--|:--|
| (기본) | **1.8.0_333** | **설정 단계 실패** | — |
| `/Users/appdevloperteam/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home` | **17.0.14** | **BUILD SUCCESSFUL in 17s** (30 tasks executed) | LINE 98/98, BRANCH 18/18, CLASS 14/14 |
| `/Applications/Android Studio.app/Contents/jbr/Contents/Home` | **21.0.10** | **BUILD SUCCESSFUL in 22s** (29 tasks executed) | LINE 98/98, BRANCH 18/18, CLASS 14/14 |

**결론: 둘 다 동작하고 산출 수치가 동일하다. 불일치는 차단 요인이 아니었다.**

### 확정: `JAVA_HOME=/Users/appdevloperteam/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home`

1. **경로에 공백이 없다.** Studio 경로는 `Android Studio.app`에 공백이 있어 따옴표를 빠뜨리면 조용히 다른 JVM으로 떨어진다 — 스크립트 붙여넣기에서 실제로 잘 발생하는 사고다.
2. `build.gradle.kts`가 선언한 `sourceCompatibility = VERSION_17` / `jvmTarget = JVM_17`과 **버전이 일치**한다.
3. 베이스라인을 **바이트 단위로 동일하게 재현**함을 위 표에서 확인했다.

> `gradle.properties`에 `org.gradle.java.home`을 넣지 않는 관례(머신 고유 절대경로)는 그대로 유지한다. **환경변수로만 지정한다.**

---

## 10. 화이트리스트 대조 (직접 실행 확인)

`pipeline/impact-report.json`의 `allowed_files`/`allowed_globs`와 이 계획의 **모든 파일 경로**를 대조했다. 대조는 눈으로 보지 않고 **scope-guard hook이 실제로 쓰는 `fnmatch`로 실행**했다(`guard-impact-scope.py`가 `os.path.relpath` + `fnmatch.fnmatch`를 쓰는 것을 소스에서 확인).

| Step | 경로 | 근거 | 판정 |
|:--|:--|:--|:--|
| 1 | `app/src/main/java/com/example/geckoviewtest/bridge/AppBridgeHost.kt` | glob `.../geckoviewtest/*AppBridgeHost.kt` (fnmatch `*`가 `/`를 넘음 — **실행 확인**) | **통과** |
| 1,2 | `app/src/main/java/com/example/geckoviewtest/App.kt` | `allowed_files` | **통과** |
| 2 | `app/src/main/java/com/example/geckoviewtest/AppContainer.kt` | glob `.../geckoviewtest/*AppContainer.kt` | **통과** |
| 3 | `app/build.gradle.kts` | `allowed_files` | **통과** |
| 4 | `app/src/test/java/com/example/geckoviewtest/bridge/AppBridgeHostTest.kt` | glob `app/src/test/**` | **통과** |
| 5 | `app/src/main/java/com/example/geckoviewtest/MainActivity.kt` | `allowed_files` | **통과** |
| 1~6 | `pipeline/**`, `doc/**` | hook 상시 허용 경로 | **통과** |

**화이트리스트를 벗어나는 경로 0건. impact-analyzer 재실행 불필요.**

R-10이 경고한 두 가지를 **모두 회피했다**: (a) 파일명 = 클래스명 관례를 지킨다(`AppContainer.kt`, `AppBridgeHost.kt`), (b) 기존 파일 병합을 하지 않는다 — 특히 `bridge/BridgeDispatcher.kt`에 `AppBridgeHost`를 합치지 않는다. 그것은 이 배치가 없애려는 "한 파일에 여러 클래스"를 그대로 재생산하는 일이다.

---

## 11. `architecture.md` 대조

| 원칙 | 이 계획의 해당 여부 |
|:--|:--|
| 레이어 단방향 의존 | **위배 없음.** 의존 방향 불변 — 루트(`App`,`AppContainer`) → `bridge`/`data`. `bridge`는 루트를 참조하지 않는다(Step 1 후에도 `AppBridgeHost`의 의존은 `BridgeHost` 하나) |
| ViewModel에 Context 주입 금지 | 해당 없음 — `MainViewModel` 미수정 |
| 도메인 로직은 프레임워크 비의존 | **개선.** `AppBridgeHost`(순수 Kotlin)가 안드로이드 진입점 패키지에서 도메인 패키지로 이동 |
| 상태 관리 / 비동기 | 해당 없음 — `applicationScope`·`SupervisorJob`·`Dispatchers` 주입 구조 **무변경**. `GlobalScope`·`runBlocking` 미사용 유지 |
| 새 라이브러리는 evaluator 승인 필요 | **추가 없음** (§8) |
| 빌드 설정 변경은 리스크 항목 명시 | `coverageExclusions` 1줄 삭제 — Step 3 및 R-P2에 명시. AGP·Kotlin 버전·minSdk **불변** |
| `!!` 남용 / 빈 catch / 하드코딩 문자열 | 해당 없음 — 본문 무변경 계약(R-P6) |
| 기존 public API 시그니처 변경 | **패키지가 바뀌므로 `AppBridgeHost`의 FQCN이 바뀐다.** 영향 파일(`App.kt`, `AppContainer.kt`, `MainActivity.kt`)이 전부 화이트리스트 안이므로 허용 조건 충족 |
| 신규/변경 로직 단위 테스트 필수 | Step 4 |
| **변경 클래스 70% 이상** | §3.3 — `AppBridgeHost` 100% |
| **전체 커버리지 베이스라인 대비 하락 금지** | §3.1~3.2 — 102/102 = 100.00%, 하락 지표 0건 |

# QA 리포트 (파이프라인 6단계) — 배치 02

- **일시**: 2026-08-11 08:18:18
- **대상**: `AppBridgeHost` → `bridge/` 패키지 이동, `AppContainer` 파일 분리 (리팩터링 배치)
- **입력**: `pipeline/handoff/5-code-reviewer.md` → `4-developer.md` → `3-evaluator.md` → `2-planner.md` → `1-impact-analyzer.md`, `pipeline/review.md`
- **수정 파일**: `app/src/test/java/com/example/geckoviewtest/bridge/AppBridgeHostTest.kt` (**유일**)
- **판정**: **PASS**

---

## 1. 판정 요약

| 항목 | 결과 |
|:--|:--|
| 단위 테스트 | **51건 / 실패 0 / skipped 0** |
| 커버리지 6개 지표 | 베이스라인 대비 **하락 0건** |
| 리팩터링 배선 생존 | 실기기 `appFinish` **3/3** — `ActivityRecord` 완전 소멸 |
| 프로덕션 결함 | **0건** |
| 프로덕션 코드 수정 | **0건** (검증용 일시 변조 4회, 전부 원복·md5 확인) |

---

## 2. 테스트 목록 (51건)

`AppBridgeHostTest` 3건이 이 배치의 대상이고, 나머지 48건은 배치 01에서 이월된 회귀 감시용이다.

### `bridge.AppBridgeHostTest` — 3건 (배치 02 신규 스위트)

| # | 케이스 | 결과 | 비고 |
|:--|:--|:--|:--|
| 1 | `콜백이 등록돼 있으면 requestFinish가 그 콜백을 정확히 1회 호출한다` | PASS | **주석 수정**(MINOR-1) |
| 2 | `콜백이 없으면 requestFinish는 예외 없이 아무 일도 하지 않는다` | PASS | 무변경 |
| 3 | `해제한 뒤 다시 등록하면 requestFinish는 새 콜백을 부른다` | PASS | **6단계 신규 추가** |

### 이월 48건 — 전부 PASS

| 스위트 | 건수 |
|:--|:--|
| `MainViewModelTest` | 12 |
| `bridge.BridgeDispatcherTest` | 7 |
| `bridge.BridgeProtocolTest` | 11 |
| `bridge.BridgeWireContractTest` | 7 |
| `bridge.ExtensionManifestTest` | 8 |
| `data.AppInfoRepositoryTest` | 3 |

48 + 3 = **51**. 4단계 종료 시점 50건에서 **+1**.

---

## 3. MINOR-1 수정 — 직접 실험으로 반증 경로를 확인한 뒤 작성

code-reviewer의 결론을 옮겨 적지 않고 **프로덕션을 변조해 직접 재현**했다(V7).

### 실험 A — 커스텀 getter (기존 주석이 주장하던 경로)

```kotlin
var onFinishRequested: (() -> Unit)? = null
    get() = null            // ← 변조
```

| 케이스 | 결과 |
|:--|:--|
| 1 | **FAIL — `java.lang.AssertionError: expected:<1> but was:<0>` @ `AppBridgeHostTest.kt:32`** |
| 2 | PASS |

**FAIL 지점이 `:32`(`assertEquals`)다. `:36`의 `assertSame`은 도달조차 하지 않았다.**
`callCount`가 0이라는 것은 `requestFinish()`조차 변조된 getter를 타고 `null`을 받았다는 뜻이다 —
Kotlin은 커스텀 접근자가 생기는 순간 클래스 **내부**의 프로퍼티 읽기도 getter를 경유하도록 컴파일한다.
backing field 직접 접근(GETFIELD)은 **기본 접근자일 때만**의 최적화다.

→ **기존 주석 3번째 줄은 거짓이다.** 이 시나리오는 `assertSame`이 없어도 `assertEquals`가 이미 잡는다.

### 실험 B — 감싸는 setter (실제 반증 경로)

```kotlin
var onFinishRequested: (() -> Unit)? = null
    set(value) { field = if (value == null) null else ({ value.invoke() }) }   // ← 변조
```

| 케이스 | 결과 |
|:--|:--|
| 1 | **FAIL — `expected same:<…AppBridgeHostTest$$Lambda$85…> was not:<…AppBridgeHost$$Lambda$82…>` @ `AppBridgeHostTest.kt:38`** |
| 2 · 3 | PASS |

래퍼가 원본을 부르므로 `callCount == 1`이 되어 **`assertEquals`는 초록이고 `assertSame`만 RED**다.
`assertSame`은 반증 가능하므로 **존치**가 옳다(code-reviewer 판정과 독립적으로 재확인).

### 수정 내용 (`AppBridgeHostTest.kt:34-37`)

1~2번째 줄(getter 미실행 사실)은 **참이라 보존**하고, 거짓인 3번째 줄만 실측한 반증 경로로 교체했다.

```kotlin
// 넣은 것이 그대로 조회되는지도 함께 본다. `requestFinish`는 프로퍼티의 backing field를
// 직접 읽으므로 이 단정이 없으면 **getter가 한 번도 실행되지 않는다.**
// 반증 경로는 setter다 — 값을 감싸거나 정규화하면(`field = { value.invoke() }`)
// 래퍼가 원본을 부르므로 호출 횟수는 그대로 1이라 위 단정은 초록인데, 이 단정만 RED가 된다.
```

---

## 4. 케이스 추가 판단 — 커버리지가 아니라 "조용히 통과하는 회귀"가 근거다

`102/102`·`METHOD 4/4`는 이미 충족이라 커버리지는 추가 사유가 될 수 없다(T5).
**추가 후 커버리지 6개 지표가 한 자리도 움직이지 않는다**는 사실이 이 케이스가 수치 목적이 아님을 증명한다.

### 실험 C — 현재 스위트가 놓치는 회귀가 실재하는가

프로덕션에 **1회성 가드**를 넣어 전체 스위트를 돌렸다:

```kotlin
private var finished = false
override fun requestFinish() {
    if (finished) return
    finished = true
    onFinishRequested?.invoke()
}
```

**결과: 50건 전부 PASS — BUILD SUCCESSFUL.** 기존 두 케이스는 매번 새 `host`로 시작해
`requestFinish`를 한 번씩만 부르므로 1회성 가드를 원리적으로 감지할 수 없다.

### 이 경로가 현실인가 — 확인함

- `App.kt:49` : `val container: AppContainer by lazy { AppContainer(this) }` → `Application` 스코프
- `AppContainer.kt:58` : `val bridgeHost: AppBridgeHost = AppBridgeHost()` → **프로세스당 1개**
- `MainActivity.kt:129` 등록 / `MainActivity.kt:201` `null` 대입

즉 화면 회전 등으로 `MainActivity`가 재생성될 때마다 **하나의 `AppBridgeHost`가 등록/해제를 되풀이해 겪는다.**
1회성 가드가 들어오면 **회전 뒤에 온 `appFinish`가 조용히 무시**되는데, 기존 스위트는 초록이다.

→ **추가한다.** 근거는 커버리지가 아니라 이 회귀다.

### 단정 하나는 실험으로 기각하고 삭제했다

초안에는 `assertEquals(1, oldCount)`("해제된 옛 콜백이 되살아나지 않는다")도 있었으나,
**독립적으로 RED가 될 수 없음을 실험으로 확인하고 제거**했다(V1 — 각 조건은 독립적으로 깨질 수 있어야 한다).

실험 D — `set(value) { if (field == null && value != null) field = value }` (재등록·해제를 모두 무시):

| 케이스 | 결과 |
|:--|:--|
| 3 | FAIL — `expected:<1> but was:<0>` @ **`:74`(`newCount`)** — `oldCount` 줄은 미도달 |
| 2 | FAIL — `expected:<0> but was:<1>` |
| 1 | PASS |

`oldCount`를 2로 만드는 모든 변조는 동시에 `newCount`를 0으로 만들어 **앞선 단정에서 먼저 걸린다.**
어떤 변조로도 첫 실패 지점이 될 수 없으므로 장식 단정이다 → 삭제.

---

## 5. V2 — RED 확인 (고친/추가한 테스트별)

| # | 대상 | 깨뜨린 방법 | RED 관측 | 원복 |
|:--|:--|:--|:--|:--|
| RED-1 | **신규 케이스 3** | `requestFinish`에 1회성 가드 | **51건 중 케이스 3만 FAIL** — `expected:<1> but was:<0>` @ `:73` | md5 `e6bcc47f…` 일치, `diff` 0줄 |
| RED-2 | **케이스 1의 `assertSame`** (MINOR-1 주석이 서술하는 단정) | 감싸는 setter | **51건 중 케이스 1만 FAIL** — `expected same:… was not:…` @ `:38` | md5 일치, `diff` 0줄 |

두 RED가 **서로 다른 케이스 단 하나씩만** 빨갛게 만든다 — 케이스끼리 서로를 가리지 않는다.
RED-2는 수정한 주석이 서술하는 경로와 **정확히 일치**하므로, 주석은 실측 기반이다.

**프로덕션 일시 변조 총 4회(A·B·C·D 및 RED 재현) — 전부 원복 확인.**
`AppBridgeHost.kt`는 **git 미추적 파일**이라 `git checkout --`로 되돌릴 수 없어(§8 참조)
바이트 단위 백업본과 `md5`/`diff`로 검증했다. 최종 md5 `e6bcc47f9e415da11358b252ddc573da` — 착수 시와 동일.

---

## 6. 커버리지 재측정 (`clean` 부착 · V7)

```
JAVA_HOME=…/jbr-17.0.14/… ./gradlew clean jvmCoverageReport --rerun-tasks
→ BUILD SUCCESSFUL / 30 actionable tasks: 30 executed   (UP-TO-DATE 회피 확인)
```

| 지표 | 베이스라인 | developer·reviewer 보고 | **QA 재측정(착수 시점)** | **QA 재측정(최종)** | 방향 |
|:--|:--|:--|:--|:--|:--|
| LINE | 98/98 | 102/102 | **102/102 = 100.00%** | **102/102 = 100.00%** | 유지 |
| BRANCH | 18/18 | 20/20 | **20/20 = 100.00%** | **20/20 = 100.00%** | 유지 |
| CLASS | 14/14 | 15/15 | **15/15 = 100.00%** | **15/15 = 100.00%** | 유지 |
| INSTRUCTION | 607/636 = 95.44% | 626/655 | **626/655 = 95.57%** | **626/655 = 95.57%** | 상승 |
| METHOD | 45/55 = 81.82% | 49/59 | **49/59 = 83.05%** | **49/59 = 83.05%** | 상승 |
| COMPLEXITY | 54/64 = 84.38% | 59/69 | **59/69 = 85.51%** | **59/69 = 85.51%** | 상승 |
| 테스트 | 48 | 50 | **50 / 실패0 / skip0** | **51 / 실패0 / skip0** | +1 |

- **보고값과의 불일치: 없음.** 6개 지표 전부 자릿수까지 일치.
- **6개 지표 하락 0건** — 7단계 게이트 전제 유지.
- 신규 케이스 추가로 **6개 지표가 전혀 변하지 않았다** → 커버리지 목적이 아님의 기계적 증거.

부가 관측 (직접 파싱):
- `<class>` 원소 **29개**. `AppContainer` **분모에 없음**(제외 유지), `bridge/AppBridgeHost` **있음**.
- `AppBridgeHost`: LINE 4/4 · BRANCH 2/2 · METHOD **4/4** · INSTR 19/19 · CXTY 5/5
  메서드별 `<init>` 3/3 · `getOnFinishRequested` **3/3** · `setOnFinishRequested` 4/4 · `requestFinish` 9/9

---

## 7. 리팩터링이 배선을 끊지 않았는가 (③ — 이 배치의 본질)

**"빌드 성공"은 대리 신호라 판정에 쓰지 않았다**(V1, R-06).

### 7-1. 기존 48건이 여전히 같은 것을 보장하는가 — 기계 대조

| 확인 | 방법 | 결과 |
|:--|:--|:--|
| 구 패키지 잔존 참조 | `grep -rn 'geckoviewtest\.AppBridgeHost'` | **0건** |
| 와일드카드 import (조용한 재바인딩 경로) | `grep -rn 'import .*\.\*'` | **0건** |
| `internal` 가시성 (이동으로 깨질 수 있는 유일 축) | `grep -rn '^internal '` | **0건** |
| `FakeBridgeHost` 이름 충돌 | 프로덕션 전수 `grep` | **0건** |
| 48건 개별 결과 | 결과 XML 케이스별 파싱 | **48건 전부 PASS, 이름·소속 스위트 불변** |

`BridgeDispatcherTest:53`의 `FakeBridgeHost`(private class)와 `BridgeWireContractTest:153`의
`FakeBridgeHost`(object 표현식)는 둘 다 **테스트 파일 안에서 선언**되고 같은 패키지의
`BridgeHost`(`BridgeDispatcher.kt:59`)를 구현한다. `AppBridgeHost`를 참조하지 않으므로
패키지 이동의 영향을 받지 않는다 — 이름이 겹치는 프로덕션 심볼도 0건이다.

**결론: 이동한 클래스를 참조하는 테스트는 `AppBridgeHostTest` 하나뿐이고, 나머지 48건에 대해 이동은 의미상 무연산이다.**

### 7-2. 배선의 직접 증거 — 실기기 `appFinish` (아래 §8 G6-d)

---

## 8. 실기기 회귀 재확인 (SM-G981N / R3CN60L0QMT, API 33) — V7 재실행

**착수 시 기기가 잠금 화면이었다**(`mWakefulness=Awake`인데 `화면을 미세요` 표시).
그 상태의 첫 측정은 `dumpsys` 카운트가 **21**로 나와 developer 보고(35)와 어긋났는데,
**앱 결함이 아니라 환경 문제**였다(V3). 잠금 해제 후 재측정하니 **35로 정확히 일치**했다.

| 게이트 | 관측값 | 판정 |
|:--|:--|:--|
| G6-a | `브리지 상태:` = **`READY`** (초기값 `(JS 미실행)` 아님) — 스크린샷 확인 | **PASS** |
| G6-b | `#result` = **`versionName = 1.0.0`** — **3회 중 3회** (라운드마다 force-stop + 재기동해 결과창 초기화 후 관측) | **PASS** |
| G6-c | `#result` = **`오류: UNKNOWN_FUNCTION / 브리지에 없는 함수명이다: thisFunctionDoesNotExist`**, 앱 생존(pid 26537, ActivityRecord 7) | **PASS** |
| G6-d | `appFinish` → **`ActivityRecord` 7 → 0**, developer 방식 `grep -c` **35 → 0** — **3회 중 3회** | **PASS** |
| G6-e | `AndroidRuntime` **0건**, `FATAL` **0건**, `GeckoSession` E/F **0건** (1,864줄 버퍼) | **PASS** |

### G6-d 상세 — 배선 생존의 직접 증거

`appFinish`가 `AppBridgeHost.requestFinish()`를 지나는 **유일한 경로**다.

| 라운드 | ActivityRecord 전 → 후 | `grep -c` 전 → 후 | pid |
|:--|:--|:--|:--|
| 1 | 7 → **0** | 35 → **0** | 26980 |
| 2 | 7 → **0** | 35 → **0** | 27340 |
| 3 | 7 → **0** | 35 → **0** | 27694 |

종료 후 `dumpsys activity activities`에서 `com.example.geckoviewtest` **대소문자 무시 grep 0건** —
`finishing=`/`state=` 잔재조차 없이 **ActivityRecord 자체가 소멸**했다. 포커스는 런처로 복귀
(`topResumedActivity=…launcher/.activities.LauncherActivity`).

**주의 — 프로세스는 살아 있다.** 종료 후에도 pid 27694와 `:gpu_…`/`:tab_…` 자식 프로세스가 남는다.
`finish()`는 Activity를 끝낼 뿐 프로세스를 죽이지 않는 정상 동작이며 **결함이 아니다.**
따라서 종료 판정에 `pidof`를 쓰면 안 된다 — **ActivityRecord 소멸이 정본 신호다.**

### 필터 주의 — 유사 이름 앱 실재 확인

기기에 `kr.co.chunjae.android.geckoviewtestapp`가 **실제로 설치·구동 중**이다(pid 22616 외 4개).
`ActivityRecord{.*geckoviewtest`처럼 느슨하게 잡으면 이 앱이 섞여 들어온다.
전 구간에서 `com\.example\.geckoviewtest/`로 정확히 필터했고, `pm list packages --user 0`도
`-x 'package:com.example.geckoviewtest'` 완전 일치로 조회했다.

### G6-e 상세 — E 레벨 로그의 정체

E/F 레벨 총 41건이나 **앱 코드 기인은 0건**이다:
- `GeckoLibLoad` **20건** — `Load sqlite start/done` 등 GeckoView 자체의 라이브러리 로딩 추적(E 레벨로 찍는다)
- `Web Content` **6건** — `EventDispatcher is only available in the parent process`
  (`GeckoViewStartup.sys.mjs:277`). GeckoView 내부 자식 프로세스 기동 로그이고
  3줄짜리 스택이 콘텐츠 프로세스 2개분 = 6줄이다. developer가 "2건"으로 센 것과 **같은 현상**이다.
- 나머지는 `SecPowerUI`·`WindowManager`·`Bluetooth…` 등 **시스템 로그**

---

## 9. 범위 (V9) — 발견했으나 손대지 않은 것

| 항목 | 처리 |
|:--|:--|
| **MINOR-2** (`MainActivity.kt:122-123` KDoc 링크) | **프로덕션 주석 — 범위 밖.** 미개입, 이월 |
| **MINOR-3** (`AppContainer.kt:15-21` 소유자 맥락) | **프로덕션 주석 — 범위 밖.** 미개입, 이월 |
| **MINOR-4** (`AppBridgeHost.kt` 주석 6줄) | **프로덕션 주석 — 범위 밖.** 기록만 |
| **F-1** (assets `inputs.dir`) | `build.gradle.kts` **미개입** (`git diff --stat` 상 QA 변경 0줄) |
| `interface BridgeHost` 분리 | 읽기만 함. 무변경 |
| `private set` 검토 | 범위 밖. 미개입 |
| **신규**: `AppBridgeHost.kt`가 **git 미추적** | 리드 지시의 "`git checkout --`로 되돌릴 수 있다"가 이 파일엔 **성립하지 않는다**(§10) |

**수정한 파일은 `AppBridgeHostTest.kt` 하나뿐이다.** `git status` 파일 목록 착수 시점과 **불변**.

---

## 10. 다음 단계가 알아야 할 사실 (인계 대상)

1. **`AppBridgeHost.kt`·`AppContainer.kt`·`AppBridgeHostTest.kt`는 git 미추적(`??`)이다.**
   `git checkout -- <파일>`은 `pathspec did not match any file(s) known to git`로 실패한다.
   이 배치에서 프로덕션을 변조할 일이 있으면 **반드시 별도 백업본 + md5**로 원복을 보증해야 한다.
2. **종료 판정에 `pidof`를 쓰지 마라** — `finish()` 후에도 프로세스는 남는다(§8).
3. **`dumpsys` 카운트는 화면 잠금 상태에서 21로 떨어진다.** 35와 어긋나면 앱이 아니라 잠금 화면을 의심하라.

---

## 11. 판정

**PASS**

- 단위 테스트 **51건 전부 GREEN**(실패 0 / skipped 0), 프로덕션 결함 **0건**.
- 커버리지 6개 지표 **하락 0건**, developer·code-reviewer 보고와 **불일치 0건**(`clean` 재측정).
- MINOR-1을 **직접 실험(A·B)으로 반증 경로를 확인한 뒤** 수정했다. `assertSame`은 존치.
- 신규 케이스 1건은 **커버리지가 아니라 실측된 회귀 공백**을 근거로 추가했고, V2 RED로 검증했다.
  독립 반증이 불가능한 단정 1개는 실험으로 기각하고 **삭제**했다.
- 리팩터링 배선 생존이 실기기 `appFinish` **3/3**(ActivityRecord 완전 소멸)으로 직접 확인됐다.
- **프로덕션 코드 수정 0건**, 화이트리스트 이탈 0건, 범위 확대 0건.

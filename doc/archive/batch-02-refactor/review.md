# 코드 리뷰 (파이프라인 5단계) — 배치 02

- **일시**: 2026-08-10
- **대상**: `App.kt`의 `AppContainer`·`AppBridgeHost` 분리 (plan.md Step 1~6)
- **입력**: `pipeline/handoff/4-developer.md`, `doc/20260810/20260810_155414_developer.md` §2·§3, plan §4, evaluation §3
- **판정**: **APPROVED** (MAJOR 0건 · MINOR 4건 — 전부 주석/문언, 재작업 없이 6단계에서 흡수 가능)

---

## 1. 최우선 판정 — `AppBridgeHostTest.kt:36`의 `assertSame` 한 줄

**결론: 단정 자체는 정당하다(장식 아님). 단, 그것을 정당화한 developer의 근거는 사실과 다르다.**

developer의 §3 근거 1과 테스트 주석 `:33-35`가 주장하는 것:

> "누군가 커스텀 getter를 끼워 넣어 다른 값을 돌려줘도 `requestFinish` 기반 단정은
> backing field를 읽어 그대로 초록이다."

이 주장을 인용하지 않고 **프로덕션을 직접 변조해 실험했다**(V7).

### 실험 A — 커스텀 getter (developer의 근거를 그대로 재현)

```kotlin
@Volatile
var onFinishRequested: (() -> Unit)? = null
    get() = null            // ← 변조
```

| 케이스 | 결과 |
|:--|:--|
| 1 `콜백이 등록돼 있으면 … 정확히 1회 호출한다` | **FAIL — `AssertionError: expected:<1> but was:<0>`** |
| 2 `콜백이 없으면 …` | PASS |

**FAIL을 낸 것은 `assertEquals`이고, `assertSame`은 도달조차 하지 않았다.**
Kotlin은 커스텀 접근자가 **생기는 순간** 클래스 내부의 프로퍼티 읽기도 그 getter를 경유하도록 컴파일한다.
backing field 직접 접근(GETFIELD)은 **기본 접근자일 때만** 성립하는 최적화다.
따라서 `requestFinish()`도 변조된 getter를 타고 `null`을 받아 아무것도 호출하지 않는다.

→ **developer가 든 시나리오는 `assertSame`이 없어도 `assertEquals`가 이미 잡는다. 근거 1은 거짓이다.**

### 실험 B — 감싸는 setter (단정의 독립 반증 가능성)

```kotlin
@Volatile
var onFinishRequested: (() -> Unit)? = null
    set(value) { field = if (value == null) null else ({ value.invoke() }) }   // ← 변조
```

| 케이스 | 결과 |
|:--|:--|
| 1 | **FAIL — `expected same:<AppBridgeHostTest$$Lambda$83…> was not:<AppBridgeHost$$Lambda$82…>`** |
| 2 | PASS |

여기서는 래퍼가 원본을 호출하므로 `callCount == 1`이 되어 **`assertEquals`는 초록이고,
`assertSame`만 RED가 된다.**

### 판정

- **V1 위반 아니다.** "사실상 항상 참"이 아님이 실험 B로 증명됐다 — `assertEquals`가 못 잡는 변조를
  이 한 줄이 단독으로 잡는다. 고정하는 계약은 **"프로퍼티는 넣은 인스턴스를 그대로 돌려준다"**이며,
  정규화·래핑·캐싱을 끼워 넣는 setter 변경이 실제 반증 경로다.
- **지표 게이밍 아니다.** 커버리지가 계기가 된 것은 맞지만, 그 결과 추가된 단정이 반증 가능하므로
  "수치만 채운 가짜 단정"에 해당하지 않는다.
- **다만 V2 절차 공백이 있다.** C-2는 케이스 1·2에 대해서만 RED를 증명했고,
  **`assertSame` 줄 자체는 RED 증명 없이 근거만 서술한 채 들어갔다.** 그 서술이 실험 A로 반증됐다.
  이 줄의 RED 증명은 이번 리뷰(실험 B)가 대신 채웠다.

---

## 2. 지적 사항

### MAJOR — **0건**

### MINOR-1 · `AppBridgeHostTest.kt:33-35` — 주석이 사실과 다르다 (근거: 실험 A)

```kotlin
// 넣은 것이 그대로 조회되는지도 함께 본다. `requestFinish`는 프로퍼티의 backing field를
// 직접 읽으므로 이 단정이 없으면 **getter가 한 번도 실행되지 않는다** —
// 누군가 커스텀 getter를 끼워 넣어 다른 값을 돌려줘도 위 단정은 그대로 초록이다.
```

- 1~2번째 줄은 **참**이다(JaCoCo에서 `getOnFinishRequested` INSTR 0/3이었던 것이 근거).
- **3번째 줄이 거짓이다.** 실험 A에서 커스텀 getter는 `assertEquals`를 먼저 깨뜨렸다.
- 해악: 이 줄이 실제로 지키는 계약(**setter가 값을 변형하지 않는다**)이 문서화되지 않고,
  지키지 않는 계약(커스텀 getter)이 지킨다고 적혀 있다. 다음 사람이 보호 범위를 오해한다.
- **수정 방향** — 3번째 줄을 실제 반증 경로로 교체:
  `// setter가 값을 감싸거나 정규화하면 requestFinish는 여전히 1회 호출돼 위 단정은 초록이지만, 이 단정이 RED가 된다.`
- comment-style.md "코드와 어긋난 주석: MINOR (오해 유발 시 MAJOR 후보)". 프로덕션 위험이 없고
  단정 자체는 유효하므로 **MINOR**로 둔다.

### MINOR-2 · `MainActivity.kt:122-123` — KDoc 링크 문법이 동작하지 않는 자리에 있다

```kotlin
        /*                                             ← `/**`가 아니라 `/*`
         * 흐름: … → BridgeDispatcher
         *       → [com.example.geckoviewtest.bridge.AppBridgeHost]
```

- `:119`에서 열리는 것은 **일반 블록 주석 `/*`**이고, 위치도 `onCreate` **함수 본문 안**이다.
  KDoc 링크 `[...]`는 `/** */`에서만 해석되므로 여기서는 **대괄호 그대로 렌더링**된다.
- 게다가 FQN이라 흐름 도식 한 줄이 두 줄로 늘어나 가독성이 떨어졌다(원래 한 줄이던 도식).
- **수정 방향**: 대괄호와 패키지 접두사를 빼고 `` `AppBridgeHost` ``로 되돌려 한 줄 도식을 유지한다.

### MINOR-3 · `AppContainer.kt:15-21` — 분리 후 "누가 나를 소유하는가"가 파일에서 사라졌다

- developer의 판단("plan Step 2에 KDoc 보강 지시가 없다")은 **문언상 정확하다.**
  plan Step 1에만 "새 파일 KDoc에 한 문장을 덧붙인다"가 있고 Step 2에는 없음을 확인했다.
- 그러나 결과적으로, `AppBridgeHost`는 새 KDoc에서 "누가 만들고 누가 콜백을 등록하는가"를 얻은 반면
  `AppContainer`는 **자기 파일만 읽어서는 `App`이 `by lazy`로 소유하는 프로세스당 1개라는 사실을 알 수 없다.**
  (`App.kt:49`에만 있다.) 파일이 나뉘면 맥락도 나뉜다는 지적에 해당한다.
- 정보가 코드베이스에서 소실된 것은 아니고(`App.kt:16`의 `[AppContainer]` 링크는 같은 패키지라 계속 해석된다)
  plan을 어긴 것도 아니므로 **권고**로 둔다.
- **수정 방향**(6단계 이후/후속 배치): KDoc에 한 줄 —
  `App이 by lazy로 만들어 소유하는 프로세스당 1개짜리다(App.kt:49).`

### MINOR-4 · `AppBridgeHost.kt:10-15` — plan이 정한 주석 분량 초과

- plan Step 1: "**한 문장을 덧붙인다** … (D-01 근거 **3줄 이내**)".
- 실제 추가분은 **본문 5줄 + 빈 줄 1줄 = 6줄**. 내용 자체는 D-01의 WHY로 타당하고 규칙 4에 부합하나
  계획이 명시한 분량의 약 2배다. 판정에 영향 없음 — **기록만.**

---

## 3. `git diff` 검증 (V7 — developer 보고를 인용하지 않고 재실행)

원본은 `git show HEAD:…/App.kt`로 뽑아 구간 대조했다.

| 항목 | 방법 | 결과 |
|:--|:--|:--|
| `AppBridgeHost` 본문 무변경 | 원본 `158-167` vs 신규 `16-25` `diff` | **차이 0줄** |
| `AppContainer` KDoc+본문 무변경 | 원본 `93-149` vs 신규 `15-71` `diff` | **차이 0줄** |
| `AppBridgeHost` KDoc | 원본 `151-157` vs 신규 `3-15` | 6줄 추가뿐(삭제·수정 0) → MINOR-4 |
| **`@Volatile` 생존** | `grep` | **살아 있다** (`:19` 애너테이션 + `:18` KDoc 언급) |
| **`?.` → `!!` 미변질** | `grep` | `:23` `onFinishRequested?.invoke()` **원형 유지**, `!!` **0건** |
| `MainActivity.kt` 5줄 | `git diff` 육안 + 블록 경계 확인 | **전부 `/* */` 주석 내부. 실행 코드 0줄** |
| `:129`·`:201` | 현재 파일 확인 | **한 글자도 안 바뀜** (R-06대로 타입 추론이라 import 불요) |
| `build.gradle.kts` | `git diff` | 제외 **1줄 삭제** + 주석 **8줄 추가**. `dependencies` 변경 **0건** |

**diff로만 잡히는 두 항목(`@Volatile`, `?.`)이 모두 무사하다.**

---

## 4. 커버리지 재측정 (`clean` 부착)

```
JAVA_HOME=…/jbr-17.0.14/… ./gradlew clean jvmCoverageReport --rerun-tasks   → BUILD SUCCESSFUL
```

| 지표 | 베이스라인 | developer 보고 | **내 재측정** | 방향 |
|:--|:--|:--|:--|:--|
| LINE | 98/98 | 102/102 | **102/102 = 100.00%** | 동일 |
| BRANCH | 18/18 | 20/20 | **20/20 = 100.00%** | 동일 |
| CLASS | 14/14 | 15/15 | **15/15 = 100.00%** | 동일 |
| INSTRUCTION | 607/636 = 95.44% | 626/655 | **626/655 = 95.57%** | **상승** |
| METHOD | 45/55 = 81.82% | 49/59 | **49/59 = 83.05%** | **상승** |
| COMPLEXITY | 54/64 = 84.38% | 59/69 | **59/69 = 85.51%** | **상승** |
| 테스트 | 48 | 50 / 실패0 / skip0 | **50 / 실패 0 / skipped 0** | — |

**6개 지표 전부 대조 — 하락 0건. developer 보고와 불일치 0건.**

부가 관측:
- `<class>` 원소 **29개**. `AppContainer`는 분모에 **없다**(제외 유지). `bridge/AppBridgeHost`는 **있다**. → G2-c 실질 기준 충족.
- `AppBridgeHost` 클래스: LINE 4/4 · BRANCH 2/2 · METHOD **4/4** · INSTR 19/19 · CXTY 5/5.
  메서드별로 `<init>` 3/3 · `getOnFinishRequested` **3/3** · `setOnFinishRequested` 4/4 · `requestFinish` 9/9.
- 제외 목록 **11 → 10**. 지워진 것은 `AppBridgeHost*.class` 한 줄뿐이고 **넓어진 항목 0건**.

---

## 5. C-1 · C-2 이행 확인

### C-1 — 컴파일 경고를 근거로 쓰지 않았는가 → **이행됨**

import를 직접 대조했다(경고 인용 아님).

- `App.kt` 잔존 **8개** — plan `:199` 목록(`Application`, `bridge.BridgeProtocol`, `gecko.await`,
  `Deferred`, `async`, `GeckoRuntime`, `GeckoRuntimeSettings`, `WebExtension`)과 **1:1 일치. 누락·잉여 0.**
- `AppContainer.kt` **11개** — plan `:198` 목록과 **1:1 일치.**
- `AppBridgeHost.kt` import **0개** — `BridgeHost`가 같은 패키지가 되어 불필요해진 것이 맞다.

### C-2 — RED 2회가 거울상인가 / 원복이 완전한가 → **이행됨**

- 거울상 구조는 developer 보고대로다. 내 실험 A·B도 **항상 한 케이스만 FAIL**했고 다른 케이스는 PASS라
  두 케이스가 서로 다른 변조를 잡는다는 구조가 재확인됐다.
- **원복 완전성 직접 확인**: 리뷰 착수 시점 `AppBridgeHost.kt`의 md5가
  **`e6bcc47f9e415da11358b252ddc573da`** — developer가 보고한 복원 해시(`e6bcc47f…`)와 **일치**.
  즉 developer의 변조 잔존 0건.
- **내 실험 후 원복도 확인**: 백업 파일과 `diff` 0줄, md5 동일, `git status` 파일 목록 불변,
  `get() = null` / `set(value)` / `!!` 잔존 **0건**.

---

## 6. 범위 (scope-guard · V9)

| 항목 | 결과 |
|:--|:--|
| 변경된 6개 경로 | `App.kt`·`MainActivity.kt`·`build.gradle.kts`(allowed_files), `AppContainer.kt`·`bridge/AppBridgeHost.kt`(allowed_globs), `test/**/AppBridgeHostTest.kt`(`app/src/test/**`) → **이탈 0건** |
| F-1 (assets `inputs.dir`) | `grep -c 'inputs.dir' build.gradle.kts` = **0** — 미개입 확인 |
| `interface BridgeHost` 분리 | `BridgeDispatcher.kt` `git status` 무변경 — **손대지 않음** |
| 스코프 변경(Application→Activity) | 없음. `AppBridgeHost` 본문 0줄 변경이 이를 보증 |
| 개명 | 없음(클래스명·프로퍼티명 전부 동일) |
| 새 라이브러리 | `dependencies` 추가/삭제 줄 **0건** |
| 제외 목록 | 11 → **10**, 좁아짐(확대 아님) |

Bash 생성 파일까지 포함해 `git status` 전체를 대조했고, 화이트리스트 밖 산출물은 없다.

---

## 7. 계획 문언 오기 2건 — **둘 다 developer 주장이 맞다**

### G2-c (plan `:204`) — **실행 불가능한 문언**

계획 스니펫(`plan.md:165`)의 필터가 `if 'App' in n`이므로, 게이트가 요구한
*"`[App계열]` 출력이 `bridge/AppBridgeHost` 한 줄만"*은 성립할 수 없다.
현재 리포트에 스니펫을 **그대로 실행한 결과**:

```
  [App계열] com/example/geckoviewtest/bridge/AppBridgeHost
  [App계열] com/example/geckoviewtest/data/AppInfoRepository
  [App계열] com/example/geckoviewtest/data/AppInfoRepositoryImpl
  [App계열] com/example/geckoviewtest/data/AppInfoRepositoryImpl$Companion
```

`AppInfoRepository` 계열 3줄은 이 배치와 무관하게 **베이스라인에서도 나온다.**
developer가 실질 기준(`AppContainer` 부재 + `bridge/AppBridgeHost` 존재)으로 대체 판정한 것은 **타당하다.**

### G2-b (plan `:203`) — **숫자 2개가 틀렸다**

- *"현재 169줄"* → 실제 원본 **168줄**(`git show HEAD:…App.kt | wc -l`).
- *"90줄 내외로 줄어든다"* → 실제 **81줄**. 본질(`grep -c '^class '` = **1**)은 충족.

### 기록 위치 판정

다음 배치가 같은 스니펫을 복사해 재발시키지 않도록 **`pipeline/plan.md`가 아니라
`pipeline/handoff/5-code-reviewer.md`와 이 문서에 남긴다** — plan은 3단계에서 APPROVED된 확정 문서라
5단계가 사후 편집하면 승인 이력과 어긋난다. 다음 배치의 planner가 스니펫을 재사용할 때
**필터를 `'App' in n`이 아니라 정확한 클래스 경로 대조로 바꿀 것**을 인계한다.

---

## 8. 판정

**APPROVED**

- MAJOR **0건**. MINOR 4건은 전부 주석·문언이며 프로덕션 동작·커버리지·범위에 영향이 없다.
- 본문 무변경이 바이트 단위로 증명됐고, diff로만 잡히는 `@Volatile`·`?.` 두 항목이 무사하다.
- 6개 지표 하락 0건이 `clean` 재측정으로 독립 확인됐고 developer 보고와 불일치가 없다.
- 최우선 판정 항목인 `assertSame`은 **반증 가능하므로 존치**한다. 다만 그 줄을 정당화한 주석은
  실험으로 반증됐으므로 **MINOR-1로 수정을 요구**한다(재작업 루프 없이 6단계에서 흡수 가능).

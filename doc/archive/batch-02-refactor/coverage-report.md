# 커버리지 리포트 (파이프라인 7단계 — 최종 게이트) — 배치 02

- **일시**: 2026-08-11 08:35:42
- **대상**: `AppBridgeHost` → `bridge/` 패키지 이동(+ 커버리지 제외 해제 + 테스트 추가), `AppContainer` 루트 패키지 유지(파일만 분리)
- **판정**: **PASS**
- **측정 명령**: `JAVA_HOME=…/jbr-17.0.14/… ./gradlew clean jvmCoverageReport --rerun-tasks`
  → `BUILD SUCCESSFUL` / **`30 actionable tasks: 30 executed`** (UP-TO-DATE 회피 확인, `clean` 부착 — planner R-P3)

---

## 1. 판정 요약

`architecture.md` 테스트 절의 **두 조항을 각각 판정**했다. 둘 다 충족한다.

| 조항 | 기준 | 결과 |
|:--|:--|:--|
| (a) 변경 클래스 라인 커버리지 | **70% 이상** | `bridge/AppBridgeHost` **4/4 = 100%** ✅ |
| (b) 전체 커버리지 | **베이스라인 대비 하락 금지** | **6개 지표 하락 0건** (3개는 상승) ✅ |

**인계 보고(developer·code-reviewer·qa 3자)와의 불일치: 없음.** 6개 지표 전부 자릿수까지 일치.

---

## 2. 6개 지표 재측정 (V7 — 인계값을 옮겨 적지 않고 직접 측정)

베이스라인도 **직접 재산출했다.** `git archive HEAD`로 배치 01 종료 시점을 스크래치패드에 독립 전개하고
(`local.properties`만 복사) 같은 명령을 실행했다 — 작업 트리는 건드리지 않았다.
전개본에서 제외 항목 **11개**, `App.kt`의 `^class ` **3개**를 확인해 베이스라인 시점이 맞음을 검증했다.

| 지표 | 베이스라인 (재산출) | **현재 (재측정)** | 증감 | 방향 |
|:--|:--|:--|:--|:--|
| **LINE** | 98/98 = 100.00% | **102/102 = 100.00%** | 분자 +4 / 분모 +4 | 유지 |
| **BRANCH** | 18/18 = 100.00% | **20/20 = 100.00%** | +2 / +2 | 유지 |
| **CLASS** | 14/14 = 100.00% | **15/15 = 100.00%** | +1 / +1 | 유지 |
| **INSTRUCTION** | 607/636 = 95.44% | **626/655 = 95.57%** | +19 / +19 | **상승 +0.13pp** |
| **METHOD** | 45/55 = 81.82% | **49/59 = 83.05%** | +4 / +4 | **상승 +1.23pp** |
| **COMPLEXITY** | 54/64 = 84.38% | **59/69 = 85.51%** | +5 / +5 | **상승 +1.13pp** |
| 테스트 | 48 / 실패 0 / skip 0 | **51 / 실패 0 / skip 0** | +3 | — |

**하락 지표 0건.** 상승 3건은 전부 **분자와 분모가 같은 값만큼 늘어난** 결과다 —
즉 분모를 줄여서 만든 상승이 아니다. 증분(LINE 4 · BRANCH 2 · CLASS 1 · INSTR 19 · METHOD 4 · CXTY 5)이
`AppBridgeHost` 단일 클래스의 규모와 **정확히 일치**해 출처가 하나로 특정된다.

`<class>` 원소는 28개 → **29개**. 늘어난 것은 `bridge/AppBridgeHost` **하나뿐**이며,
베이스라인 분모에 있던 28개는 한 항목도 사라지지 않았다(전수 대조).

### 테스트 51건인데 6개 지표가 안 움직인 것에 대해

qa 단계에서 테스트가 50 → 51이 되었으나 6개 지표는 한 자리도 변하지 않았다.
**이상 징후가 아니다.** 추가된 케이스(재등록 전이)가 이미 덮인 4라인·2분기만 다시 지나기 때문이며,
qa는 이 케이스의 근거를 커버리지가 아니라 **실측된 회귀 공백**(1회성 가드를 넣어도 기존 50건이 전부 초록)에 두었다.
**수치가 안 움직인다는 사실 자체가 이 케이스가 수치 목적이 아님의 기계적 증거**다.

---

## 3. 클래스 단위 커버리지 — `bridge/AppBridgeHost`

| 지표 | 값 | 비율 |
|:--|:--|:--|
| LINE | **4/4** | **100%** (게이트 70% 대비 +30pp) |
| BRANCH | **2/2** | 100% |
| CLASS | **1/1** | 100% |
| INSTRUCTION | **19/19** | 100% |
| METHOD | **4/4** | 100% |
| COMPLEXITY | **5/5** | 100% |

메서드별 (XML `<method>` 직접 파싱):

| 메서드 | line | INSTR | LINE | BRANCH | METHOD |
|:--|:--|:--|:--|:--|:--|
| `<init>` | 16 | 3/3 | 1/1 | — | 1/1 |
| `getOnFinishRequested` | 20 | 3/3 | 1/1 | — | 1/1 |
| `setOnFinishRequested` | 20 | 4/4 | 1/1 | — | 1/1 |
| `requestFinish` | 23 | 9/9 | 2/2 | **2/2** | 1/1 |

INSTR 합 3+3+4+9 = **19** — 클래스 총계와 일치.

### 변경된 다른 클래스의 (a) 조항 판정

| 클래스 | 이 배치의 변경 | 분모 | 판정 |
|:--|:--|:--|:--|
| `bridge/AppBridgeHost` | 패키지 이동 + 제외 해제 | **안 (신규 진입)** | **100% ≥ 70%** ✅ |
| `AppContainer` | 파일만 분리(패키지 불변) | 밖 (구조적 예외) | 해당 없음 — §4에서 타당성 검증 |
| `App` | 클래스 2개 제거 + import 정리 (-87줄) | 밖 (기존 예외) | 해당 없음 |
| `MainActivity` | **주석 5줄만** (`git diff -U0`로 실행 코드 0건 확인) | 밖 (기존 예외) | 해당 없음 |

---

## 4. 제외 범위 타당성 — 확대가 아님을 원문 대조로 확인

**이 단계가 잡아야 할 대표적 거짓 그린**이 제외 범위의 잠행 확대다. 기계 대조했다.

`git show HEAD:app/build.gradle.kts`의 목록과 현재 목록을 파싱해 집합 비교:

```
baseline entries: 11   current entries: 10
ADDED   (범위 확대에 해당): 없음
REMOVED: ['com/example/geckoviewtest/AppBridgeHost*.class']
현재 목록이 베이스라인의 진부분집합인가: True
```

**추가 0건 · 삭제 1건 — 엄격한 진부분집합이다. 확대가 아니라 축소이며, 100%의 의미가 강해졌다.**
`git diff`상 나머지 변경은 제외 기준을 설명하는 주석 블록뿐이다.

### 남은 제외의 정당성

- **`AppContainer` 제외 유지 — 정당하다.** 빌드 스크립트가 스스로 밝힌 기준은
  "테스트하기 귀찮아서가 아니라 **JVM 테스트로 실행 자체가 불가능한가**"다. 원문 확인 결과 의존이 실재한다:
  `AppContainer.kt:22` `Application` 생성자 파라미터, `:42·45·48` `app.packageManager`,
  `:33` `Dispatchers.Main.immediate` — 마지막 것은 `Dispatchers.setMain` 없이는 **생성자 실행 자체가 실패**한다.
  → 기준 충족. 위치가 바뀌어도 판정은 그대로다.
- **`AppBridgeHost` 제외 해제 — 정당하다.** 같은 기준에 **미달**한다:
  `grep -c "android\|Application\|Context\|Dispatchers"` = **0**. 안드로이드 타입 0개인 순수 Kotlin 4줄이다.
  제외할 근거가 없었으므로 목록에서 빠졌다. **이것이 F-2의 내용이다.**

두 클래스가 형제처럼 보이지만 기준의 양쪽에 있다 — 판정이 갈린 것이 옳다.

---

## 5. 글롭 의미론 두 개가 공존한다 (V8) — 실행으로 확인

이 배치 설계의 근거가 "Gradle Ant PatternSet의 `*`는 `/`를 넘지 않는다"(R-01)이다.
**인용하지 않고 판별 실험을 실행했다.** 현재 실행만으로는 분리되지 않는다 —
`AppBridgeHost`는 패키지 이동과 패턴 삭제가 **동시에** 일어났기 때문이다.
그래서 스크래치패드 베이스라인 전개본에 패턴 한 줄을 주입해 단일 변수로 격리했다:

```kotlin
"com/example/geckoviewtest/*BridgeDispatcher*.class",   // 주입
```

대상 `bridge/BridgeDispatcher.class`는 **하위 패키지**에 있다.
- `*`가 `/`를 넘는다면 → 제외되어 LINE이 6줄 줄어야 한다.
- 넘지 않는다면 → 분모 불변.

**결과: LINE 98/98 불변, `BridgeDispatcher` 2개 항목 모두 분모에 잔존.**
→ **Ant `*`는 `/`를 넘지 않는다. R-01은 참이다.** (실험 패턴은 원복 확인, 스크래치패드 한정 — 실제 트리 무변경)

반대편도 확인했다. guard hook의 소스 `guard-impact-scope.py:52`가
`fnmatch.fnmatch(rel, g)`를 쓰고, 실행 결과 `…/geckoviewtest/*AppBridgeHost.kt`가
`bridge/AppBridgeHost.kt`는 물론 `gecko/deep/AppBridgeHost.kt`까지 **ALLOW**한다.

> **두 의미론이 한 저장소에 공존한다.** 빌드 스크립트의 제외 패턴은 좁게(하위 패키지 미포함),
> 스코프 가드의 화이트리스트는 넓게(임의 깊이 포함) 동작한다. code-reviewer의 신규 관측이 맞다.
> 이번 배치에서는 양쪽 다 의도한 결과를 냈지만, **다음 배치가 화이트리스트를 "좁게 썼다"고 믿으면 오탐한다.** → 후속 과제 F-6.

---

## 6. F-2 해소 여부 — **닫혔다**

F-2는 "`AppBridgeHost`가 제외 목록에 있는데 그 근거가 없다"였다. 세 조건을 전부 확인했다:

1. **제외 목록에서 빠졌다** — 원문 대조, §4.
2. **분모에 실제로 들어왔다** — XML `<class>` 원소에 `com/example/geckoviewtest/bridge/AppBridgeHost` 존재, §3.
3. **짝(R-04)이 지켜졌다** — 테스트가 4라인·2분기·4메서드를 **전부** 덮어 회귀 0건. 제외 해제와 테스트가 같은 배치에서 왔다.

한쪽만 했다면 96.08%로 회귀했겠지만 짝이 맞아 **100.00%를 유지한 채** 닫혔다. **F-2 CLOSED.**

---

## 7. METHOD 4/4는 지표 게이밍인가 — **아니다** (독립 판단)

`getOnFinishRequested`는 **프로덕션에서 아무도 읽지 않는다**(`requestFinish`는 backing field를 직접 읽고
`MainActivity`는 쓰기만 한다). METHOD 4/4는 테스트의 `assertSame` 한 줄이 태워서 달성됐다.
"수치를 맞추려고 넣은 장식 단정 아닌가"가 이 단계가 검토해야 할 지점이다.

**인계된 결론을 옮겨 적지 않고 직접 변조 실험했다**(V7). 실제 트리는 건드리지 않기 위해
작업 트리를 스크래치패드에 복사해 그곳에서만 변조했다. 복사본 md5가 `e6bcc47f9e415da11358b252ddc573da`로
3개 단계가 보고한 해시와 일치해 **원본이 복원된 상태임도 함께 확인**됐다.

감싸는 setter를 주입:

```kotlin
set(value) { field = if (value == null) null else { -> value.invoke() } }
```

**결과: 51건 중 정확히 1건만 FAIL.**

```
AppBridgeHostTest > 콜백이 등록돼 있으면 requestFinish가 그 콜백을 정확히 1회 호출한다 FAILED
    java.lang.AssertionError: expected same:<AppBridgeHostTest$$Lambda…> was not:<AppBridgeHost$$Lambda…>
    at AppBridgeHostTest.kt:38
```

**`:38`(`assertSame`)에서만 깨졌다. 같은 케이스의 `:33`(`assertEquals(1, callCount)`)은 초록으로 통과했다** —
래퍼가 원본을 호출하므로 호출 횟수는 그대로 1이기 때문이다.

**판단: 게이밍이 아니다.** 판정 근거는 `verification-honesty.md` V1의 기준
"각 조건이 **독립적으로** 깨질 수 있어야 한다"이며, `assertSame`은 다른 단정이 잡지 못하는 결함을
**단독으로** 잡는다. 고정하는 계약("넣은 것이 그대로 조회된다")도 public mutable 프로퍼티의 실재하는 계약이고,
값을 감싸거나 정규화하는 setter는 로깅·검증을 덧붙일 때 충분히 나올 수 있는 현실적 변경이다.
METHOD 4/4는 그 단정의 **부수 효과이지 목적이 아니다.**

**다만 정직하게 덧붙인다.** 이 단정은 **프로덕션이 현재 의존하지 않는 접근자**의 계약을 고정한다.
게이밍은 아니지만, 더 근본적인 해법은 단정을 늘리는 쪽이 아니라 **읽히지 않는 public 접근자를 좁히는 것**
(`private set` 또는 노출 축소)이다. 이는 public API 변경이라 이번 범위 밖이며 **F-7로 넘긴다.**

---

## 8. 검증 무결성

- **프로덕션·테스트 코드 수정 0건.** 모든 변조 실험은 스크래치패드 복사본에서만 수행했다.
- 종료 시점 실제 트리 재확인: `AppBridgeHost.kt` md5 **`e6bcc47f9e415da11358b252ddc573da`**(착수 시와 동일),
  `AppContainer.kt` `fbc251b8…`, `AppBridgeHostTest.kt` `7ab7da0e…`, `git status` 목록 불변, 제외 항목 **10개**.
- **신규 3개 파일은 git 미추적(`??`)** — `git diff`가 못 보므로 `git status`로 따로 확인했다.
- **mtime을 판정에 쓰지 않았다.** 변경 판정은 전부 내용(`git diff`·md5·XML 파싱)으로 했다.
- "빌드 성공"을 판정 근거로 쓰지 않았다(V1). 판정은 커버리지 XML 분모와 원문 대조로 했다.

---

## 9. `enforce` 처리

**PASS이므로 `scope-guard.md` 규칙 4에 따라 `pipeline/impact-report.json`의 `enforce`를 `true` → `false`로 내렸다.**
다른 필드는 한 글자도 건드리지 않았다(`allowed_files`·`allowed_globs`·`risk_notes`·`readonly_context` 전부 원형).
리포트 파일 자체는 보존한다.

---

## 10. 배치 아카이브

`work-logging.md`의 "배치 종료 시 아카이브" 절대로 **아카이브 대상이다.** 다음 배치가 이 배치의 전문
(`plan.md` 38KB, `evaluation.md` 34KB, `qa-report.md` 16KB, `review.md` 14KB)을 통째로 읽지 않게 하는 것이 목적이다.

- **이동 대상**: `pipeline/*.md` + `pipeline/impact-report.json` → **`doc/archive/batch-02-refactor/`**
- **유지**: `pipeline/handoff/` (다음 배치의 출발 컨텍스트)
- **실행 여부**: **판단만 하고 실행하지 않았다.** 오케스트레이터 확인 후 처리한다.
  (`enforce: false`로 내린 `impact-report.json`도 이동 대상에 포함되며, **삭제가 아니라 이동**이다.)

---

## 11. 후속 과제

| # | 내용 | 출처 |
|:--|:--|:--|
| **F-1** | assets `inputs.dir` 미선언 (UP-TO-DATE 함정) — 이번 배치 미개입(`grep` 0건) | 배치 01 이월 |
| **F-3** | AC-003-2 역주입 | 이월 |
| **F-4** | `coverage-baseline.json` 신설 — 베이스라인이 산문에만 있어 **이번에도 내가 직접 재산출**해야 했다. 매 배치 반복되는 비용 | 이월 (재확인) |
| **F-5** | *(폐기)* "git 저장소가 아니다" — R-09로 폐기됨 | — |
| **F-6** | **글롭 의미론 두 개 공존** (Ant `*`는 `/` 미통과 / hook `fnmatch`는 통과). 화이트리스트가 의도보다 넓게 동작한다 | **신규 (§5)** |
| **F-7** | **`onFinishRequested`의 getter를 프로덕션에서 아무도 읽지 않는다.** `private set`/노출 축소 검토 — public API 변경이라 범위 밖 | **신규 (§7)** |
| **F-8** | `interface BridgeHost`가 `bridge/BridgeDispatcher.kt:59`에 있다 — 같은 "한 파일에 여러 클래스" 문제 | 이월 |
| **F-9** | **`plan.md` 문언 오기 2건** (G2-b "169줄"→168·"90줄 내외"→81, G2-c 필터 `'App' in n`이 베이스라인에서도 3줄 반환). 3단계 APPROVED 확정 문서라 사후 편집하지 않음 — **다음 배치 planner는 이 스니펫을 복사하지 말 것** | 이월 |
| **F-10** | 프로덕션 주석 MINOR-2·3·4 (`MainActivity.kt:122-123` KDoc 링크 미해석, `AppContainer.kt:15-21` 소유자 맥락, `AppBridgeHost.kt:10-15` 분량) | 5단계 이월 |

---

## 12. 최종 판정

**PASS.**

- (a) 변경 클래스 `bridge/AppBridgeHost` **라인 100% ≥ 70%** ✅
- (b) 전체 커버리지 **6개 지표 하락 0건** (LINE·BRANCH·CLASS 유지, INSTR·METHOD·CXTY 상승) ✅
- 제외 범위 **확대 0건 — 진부분집합으로 축소** ✅
- **F-2 CLOSED** ✅
- 테스트 **51 / 실패 0 / skipped 0** ✅
- 인계 보고와 **불일치 없음** ✅

**파이프라인 종료. `enforce: false` 처리 완료.**

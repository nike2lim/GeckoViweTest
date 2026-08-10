# developer 핸드오프
- **판정**: **PASS** (Step 1~6 게이트 21개 통과. 단 G4-b는 1차 FAIL 후 통과 — 2번)
- **로그**: `doc/20260810/20260810_155414_developer.md`

## 다음 단계가 반드시 알아야 할 것

1. **최종 실측**(출처: 방금 실행한 `clean jvmCoverageReport --rerun-tasks` XML): **LINE 102/102 = 100.00%, BRANCH 20/20, CLASS 15/15, INSTR 626/655, METHOD 49/59, CXTY 59/69. 하락 지표 0건.** 테스트 48 → **50**, 실패 0, skipped 0. **96.08% 중간 상태는 4단계 종료 시점에 없다.**
2. **G4-b(`AppBridgeHost` METHOD 4/4)가 1차 시도에서 FAIL(3/4)이었다. 로그 §3을 반드시 읽어라.** 미커버는 **프로퍼티 getter**였다 — `requestFinish`는 backing field를 직접 읽고 `MainActivity`는 쓰기만 한다(`:129` 등록, `:201` null). 케이스를 늘리지 않고 **케이스 1에 `assertSame` 1줄**을 넣어 해결했다. **이 판단(수치 맞추기인가 실제 검증인가)이 code-reviewer가 독립 재검토해야 할 유일한 지점이다.**
3. **plan §3.2 / evaluation §2(b)의 "METHOD +4/+4"는 자동으로 성립하지 않는다.** 4개 메서드 전부 커버를 가정한 값이다. getter를 태우기 전에는 **INSTR·METHOD·CXTY 3개 지표가 베이스라인 대비 하락**했다(95.11%/81.36%/84.06%).
4. **plan G2-c의 문언은 실행 불가능하다 — 구현 문제가 아니다.** *"`[App계열]`이 `bridge/AppBridgeHost` 한 줄만"*이라 적혔으나 스니펫 필터가 `'App' in n`이라 **베이스라인에서도** `data/AppInfoRepository` 계열 3줄이 항상 나온다(베이스라인에서 실행 확인). 실질 기준(`AppContainer` 부재 + `bridge/AppBridgeHost` 존재)으로 기계 판정했고 둘 다 충족.
5. **"빌드 성공"을 근거로 쓰지 않았다.** 판정은 전부 커버리지 XML 분모와 `git diff`로 했다. `MainActivity.kt:129·201`은 **한 글자도 안 고쳤고** 그래도 컴파일된다(R-06 재확인).
6. **G1-d 현장 확인 — R-01 전제는 참이다.** `bridge/AppBridgeHost`가 분모 진입(LINE 0/4 → 98/102). 반환 사유 없음.

## 확정된 결정

- **본문 무변경 이행.** 두 클래스 모두 옛 `App.kt` 구간과 새 파일을 뽑아 `diff` — **차이 0줄**(파일 끝 빈 줄 제외). `@Volatile` 살아 있고 `?.` 그대로. `git diff --stat`: `App.kt` -87, `build.gradle.kts` +9/-2, `MainActivity.kt` 5줄(**전부 주석** — `git diff -U0` 기계 판정으로 실행 코드 0건).
- **C-1 이행**: `App.kt` 잔존 import **8개**, `AppContainer.kt` 이동 import **11개** — plan §4 Step 2 목록과 **1:1 일치, 누락·잉여 0건.** 컴파일 경고는 근거로 쓰지 않았다.
- **C-2 이행**: RED를 **프로덕션 변조로 2회.** ① `requestFinish()` 빈 몸통 → **케이스 1만 FAIL**(`expected:<1> but was:<0>`) ② `?.`→`!!` → **케이스 2만 FAIL**(NPE). **정확히 거울상**이라 각 케이스가 상대가 못 잡는 변조를 잡는다. 원복은 **md5 `e6bcc47f...` 일치**로 확인 후 50건 GREEN 재확인. RED-2가 R-P6·R-P7을 동시 방어.
- **Step 3**: 제외 목록 **11 → 10**. G3-b에서 수치가 **1도 안 움직여** 그 줄이 죽은 패턴이었음이 확인됐다.
- **`AppContainer` KDoc에 문장을 덧붙이지 않았다.** plan Step 1은 KDoc 보강을 명시했지만 Step 2에는 없고, 패키지가 그대로라 `App.kt:26`의 `[AppContainer]` 링크가 계속 해석된다(E-5).
- **실기기 회귀 없음**(SM-G981N): G6-a `READY`, G6-b `versionName = 1.0.0` **3/3**, G6-c `UNKNOWN_FUNCTION` + 앱 생존, **G6-d `dumpsys` 35 → 0 — 3/3**, G6-e `AndroidRuntime`·`FATAL`·`GeckoSession` E/F **각 0건**. **G6-d가 `requestFinish()`를 지나는 유일한 경로라 배선 생존의 증거다.**

## 전문을 열어야 하는 경우

- **code-reviewer**: **로그 §3**(G4-b 1차 FAIL과 `assertSame` 1줄의 정당성) — **여기만은 반드시.** 그리고 **§2**(C-1 대조표, C-2 RED 관측값).
- **qa**: **로그 §3** — getter를 프로덕션에서 아무도 안 읽는다는 사실. evaluator 주의 ③대로 **케이스 추가 권한 유지**(등록→해제→재등록 전이). 102/102에는 불요.
- **coverage-reporter**: **로그 §5** 수치표. `clean` 부착과 `testDebugUnitTest` non-UP-TO-DATE를 매 회 확인.

## 미해결 / 이월

- **F-2 닫힘.** `AppBridgeHost`가 분모에 들어왔고 테스트가 4줄·2분기를 전부 덮는다.
- **F-1**(assets `inputs.dir`) **손대지 않았다** — `grep -c 'inputs.dir'` = **0**. 후속 배치.
- **`interface BridgeHost`**(`BridgeDispatcher.kt:59`) — 읽기만 함. 후속 후보.
- **신규 후속 후보**: `onFinishRequested`의 **getter를 프로덕션에서 아무도 쓰지 않는다.** `private set` 검토 여지가 있으나 public API 변경이라 범위 밖.
- **plan 문언 오기 2건**: G2-c(위 4번), G2-b "90줄 내외" vs **실제 81줄**. 판정 영향 없으나 다음 배치가 같은 스니펫을 복사하면 재발한다.
- **화이트리스트에 막힌 시도 0건. 범위 확대 0건.**

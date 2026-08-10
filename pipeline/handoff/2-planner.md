# planner 핸드오프
- **판정**: 해당 없음 (planner는 게이트가 아님)
- **전문**: `pipeline/plan.md` — §2 결정 / §3 커버리지 / §4 Step·게이트 / §6 evaluator 요청
- **로그**: `doc/20260810/20260810_150544_planner.md`

## 다음 단계가 반드시 알아야 할 것

1. **4단계와 6단계 사이에 LINE 96.08%가 관측될 수 있다. 회귀가 아니라 계획된 중간 상태다.** `AppBridgeHost`가 분모에 들어온 뒤(Step 1) 테스트가 붙기 전(Step 4) 구간이다. **code-reviewer는 이 수치만으로 CHANGES_REQUESTED를 내지 말 것.** 커버리지 최종 판정은 7단계에서만 한다(plan §3.4).
2. **커버리지 측정에 `clean`을 반드시 붙여라 — `--rerun-tasks`만으로는 부족하다.** `AppBridgeHost.class`가 옛 위치에 남은 채 `bridge/`에도 생기면 옛 것은 제외되고 새 것은 포함돼 수치가 흔들린다. **내가 새로 잡은 리스크다**(plan R-P3. impact-analyzer의 R-08은 `--rerun-tasks`만 요구했다).
3. **JAVA_HOME 불일치는 실측 결과 차단 요인이 아니었다.** JBR 17(17.0.14)과 Studio JBR(21.0.10) **둘 다 빌드 성공, 수치 동일**(각각 1회씩 실행). 그래도 하나로 고정한다 → **`JAVA_HOME=/Users/appdevloperteam/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home`** (경로에 공백 없음 + `jvmTarget = JVM_17` 일치). 기본 JVM은 Java 8이라 설정 단계에서 죽는다.
4. **"빌드 성공"을 근거로 쓰지 마라.** `MainActivity.kt:129·201`은 타입 추론이라 import가 없어(직접 확인) 패키지를 바꿔도 손대지 않고 컴파일된다. 판정은 **커버리지 XML의 분모 목록**과 **`git diff`**로 한다.
5. **Step 1·2는 "본문 무변경"이 계약이다.** `@Volatile` 누락이나 `?.`→`!!` 변경은 **컴파일도 테스트도 통과하므로 diff로만 잡힌다**(R-P6).
6. **베이스라인은 내가 재측정했다**(출처: 방금 실행한 `jvmCoverageReport.xml`): **LINE 98/98, BRANCH 18/18, CLASS 14/14, INSTRUCTION 607/636, METHOD 45/55, COMPLEXITY 54/64.**

## 확정된 결정

- **`AppBridgeHost` → `com.example.geckoviewtest.bridge`** (`bridge/AppBridgeHost.kt`). 구현하는 `interface BridgeHost`가 `bridge/BridgeDispatcher.kt:59`에 있고 의존 대상이 그것 하나뿐이다. 실패 모드도 안전한 쪽(제외 줄 삭제를 잊어도 분모에 들어옴).
- **`AppContainer` → 루트 패키지 유지**, 파일만 `AppContainer.kt`로 분리. **`di/`로 옮기지 마라** — 제외 패턴이 조용히 깨져 80.33%로 회귀하는데 얻는 이득이 0이다.
- **두 선택이 다른 것이 옳다.** 같은 기준("무엇에 소속되는가")으로 답이 갈렸다(§2 D-03).
- **제외 처리**: `AppBridgeHost` **해제**(안드로이드 타입 0개 → 기준 미달), `AppContainer` **유지**(`Application`·`packageManager`·`Dispatchers.Main.immediate` → 충족). 패턴은 **`AppBridgeHost*.class` 1줄 삭제만**.
- **개명 없음. 새 라이브러리 없음** (Step 4 테스트는 기존 JUnit만으로 충분).
- **목표**: LINE **102/102 = 100.00%**, BRANCH **20/20**, CLASS **15/15**. `AppBridgeHost` 규모는 JaCoCo core로 직접 측정 — **LINE 4 / BRANCH 2 / METHOD 4 / INSTR 19 / CXTY 5**. 하락 지표 0건.
- **화이트리스트 이탈 0건.** hook과 같은 `fnmatch`로 6개 경로 전부 실행 대조. **impact-analyzer 재실행 불필요.**

## 전문을 열어야 하는 경우

- **evaluator**: **§6 전체.** E-1~E-5가 판정 요청이다. **E-2**(패키지를 안 옮기고 파일만 분리하는 것이 요청 충족인가)와 **E-3**(테스트를 developer가 4단계에서 쓰는 것이 역할 위반인가)이 REJECTED면 계획이 실제로 바뀐다 — 대안 경로는 각 항목에 적혀 있다. 아키텍처 대조는 §11.
- **developer**: **§4 Step 1~5**(옮길 import 목록까지 명시) + 착수 전 **§7 행동 규약**. 분모 확인용 파이썬 스니펫은 §4 공통 실행 규약에.
- **qa**: **§4 Step 4.** 케이스 2개(콜백 등록 / null)로 4라인·2분기를 전부 덮어야 102/102다. null 분기가 이 스위트의 존재 이유다(`MainActivity.onDestroy`가 실제로 `null`을 넣는다). **G4-a가 V2(의도적 RED 확인)를 요구한다.**
- **coverage-reporter**: **§3.2 예상 수치표**, **§9 JAVA_HOME**.
- **code-reviewer**: **§3.4**(96.08% 중간 상태), **R-P1·R-P6**.

## 미해결 / 이월

- **F-2**(AppBridgeHost 제외 근거 부재) = **이 배치에서 닫힌다.** Step 3(제외 해제) + Step 4(테스트) 짝으로.
- **F-1**(assets `inputs.dir` 미선언) **범위 밖.** `build.gradle.kts`가 화이트리스트에 있어 물리적으로 고칠 수 있지만 **손대지 마라**(V9 / 리드 지시 ⑤). 후속 배치.
- **F-3**(AC-003-2 역주입), **F-4**(`coverage-baseline.json`) 이월.
- **신규 후속 후보**: `interface BridgeHost`가 `bridge/BridgeDispatcher.kt` 안에 있다 — 같은 "한 파일에 여러 클래스" 문제지만 이동 대상이 아니고 화이트리스트 밖이다. **넓히지 마라.**

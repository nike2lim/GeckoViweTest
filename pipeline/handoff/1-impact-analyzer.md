# impact-analyzer 핸드오프
- **판정**: 해당 없음
- **전문**: `pipeline/impact-report.json` — `risk_notes` R-01~R-11
- **로그**: `doc/20260810/20260810_145035_impact-analyzer.md`

## 다음 단계가 반드시 알아야 할 것

1. **커버리지 조항은 두 개다.** `architecture.md`가 요구하는 것은 "변경 클래스 70% 이상" **그리고** "전체 커버리지 베이스라인 대비 하락 금지"다. 배치 01과 리드 인계가 정리한 "세 경우 모두 70% 게이트 통과"는 참이지만 **두 번째 조항을 덮지 못한다.** 현재 베이스라인이 LINE 100.00%(내가 `--rerun-tasks`로 직접 재측정)이므로, 테스트 없이 분모만 넓히면 96.08%든 80.33%든 전부 회귀다. → **제외 해제와 테스트 추가는 같은 배치 안에서 짝으로 가야 한다** (R-03/R-04).
2. **제외 패턴이 깨지는 조건을 실측했다.** 스크래치패드 독립 Gradle 프로젝트에서 `coverageExclusions` 원문을 그대로 실행한 결과 — 같은 패키지(`AppContainer.class`)는 계속 제외되고, **하위 패키지(`di/AppContainer.class`, `bridge/AppBridgeHost.class`)만 조용히 분모에 들어온다.** Gradle Ant PatternSet의 `*`가 `/`를 넘지 않기 때문이다. 즉 리드의 경고는 정확하되 **발현 조건이 있다: 하위 패키지를 고를 때만** (R-01).
3. **어느 선택지든 `coverageExclusions` 갱신은 필수다.** 같은 패키지 유지 → 패턴이 계속 맞아 수치 불변이지만 **F-2가 해결되지 않는다**(파일만 나뉘고 AppBridgeHost는 여전히 분모 밖). 하위 패키지 이동 → 패턴이 어긋나 0%로 진입. 갱신하지 않으면 게이트 수치가 의도와 무관하게 정해진다 (R-02).
4. **컴파일 성공은 증거가 아니다.** MainActivity의 `container.bridgeHost` 참조 2건(129·201행)은 타입 추론 기반이라 import가 없다. 패키지를 바꿔도 MainActivity를 안 고치고 빌드가 통과한다 (R-06).
5. **`./gradlew`를 그냥 부르면 실패한다.** 기본 JAVA_HOME이 Java 8이다. 모든 Gradle 호출 앞에 `JAVA_HOME=/Users/appdevloperteam/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home`를 붙일 것 (R-07, 실측).
6. **이 프로젝트는 이제 git 저장소다.** 배치 01 F-5("git 저장소가 아니다")는 폐기됐다. 커밋 2개, 작업 트리 클린, remote origin 있음. code-reviewer는 `git diff`를 실제 근거로 쓸 수 있다 (R-09).

## 확정된 결정

- **스코프(Application 수명)는 유지한다.** `GeckoRuntime`이 프로세스당 1회 제약. 바꾸는 것은 파일 위치·패키지뿐이다. 재론 금지 — 사용자가 이미 동의했다.
- **`AppContainer`는 제외를 유지한다.** `Application`·`packageManager`·`Dispatchers.Main.immediate`에 실제로 의존해 build.gradle.kts의 제외 기준("JVM 실행 자체가 불가능한가")을 **충족**한다. 위치가 바뀌어도 판정은 그대로이고, **패턴 경로만 새 위치에 맞춰 고친다** (R-05).
- **`AppBridgeHost`는 기준에 미달한다.** 안드로이드 타입 0개, 4라인/2분기. 이것이 F-2의 내용이다 (R-05).
- **화이트리스트는 impact-analyzer만 고친다.**

## 전문을 열어야 하는 경우

- **planner**: 패키지 위치를 정하기 전에 **R-01·R-02·R-03·R-04**. 두 선택지의 커버리지 귀결이 다르고 그 차이가 곧 계획의 필수 작업 항목이 된다.
- **planner / developer**: 새 파일 이름을 정하기 전에 **R-10** — 화이트리스트는 "파일명 = 클래스명" 전제다.
- **qa**: **R-04** — 4라인/2분기(`onFinishRequested` 등록/미등록 두 갈래)를 전부 덮어야 102/102 = 100%가 유지된다.
- **coverage-reporter**: **R-07·R-08** — 측정은 JAVA_HOME 지정 + `--rerun-tasks`.
- **code-reviewer**: **R-06** — 참조 갱신을 빌드 성공이 아니라 `git diff`로 대조하라.

## 미해결 / 이월

- **F-2**(AppBridgeHost 제외 근거 부재) = **이번 배치의 해결 대상.** 단 R-04의 짝(테스트 동반)을 지켜야 회귀 없이 닫힌다.
- **F-1**(assets `inputs.dir` 미선언) 범위 밖 — assets를 안 건드려 발현하지 않는다. `build.gradle.kts`가 화이트리스트에 있어 물리적으로는 고칠 수 있지만 **넓히지 말고 후속 배치로 넘겨라**(V9).
- **F-3**(AC-003-2 역주입) 이월 — 만료 조건인 `NativeBridgeHandler` 수정이 이번 배치에 없다.
- **F-4**(`coverage-baseline.json` 신설) 미착수 — 베이스라인이 산문에만 있어 이번에도 내가 직접 재측정해야 했다.

## 참조 지점 (grep 전수 — 전체 표는 작업 로그 §2)

수정이 필요한 곳은 **`App.kt`**(7·26·59·73·79·100·136·140·158행)와 **`build.gradle.kts:121·122`**(R-01 발현 지점)뿐이다.
`MainActivity.kt:129·201`은 타입 추론이라 손대지 않아도 컴파일되고(R-06), 70·122·125행은 주석이다.
`bridge/BridgeDispatcher.kt:59`의 `interface BridgeHost`와 테스트의 `FakeBridgeHost`는 **이동 대상이 아니다.**

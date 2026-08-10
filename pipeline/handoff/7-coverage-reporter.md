# coverage-reporter 핸드오프
- **판정**: **PASS** — 배치 02 종료. `enforce: false` 처리 완료.
- **전문**: `pipeline/coverage-report.md`
- **로그**: `doc/20260811/20260811_083542_coverage-reporter.md` ⚠️ 배치 02 로그는 두 날짜 디렉터리에 걸쳐 있다.

## 다음 배치가 반드시 알아야 할 것

1. **새 베이스라인** (출처: 방금 실행한 `clean jvmCoverageReport --rerun-tasks` XML 직접 파싱, `30 tasks: 30 executed`):
   **LINE 102/102 · BRANCH 20/20 · CLASS 15/15 · INSTR 626/655 · METHOD 49/59 · CXTY 59/69 · 테스트 51.** `<class>` 원소 29개.
   **이 값 대비 하락 금지.** LINE·BRANCH·CLASS가 100%라 **분모만 넓히면 즉시 회귀**다 — 제외 해제와 테스트는 같은 배치에서 짝으로 가라(R-04).
2. **베이스라인은 산문에만 있어 매 배치 재산출해야 한다(F-4).** `git archive HEAD | tar -x`로 스크래치패드에 전개하고 `local.properties`만 복사하면 빌드된다. **작업 트리를 건드리지 않고 베이스라인을 얻는 방법이다.**
3. **글롭 의미론이 두 개 공존한다 — 실행으로 확정(F-6).**
   - Ant PatternSet(`coverageExclusions`): `*`가 `/`를 **안 넘는다.** 전개본에 `…/*BridgeDispatcher*.class`를 주입해 단일 변수로 격리 → LINE 98/98 불변, `bridge/BridgeDispatcher` 분모 잔존.
   - guard hook(`guard-impact-scope.py:52` `fnmatch`): `*`가 `/`를 **넘는다.** `…/geckoviewtest/*AppBridgeHost.kt`가 `gecko/deep/`까지 ALLOW.
   → **화이트리스트를 "좁게 잡았다"고 믿고 설계하면 오탐한다.**
4. **`clean`을 반드시 붙여라.** `--rerun-tasks`만으로는 옛 위치 `.class`가 남아 수치가 흔들린다(R-P3). 실행 후 `N tasks: N executed` 확인.
5. **환경**: `JAVA_HOME=/Users/appdevloperteam/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home` (기본 JVM은 Java 8이라 설정 단계에서 죽는다).
6. **신규 3개 파일은 여전히 git 미추적(`??`)** — `git diff`가 못 보고 `git checkout --`도 실패한다. `git status`로 따로 보고, 변조 실험은 백업본+md5로 관리하라.

## 확정된 결정

- **두 조항을 각각 판정했다.** (a) `bridge/AppBridgeHost` **LINE 4/4 = 100%** ≥ 70%. (b) **6개 지표 하락 0건** — LINE·BRANCH·CLASS 유지, INSTR(95.44→95.57%)·METHOD(81.82→83.05%)·CXTY(84.38→85.51%) 상승. 상승 3건은 분자·분모가 **같은 값만큼** 늘어난 결과라 분모 조작이 아니다.
- **인계 보고(3자)와 불일치 0건.** 3자 일치가 재측정을 면제하지 않으므로 베이스라인 포함 전부 직접 측정했다(V7).
- **제외 범위는 확대가 아니다 — 집합 비교로 확정.** 11개 vs 10개: **추가 0건 · 삭제 1건(`AppBridgeHost*.class`) · 진부분집합 True.**
- **`AppContainer` 제외 유지 정당** — `Application`(`:22`)·`packageManager`(`:42·45·48`)·`Dispatchers.Main.immediate`(`:33`) 의존 실재. **`AppBridgeHost`는 안드로이드 타입 grep 0건**이라 기준 미달 → 해제가 옳다.
- **F-2 CLOSED.** 목록에서 빠짐 · 분모 실제 진입 · 테스트가 4라인·2분기·4메서드 전부 커버 — 세 조건 확인.
- **METHOD 4/4는 게이밍이 아니다 — 직접 변조로 확인.** 감싸는 setter 주입 시 **51건 중 1건만 FAIL**(`AppBridgeHostTest.kt:38` `assertSame`), 같은 케이스 `:33` `assertEquals`는 초록. 독립 반증 가능 → V1 충족. **`assertSame`을 삭제하지 마라.**
- **테스트 50 → 51인데 지표 불변은 정상이다.** 추가 케이스가 이미 덮인 라인만 지난다. **수치가 안 움직인다는 사실 자체가 수치 목적이 아님의 증거다.**
- **프로덕션·테스트 코드 수정 0건.** 변조 실험은 전부 스크래치패드 복사본에서 했다. 종료 시 실제 트리 재확인 — md5 **`e6bcc47f…`**(착수 시 동일), `git status` 불변, 제외 항목 10개.

## 전문을 열어야 하는 경우

- **impact-analyzer / planner**: **§11 후속 과제표** — 다음 배치 요구사항의 출발점.
- **글롭·화이트리스트 설계 시**: **§5**. **`private set`/API 축소 검토 시**: **§7**.
- **배치 03 coverage-reporter**: **§2**(베이스라인 재산출) · **§4**(제외 목록 집합 비교).

## 미해결 / 이월

- **아카이브 — 판단만 하고 실행하지 않았다.** `pipeline/*.md` + `pipeline/impact-report.json` → **`doc/archive/batch-02-refactor/`**로 **이동**(삭제 아님). `pipeline/handoff/`는 **유지**. 오케스트레이터 확인 후 처리.
- **신규**: **F-6**(글롭 의미론 공존) · **F-7**(`onFinishRequested` getter를 프로덕션이 안 읽음 → `private set` 검토, public API 변경이라 범위 밖).
- **이월**: F-1(assets `inputs.dir`) · F-3(AC-003-2 역주입) · F-4(`coverage-baseline.json`) · F-8(`interface BridgeHost` 분리) · F-9(`plan.md` 문언 오기 2건 — **다음 planner는 G2-b·G2-c 스니펫을 복사하지 말 것**) · F-10(프로덕션 주석 MINOR-2·3·4). **F-5는 R-09로 폐기.**

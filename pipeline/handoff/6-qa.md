# qa 핸드오프
- **판정**: **PASS** (테스트 51 / 실패 0 / skipped 0 · 프로덕션 결함 0건)
- **전문**: `pipeline/qa-report.md`
- **로그**: `doc/20260811/20260811_081818_qa.md`

## 다음 단계가 반드시 알아야 할 것

1. **커버리지 `clean` 재측정 완료 — 인계 보고와 불일치 0건.** (출처: 방금 실행한 `clean jvmCoverageReport --rerun-tasks` XML 직접 파싱, `30 tasks: 30 executed`로 UP-TO-DATE 회피 확인)
   **LINE 102/102 · BRANCH 20/20 · CLASS 15/15 · INSTR 626/655 · METHOD 49/59 · CXTY 59/69.**
   베이스라인(98/98 · 18/18 · 14/14 · 607/636 · 45/55 · 54/64) 대비 **6개 지표 하락 0건.**
2. **테스트는 50 → 51인데 6개 지표는 한 자리도 안 움직였다.** 추가 케이스가 이미 덮인 라인만 지나기 때문이다. **"테스트 +1인데 수치 동일"을 이상 징후로 오판하지 마라.**
3. **`AppBridgeHost.kt`·`AppContainer.kt`·`AppBridgeHostTest.kt`는 git 미추적(`??`)이다.** `git checkout --`가 `pathspec did not match…`로 **실패한다**(변조 실험 시 별도 백업본+md5 필수). **`git diff`도 이 신규 파일들을 못 본다 — `git status`로 따로 보라.**
4. **실기기 종료 판정에 `pidof`를 쓰지 마라.** `finish()`는 Activity만 끝내고 프로세스는 남는다(pid + `:gpu_…`/`:tab_…` 자식 생존 관측). **정본 신호는 `ActivityRecord` 소멸.**
5. **`dumpsys` 카운트가 35 아닌 21이면 잠금 화면을 의심하라.** `mWakefulness=Awake`인데도 잠겨 있어 21이 나왔고, 해제 후 **35로 일치**했다(V3).
6. **유사 이름 앱 `kr.co.chunjae.android.geckoviewtestapp`가 기기에서 실제 구동 중** — **`com\.example\.geckoviewtest/` 정확 필터 필수.**

## 확정된 결정

- **MINOR-1 수정 완료 — 리뷰어 결론을 옮겨 적지 않고 직접 변조 실험했다**(V7).
  A(커스텀 getter): 케이스 1이 **`:32`(`assertEquals`)**에서 `expected:<1> but was:<0>` FAIL, `assertSame`은 **도달조차 못 함** → 옛 주석 3번째 줄이 거짓.
  B(감싸는 setter): 케이스 1이 **`:38`(`assertSame`)**에서만 FAIL, `assertEquals`는 초록 → **실제 반증 경로.**
  참인 1~2번째 줄은 보존하고 3번째 줄만 교체. **`assertSame` 존치**.
- **케이스 1건 추가 — 근거는 커버리지가 아니라 실측된 회귀 공백.**
  `requestFinish`에 1회성 가드(`if (finished) return`)를 넣자 **50건 전부 PASS**. 기존 두 케이스는 매번 새 `host`로 한 번씩만 호출해 못 잡는다.
  현실 경로: `App.kt:49` `by lazy` → `AppContainer.kt:58` → **프로세스당 1개**가 `MainActivity:129` 등록 / `:201` 해제를 반복해 겪는다(화면 회전). 가드가 들어오면 회전 뒤 `appFinish`가 먹통.
- **초안 단정 1개(`assertEquals(1, oldCount)`)는 실험으로 기각·삭제.** `oldCount`를 2로 만드는 변조는 동시에 `newCount`를 0으로 만들어 **앞 단정에 먼저 걸린다** — 첫 실패 지점이 될 수 없는 장식 단정(V1).
- **V2 RED 2건**(각각 51건 중 **단 1건만** 빨감): RED-1 = 1회성 가드 → 신규 케이스만 FAIL `:73` / RED-2 = 감싸는 setter → 케이스 1만 FAIL `:38`.
  **프로덕션 변조 4회 전부 원복** — 최종 md5 `e6bcc47f…`(착수 시와 동일), `diff` 0줄, `git status` 목록 불변.
- **배선 생존 — 실기기 `appFinish` 3/3.** `ActivityRecord` **7 → 0**(developer 방식 `grep -c`로도 **35 → 0**), 종료 후 대소문자 무시 grep도 **0건**, 포커스 런처 복귀.
  G6-a `READY` / G6-b `versionName = 1.0.0` **3/3**(라운드마다 재기동해 전이 관측) / G6-c `UNKNOWN_FUNCTION` + 앱 생존 / G6-e `AndroidRuntime`·`FATAL`·`GeckoSession` E/F **각 0건**.
- **기존 48건 무영향을 기계 대조로 확인** — "빌드 성공"은 판정에 안 썼다(V1·R-06). 구 패키지 참조 0 · 와일드카드 import 0 · `internal` 0 · `FakeBridgeHost` 충돌 0 · 48건 전부 PASS. 두 `FakeBridgeHost`는 테스트 파일 내부 선언이라 이동과 무관.
- **수정 파일은 `AppBridgeHostTest.kt` 하나뿐.** 프로덕션 수정 0 · 화이트리스트 이탈 0 · 범위 확대 0.

## 전문을 열어야 하는 경우

- **coverage-reporter**: **§6**(수치표 대조), **§4**(추가 케이스가 지표를 안 움직이는 이유).
- **실기기 재확인 시**: **§8** — 잠금 화면 오측정, `pidof` 부적합, 유사 이름 앱 필터, E 레벨 41건의 정체(전부 GeckoView·시스템 기인).
- **다음 배치 developer**: **§3·§4의 실험 A~D** — 접근자 동작과 반증 가능성 판정법.

## 미해결 / 이월

- **MINOR-2·3·4** — 전부 **프로덕션 주석이라 QA 범위 밖.** 미개입·이월.
- **F-1**(assets `inputs.dir`) 미개입(`build.gradle.kts` QA 변경 **0줄**). **`interface BridgeHost`**·**`private set`**도 범위 밖.
- **`onFinishRequested`의 getter는 여전히 아무도 읽지 않는다.** 추가한 케이스도 getter를 읽지 않는다 — 고정한 계약은 "재등록이 새 콜백으로 간다"이다.

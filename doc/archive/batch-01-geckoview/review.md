# 코드 리뷰 (파이프라인 5단계)

## 개정 이력

| 판 | 일시 | 판정 | 내용 |
|:--|:--|:--|:--|
| **rev.2 (재리뷰)** | 2026-08-05 13:49 | **APPROVED** | developer 재작업(8개 파일) 검증. MAJOR-1 **해소 확인**. 신규 MINOR 2건(비차단). → **§10 이하** |
| rev.1 (1차 리뷰) | 2026-08-05 13:29 | CHANGES_REQUESTED | MAJOR 1건 + MINOR 9건. AC-011-3·AC-002-2 수용 판정. → **§1~§9** |

> **rev.1 본문은 지우지 않고 그대로 둔다.** 아래 §1~§9는 1차 리뷰 시점의 기록이며, 그 이후의 변화는 §10 이하에 적는다.
> §1의 후속 조치(requirements 문언 정정)는 rev.3에서 완료됐고 §11에서 대조했다.

---

# rev.1 — 1차 리뷰 (2026-08-05 13:29) · CHANGES_REQUESTED

- **일시**: 2026-08-05 13:29
- **대상**: developer가 완료한 plan.md rev.2 §4 Step 0~9 구현 (소스 39개)
- **입력**: `doc/20260805/20260805_131319_developer.md`, `pipeline/plan.md` rev.2, `pipeline/evaluation.md`, `pipeline/requirements.md` rev.2, `pipeline/impact-report.json`, 규칙 5종
- **검증 환경**: 실기기 `R3CN60L0QMT`(SM-G981N, **API 33**), JDK = Android Studio JBR

## 판정: **CHANGES_REQUESTED**

MAJOR **1건**(2개 파일). 그 외 MINOR 9건.

**MAJOR 1건은 코드 로직이 아니라 주석이다.** 구현 자체는 아키텍처·검증 정직성·화이트리스트 전 항목을 통과했고, developer가 FAIL로 올린 2건도 아래에서 **둘 다 수용**으로 판정했다. 재작업 범위는 **주석 2곳 수정**이며 그 외에는 developer가 손댈 것이 없다. MINOR는 전부 선택 사항이되, MINOR-6은 함께 처리하기를 강하게 권한다(같은 파일군).

---

## 1. developer가 넘긴 2건 — 판정

### ① AC-011-3 (뒤로가기로 Activity가 소멸하지 않는다) → **수용. requirements.md 문언 정정 필요. 구현 변경 불요.**

**직접 재현했다(V7 — developer 보고를 인용하지 않았다).**

| 실험 | 조작 | 관측값 (내가 직접 실행) |
|:--|:--|:--|
| A | 앱 기동 → `index.html`(브리지 `READY`) 상태에서 `KEYCODE_BACK` | `state=STOPPED stopped=true **finishing=false**`, 태스크 `visible=false visibleRequested=false`, 프로세스 4개 생존 |
| B | naver.com에서 `KEYCODE_BACK` | `index.html` 재렌더 + `state=**RESUMED** finishing=false` (포그라운드 유지) |
| C | `index.html`에서 `appFinish` 버튼 탭 (명시적 `finish()`) | `MainActivity` ActivityRecord **grep 0건**(완전 소멸), 이후 3초 logcat `FATAL EXCEPTION`/`Fatal signal` **0건** |

**판정 근거:**
- 실험 C가 `finish()` 경로의 정상 동작을 증명한다 → **구현 결함이 아니다.** developer의 진단은 맞다.
- 실험 A와 B가 갈린다는 것이 결정적이다. 콜백이 항상 켜져 있었다면 A에서도 포그라운드에 남았어야 하고, 항상 꺼져 있었다면 B에서 `index.html`로 못 돌아왔어야 한다. **`canGoBack` 상태 관리는 실제로 동작한다 → AC-011-4의 의도는 충족된다.**
- 원인은 **API 31+ 에서 루트 런처 Activity의 뒤로가기 기본 동작이 `finish()`에서 태스크 백그라운드 이동으로 바뀐 것**이다. 실험 A의 `rootOfTask=true` + `finishing=false` + 태스크 `visible=false`가 이 동작과 정확히 일치한다. 기기는 API 33.
- REQ-011 원문 *"웹 히스토리가 없으면 뒤로가기는 **통상대로** 앱을 종료한다"* — API 33에서 "통상"이 곧 태스크 백그라운드 이동이다. 강제로 `finish()`를 부르면 오히려 플랫폼 표준에서 이탈한다. **손대지 않은 developer의 판단이 옳다.**

**추가로 발견한 사실 (developer 로그에 없다 — MAJOR 지적의 근거이기도 하다):**
> 이 앱의 `minSdk`는 **26**이다. **API 26~30 기기에서는 구 동작이 그대로 적용되어 뒤로가기가 실제로 Activity를 종료한다.** 즉 AC-011-3은 **문언 그대로도 API 30 이하에서는 PASS, API 31+ 에서는 FAIL**이다. 이것은 "언제나 실패하는 기준"이 아니라 **버전에 따라 갈리는 기준**이며, 수용 기준·주석·QA 절차 모두 이 분기를 명시해야 한다.

**후속 조치(코드 아님):** `requirements.md` AC-011-3을 아래 취지로 정정할 것을 권고한다. 주체는 planner-analyzer이며, **설계 변경이 아니므로 evaluator 재승인은 불요**하다.
> AC-011-3(정정안): `index.html` 상태(웹 히스토리 없음)에서 뒤로가기를 누르면 **콜백이 비활성이 되어 플랫폼 기본 동작이 수행된다** — API 31+ 에서는 태스크가 백그라운드로 이동하고(`dumpsys`의 `finishing=false` + 태스크 `visible=false`), API 30 이하에서는 Activity가 종료된다. AND 같은 구간 logcat에 `FATAL EXCEPTION`이 없다. AND 대조로 `appFinish`(명시적 `finish()`) 경로에서는 ActivityRecord가 소멸한다.

### ② AC-002-2 (`androidx.compose.*` 아티팩트 0건이 아니다) → **ⓐ 현상 수용. 의존성 변경 불요 → evaluator 재승인도 불요. requirements.md 문언 정정 권고.**

**dex를 직접 다시 열어 확인했다(V7 — developer 보고를 믿지 않았다).** `dexdump`로 APK의 7개 dex 전수 조사:

```
Landroidx/compose/runtime/Immutable;
Landroidx/compose/runtime/Stable;
Landroidx/compose/runtime/StableMarker;
Landroidx/compose/runtime/annotation/FrequentlyChangingValue;
Landroidx/compose/runtime/annotation/RememberInComposition;
Landroidx/compose/runtime/annotation/R;
→ 정의된 androidx/compose 클래스 = 6개. 전부 애너테이션 타입 + 리소스 R.
→ compose.ui / foundation / material / runtime 본체 = 0건.
```

- 의존성 트리 재확인: `debugRuntimeClasspath`에 `androidx.compose.runtime:runtime-annotation:1.9.0`(+`-android`) **1종만**. 유입 경로 `androidx.activity(L-03, 1.13.0) → androidx.navigationevent:navigationevent:1.0.0 → androidx.compose.runtime:runtime-annotation:1.9.0`도 트리에서 직접 확인했다. **developer 보고와 일치.**
- `app/build.gradle.kts`에 `buildFeatures.compose` 미설정, Compose 컴파일러 플러그인 미적용 — 확인.

**판정 근거:**
- REQ-002의 **의도**("Compose를 UI로 쓰지 않는다")는 **완전히 충족**된다. 화면은 `activity_main.xml` + ViewBinding이고 Compose 툴킷은 한 클래스도 들어오지 않았다.
- AC-002-2의 **문언**("아티팩트 0개")은 미충족이다. 그러나 이 문언은 **애너테이션 전용 아티팩트가 androidx 전반의 전이 의존으로 퍼지는 현실을 반영하지 못한 기준**이다.
- ⓑ(`exclude group="androidx.compose.runtime"`)와 ⓒ(activity 버전 하향)는 **승인된 의존성 결정(L-03)을 바꾸는 것이라 evaluator 재승인이 필요**하고, 얻는 것은 "문언 충족"뿐인데 잃는 것은 navigationevent 동작 리스크(ⓑ) 또는 검증되지 않은 버전 조합(ⓒ)이다. **비용이 이익을 넘는다.**

**후속 조치(코드 아님):** AC-002-2를 아래 취지로 정정 권고. 주체는 planner-analyzer.
> AC-002-2(정정안): Compose UI 툴킷 아티팩트(`androidx.compose.ui` / `foundation` / `material` / `runtime` 본체)가 0개이고, Compose 컴파일러 플러그인 미적용, `buildFeatures.compose` 미설정이다. **androidx 전이 의존으로 들어오는 애너테이션 전용 아티팩트(`androidx.compose.runtime:runtime-annotation`)는 예외로 한다** — 판정은 `dexdump`로 APK에 `androidx/compose` **비(非)애너테이션 클래스가 0개**임을 확인하는 것으로 대체한다.

**단, 코드 쪽에 남길 것이 하나 있다 → MINOR-6 참조.**

---

## 2. 지적 사항

### MAJOR-1 — 뒤로가기 기본 동작을 설명하는 주석 2곳이 실측과 정반대다

**파일·라인:**
1. `app/src/main/java/com/example/geckoviewtest/MainActivity.kt:39-44` (`backCallback` KDoc)
   > `* - false → 콜백을 건너뛰고 시스템 기본 동작(Activity 종료)이 일어난다(AC-011-3)`
2. `app/src/main/java/com/example/geckoviewtest/gecko/AppNavigationDelegate.kt:11-12`
   > `* 갈 곳이 있으면 페이지를 뒤로, 없으면 앱을 종료(AC-011-3).`

**문제:** 위 §1①의 실험 A에서 **`finishing=false`로 Activity가 살아 있음을 직접 관측**했다. 두 주석은 targetSdk 36 / 검증 기기 API 33에서 **거짓**이다.

**왜 MINOR가 아니라 MAJOR인가:**
- `comment-style.md` 리뷰 심각도 — *"코드와 어긋난 주석: MINOR (어긋난 주석이 **오해를 유발하면 MAJOR 후보**)"*. 이 주석은 **`(AC-011-3)`이라고 수용 기준 번호까지 달아** 그 기준이 충족된다고 단정한다. 다음에 이 파일을 읽는 사람은 파이프라인이 방금 FAIL로 올린 바로 그 사실을 정반대로 학습한다.
- 이번 배치에서 AC-011-3을 **"수용"으로 판정했기 때문에** 코드 주석이 이 사실의 유일한 항구적 기록이 된다. 지금 고치지 않으면 이 발견은 파이프라인 문서에만 남고 코드에서는 사라진다.
- 더구나 `minSdk = 26`이라 **동작이 API 버전에 따라 실제로 갈린다.** 단순히 "틀린 문장"이 아니라 **지원 범위 안에서 두 갈래로 동작하는 사실이 통째로 누락**돼 있다.

**어떻게 고칠 것인가 (두 곳 모두):** `isEnabled = false`일 때의 동작을 아래 세 가지가 다 드러나게 다시 쓸 것.
1. 콜백을 건너뛰고 **플랫폼 기본 동작**이 수행된다(우리 코드는 여기서 끝난다).
2. 그 기본 동작이 **API 31+ 에서는 태스크 백그라운드 이동, API 30 이하(minSdk 26)에서는 Activity 종료**다.
3. **앱을 확실히 끝내는 경로는 `appFinish` → `MainUiEvent.Finish` → `finish()`이며 그쪽은 실제로 ActivityRecord가 소멸한다**(대조 실험 근거).

`(AC-011-3)` 참조는 유지하되 "이 기준의 문언은 API 31+ 동작을 반영하지 못해 정정 대기 중"임을 한 줄로 덧붙일 것.

---

### MINOR (9건 — APPROVED를 막지 않는다)

| # | 파일·라인 | 내용 | 권고 |
|:--|:--|:--|:--|
| MINOR-1 | `App.kt:67-77` + `MainActivity.kt:150-171` | `ensureBuiltIn`이 실패하면 `bridgeExtension.await()`가 `lifecycleScope.launch` 안에서 예외를 던져 **처리되지 않은 코루틴 예외로 앱이 죽는다.** `MainUiState`에 오류 표현이 없어 화면에 사유가 남지 않는다. 계획이 규정하지 않은 경로라 이번 배치의 이탈은 아니다 | `loadExtensionPage`에 `try/catch` + logcat + 화면 표시. 범위를 넓히기 싫으면 **다음 배치 요구사항으로 넘겨라**(V9) |
| MINOR-2 | `bridge/NativeBridgeHandler.kt:51` | `catch (e: Exception)`이 `CancellationException`까지 잡아 **코루틴 취소 전파를 끊는다** | `catch (e: CancellationException) { throw e }`를 앞에 두거나 `catch (e: Exception)`을 좁힐 것 |
| MINOR-3 | `MainUiState.kt:18-23` | plan D-06이 명시한 `lastBridgeResult` 필드가 없다. **판단 자체는 옳다**(브리지 결과는 웹 페이지가 표시하므로 안드로이드 UiState에 두면 죽은 필드가 된다). 다만 developer 로그의 "계획과 다른 점"에 기록되지 않았다 | 코드 변경 불요. 다음 로그에 이탈로 기록 |
| MINOR-4 | `MainActivity.kt:51-53`, `188` | plan D-11은 *"컨테이너가 `Dispatchers.Default`를 넘긴다"*인데 실제로는 `MainActivity`가 `Dispatchers.Default`와 `NAVER_URL`을 직접 넘긴다. `AppContainer`를 거치지 않는다. 생성자 주입과 테스트 교체 가능성은 그대로라 기능적 영향은 없다 | 배선을 `AppContainer`로 옮기거나, 현 배선이 의도임을 주석 한 줄로 남길 것 |
| MINOR-5 | `bridge/BridgeDispatcherTest.kt:95` | 테스트명 `Repository가 예외를 던져도 결과 타입은 유지된다`가 실제 단정(예외가 **전파되어 `handle`이 실패한다**)과 정반대로 읽힌다. 또 `FakeAppInfoRepository(version = null)`이 "null이면 던진다"는 뜻으로 쓰여 프로덕션 의미(null → `UNKNOWN` 폴백)와 충돌한다 | 이름을 `Repository 예외는 삼켜지지 않고 그대로 전파된다`로. Fake는 `throws` 플래그를 따로 둘 것 |
| MINOR-6 | `gradle/libs.versions.toml:17` 또는 `app/build.gradle.kts:81-83` | **AC-002-2 유입 경로가 코드 어디에도 기록돼 있지 않다.** 다음 사람이 의존성 트리에서 `androidx.compose.runtime`을 보고 놀라 `exclude`로 "고치다가" navigationevent를 깬다 | activity-ktx 선언 옆에 한 줄: *"activity 1.13.0 → navigationevent 1.0.0 → compose.runtime:runtime-annotation(애너테이션 전용, Compose UI 아님). REQ-002 위반이 아니며 exclude하지 말 것"* — **MAJOR-1과 함께 처리 권장** |
| MINOR-7 | `.gitignore` | 빌드 산출물(`app/build/`, `.gradle/`, `.kotlin/`, `local.properties`, `.idea/`)이 하나도 없다. 현재 git 저장소가 아니라 즉시 피해는 없으나 `git init` 시 **198 MB APK가 커밋 대상**이 된다. 이 파일은 화이트리스트 안이다 | 표준 Android `.gitignore` 항목 추가 |
| MINOR-8 | `assets/messaging/page-bridge.js:78-111` | 검증 프로브(고정 배지 + 모든 페이지 로드 시 자동 `getVersionName` 호출)가 **빌드 타입과 무관하게 항상 실행**된다. requirements REQ-010은 *"프로덕션 코드에 포함, **debug 빌드 한정 권장**"*이라 적었다. release가 스코프 아웃이라 이번 배치의 결함은 아니다 | release 도입 배치의 **필수 선행 작업**으로 기록. 지금은 손대지 말 것 |
| MINOR-9 | `App.kt:37-38` | `by lazy` 근거 주석 *"onCreate()에서 미리 만들지 않는 이유는 런타임 생성이 무겁기 때문이다"* — 실제 첫 접근은 `MainActivity.onCreate`(메인 스레드, `session.open(app.geckoRuntime)`)이므로 **비용이 회피되지 않고 옮겨질 뿐**이다 | 근거를 정확히 하거나(예: GeckoRuntime이 필요 없는 프로세스 시작에서 비용을 내지 않는다) 삭제 |

---

## 3. 화이트리스트 전수 대조 — **위반 0건 (내가 직접 셌다)**

hook의 fnmatch 로직을 재현해 **실제 파일 트리 전수**를 `impact-report.json`과 대조했다(Bash 생성 파일 포함, 가드가 검사하지 못하는 경로 포함).

| 항목 | 값 |
|:--|:--|
| 대조 대상 파일 (빌드 산출물·`doc/`·`pipeline/` 제외) | **42** |
| 화이트리스트 내 | **40** |
| 화이트리스트 밖 | **2** — `.claude/settings.json`, `initRequire.md` |
| **위반** | **0건** — 밖의 2개는 **파이프라인 착수 전부터 있던 파일**이다(mtime 2026-08-04 12:47 / 13:23 < impact-report `created_at` 14:01:45). developer가 만들지도 고치지도 않았다 |

- **`app/src/main/kotlin/` 부재 확인** — `app/src/main/` = `AndroidManifest.xml`, `assets`, `java`, `res`. 소스 루트는 `java/` 단일. impact-report `risk_notes` 마지막 항목의 금지사항 준수.
- **`.claude/**` 변경 확인** — `.claude/settings.json` 1개뿐이고 내용은 `enabledPlugins` 4줄, mtime은 착수 전. **가드 밖 영역의 무단 변경 0건.**
- **developer 보고와의 차이**: developer는 "소스 **40개** 전수 대조"라 적었으나 실제 developer 산출 파일은 **39개**다(화이트리스트 내 40개 중 `.gitignore`는 착수 전 파일). developer 로그의 자체 목록을 합산해도 39다(섹션 라벨 "빌드/설정 (9)"가 실제 10, "Kotlin (11)"이 실제 12). **결론(위반 0건)에는 영향 없는 계수 착오다.**

---

## 4. 필수 주석 4건 — **전부 존재하고 내용도 정확하다**

읽고 내용까지 대조했다(있기만 한 것을 통과시키지 않았다).

| # | 위치 | 판정 |
|:--|:--|:--|
| 1 | `background.js:1-23` | **정확.** *"이 파일을 거치는 구조는 '우회'가 아니라 **필수**"*가 박스로 강조돼 있고, #220의 적용 범위를 *"content script에서 `sendNativeMessage`를 **직접** 부르는 경우"*로 정확히 한정했다(plan §2.10의 축소 정정과 일치). 두 경로 다이어그램과 *"코드로는 막을 수 없고 이 주석이 유일한 방어 수단"*까지 있다 |
| 2 | `content.js:1-27` | **정확.** isolated world + **Xray vision**을 이름으로 짚고, *"`window.NativeBridge = {...}`로 대입해도 페이지 세계 JS는 볼 수 없다"*는 결과까지 적었다. *"page-bridge.js를 없애면 겉보기에는 동작하는 것처럼 보이지만 실제 웹페이지에서는 브리지를 쓸 수 없다"* — 거짓 그린의 기전을 정확히 서술 |
| 3 | `MainActivity.kt:153-166` | **정확.** 명시적으로 *"결정적인 이유는 크래시 우려(mozilla/geckoview#199)가 **아니라** **매치 패턴 제약**"*이라 적고, 지원 스킴 목록(http/https/ws/wss/ftp/data/file)과 *"`resource://`는 그 집합에 없어 manifest 자체가 거부된다"*까지 있다. **근거를 크래시로 적지 않았다** |
| 4 | `index-page.js:1-10` | **정확.** 확장 페이지 기본 CSP `script-src 'self' 'wasm-unsafe-eval'`와 **실제로 겪은 logcat 원문**(`Content-Security-Policy: The page's settings blocked an inline script (script-src-elem)`)을 함께 박았다. *"외부 파일로 두는 것은 취향이 아니라 제약"* |

**그 외 `comment-style.md` 전반 — 충족.**
- 한글 주석: 프로덕션·테스트 전 파일 한글. 위반 0건.
- 파일/클래스 상단 KDoc: 프로덕션 12개 Kotlin 파일 + 7개 자산 전부 존재. 데이터 흐름을 명시한 것이 다수.
- Android 개념 첫 등장 설명: ViewModel / StateFlow / `viewModelScope` / `lifecycleScope` / `repeatOnLifecycle` / ViewBinding / `by lazy` / BuildConfig / `@Volatile` / GeckoView / WebExtension / native messaging / `GeckoResult` / `ensureBuiltIn` / `suspendCancellableCoroutine` / `Deferred` / 델리게이트 — **전부 첫 등장 지점에 한 줄 설명이 있고 파일마다 반복하지 않았다.**
- **테스트 T1~T5 — 충족.** T1: 4개 스위트 전부 "보장하는 것 / **보장하지 않는 것**"을 분리해 적었고, 특히 `BridgeProtocolTest`·`BridgeDispatcherTest`가 *"JS 쪽 목록과 일치하는지는 여기서 알 수 없다"*를 명시한 것은 모범이다. T2: `Dispatchers.setMain` / Turbine `test{}` / `runTest` / `UnconfinedTestDispatcher` 모두 첫 등장 설명 있음. **T5 위반 없음** — 31케이스 중 자명한 케이스(`onPageStart는 로딩을 켠다` 등)에는 주석이 없고, 주석이 붙은 케이스는 전부 "이 케이스가 없으면 어떤 회귀가 조용히 통과하는가"를 적었다. **삭제를 요구할 노이즈 주석 0건.**
- 어긋난 주석: **MAJOR-1의 2건**과 MINOR-9의 1건. 그 외 없음.

---

## 5. architecture.md 대조 (D-01~D-13) — **전 항목 충족**

계획서가 아니라 **실제 코드**로 확인했다.

| 항목 | 확인 방법 | 결과 |
|:--|:--|:--|
| **ViewModel 오염** | `MainViewModel.kt` 전문 통독 + `grep "android\.|androidx.activity|View"` | **누출 0건.** import는 `androidx.lifecycle.*` + `kotlinx.coroutines.*`뿐. `Context`·View·`OnBackPressedCallback`·`GeckoSession` 전무 |
| **D-02 Repository 격리** | `AppInfoRepositoryImpl.kt` | **실질적이다.** `versionNameProvider: () -> String?` 람다만 받는다. `PackageManager` 등장은 `App.kt:113-129` **단 한 곳** |
| **D-03 일회성 이벤트** | `MainUiState.kt:32-41`, `MainViewModel.kt:53-54` | `Channel(BUFFERED)` + `receiveAsFlow()`. `appFinish`·`Navigate`·`NavigateBack`이 상태가 아닌 이벤트. 테스트 `이벤트는 구독자가 붙기 전에 발행돼도 유실되지 않는다`가 이 결정을 고정한다 |
| **D-04/D-05 소유권** | `App.kt:40-51`, `MainActivity.kt:36,62-65` | `GeckoRuntime`은 Application `by lazy`, `GeckoSession`은 Activity. ViewModel은 어느 쪽도 갖지 않는다 |
| **D-06 단일 UiState + StateFlow** | `MainUiState.kt`, `MainActivity.kt:117-124` | 단일 data class + `repeatOnLifecycle(STARTED) { uiState.collect { render(it) } }`. (`lastBridgeResult` 누락 → MINOR-3) |
| **D-07 로딩의 진실의 원천** ← *plan이 지목한 중점 확인 대상* | `grep -rn "isVisible\|visibility" app/src/main/java app/src/main/res/layout` | **대입 지점은 `MainActivity.kt:134`의 `render()` 안 1곳뿐.** `gecko/` 델리게이트 2개는 View 참조가 0건이고 콜백만 위로 전달한다. `activity_main.xml:43`의 초기값 `gone`은 선언이지 대입이 아니다. **델리게이트가 `progressBar.visibility`를 직접 건드리는 구조가 아니다** |
| **D-08 콜백 소유** | `MainActivity.kt:45,91-93,135` | Activity가 `OnBackPressedCallback`을 소유. 콜백 본문은 `viewModel.onBackPressed()` 호출뿐이고 `session.goBack()`은 `handleEvent`가 부른다 |
| **D-09 `GeckoResult` 어댑터** | `gecko/GeckoResultExt.kt` + `grep "\.poll("` | 변환은 이 파일 1곳. `suspendCancellableCoroutine` + `invokeOnCancellation { cancel() }`. **`poll()` 0건** |
| **D-10 스코프** | `App.kt:106` + `grep GlobalScope` | `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)`. **`GlobalScope` 0건**(주석 언급 1건뿐), `runBlocking` 프로덕션 0건 |
| **D-11 Dispatcher 주입** | `MainViewModel.kt:33,109`, `BridgeDispatcher.kt:28,31` | **주입만 받고 안 쓰는 "장식"이 아니다.** `viewModelScope.launch(dispatcher)`와 `withContext(dispatcher)`로 **실제 사용**된다. 두 클래스 모두 기본값 없음 |
| **D-12 버전 카탈로그** | `libs.versions.toml`, `app/build.gradle.kts` | 리터럴 버전 문자열 0건. `jacoco.toolVersion`도 `libs.versions.jacoco.get()` |
| **§6.1 승인 의존성 11건** | `app/build.gradle.kts` dependencies 전수 | geckoview / activity-ktx / appcompat / constraintlayout / lifecycle-viewmodel-ktx / lifecycle-runtime-ktx / serialization-json / junit / coroutines-test / turbine / jacoco = **승인 목록과 정확히 일치. 승인 밖 추가 0건** |
| **D-13 문자열 리소스** | `grep 'android:text="[^@]'` | **하드코딩 0건.** `app_name`/`btn_naver`/`loading` 3개 전부 `strings.xml`. `index.html` 내부 텍스트는 D-13이 대상 외로 결정한 웹 리소스 — 올바르게 적용됨 |
| **금지 사항** | `grep '!!'` / 빈 catch 검사 | **`!!` 0건.** 빈 catch 0건 — `content.js:97`은 `ranInPageWorld = false` 할당, `App.kt:124`는 null 반환 + 사유 주석, `GeckoResultExt.kt:60`은 `completeExceptionally` |
| **컴파일 경고** | `compileDebugKotlin --rerun-tasks` | 경고 **0건** |

---

## 6. 검증 정직성 (verification-honesty.md)

### V1 — 대리 신호 아님. **G4-c는 진짜 신호다(코드로 확인).**

`[PAGE_WORLD]` 마커의 구조를 코드로 추적했다:
1. `page-bridge.js:89` — `window.__bridgeProbeRanInPageWorld = true`. 이 파일은 `content.js:37-43`이 `<script src>`로 **페이지에 끼워 넣은** 파일이라 페이지 세계에서 실행된다.
2. `content.js:96` — `window.wrappedJSObject.__bridgeProbeRanInPageWorld === true`일 때만 마커를 붙인다. `wrappedJSObject`는 **격리 세계에서 페이지 세계 window를 읽는 Firefox 전용 통로**다.
3. 따라서 **page-bridge.js가 격리 세계에서 돌았다면 페이지 세계 window에는 그 플래그가 없어 마커가 붙지 않는다.** 프로브 신호(`direction: "probe"` postMessage)는 어느 세계에서든 도착하지만 **마커 판정은 그것과 독립**이다.

→ **격리 세계에서만 동작해도 겉보기 성공이 되는 거짓 그린은 이 구조로 차단된다.** V1이 요구하는 "각각 독립적으로 깨질 수 있는 AND"를 만족한다.

**실기기 재현(내가 직접):** naver.com 로드 → uiautomator dump에 `text="versionName = 1.0.0 [PAGE_WORLD]"` **AND** 같은 덤프에 네이버 콘텐츠(`NAVER`, `AI 검색`, 메뉴 텍스트) 정상 렌더 = G4-a·b·c·e 동시 성립. 값 `1.0.0`은 `dumpsys package … versionName=1.0.0`과 **문자 단위 일치**.

### V2 — RED 확인 4건. **원복 완전함을 코드로 확인.**

developer가 깨뜨렸다고 보고한 4지점을 현재 코드에서 직접 읽었다: `BridgeProtocol.kt:52` `explicitNulls = false` ✓, `BridgeDispatcher.kt:45` `ErrorCode.UNKNOWN_FUNCTION` ✓, `AppInfoRepositoryImpl.kt:20` `?: UNKNOWN` ✓, `MainViewModel.kt:73` `isLoading = false` ✓. **4건 전부 원상태.** 대응 테스트 4건도 현존한다.

### V6 — 알려진 결함을 초록으로 덮지 않았다.

AC-011-3·AC-002-2를 **FAIL로 명시 보고**하고 판정을 넘긴 것은 올바른 처리다. `@Ignore`로 숨기거나 기준을 완화해 통과시킨 흔적 없음(테스트 31건 중 skip **0건**).

### V7 — developer 보고값 재확인. **불일치: 계수 착오 1건뿐.**

| 항목 | developer 보고 | **내 재측정** | 판정 |
|:--|:--|:--|:--|
| `gradle-wrapper.jar` | 46,175 B | **46,175 B** (sha256 `b3a875dd…`) | 일치 |
| 매니페스트 `<service>` | 89개 | **89개** (`merged_manifest/debug/…/AndroidManifest.xml`) | 일치 |
| `adb install -r` | 6.845초 | **6.952초** (내가 다시 설치) | 정합 (evaluator 6.872초와 같은 대역) |
| 커버리지 | LINE 100% | **`jvmCoverageReport --rerun-tasks` 강제 실행 → LINE 98/98 = 100.0%, 미달 클래스 0건** | 일치 |
| 테스트 케이스 | 31건 | **31건, 실패 0, skip 0** (test-results XML 4개 집계) | 일치 |
| APK | 198,619,378 B | **198,619,378 B** | 일치 |
| ABI | arm64-v8a 단일 | **`lib/` = `arm64-v8a` 한 줄, .so 13개** | 일치 |
| `androidx.compose` dex 심볼 | 6개, 전부 애너테이션 | **6개** (`dexdump` 전수) | 일치 |
| 소스 파일 수 | **40개** | **39개** | **불일치 — 계수 착오. 결론(위반 0건)에는 영향 없음** |

캐시된 `up-to-date`를 근거로 쓰지 않았다 — 커버리지·컴파일 모두 `--rerun-tasks`로 강제 실행했다.

### 임시 코드 잔존 — **4건 전부 원복 확인 (직접 grep)**

| 임시 변경 | 현재 상태 |
|:--|:--|
| naver URL → example.com | `MainActivity.kt:188` `const val NAVER_URL = "http://naver.com"` ✓ |
| `versionName` → `7.7.7-probe` | `app/build.gradle.kts:30` `versionName = "1.0.0"` ✓ (기기 `dumpsys`도 1.0.0) |
| `sendNativeMessage` 주석 처리 | `background.js:71` 활성 코드로 존재 ✓ |
| 배지 AC-010-5 검증용 `onclick` | `page-bridge.js`에 `onclick` 0건 ✓ |

추가 전수 검색(`7.7.7-probe` / `10.255.255.1` / `AC-010-5 검증용` / `TODO` / `FIXME` / `임시`): **잔존 0건.** 비행기모드 `airplane_mode_on = 0` 확인.

---

## 7. C-1~C-8 반영 대조 (evaluator가 위임한 것) — **8건 전부 반영, developer도 준수**

| 조건 | plan.md rev.2 반영 | developer 준수 |
|:--|:--|:--|
| **C-1** [필수] | ✓ `plan.md:343-344`에서 150 MB 규칙 **폐기**를 명시. ABI 목록 판정 명령이 `:335`에 있음 | ✓ **크기가 아니라 ABI 목록으로 판정했고 APK 198 MB대를 실패로 처리하지 않았다.** 크기는 "산출 기록"으로만 남겼다 |
| **C-2** [필수] | ✓ "마비" 서술 **0건**. `plan.md:90`에 실측 6.872초, 근거는 "483.1 MiB" | ✓ 설치 시간을 숫자로 기록 |
| **C-3** [필수] | ✓ §0.2 신설 — planner 미측정분과 evaluator 대행분을 분리 표기 | ✓ 인계값을 재측정 후 인용 |
| **C-4** [권고] | ✓ G2-d·G3-d가 게이트에서 빠지고 "wire 포맷 판정(구 G2-d)" / "cleartext 판정(구 G3-d)" 기록 항목으로 이동(`:372`, `:392-393`) | ✓ 게이트 표가 아닌 "산출 기록"으로 보고 |
| **C-5** [권고] | ✓ `plan.md:715` — "이 배치의 `abiFilters`(arm64-v8a 단일) 설정에서는" | — |
| **C-6** [필수] | ✓ H-1·H-2·H-3이 `plan.md:222-224`에 조건표로 | ✓ **3건 전부 코드로 확인했다** (아래) |
| **C-7** [필수] | ✓ `plan.md:690-691` 폴백 1·2안 행에 "planner 재실행 + evaluator 재승인 필요" 명기 | ✓ **폴백 미발동.** 확인함 — 아래 |
| **C-8** [권고] | ✓ `plan.md:118`·`:538` "최신 stable" → "검증 완료된 고정 버전" | ✓ |

### C-6 — 오리진 훅이 "전체 허용"으로 출고되는가 → **충족 (코드 직접 확인)**

`background.js:55-58`:
```js
function isOriginAllowed(sender) {
  // 기본 분기는 무조건 통과한다. 사용자가 요구한 범위를 좁히지 않기 위해서다.
  return true;
}
```
- **H-1**: 분기가 하나도 없이 `return true`. **훅이 꺼진 상태로 출고된다.** 사용자 결정 A-08 위반 아님.
- **H-2**: G4-d(제2 사이트)를 developer가 example.com으로 통과시켰고, 나는 **naver.com에서 `[PAGE_WORLD]` 배지를 직접 재현**했다. 훅이 존재하는 상태에서 외부 사이트 브리지가 살아 있다.
- **H-3**: `background.js:44-54`에 한글 주석으로 *"기본값은 '전체 허용'이며 이것을 켜는 것은 사용자 결정 사항이다(requirements.md §5.1, 가정 A-08)"* + *"켠 상태로 출고하면 REQ-010을 뒤집는 것이므로 금지다(plan.md H-1)"* 존재.

### C-7 — 폴백 미발동 보고가 맞는가 → **맞다 (구조로 확인)**

폴백 1안(assets → `file://` 복사)이 발동했다면 `matches`에 `file:///*`가 들어가고 복사 로직이 생겼어야 한다. `manifest.json:26`의 `matches`는 `["http://*/*", "https://*/*"]`뿐이고 파일 복사 코드는 0건이다. 폴백 2안(`Loader().data(html,…)` + `bridge-client.js` 인라인 병합)이 발동했다면 `index.html`에 JS가 인라인됐어야 하는데 `<script src="bridge-client.js">`로 분리돼 있다. `MainActivity.kt:167`은 `extension.metaData.baseUrl`을 그대로 쓴다 — **1안이 폐기했어야 할 바로 그 API다.** → **폴백 발동 흔적 없음.**

> `index-page.js` 신설은 폴백이 아니다. 확장 페이지 CSP가 인라인 스크립트를 막는 **배선 오류 수정**이며, 파일은 승인된 글롭 `app/src/main/assets/**` 안이다. developer의 분류가 옳다.

---

## 8. developer가 고쳐야 할 항목 (CHANGES_REQUESTED 대응)

**필수 1건:**
1. **MAJOR-1** — `MainActivity.kt:39-44`와 `AppNavigationDelegate.kt:11-12`의 뒤로가기 기본 동작 주석을 §2 MAJOR-1의 3개 요소(플랫폼 기본 동작 / API 31+ vs API 30 이하 분기 / `appFinish`가 확실한 종료 경로)가 드러나게 다시 쓸 것.

**함께 처리 권장 1건:**
2. **MINOR-6** — activity-ktx 선언 옆에 compose 애너테이션 유입 경로와 "exclude 금지"를 한 줄 기록.

**나머지 MINOR 7건은 선택**이며, 특히 **MINOR-1·MINOR-8은 이번 배치에서 손대지 말고**(V9 — 범위 밖) 다음 배치 요구사항으로 넘길 것을 권한다.

**코드 아닌 후속(주체가 developer가 아니다):**
- `requirements.md` AC-011-3·AC-002-2 문언 정정 → planner-analyzer. **설계·의존성 변경이 없으므로 evaluator 재승인 불요.**

---

## 9. QA(6단계) 인계 사항

1. **AC-011-3은 기기 API 버전에 따라 결과가 갈린다.** API 31+ = 태스크 백그라운드 이동(`finishing=false`), API 30 이하(minSdk 26) = Activity 종료. **검증 기기의 `ro.build.version.sdk`를 반드시 함께 기록하라.** 이 앱을 API 30 이하에서 돌리면 AC-011-3 원문이 그대로 PASS한다.
2. **`dumpsys`로 종료를 판정할 때는 `finishing=` / `state=`를 보라.** 단순 grep 건수로는 STOPPED와 소멸을 구분하지 못한다(나도 실험 A/C로 재확인했다). 종료 판정의 확실한 신호는 **`Hist #0: ActivityRecord{… MainActivity}` 자체가 사라지는 것**이다.
3. **AC-006-2의 실기기 경로는 Kotlin 분기를 타지 않는다.** 화면에 뜬 `브리지에 없는 함수명이다: …`는 **`background.js:106`(JS 화이트리스트)의 문자열**이고, Kotlin `BridgeDispatcher.kt:46`의 문구는 `지원하지 않는 함수명이다: …`로 **다르다.** 두 계층 모두 `UNKNOWN_FUNCTION`을 내지만 JS가 먼저 막으므로 Kotlin 분기는 **단위 테스트로만 덮인다.** 실기기에서 Kotlin 분기를 보려면 `ALLOWED_FUNCTIONS`에 임시로 이름을 추가해야 한다(설계상 의도된 이중 방어 — `BridgeDispatcher.kt:13-19` 주석 참조).
4. **프로브 배지가 모든 http/https 페이지 최상단을 덮는다**(고정 검정 배경 + 초록 글씨, `position:fixed; z-index:2147483647`). 스크린샷 기반 판정 시 이 영역을 콘텐츠 렌더 실패로 오독하지 말 것. requirements가 명시한 검증 장치다.
5. **AC-002-2 재측정은 `dexdump`로 하라.** `:app:dependencies` grep은 애너테이션 전용 아티팩트까지 잡아 항상 1건이 나온다. 판정 명령: APK의 각 dex에 `dexdump <f> | grep "Class descriptor" | grep androidx/compose` → **6개, 전부 애너테이션 + R**이면 정상.
6. **`adb shell pm list packages`에 `--user 0`** 필수(이 기기는 없으면 SecurityException). 기기에 `kr.co.chunjae.android.geckoviewtestapp`라는 **무관한 유사 이름 앱**이 있으니 `com.example.geckoviewtest`로 정확히 필터할 것.
7. **기기 상태(내가 반납한 상태)**: 앱 설치됨(방금 `install -r` 재설치), 마지막 조작은 `appFinish`로 정상 종료, **비행기모드 해제 확인**(`airplane_mode_on = 0`).
8. **커버리지 베이스라인**: LINE 98/98 = 100.0%, BRANCH 16/18 = 88.9%, 클래스 14개. coverage-reporter(7단계)는 이 값을 회귀 기준으로 쓸 수 있다. 계측 테스트는 JaCoCo에 집계되지 않으니(V8) 늘려도 수치는 오르지 않는다.
9. **범위 밖 기록(developer가 넘긴 것을 확인함)**: naver.com 로드 시 `GeckoConsole: Permission error: No listener for GeckoView:ContentPermission`. `PermissionDelegate` 미구현이 원인이며 requirements §6에서 명시적으로 스코프 아웃됐다. REQ-001~011 어느 것도 깨지 않는다 — **결함으로 올리지 말고 후속 과제로 유지하라.**

---
---

# rev.2 — 재리뷰 (2026-08-05 13:49) · **APPROVED**

- **입력**: `doc/20260805/20260805_134145_developer.md`(재작업 로그), `pipeline/requirements.md` **rev.3**, `doc/20260805/20260805_134039_planner-analyzer.md`, rev.1 리뷰(위 §1~§9)
- **검증 환경**: 실기기 `R3CN60L0QMT`(SM-G981N, API 33), JDK = Android Studio JBR

## 10. 판정: **APPROVED**

**MAJOR 잔여 0건.** rev.1의 MAJOR-1이 해소됐고, 처리 대상 MINOR 6건(2·4·5·6·7·9)도 전부 반영됐다. 재검증에서 **회귀 0건**이며 developer 보고값과의 **불일치도 없다**(파일 수 1건 제외 — 아래 §14).

신규 MINOR **2건**(MINOR-10·11)이 나왔으나 **둘 다 주석 한 줄 수정이고 거짓 그린을 만들지 않는다.** 재작업 루프를 한 번 더 도는 비용이 이익을 넘으므로 **후속 권고로 넘긴다**(§13).

---

## 11. MAJOR-1 — **해소 확인**

두 파일의 주석을 직접 읽어 rev.1이 요구한 3요소를 대조했다.

| 요구 요소 | `MainActivity.kt:38-63` | `AppNavigationDelegate.kt:10-27` |
|:--|:--|:--|
| ① 콜백을 건너뛰고 **플랫폼 기본 동작**이 수행된다(우리 코드는 여기서 끝난다) | ✓ L41 *"콜백을 건너뛰고 **플랫폼 기본 동작**이 수행된다. **우리 코드는 여기서 끝난다.**"* | ✓ L13 동일 취지 |
| ② **API 31+ 태스크 백그라운드 이동 vs API 30 이하 Activity 종료** | ✓ L43-52 박스. `minSdk`가 26이라 **지원 범위 안에 두 동작이 모두 들어 있다**는 사실까지 명시 | ✓ L15-20 동일 |
| ③ `appFinish`가 확실한 종료 경로 | ✓ L54-56 *"앱을 확실히 끝내는 경로는 이쪽이 아니다"* + `finish()` 경로 명시 | ✓ L22-23 |

**정직성 — 없는 관측을 주장하지 않았다(V7). 확인함.**
- 실측값에는 출처가 붙어 있다: *"실측: API 33 기기에서 `dumpsys activity activities`가 `state=STOPPED stopped=true finishing=false`"* — 내가 실험 A에서 관측한 값과 **문자 단위로 일치**한다.
- 미실측 사실에는 한계가 붙어 있다: `MainActivity.kt:51` *"(API 30 이하 실기기는 이번 검증에 없어 **문서상 동작이며 실측하지 않았다**.)"*, `AppNavigationDelegate.kt:20` *"(API 30 이하는 이번 검증에 실기기가 없어 **미실측**)"*. **두 파일 모두에 있다.**
- 초보자 기준(`comment-style.md` 독자 기준)에 맞춰 "태스크 백그라운드 이동"을 *"홈 버튼을 누른 것과 비슷하다"*로 풀어쓴 것은 규칙의 취지에 부합한다.

→ **MAJOR-1 해소. 이 지적으로 인한 차단 사유는 사라졌다.**

---

## 12. 나머지 지적 처리 결과 — 직접 대조

| # | 조치 | 확인 방법 | 판정 |
|:--|:--|:--|:--|
| **MINOR-2** (유일한 프로덕션 로직 변경) | `NativeBridgeHandler.kt:52-56`에 `catch (e: CancellationException) { throw e }` 추가 | **`catch (e: Exception)`(L57)보다 앞에 있다** — 순서 확인. import는 `kotlinx.coroutines.CancellationException`(L5). 근거 주석 3줄 존재 | **해소.** 재던지기 선택이 타당하다 — `catch`를 좁히면 새 예외 타입이 생길 때마다 목록을 유지해야 하지만, 재던지기는 **취소만 통과시키고 나머지 전부의 `INTERNAL_ERROR` 변환을 유지**한다. 실기기 재확인은 §13 |
| **MINOR-4** | 배선 이동 대신 `MainActivity.kt:69-74`에 의도임을 KDoc으로 명시 | 주석 내용 확인 + `MainViewModelTest`가 실제로 `naverUrl`(`"http://other.example"`)과 `dispatcher`를 **둘 다 바꿔 넣는지** 확인 → 확인됨 | **해소.** D-11의 실질(생성자 주입 + 테스트 교체)이 유지되고 그 근거가 코드로 뒷받침된다. 검증 완료된 배선을 흔들지 않은 판단이 옳다 |
| **MINOR-5** | Fake를 `version: String` + `throws: Boolean`로 분리(`BridgeDispatcherTest.kt:36-42`), 테스트명 → `Repository 예외는 삼켜지지 않고 그대로 전파된다`(L102) | 이름이 실제 단정(`thrown.isFailure`)과 **일치**. Fake KDoc(L29-35)이 **왜** 플래그를 분리했는지(프로덕션에서 null은 `UNKNOWN` 폴백이라 정반대 의미를 학습시킨다) 설명 | **해소.** T5 신규 위반 없음 — 자명한 케이스 3건(L60·67·85)에는 주석이 없다. (단 클래스 KDoc이 뒤처졌다 → **MINOR-10**) |
| **MINOR-6** | `app/build.gradle.kts:83-92`, `activity-ktx` 선언 바로 위 | 유입 경로·"exclude하지 말 것"·dex 실측 근거 6개 클래스명·"REQ-002 위반 아님"이 전부 있다 | **해소.** `libs.versions.toml`이 아니라 `dependencies` 블록을 고른 판단이 옳다 — `exclude`를 쓰려는 사람이 실제로 손대는 파일이다 |
| **MINOR-7** | `.gitignore`에 `build/`·`app/build/`·`.gradle/`·`.kotlin/`·`local.properties`·`.idea/`·`*.iml`·`.DS_Store` 추가 | 기존 Claude Config Manager 규칙 블록이 **한 줄도 손상되지 않았음**을 확인. 추가분에 "왜"(198 MB APK) 주석 있음 | **해소** |
| **MINOR-9** | `App.kt:37-44` `by lazy` 근거 재작성 | *"**'무거우니까 나중에'가 아니다** — 실제로는 `MainActivity.onCreate`에서 곧바로 읽으므로 그 비용은 회피되지 않고 옮겨질 뿐이다"* + 진짜 이유(화면 없는 프로세스 시작에서는 엔진을 만들지 않는다) | **해소.** 틀린 근거를 지우고 정확한 근거로 대체했다 |
| **MINOR-3** | 코드 변경 없음. 재작업 로그 §3에 이탈로 기록 | 로그에 사유(웹 페이지가 자기 DOM에 표시하므로 죽은 필드가 된다) 기재 확인 | **해소**(기록 의무 이행) |
| **MINOR-1 · MINOR-8** | **의도적으로 손대지 않음** | 재작업 로그 §4에 각각 "다음 배치 요구사항" / "release 도입 선행 작업"으로 근거와 함께 기록 | **정당하다.** rev.1의 권고와 V9에 따른 것이며 **반려 사유가 아니다** |

---

## 13. 재검증 — 내가 직접 다시 측정했다 (V7)

### 빌드·테스트·커버리지 — `clean` + `--rerun-tasks` 강제 실행

```
JAVA_HOME=<AS JBR> ./gradlew clean assembleDebug jvmCoverageReport --rerun-tasks
→ BUILD SUCCESSFUL in 9s   (^e: 0건, ^w: 0건)
```

| 항목 | rev.1 베이스라인(§9-8) | **재리뷰 실측** | 판정 |
|:--|:--|:--|:--|
| LINE 커버리지 | 98/98 = 100.0% | **98/98 = 100.0%** | **회귀 없음** |
| BRANCH 커버리지 | 16/18 = 88.9% | **16/18 = 88.9%** | **회귀 없음** |
| CLASS | 14 | **14/14 = 100.0%** | 동일 |
| 70% 미달 클래스 | 0건 | **0건**(14개 클래스 전건 100.0%) | 동일 |
| 테스트 | 31건 / 실패 0 / skip 0 | **31건 / 실패 0 / skip 0** | 동일 |
| 컴파일 경고 | 0건 | **0건** | 동일 |

캐시된 `up-to-date`를 근거로 쓰지 않았다 — `clean` 후 `--rerun-tasks`로 전 태스크를 강제 실행했다.

### 실기기 회귀 — MINOR-2가 지나는 경로를 직접 다시 태웠다

새로 빌드한 APK를 `install -r`로 재설치하고 force-stop 후 재기동했다.

| 경로 | 관측값 (내가 직접) | 판정 |
|:--|:--|:--|
| 확장 페이지 + JS 실행 | `text="READY"` | PASS |
| **3단 왕복**(bridge-client → background → **NativeBridgeHandler** → Repository) | 화면 `versionName = 1.0.0` ↔ `dumpsys package … versionName=1.0.0` **문자 단위 일치** | PASS |
| **5단 경로**(page-bridge → content → background → **NativeBridgeHandler**) | naver.com 배지 `versionName = 1.0.0 **[PAGE_WORLD]**` **AND** 네이버 콘텐츠 정상 렌더 | PASS |
| 브리지 예외 로그 | `NativeBridge: onMessage … javaType=java.lang.String` **2건**(왕복 2회분), `브리지 처리 중 예외` **0건** | PASS |
| 치명 오류 | `FATAL EXCEPTION`/`Fatal signal`/`libc: Fatal` **0건** | PASS |

→ **MINOR-2의 `catch` 절 추가가 브리지를 깨지 않았다.** 단위 테스트가 닿지 않는 클래스이므로(`NativeBridgeHandler`는 커버리지 제외) 이 실기기 확인이 유일한 검증 수단이며, developer 보고를 인용하지 않고 재수행했다.

### V2 — 수정한 테스트의 RED 재증명이 완전히 원복됐는가

developer는 `BridgeDispatcher`를 `runCatching{}.getOrDefault("SWALLOWED")`로 일시 변경해 RED를 만든 뒤 원복했다고 보고했다. **직접 확인했다:**
- `BridgeDispatcher.kt` 전문을 다시 읽어 **1차 리뷰 시점과 내용이 동일**함을 확인(L44-47의 `else -> BridgeResult.Failure(ErrorCode.UNKNOWN_FUNCTION, …)` 그대로, `runCatching`·`getOrDefault` 0건).
- 전수 grep `SWALLOWED|getOrDefault|7.7.7-probe|10.255.255.1|AC-010-5 검증용|TODO|FIXME` → **0건**.
- 백업 파일(`*.bak`/`*.orig`/`*~`) 잔존 → **0건**.
- `NAVER_URL = "http://naver.com"`, `versionName = "1.0.0"` 유지 확인.

### 화이트리스트 — 위반 0건

hook의 fnmatch 로직을 다시 실행했다. 전체 42개 파일 중 화이트리스트 밖은 여전히 `.claude/settings.json`·`initRequire.md` 2개뿐이며 **둘 다 착수 전 파일로 이번에도 변경되지 않았다.** 1차 리뷰 이후 mtime이 바뀐 파일 **8개는 전부 ALLOW**다.

---

## 14. developer 보고와의 대조 — **불일치 1건 (결함 아님)**

| 항목 | developer 보고 | 내 재측정 | 판정 |
|:--|:--|:--|:--|
| 빌드 경고 | 0건 | **0건** | 일치 |
| 테스트 | 31건 / 0 / 0 | **31건 / 0 / 0** | 일치 |
| LINE / BRANCH | 100.0% / 88.9% | **100.0% / 88.9%** | 일치 |
| 3단·5단 실기기 왕복 | 통과 | **통과**(내가 재수행) | 일치 |
| 임시 코드 잔존 | 0건 | **0건** | 일치 |
| **수정 파일 수** | **7개** | **8개** | **불일치 — 결함 아님** |

**8번째 파일은 `app/src/main/java/com/example/geckoviewtest/bridge/BridgeDispatcher.kt`**(mtime 13:40:24)다. MINOR-5의 RED 실험 대상이었고 **백업본 복원으로 mtime만 갱신된 것**이다 — 내용을 전문 대조한 결과 1차 리뷰 시점과 **동일**하다. 즉 원복은 완전하며 **미보고 변경이 아니다.** 다만 로그의 "수정한 7개 파일" 목록에는 이 파일이 없어, mtime만 보는 후속 단계가 "보고되지 않은 변경"으로 오인할 수 있다. **§15-4에서 QA에 인계한다.**

---

## 15. requirements.md rev.3 문언 정정 대조 — **정정안대로 반영됐고, 일부는 더 낫다**

### AC-002-2 — **판정 명령이 `dexdump`로 교체됐다** (핵심 요구)

`requirements.md:105-113` 확인:
- (a) dex에 `androidx/compose/ui/`·`foundation/`·`material/` 및 `compose.runtime` **본체** 0개 / (b) `buildFeatures.compose` 미설정 / (c) Compose 컴파일러 미적용 — **3조건 AND** ✓
- **애너테이션 전용 아티팩트 예외**를 유입 경로(`activity-ktx:1.13.0 → navigationevent:1.0.0 → runtime-annotation:1.9.0`)와 함께 명문화 ✓
- **판정 명령이 `:app:dependencies`에서 `dexdump` 루프로 교체됐다** ✓ — 기대 출력 6개 클래스명까지 열거. **`:app:dependencies`가 남아 있었다면 항상 1건이 나와 영원히 실패하는 기준이 됐을 것**인데, 그 함정이 제거됐다.

### AC-011-3 — **기기 API 기록 의무가 들어갔다** (핵심 요구)

`requirements.md:179-185` 확인. 네 조건 AND:
- (a) API 31+ / API 30 이하 갈래를 관측값(`finishing=false` + 태스크 `visible=false` / ActivityRecord 소멸)까지 특정 ✓
- (b) `FATAL EXCEPTION`·`Fatal signal` 부재 ✓
- (c) **`appFinish` 대조 조건** — 내 정정안에 있던 항목이 그대로 들어갔고, *"이 대조가 없으면 `finishing=false`만으로는 '종료되지 않음'과 '애초에 실행되지 않음'을 구분할 수 없다"*는 근거까지 붙었다 ✓
- (d) **`ro.build.version.sdk` 기록 의무** ✓

**내 정정안보다 나아진 점 1건**: rev.3은 이 동작 변경이 **`targetSdk`가 아니라 기기 OS 버전으로 적용된다**는 사실을 [문서] 근거와 함께 명시했다(`:29`, `:184`). 내가 명시하지 않은 부분이고, (d)의 "targetSdk가 아니라 기기 API를 기록하라"는 요구의 논리적 근거가 된다. **타당하다.**

### 함께 손본 3건 — 전부 타당

1. **REQ-011 본문**(`:89`) — *"통상대로 앱을 종료한다"* → *"통상대로(=플랫폼 기본 동작으로) 앱에서 벗어난다"*. **사용자 원문의 "통상대로"를 보존하면서** 관측 사실과 합치시켰다. 요구 범위 불변 — 타당.
2. **AC-011-4**(`:186`) — *"두 조작의 결과가 서로 다르다"*로 변경. **이것이 이번 대조에서 가장 신중히 본 항목이다(V1: 여전히 독립적으로 깨질 수 있는가).**
   - 콜백이 **항상 켜져 있으면**: index에서 뒤로가기가 `goBack()`을 부르지만 히스토리가 없어 아무 일도 안 일어나 **포그라운드 유지(`RESUMED`)** → 두 결과가 같아짐 → **잡힌다.**
   - 콜백이 **항상 꺼져 있으면**: naver에서 뒤로가기가 플랫폼 기본 동작을 타 `index.html`로 복귀하지 못함 → AC-011-1이 깨지고 두 결과도 같아짐 → **잡힌다.**
   - 두 항이 같은 기기·같은 조작 쌍에서 **반대 결과**로 정의돼 있어 어느 쪽도 "사실상 항상 참"이 아니다.
   → **V1 충족. rev.2 문언보다 오히려 강해졌다** — rev.2의 AC-011-4는 AC-011-3의 "종료" 단정에 기대고 있었는데 그 단정이 API 31+ 에서 거짓이라 **검증 기기에서 원리적으로 판정 불가**였다. rev.3이 그 결함을 없앴다.
3. **REQ-011 [미확인] 해소**(`:187`) — *"확장 페이지가 GeckoSession 히스토리에 남는가"*를 해소 처리. 내가 실험 B에서 `index.html` 재렌더 + 브리지 재동작을 직접 관측했으므로 **근거가 실재한다.** 해소된 항목을 [미확인]으로 남기면 후속 단계가 없는 리스크를 계속 안고 간다는 판단도 옳다.

### 출처 표기 (V7) — 모범적

rev.3은 실기기 관측값을 **"재현하지 않고 인용 — 출처 명시"**로 분류하고 *"planner-analyzer는 기기를 조작하지 않았다"*고 적었다(`:30`). `dexdump` 전수 조사와 `minSdk`/`targetSdk`는 **직접 재측정 [확인]**으로 분리했다. **자기가 하지 않은 관측을 자기 것으로 적지 않았다.**

---

## 16. 남은 지적 — 신규 MINOR 2건 (**비차단**)

| # | 파일·라인 | 내용 | 권고 |
|:--|:--|:--|:--|
| **MINOR-10** | `app/src/test/java/com/example/geckoviewtest/bridge/BridgeDispatcherTest.kt:16-17` | 클래스 KDoc의 **"보장하는 것"** 이 아직 *"Repository가 예외를 던졌을 때 브리지가 죽지 않고 **오류 응답으로 바뀌는지**"* 라고 적혀 있다. **이 스위트는 그것을 보장하지 않는다** — MINOR-5로 날카로워진 케이스 주석(L104-105)이 *"전파된 예외를 INTERNAL_ERROR 응답으로 바꾸는 것은 `NativeBridgeHandler`의 몫이며 그 변환은 여기서 검증하지 못한다"*고 **정반대로** 말한다. KDoc이 케이스 주석보다 넓게 단정해 T1의 "무엇을 보장하는가"가 부정확해졌다 | *"Repository가 예외를 던졌을 때 그 예외가 **삼켜지지 않고 전파되는지**"* 로 한 줄 교체. **거짓 그린은 만들지 않는다**(테스트 자체는 올바르고 회귀를 실제로 잡는다 — RED 재증명 확인). 케이스 주석이 바로 아래에서 정정하고 있어 오독 여지도 작다 |
| **MINOR-11** | `MainActivity.kt:58-59`, `AppNavigationDelegate.kt:25` | *"(AC-011-3) — … **문언 정정 대기 중**"* 표현이 낡았다. rev.3이 **13:38:42에 정정을 완료**했다 | *"requirements.md rev.3에서 정정됐다"* 로 교체. **developer의 부주의가 아니다** — 주석 작성(13:37:43·13:39:01)과 rev.3 발행(13:38:42)이 **동시에 진행**됐다. 다음에 이 파일을 만질 때 함께 고치면 된다 |

**두 건 모두 주석 한 줄이며 동작·검증에 영향이 없다.** rev.1의 MAJOR-1과 달리 **틀린 사실을 단정하지 않는다**(MINOR-10은 범위 과잉, MINOR-11은 시점 지연). 재작업 루프 비용이 이익을 넘으므로 **APPROVED를 막지 않고 후속 권고로 넘긴다.**

---

## 17. QA(6단계) 인계 — rev.1 §9에서 **달라진 것 중심**

rev.1 §9의 9개 항목은 **그대로 유효**하다. 아래는 재리뷰로 바뀌거나 추가된 것이다.

1. **[변경] AC-011-3·AC-002-2는 이제 `requirements.md` rev.3 문언으로 판정하라.** rev.2 문언(*"Activity가 종료된다"*, *"아티팩트 0개"*)으로 판정하면 **정상 구현이 FAIL로 나온다.**
   - AC-002-2 판정 명령은 **`dexdump`**다(`:app:dependencies` 아님). 기대 출력은 애너테이션 + R **6개**.
   - AC-011-3은 **4조건 AND**이며 (c) `appFinish` 대조와 (d) **`ro.build.version.sdk` 기록**이 필수다.
2. **[변경] AC-011-4의 판정 기준이 "종료 여부"가 아니라 "두 조작의 결과가 서로 다르다"이다.** naver 뒤로가기 = `index.html` 복귀 + `state=RESUMED`, index 뒤로가기 = 포그라운드 이탈(API 31+ 는 `state=STOPPED finishing=false`). **두 결과가 같아지면 `canGoBack` 배선이 죽은 것이다.**
3. **[신규] `NativeBridgeHandler`에 `catch (CancellationException) { throw e }`가 추가됐다**(이번 배치의 유일한 프로덕션 로직 변경). **단위 테스트로 덮이지 않는다** — 이 클래스는 커버리지 제외 대상(`app/build.gradle.kts`의 `coverageExclusions`)이다. 브리지 관련 회귀를 볼 때 **3단·5단 왕복을 둘 다** 태워라(한쪽 결과를 다른 쪽 근거로 쓰지 말 것 — AC-006-4).
4. **[신규] mtime만으로 변경 파일을 세지 마라.** `BridgeDispatcher.kt`는 RED 실험 후 백업 복원으로 **mtime만 갱신됐고 내용은 동일**하다(§14). 변경 판정은 내용 대조로 하라.
5. **[유지·재확인] 커버리지 베이스라인**: LINE **98/98 = 100.0%**, BRANCH **16/18 = 88.9%**, CLASS **14**. `clean` + `--rerun-tasks`로 재측정한 값이다. coverage-reporter(7단계)는 이 값을 회귀 기준으로 쓰면 된다.
6. **[유지] 기기 반납 상태**: `R3CN60L0QMT`(API 33)에 최신 APK 설치됨. 마지막 조작은 **naver.com을 띄운 채 포그라운드**이며(종료하지 않았다) 비행기모드 해제 확인(`airplane_mode_on = 0`).
7. **[유지] 다음 배치로 넘어간 2건**: MINOR-1(`ensureBuiltIn` 실패 시 미처리 코루틴 예외 → "확장 설치 실패의 사용자 대면 처리"), MINOR-8(프로브 배지 always-on → release 도입 선행 작업). **이번 배치의 결함으로 올리지 말 것.**
8. **[신규] 후속 권고 2건**: MINOR-10(테스트 클래스 KDoc 범위 과잉), MINOR-11(주석의 "정정 대기 중" 표현이 낡음). 둘 다 주석 한 줄이며 **QA 판정 대상이 아니다.**

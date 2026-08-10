# 작업 계획: GeckoView(XML View) 앱 신규 스캐폴딩 + WebExtension 브리지

- 작성 일시: 2026-08-04 14:18:32 (KST)
- **개정 일시: 2026-08-05 08:20:34 (KST) — rev.2**
- 재작성 여부: **rev.2 — evaluator 조건부 승인(C-1~C-8) 반영.** 설계 변경 없음
- 근거: `pipeline/impact-report.json` (task: "GeckoView(XML View) 안드로이드 앱 신규 스캐폴딩 + WebExtension 브리지(페이지↔background.js↔네이티브) 구현 — REQ-001~011")
- 요구사항: `pipeline/requirements.md` **rev.2** (REQ-001~011, AC-*)
- 판정: `pipeline/evaluation.md` — **APPROVED (조건부)**. 조건은 전부 문서 정정이므로 **evaluator 재승인 불요**, 반영 여부는 code-reviewer가 5단계에서 대조한다
- 대조한 규칙: `architecture.md`, `verification-honesty.md`, `comment-style.md`, `scope-guard.md`, `work-logging.md`

> **절 번호 표기 규약** — 이 문서 자체의 절은 `§2.4`처럼 이 문서 안에 실제로 존재하는 번호(§0~§11)만 쓴다. **`§2.6.1`·`§2.7.x`·`§2.8`·`§2.10`·`§2.12`·`§5.1`·`§7.4`·`§7.5`처럼 이 문서에 없는 번호는 전부 `requirements.md`(rev.2)의 절**이다. `REQ-*`·`AC-*`·`A-*`도 `requirements.md`의 식별자다. `C-*`와 `§0.x`는 `evaluation.md`의 식별자다.

> **단위 표기 규약 (rev.2 신설 — C-8)** — **이 문서의 모든 바이트 크기는 `MiB`(2²⁰)로 통일한다.** 결정적인 값은 원시 바이트 수를 병기한다. rev.1은 AAR 총량을 MiB로, ABI별 크기를 MB(10⁶)로 섞어 써서 **존재하지 않는 인계 오류를 기록하는 사고**를 냈다(아래 개정 이력 참조).

---

## 개정 이력

### rev.2 — 2026-08-05 08:20:34 (evaluator 조건 C-1~C-8 반영)

**설계는 하나도 바뀌지 않았다.** Step 구조, D-01~D-13, 브리지 계약(§3), 커버리지 경로(§7), 승인 대상 목록(§6)은 rev.1 그대로다. 바뀐 것은 **수치·증거 라벨·절차·분류**뿐이다.

| 조건 | 성격 | 무엇을 고쳤나 | 반영 위치 |
|:--|:--|:--|:--|
| **C-1** | **[필수·BLOCKING]** | Step 1의 APK **크기 임계값(150 MB)** 판정을 **APK 내 ABI 목록** 판정으로 교체. 정상 빌드가 189.4 MiB라 rev.1 규칙은 **올바른 빌드를 실패로 판정**해 파이프라인을 정지시켰다 | Step 1 게이트·실패 규약, R-01, U-10 |
| **C-2** | [필수] | R-01의 *"adb install이 수 분대 → 검증 루프 마비"* 를 실측(**6.872초**)으로 정정. `abiFilters` 결정은 유지하되 근거를 "3 ABI 비압축 합계 483.1 MiB"로 교체 | R-01, Step 1 게이트 |
| **C-3** | [필수] | §0·§2.4·§7.2의 `[확인]` 라벨 정정. rev.1의 검증은 **GeckoView를 패키징하지 않은 빌드**였고, GeckoView 포함 `assembleDebug`는 **evaluator가 대신 수행**했다. 출처를 evaluator 실측으로 교체 | §0(신설 표 포함), §2.4, §7.2 |
| **C-4** | [권고] | G2-d·G3-d는 술어가 "기록한다"라 AND 항으로서 항상 참이다(V1). **게이트에서 빼고 "산출 기록 항목"으로 분류 변경.** 기록 요구 자체는 유지 | Step 2·3 |
| **C-5** | [권고] | 에뮬레이터 배제 문구를 *"원리적으로 성립하지 않는다"* → *"이 배치의 `abiFilters`(arm64-v8a 단일)에서는 성립하지 않는다"*. AAR에 `x86_64`는 **있다**(32비트 `x86`만 없음) | R-16, §10 |
| **C-6** | [필수] | P2 오리진 검사 훅 **채택 승인**. 단 3개 조건(기본 전체 허용 출고 / G4-d 통과가 증거 / 한글 주석)을 명문화 | §2.5, Step 4, §11 |
| **C-7** | [필수] | 폴백 1·2안 발동 시 절차 명시 — developer는 **임의 전환 금지, FAIL 반환** → planner 재실행 → **evaluator 재승인** → 재착수. 근거: 1·2안은 우회가 아니라 **설계 변경**이다 | Step 2·3 실패 규약, §9 표 |
| **C-8** | [권고] | ① rev.1의 **"정정 ③"을 철회**한다 — rev.2 requirements의 70.7/68.2/75.6은 **MiB**, rev.1이 적은 74.2/71.5/79.2는 **MB(10⁶)** 로 **같은 바이트 수**다. requirements는 틀리지 않았다. 문서 전체 단위를 MiB로 통일 ② GeckoView 버전 문구를 "최신 stable" → **"검증 완료된 고정 버전"** | 단위 규약, §0, §6.1 L-01 |

**rev.1이 범한 두 종류의 잘못 (기록해 둔다):**
1. **`[확인]` 라벨의 과장 (C-3).** 서로 다른 세 스크래치패드 프로젝트의 결과를 한 문장으로 합쳐 적었고, 그 결과 *"빌드 성공"* 이 *"GeckoView를 포함한 빌드 성공"* 의 **대리 신호**로 쓰였다(V1). 결론은 옳았으나 **근거가 결론을 지탱하지 못했다.**
2. **존재하지 않는 인계 오류의 기록 (C-8①).** 단위 혼동을 상대 문서의 오류로 적었다. **V7은 인계값을 재측정하라는 규칙이지 인계 문서에서 오류를 찾아내라는 규칙이 아니다.** 없는 오류를 기록하는 것은 V7이 막으려는 것과 정확히 반대 방향이다.

### rev.1 — 2026-08-04 14:18:32 (초안)
목표·§2 확정 결정(D-01~D-13, DI, 소스 루트, 툴체인, WebExtension 구성)·§3 브리지 계약·Step 0~9·리스크 16건·승인 대상 22건·커버리지 경로·[미확인] 14건.

---

## 0. 이 계획의 전제가 되는 실측값 — **출처를 구분해 적는다** (verification-honesty V7)

**rev.2에서 이 절을 다시 썼다(C-3).** rev.1은 서로 다른 스크래치패드 프로젝트 세 개의 결과를 한 문장으로 합쳐 `[확인]`을 붙였고, 그 결과 *"빌드 성공"* 이 *"GeckoView를 포함한 빌드 성공"* 의 **대리 신호**로 쓰였다(V1). 아래 표는 **누가 무엇을 측정했는지**를 열로 분리한다.

### 0.1 planner가 직접 측정한 것 — 결과가 evaluator 재측정과 **일치**

| 항목 | 값 | 측정 방법 | evaluator 재측정 |
|:--|:--|:--|:--|
| GeckoView 고정 버전 | **`153.0.20260730155536`** | `curl .../maven-metadata.xml` | **일치** (`<release>` 드리프트는 §0.4 참조) |
| AAR 크기 | **240,695,932 B (229.5 MiB)** | EOCD/중앙 디렉터리 직접 파싱 (68 엔트리) | **일치** |
| **AAR minSdk** | **`<uses-sdk android:minSdkVersion="26" />`** | AAR의 AndroidManifest.xml을 HTTP Range로 추출·inflate 후 원문 확인 | **일치** — **minSdk 26은 선택이 아니라 하한** |
| AAR 동봉 ABI | arm64-v8a / armeabi-v7a / x86_64. **32비트 `x86` 없음** (`x86_64`는 **있다** — C-5) | 중앙 디렉터리 | **일치** |
| AAR 병합 권한 | `INTERNET`, `ACCESS_NETWORK_STATE`, `WAKE_LOCK`, `MODIFY_AUDIO_SETTINGS`, `HIGH_SAMPLING_RATE_SENSORS` | 원문 grep | **일치** — 앱 manifest에 다시 쓸 필요 없다 |
| AAR 병합 `<service>` 수 | **89개** | 원문 grep -c | **일치** (병합된 매니페스트에서도 89 재확인) |
| `glEsVersion` / `zygotePreloadName` | `0x00020000` / `org.mozilla.gecko.process.ZygotePreload` | 원문 grep | **일치** |
| ANDROID_HOME | `/Users/appdevloperteam/Library/Android/sdk`, **android-36 존재** | `ls`, `echo $ANDROID_HOME` | **일치** |
| **환경변수 `JAVA_HOME`** | **`/Library/Java/JavaVirtualMachines/jdk1.8.0_333.jdk/Contents/Home` (JDK 8)** | `echo $JAVA_HOME` | **일치** — **인계 문서 정정** (rev.2 §7.5는 21.0.10으로 적었다) |
| PATH 기본 `java` | **17.0.14** (JBR, `/usr/libexec/java_home` 기본값) | `env -u JAVA_HOME /usr/bin/java -version` | **일치** — **인계 문서 정정** (requirements·impact-report 둘 다 1.8.0_333으로 적었다) |
| Android Studio JBR | **21.0.10** (`/Applications/Android Studio.app/Contents/jbr/Contents/Home`) | 직접 실행 | **일치** |
| 시스템 gradle | 9.3.1 (`/opt/homebrew/bin/gradle`) | `gradle --version` | **일치** |
| 실기기 | `R3CN60L0QMT` = SM-G981N, `sdk=33`, `arm64-v8a` | `adb devices -l`, `getprop` | **일치** (+ `mWakefulness=Awake`) |
| AGP 9.3.1 + Gradle **9.3.1** 실패 | `NoClassDefFoundError: org/gradle/features/binding/ProjectTypeBinding` | 실제 실행 | (재현 요구 없음 — 배제 근거로만 쓰인다) |

> **JAVA_HOME 정정의 실무적 의미:** 셸의 `JAVA_HOME`이 JDK 8이므로 **아무 설정 없이 `./gradlew`를 실행하면 AGP가 뜨지 않는다.** 이 계획의 모든 Gradle 호출은 `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`을 앞에 붙인다. requirements와 impact-report가 서로 다른 값(21.0.10 / 1.8.0_333)을 적고 있었고 **둘 다 틀렸다.** 재측정 없이 인용했다면 Step 0부터 막혔을 것이다.

### 0.2 planner가 **측정하지 않은 것** — evaluator가 대신 수행했다 (C-3)

**rev.1의 검증은 GeckoView를 패키징하지 않은 빌드였다.** planner의 스크래치패드 산출물이 그 사실을 그대로 보여준다(evaluator가 직접 열어 확인, planner도 재확인함):

| rev.1 스크래치패드의 실제 상태 | 관측값 |
|:--|:--|
| `agp8` 프로젝트의 debug APK | **3,315,659 B, `lib/` 엔트리 0개** → GeckoView 네이티브 라이브러리가 들어간 적 없다 |
| Gradle 캐시의 `geckoview/153.0.20260730155536/` | `.pom` + `.module`만. **`.aar` 없음** → 240 MB AAR을 내려받은 적 없다 |
| `agp8/app/build.gradle.kts` 수정 시각 | APK 생성 시각보다 **20초 뒤** → GeckoView 좌표 추가는 빌드 **이후**였다 |
| `cov` 프로젝트 (JaCoCo·serialization 검증용) | GeckoView 의존성 **없음**, `jacocoVersion` **0.8.13** (승인 요청값은 0.8.15) |

**즉 rev.1 §2.4·§7.2의 `[확인]`은 세 프로젝트의 결과를 합친 것이고, GeckoView·매니페스트 병합(89 service)·네이티브 패키징을 거친 빌드는 한 번도 없었다.**

**evaluator가 계획의 전 구성요소를 한 프로젝트에 넣고 끝까지 빌드했다** — 출처: `evaluation.md` §0.3. 아래 값은 전부 **[확인 — evaluator 실측]** 이다:

| 검증 항목 | 결과 |
|:--|:--|
| `gradle wrapper --gradle-version 8.14.5` (시스템 gradle 9.3.1로 생성) | **성공** — `gradle-wrapper.jar` 46,175 B |
| **`./gradlew :app:assembleDebug` (GeckoView 포함)** | **BUILD SUCCESSFUL in 56s** (40 tasks) |
| **debug APK 크기** | **198,587,715 B = 189.4 MiB** — `abiFilters` **정상 적용 상태에서** |
| APK 내 ABI | **arm64-v8a 단일** (`unzip` 확인, `lib/` 엔트리 13개) → `abiFilters` 실효 |
| `libxul.so` 패키징 형태 | **152,735,464 B, Method=Stored (비압축)** — 크기의 원인 |
| **`adb install -r` 소요** | **6.872초** (SM-G981N, USB) |
| 매니페스트 병합 | `<service>` **89개**, 권한 5종 |
| `kotlin-stdlib` 해석 | **2.3.21** — GeckoView POM이 이를 **직접 선언**함을 POM 원문에서 확인 → **R-04·B-05·AGP 9 배제 근거 2번이 성립** |
| `androidx.core` 해석 | **1.18.0** (compileSdk 36 근거 유효) |
| `./gradlew :app:jvmCoverageReport --rerun-tasks` (**JaCoCo 0.8.15**, GeckoView 포함) | **BUILD SUCCESSFUL**, XML 생성, `gecko/**`·`MainActivity` 제외 실효 |
| `kotlinx-serialization` 기반 순수 Kotlin 클래스의 JVM 커버리지 | **LINE 100%** → §3.1 + L-07 + §7 커버리지 경로 **실증** |

### 0.3 AAR ABI별 크기 — **압축/비압축을 구분해 적는다** (C-1·C-2의 근거)

APK 크기 오판의 뿌리가 여기다. **AAR 안에서는 압축돼 있고, APK에서는 비압축(Stored)으로 들어간다.**

| | AAR 내부(압축) | **APK 저장 형태(비압축)** |
|:--|--:|--:|
| arm64-v8a | 70.7 MiB (74,150,236 B) | **167.1 MiB** (175,172,192 B) |
| armeabi-v7a | 68.2 MiB (71,541,879 B) | 130.4 MiB (136,690,000 B) |
| x86_64 | 75.6 MiB (79,234,693 B) | 185.7 MiB (194,736,352 B) |
| `assets/` | 13.6 MiB (14,304,533 B) | 14.2 MiB (14,922,046 B) |
| **3 ABI 합계** | — | **483.1 MiB (506,598,544 B)** |

- 비압축 열은 planner가 중앙 디렉터리의 uncompressed size로 **독립 재계산**해 evaluator 값과 일치함을 확인했다. `167.1 + 14.2 = 181.3 MiB`이고 evaluator 실측 APK가 **189.4 MiB**이므로 dex·리소스·서명 몫 약 8 MiB와 **정합한다** — 두 측정이 서로를 뒷받침한다.
- **AGP는 minSdk ≥ 23에서 네이티브 `.so`를 비압축으로 패키징한다**(시스템이 직접 mmap 하도록). requirements §2.2의 "85~95 MB" 추정은 **AAR 내부의 압축 크기**를 더한 값이라 실제의 약 1/2이다.

> **[C-8① — rev.1의 "정정 ③"을 철회한다]** rev.1은 requirements rev.2의 ABI 크기(70.7 / 68.2 / 75.6)가 자신의 값(74.2 / 71.5 / 79.2)과 *"수 MB 차이"* 라며 **인계 문서 오류로 기록**했다. **오류가 아니었다.** 위 표가 보이듯 같은 바이트 수를 requirements는 **MiB**로, rev.1은 **MB(10⁶)** 로 적었을 뿐이다. **requirements rev.2 §2.2는 정확하다.** 이 문서는 이제 MiB로 통일한다.

### 0.4 GeckoView `<release>` 드리프트 — **버전을 올리지 않는다** (C-8②)

- 판정 시점(2026-08-05) 기준 `<release>` = **`153.0.20260803132010`** (planner 재확인).
- 계획이 고정한 **`153.0.20260730155536`을 그대로 쓴다.** 근거: ① A-12가 동적 버전을 금지한다 ② **evaluator가 실제로 빌드·검증한 유일한 버전이다.** `<release>`를 좇아 올리면 검증되지 않은 버전으로 바뀐다.
- 이 값은 "최신 stable"이 아니라 **"검증 완료된 고정 버전"** 이다. rev.1의 "최신 stable" 표현을 §6.1 L-01에서 정정했다.
- planner 측정 시점(08-04 14:18)에는 이 값이 최신이었으므로 **불일치는 오류가 아니라 드리프트다.**

---

## 1. 목표

완료 시 사용자가 실기기(SM-G981N)에서 관찰할 수 있는 것:

1. 앱을 실행하면 Gecko 엔진이 렌더한 내장 `index.html`이 보이고, 버튼 두 개(`getVersionName` / `appFinish`)와 결과 표시 영역이 있다.
2. `getVersionName` 버튼을 누르면 `dumpsys package`의 `versionName`과 **문자 단위로 같은** 문자열이 화면에 나타난다.
3. `appFinish` 버튼을 누르면 앱이 크래시 없이 종료된다.
4. naver 버튼을 누르면 로딩 UI가 떴다가 사라지고 네이버 페이지가 렌더된다.
5. 네이버 페이지의 **페이지 세계 JS에서도** 같은 브리지가 동작해 화면에 `PAGE_WORLD` 마커가 붙은 버전 배지가 보인다.
6. 뒤로가기를 누르면 `index.html`로 복귀하고, 복귀 후에도 브리지가 다시 동작한다. `index.html`에서 뒤로가기를 한 번 더 누르면 앱이 종료된다.

---

## 2. 확정 결정 — **전건 evaluator 승인 완료** (§11.1)

> **rev.2 기준 이 절의 모든 결정은 승인됐다.** developer는 확정 사항으로 다루고 재검토하지 마라. rev.1에서 "evaluator 판정 대상"으로 표시했던 항목(D-01~D-13, Hilt 미도입, 툴체인, A-13/14/15, P2 훅)은 전부 판정이 끝났다.

### 2.1 requirements.md §3 (XML vs `architecture.md` 충돌표) — 항목별 결정

`architecture.md`의 레이어 문장은 "UI (Compose)"로 쓰여 있으나 **REQ-002가 XML을 명시**한다. 사용자 요구가 우선이므로 **"UI (Compose)"를 "UI 레이어(Activity + XML + ViewBinding)"로 읽고, 레이어 방향·상태 관리·비동기 원칙은 문자 그대로 지킨다.** 각 항목의 결정은 아래와 같다.

| # | `architecture.md` 원칙 | **이 계획의 결정** | 근거 |
|:--|:--|:--|:--|
| D-01 | UI → ViewModel → Repository → DataSource | **UI = `MainActivity` + `activity_main.xml` + ViewBinding.** Activity는 `MainViewModel`만 참조한다. `AppInfoRepository`를 Activity에서 직접 호출하지 않는다 | 레이어 방향 원칙은 렌더링 기술과 무관 |
| D-02 | **ViewModel에 Context/View 주입 금지** | **`AppInfoRepository`(순수 Kotlin 인터페이스)를 둔다.** 구현 `AppInfoRepositoryImpl`은 **`Context`를 받지 않고 `versionNameProvider: () -> String?` 람다를 받는다.** `PackageManager` 호출은 `App.kt`(Application)가 람다를 만들 때 한 번만 등장한다 | Context 격리 + **Impl 자체가 JVM 테스트 가능해진다**(§7). Impl이 Context를 들면 JVM 테스트가 불가능해져 커버리지 분모에 0%로 얹힌다 |
| D-03 | **`appFinish` = 일회성 이벤트** | **`Channel<MainUiEvent>(Channel.BUFFERED)` + `receiveAsFlow()`.** `SharedFlow(replay=0)`를 쓰지 않는다 | `SharedFlow(replay=0)`는 **활성 수집자가 없으면 이벤트를 버린다.** Activity가 STOPPED인 순간에 도착한 `Finish`가 유실되면 "가끔 종료가 안 되는" 비결정적 결함이 된다. `Channel`은 버퍼링한다 |
| D-04 | **`GeckoRuntime` 소유권** | **Application 스코프 싱글턴** (`App.kt`의 `by lazy`). Activity·ViewModel은 생성하지 않는다 | "GeckoRuntime can only be initialized once per process" [문서]. Activity에서 만들면 화면 회전 시 재생성되어 크래시한다. **`App.kt` 파일과 `AndroidManifest.xml`의 `android:name` 등록은 쌍으로 처리한다**(impact-report risk_notes) |
| D-05 | **`GeckoSession` 소유권** | **`MainActivity`(UI 레이어)가 소유한다.** ViewModel은 세션 객체를 **절대 보유하지 않고** `MainUiState`의 `currentUrl`/`isLoading`/`canGoBack`과 `MainUiEvent`만 다룬다 | `GeckoSession`은 `GeckoView` 위젯과 결합된 UI 자원이다. ViewModel이 들면 D-02 위반이자 회전 시 뷰 누수 |
| D-06 | 단일 `UiState` + `StateFlow` | **`MainUiState(isLoading, currentUrl, canGoBack, bridgeReady, lastBridgeResult)`.** Activity는 `repeatOnLifecycle(STARTED) { uiState.collect { render(it) } }`로 **단일 `render(state: MainUiState)`** 에 바인딩한다 | Compose의 recomposition에 대응하는 XML 관용구. render 함수가 1개여야 "상태의 투영"이 강제된다 |
| D-07 | "`remember`로 비즈니스 상태 보관 금지"의 XML판 | **Activity 필드/View 상태에 비즈니스 상태를 두지 않는다.** 로딩 여부의 진실의 원천은 `MainUiState.isLoading`이고 `ProgressBar.visibility`는 그 투영이다. **델리게이트가 `visibility`를 직접 건드리는 것을 금지한다** | REQ-009가 이 원칙을 어기기 가장 쉬운 지점. `AppProgressDelegate`는 `viewModel.onPageStart()/onPageStop(success)`만 호출한다 |
| D-08 | **`OnBackPressedCallback`의 소유 레이어** (rev.2 이슈 8) | **`MainActivity`가 소유한다. ViewModel은 androidx.activity 타입을 일절 모른다.** 배선: ① `AppNavigationDelegate.onCanGoBack` → `viewModel.onCanGoBackChanged(b)` → `MainUiState.canGoBack` ② Activity의 `render()`가 `backCallback.isEnabled = state.canGoBack` ③ 콜백 본문은 `viewModel.onBackPressed()` 호출 → ViewModel이 `MainUiEvent.NavigateBack` 방출 ④ Activity가 수신해 `session.goBack()` | ViewModel이 콜백을 들면 `architecture.md` "ViewModel에 View 타입 주입 금지" 위반. 이 배선이면 **뒤로가기 판단 로직 두 갈래(`canGoBack` 전이, `onBackPressed`→이벤트)가 모두 JVM 테스트 가능**해진다(§7). `canGoBack=false`일 때는 콜백이 비활성이라 dispatcher 기본 동작으로 Activity가 종료된다 → AC-011-3 |
| D-09 | `viewModelScope`만, `GlobalScope`/`runBlocking` 금지 | **`GeckoResult<T>` ↔ 코루틴 어댑터를 `gecko/GeckoResultExt.kt` 한 곳에만 둔다.** `suspend fun <T> GeckoResult<T>.await(): T`는 `suspendCancellableCoroutine` + `then/exceptionally`로 구현하고, 취소 시 `GeckoResult.cancel()`을 호출한다. 역방향 `fun <T> CoroutineScope.geckoResultOf(block: suspend () -> T): GeckoResult<T>`도 같은 파일에 둔다. **`GeckoResult.poll()`(블로킹) 사용 금지** | 변환 지점을 1파일로 모아야 `runBlocking`이 코드베이스에 흩어지지 않는다 |
| D-10 | `NativeBridgeHandler`가 쓸 스코프 | **Application 스코프 `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)`를 `App.kt`의 컨테이너가 만들어 생성자 주입한다.** `GlobalScope` 금지 | 브리지 요청은 Activity 수명과 무관하게 도착할 수 있어 `viewModelScope`가 맞지 않는다. 그렇다고 `GlobalScope`를 쓰면 금지 사항 위반 |
| D-11 | Dispatcher 주입 | **`MainViewModel`과 `BridgeDispatcher`는 `CoroutineDispatcher`를 생성자로 받는다. 기본값을 주지 않는다.** 컨테이너가 `Dispatchers.Default`/`Dispatchers.IO`를 넘긴다 | 기본값을 주면 테스트가 주입을 잊어도 통과해버려 주입 장치가 장식이 된다. GeckoView API의 `@UiThread`/`@AnyThread`/`@HandlerThread` 계약은 각 델리게이트 파일 KDoc에 명시한다 |
| D-12 | 버전은 `gradle/libs.versions.toml`에서만 | **모든 버전(AGP/Gradle/Kotlin/GeckoView/androidx/테스트/JaCoCo)을 버전 카탈로그에 둔다.** `build.gradle.kts`에 리터럴 버전 문자열을 쓰지 않는다 | — |
| D-13 | UI 노출 텍스트는 `strings.xml` | **적용 범위 = Android UI 텍스트만** — 앱 이름, naver 버튼 라벨, 로딩 안내, 오류 토스트. **`index.html`·`page-bridge.js` 내부 텍스트는 웹 리소스이므로 `strings.xml` 대상이 아니다** | 웹 자산에 `strings.xml`을 강제하면 HTML/JS가 망가진다. code-reviewer가 이 경계로 판정한다 |

### 2.2 DI — **Hilt를 도입하지 않는다** (A-09 판정)

**결정: 수동 DI.** `App.kt`에 `AppContainer`를 두고 `MainViewModel`은 `ViewModelProvider.Factory`(또는 `viewModels { factory }`)로 생성한다.

근거:
1. 화면 1개, 주입 대상 5개(`AppInfoRepository`, `BridgeDispatcher`, `CoroutineDispatcher`, Application 스코프 `CoroutineScope`, `GeckoRuntime`). Hilt의 손익분기점 아래다.
2. Hilt는 승인 대상 아티팩트를 **3개 더** 늘린다(`hilt-android`, `hilt-compiler`, `ksp` 플러그인). 이번 배치는 이미 **229.5 MiB AAR**을 들이는 중이라 빌드 시간이 최대 리스크인데(evaluator 실측: GeckoView 포함 클린 `assembleDebug` **56초**), KSP 라운드가 얹히면 developer의 빌드·검증 루프가 더 느려진다.
3. `architecture.md`는 **"Dispatcher 주입"과 "생성자 주입"만 요구하고 DI 프레임워크를 요구하지 않는다.** 수동 DI로도 원칙은 전부 충족된다.
4. 수동 DI는 배선이 `App.kt` 한 파일에 그대로 보이므로 **초보 개발자 독자 기준**(`comment-style.md`)에 오히려 유리하다.

### 2.3 소스 루트와 모듈 구성

- **소스 루트: `app/src/main/java/`로 확정한다.** `app/src/main/kotlin/`은 **만들지 않는다.** (AGP 기본값이고 requirements.md §7.1의 경로이며 impact-report risk_notes의 권고다. 두 디렉터리가 동시에 존재하면 code-reviewer가 지적한다.)
- **모듈: 단일 `app` 모듈.** `core/` 등 모듈 분리를 하지 않는다. impact-report의 `allowed_files`가 루트 빌드 스크립트를 **열거**로 잠가 두었기 때문에 모듈을 추가하면 화이트리스트가 흡수하지 못하고 impact-analyzer 재실행이 필요하다. 이번 규모(클래스 10여 개)에서 모듈 분리의 이득은 없다.

### 2.4 툴체인 — **실제 빌드로 검증한 조합** (requirements §7.4-12 해소)

requirements는 "AGP를 먼저 고르고 wrapper를 생성할 것"이라고만 남겼다. 후보 조합을 실제 Android 프로젝트로 만들어 확인했다.

> **[C-3 — 증거 출처 정정]** 아래 표의 `[확인]` 라벨은 **rev.1에서 과장돼 있었다.** planner의 세 빌드는 전부 **GeckoView를 패키징하지 않은 프로젝트**에서 수행됐다(§0.2). 따라서 planner가 확인한 것은 **"AGP·Gradle·Kotlin·JDK 조합이 성립한다"까지**이고, **"GeckoView 240 MB AAR을 포함해도 성립한다"는 evaluator가 확인했다.** 아래 표는 두 출처를 분리해 적는다.

| 후보 | planner가 확인한 범위 (GeckoView **미포함**) | evaluator가 확인한 범위 (GeckoView **포함**) |
|:--|:--|:--|
| AGP 9.3.1 + Gradle **9.3.1** | **실패 [확인 — planner]** `NoClassDefFoundError: org/gradle/features/binding/ProjectTypeBinding`. **시스템 gradle 9.3.1을 그대로 wrapper로 쓰면 안 된다는 반증** | 배제됐으므로 재현 불요 |
| AGP 9.3.1 + Gradle 9.6.1 + JDK 21 | **성공 [확인 — planner]** 단 **`org.jetbrains.kotlin.android`를 적용하면 빌드 실패**(AGP 9는 Kotlin 내장). stdlib가 **2.2.10**으로 고정 | 배제됐으므로 재현 불요 |
| **AGP 8.13.2 + Gradle 8.14.5 + Kotlin 2.3.21 + JDK 21 — 채택** | **[확인 — planner]** 조합 성립, `assembleDebug` 성공, `kotlin-stdlib` 2.3.21 해석. **단 APK에 `lib/` 엔트리 0개 — GeckoView 없음** | **[확인 — evaluator 실측, evaluation.md §0.3]** 계획의 **전 구성요소를 한 프로젝트에** 넣고 `assembleDebug` **BUILD SUCCESSFUL in 56s**, APK 내 **arm64-v8a 단일**, 매니페스트 **89 service 병합**, `kotlin-stdlib` **2.3.21**, `androidx.core` **1.18.0**, `jvmCoverageReport --rerun-tasks` 성공 |

**채택: AGP 8.13.2 / Gradle wrapper 8.14.5 / Kotlin 2.3.21 / JDK 21 / compileSdk 36 / minSdk 26 / targetSdk 36.**

**"GeckoView를 포함한 `assembleDebug`가 성공한다"의 출처는 evaluator 실측이다** — planner는 이 빌드를 수행하지 않았다. developer는 이 사실을 알고 Step 1에 착수하라.

AGP 9를 택하지 않은 이유:
1. **`architecture.md`의 "버전은 `libs.versions.toml`에서만 관리"와 충돌한다.** AGP 9의 내장 Kotlin은 Kotlin 버전이 AGP에 암묵적으로 묶여 버전 카탈로그에 나타나지 않는다.
2. **requirements.md §2.4가 [미확인]으로 남긴 "Kotlin stdlib 충돌"이 AGP 9에서는 실제로 발생한다** — 컴파일러 2.2.10 / 런타임 stdlib 2.3.21. AGP 8 + KGP 2.3.21이면 이 리스크가 **사라진다**.
3. AGP 9는 `org.jetbrains.kotlin.android` 적용 금지 같은 함정이 있고, 229.5 MiB 네이티브 AAR과의 조합은 검증 사례가 얇다.

> **[evaluator 지적]** 위 근거 **1번은 논증이 약하다** — `architecture.md`의 조항은 *"버전을 카탈로그에서 관리하라"*(= `build.gradle.kts` 하드코딩 금지)이지 *"모든 버전이 독립적으로 선언 가능해야 한다"*가 아니다. AGP 내장 Kotlin은 "다른 곳에서 관리되는" 것이 아니라 "선언 대상이 아닌" 것이므로 조항 위반으로 보기 어렵다. **배제 결정 자체는 승인됐으며, 근거 2번(stdlib 역전)이 단독으로 결정을 지탱한다** — evaluator가 GeckoView POM이 `kotlin-stdlib 2.3.21`을 **직접 선언**함을 원문에서 확인했다.

**Gradle wrapper는 `gradle wrapper --gradle-version 8.14.5`의 산출물을 쓴다.** `gradle-wrapper.jar`를 손으로 만들지 않는다(impact-report risk_notes: 바이너리는 "내용 대조"가 아니라 "출처 확인"으로 리뷰한다).

### 2.5 WebExtension 구성 결정 (A-13 · A-14 · A-15)

| 항목 | **결정** | 근거 | 판정 |
|:--|:--|:--|:--|
| `manifest_version` | **MV2** | background가 영속이라 **메시지 중계의 수명주기 문제가 없다.** MV3의 이벤트 페이지는 종료된 뒤 첫 메시지의 지연·유실을 별도로 검증해야 하는데, 이 배치는 이미 5단 경로 전체가 [미확인]이라 검증 부담을 더 얹을 여유가 없다. MV2에서는 `web_accessible_resources`도 단순 문자열 배열이다 | A-14 — **plan 단계 결정 사항이었으므로 여기서 확정** |
| `content_scripts.matches` | **`["http://*/*", "https://*/*"]`** | 실제 웹사이트를 빠짐없이 덮으면서 `data:`/`ftp:`의 오리진 의미가 불명확한 경계를 피한다. **어떤 도메인도 제외하지 않으므로 축소가 아니라 스킴 정리다.** REQ-008(naver)·AC-010-4(제2 사이트)를 모두 충족 | A-13 — evaluator 판정 |
| `all_frames` | **`false`** (manifest 기본값을 명시적으로 유지) | 서드파티 광고 iframe까지 브리지를 넣는 것은 사용자가 요구한 범위가 아니다. 사용자가 말한 "외부 사이트"는 **방문한 사이트**를 뜻하므로 요구 축소가 아니다 | A-13 — evaluator 판정 |
| 페이지 세계 노출 방식 | **(가) `web_accessible_resources` + `<script>` 주입** | 표준 WebExtension 기법이고 Xray 우회 트릭이 없어 동작이 예측 가능하다. (나) `exportFunction`은 Promise·객체의 Xray 처리가 **[미확인]**(U-07)이라 차선 | A-15 — evaluator 판정 |
| `nativeMessagingFromContent` 권한 | **선언하지 않는다** | content script가 네이티브 API를 **직접** 호출할 때만 필요하다. 본 설계는 content script → `runtime.sendMessage` → background.js 중계이므로 해당 없음. **불필요한 권한이 있으면 실제 통신 경로를 오독하게 만든다** | — |

### 2.6 P2 오리진 검사 훅 — **채택 확정** (rev.2 신설 — C-6)

rev.1은 이를 "evaluator 판정 대상 P2 제안"으로만 남겼다. **evaluator가 조건부 승인했으므로 rev.2에서 채택으로 확정하고, 조건 3개를 계약으로 못 박는다.**

**무엇을 만드는가:** `background.js`의 함수 화이트리스트와 **같은 위치**에 오리진 검사 훅의 **자리만** 만든다. 분기 1개 + 주석이 전부다.

**왜 만드는가:** requirements §5.1-4가 지적한 장기 리스크 — *"앞으로 브리지에 함수를 추가하면 추가되는 즉시 모든 웹사이트에 열린다"* — 에 대해 **검토 지점을 코드에 고정**한다. 함수를 추가하려는 다음 개발자가 화이트리스트를 고칠 때 그 트레이드오프를 반드시 마주치게 된다.

**지켜야 할 조건 3개 — 하나라도 어기면 사용자 결정(A-08) 위반이다:**

| # | 조건 | 검증 |
|:--|:--|:--|
| **H-1** | **기본값은 "전체 허용"으로 출고한다.** 훅의 기본 분기는 **무조건 통과**해야 한다. **훅을 켠 상태로 출고하는 것은 금지** — REQ-010을 뒤집는 것이다 | `background.js` 코드 검사: 기본 분기가 무조건 통과하는가 |
| **H-2** | **Step 4의 G4-a~e가 훅이 존재하는 상태에서 통과해야 한다.** 특히 **G4-d(naver가 아닌 제2 사이트)** 의 통과가 *"훅이 REQ-010을 좁히지 않았다"*는 **증거**다. 훅을 넣은 뒤 제2 사이트에서 브리지가 죽으면 **즉시 제거하라** | G4-d 통과 로그 |
| **H-3** | 훅 위에 **"이 훅은 기본 비활성이며, 켜는 것은 사용자 결정 사항이다(requirements §5.1, A-08)"** 를 **한글 주석**으로 명시한다 | code-reviewer 대조 (`comment-style.md`) |

**스코프 아웃과의 관계:** requirements §6이 배제한 것은 *"오리진 허용목록의 **기본 활성화**"*이지 훅의 존재가 아니다. **기본 비활성이면 스코프 아웃을 침범하지 않는다.**

---

## 3. 브리지 계약 (assets ↔ Kotlin 결합점)

impact-report가 지목한 결합점 3개 — ① wire JSON 스키마 ② `nativeApp` 식별자 ③ 함수명 집합 — 는 **매직 문자열 불일치 시 예외도 로그도 없이 조용히 실패**한다. 아래를 계약으로 고정한다.

### 3.1 wire 포맷 — **JSON 문자열**로 주고받는다 (설계 결정)

`sendNativeMessage`의 payload를 **객체가 아니라 `JSON.stringify()`한 문자열**로 보내고, 네이티브도 **JSON 문자열**을 resolve한다.

```js
// background.js
const res = await browser.runtime.sendNativeMessage(NATIVE_APP, JSON.stringify(req)); // res: string
const parsed = JSON.parse(res);
```
```kotlin
// NativeBridgeHandler.onMessage(nativeApp, message, sender): GeckoResult<Any>
// message.toString()이 곧 요청 JSON 문자열, 응답도 JSON 문자열을 fromValue로 돌려준다
```

**왜 이렇게 하는가 (반드시 주석으로 남길 WHY):** 이렇게 하면 Kotlin 쪽 파싱/직렬화가 `org.json.JSONObject`(안드로이드 전용, JVM 단위 테스트에서 stub이라 호출 불가)를 **한 줄도 쓰지 않고** `kotlinx.serialization`만으로 끝난다. 그 결과 `BridgeProtocol`이 **완전한 순수 Kotlin**이 되어 JVM 테스트로 커버리지를 벌 수 있다(§7). 객체를 그대로 주고받으면 `BridgeProtocol`이 안드로이드에 묶여 커버리지 목표가 원리적으로 불가능해진다.

> **[미확인 — Step 2 게이트에서 확인]** GeckoView의 native messaging이 최상위 payload로 **문자열**을 허용하는지. 허용하지 않으면 `{"json":"<문자열>"}` 한 겹 래핑으로 전환한다. **래핑해도 `BridgeProtocol`의 순수성은 유지되므로 §7의 커버리지 경로는 바뀌지 않는다.**

### 3.2 메시지 스키마 (양쪽 경로 공통 — AC-003-3)

**요청 (페이지 → background.js → 네이티브)**
```json
{ "id": "<클라이언트 생성 고유 문자열>", "type": "call", "name": "<함수명>", "payload": { } }
```
**응답 (네이티브 → background.js → 페이지)**
```json
{ "id": "<요청 id 그대로>", "type": "result", "ok": true,  "value": <임의 JSON 값> }
{ "id": "<요청 id 그대로>", "type": "result", "ok": false, "error": { "code": "<코드>", "message": "<사람이 읽는 사유>" } }
```
**오류 코드 집합 (닫힌 집합):** `UNKNOWN_FUNCTION` · `INVALID_REQUEST` · `INTERNAL_ERROR`
`AC-006-2`(없는 함수명 호출 시 Promise reject + 사유 판독)는 `UNKNOWN_FUNCTION`으로 충족한다.

**확장 페이지 경로(3단)와 외부 사이트 경로(5단)가 동일한 위 스키마로 `background.js`에 도달한다.** `background.js`의 처리 분기는 **송신자 종류에 따라 갈라지지 않는다** — AC-003-3이 코드 검사로 이를 강제한다.

### 3.3 페이지 세계 ↔ content script 봉투 (외부 사이트 경로에만 존재)

`window.postMessage`는 페이지의 다른 코드와 섞이므로 별도 봉투를 씌운다.
```json
{ "__nativeBridge": true, "direction": "req" | "res", "body": <위 3.2의 요청 또는 응답> }
```
- 수신 측은 `event.source === window` **그리고** `data.__nativeBridge === true` **그리고** `direction`이 기대값일 때만 처리한다. (`direction`이 없으면 자기 메시지를 자기가 다시 받아 무한 루프가 난다.)
- **이 봉투는 `background.js`에 도달하기 전에 벗겨진다.** 즉 3.2 스키마는 두 경로에서 동일하다.

### 3.4 상수화 방침 — 매직 문자열을 파일 2개에만 둔다

| 결합점 | Kotlin | JS | 근거 주석 |
|:--|:--|:--|:--|
| `nativeApp` 식별자 | `BridgeProtocol.NATIVE_APP = "browser"` | `background.js`의 `const NATIVE_APP = "browser"` | **양쪽 파일에 서로를 가리키는 주석을 단다**: "이 문자열은 `<상대 파일 경로>`의 `NATIVE_APP`과 반드시 같아야 한다. 어긋나면 예외도 로그도 없이 아무 일도 일어나지 않는다." |
| 함수명 | `BridgeProtocol.FN_GET_VERSION_NAME` / `FN_APP_FINISH`, 디스패치는 `BridgeDispatcher`의 `when` 하나 | `background.js`의 `const ALLOWED_FUNCTIONS = ["getVersionName", "appFinish"]` | JS 화이트리스트 위에 **"브리지 함수를 추가하면 그 즉시 `matches` 범위의 모든 웹사이트에 열린다. 추가 전 §5.1 트레이드오프를 재검토할 것"** 주석을 고정한다(requirements §5.1) |
| 필드명/오류 코드 | `BridgeProtocol`의 `@Serializable` data class와 `object ErrorCode` | `background.js`·`bridge-client.js`·`page-bridge.js`가 같은 이름 사용 | `BridgeProtocol.kt` 상단 KDoc에 3.2 스키마 전문을 그대로 적는다 — JS 쪽이 참조할 단일 원본 |
| 함수명 문자열의 이중 관리 | Kotlin `when`과 JS `ALLOWED_FUNCTIONS`에 **둘 다** 있어야 한다 | 동일 | 한쪽에만 추가하면: JS에만 추가 → 네이티브가 `UNKNOWN_FUNCTION` reject(**시끄럽게 실패, 안전**). Kotlin에만 추가 → JS가 먼저 막음(**조용히 실패**). **이 비대칭을 양쪽 주석에 적어 "JS를 먼저 고쳐라"를 남긴다** |

### 3.5 클라이언트 파사드 (REQ-006 — 두 경로 동일)

```js
// 두 경로 모두 이 시그니처를 노출한다
window.NativeBridge.call(name /* string */, payload /* object, 생략 가능 */) -> Promise<any>
```
- 확장 페이지(`index.html`): `bridge-client.js`가 `browser.runtime.sendMessage`로 어댑트한다.
- 외부 사이트: `page-bridge.js`(페이지 세계)가 `window.postMessage`로 어댑트하고 `content.js`(격리 세계)가 `browser.runtime.sendMessage`로 중계한다.
- **AC-006-3**(네이티브 함수 추가 시 페이지가 바꾸는 것은 함수명 문자열뿐)은 파사드가 `name`을 그대로 통과시키기 때문에 성립한다. 파사드에 함수별 분기를 넣지 않는다.
- `appFinish` 응답: 네이티브는 **`{ok:true}`를 먼저 resolve한 뒤** `MainUiEvent.Finish`를 방출한다. 다만 프로세스가 죽는 중이므로 **페이지 코드는 이 Promise의 resolve에 의존해서는 안 된다** — `index.html`과 `page-bridge.js` 주석에 명시한다.

---

## 4. 작업 순서

**설계 원칙: 실패하면 파일 구성이 뒤집히는 것부터 실기기로 확인한다.** Step 2·3·4가 requirements.md §7.4가 지정한 최우선 스파이크이며, **이 셋이 끝나기 전에는 UI·아키텍처를 다듬지 않는다.** 스파이크 단계의 코드는 "최소한으로 뚫는" 코드여도 되고, Step 5 이후에 정식 구조로 옮긴다.

**모든 Gradle 호출 앞에 `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`을 붙인다** (§0).

---

### Step 0 — Gradle 스캐폴딩과 툴체인 성립 (GeckoView 없이)

- **대상 파일**: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`, `gradlew`, `gradlew.bat`, `app/build.gradle.kts`, `app/proguard-rules.pro`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/**`, `app/src/main/java/com/example/geckoviewtest/MainActivity.kt`, `.gitignore`
- **변경 내용**: §2.4 툴체인으로 단일 `app` 모듈 생성. namespace/applicationId `com.example.geckoviewtest`, minSdk 26 / compileSdk 36 / targetSdk 36, `compileOptions` Java 17, `buildFeatures.viewBinding = true`, **`buildFeatures.compose`를 설정하지 않는다**. 빈 `MainActivity` + `activity_main.xml`. wrapper는 `gradle wrapper --gradle-version 8.14.5` 산출물.
- **검증 게이트**:
  - `./gradlew :app:assembleDebug`가 **BUILD SUCCESSFUL**이고 `app/build/outputs/apk/debug/app-debug.apk`가 존재한다.
  - `adb install -r` 후 `adb shell am start`로 Activity가 뜨고, `adb shell dumpsys activity activities`에 `com.example.geckoviewtest/.MainActivity`가 보인다.
  - **AC-002-2**: `./gradlew :app:dependencies` 출력에 `androidx.compose` 아티팩트가 **0건**이다.
  - `app/src/main/kotlin/` 디렉터리가 **존재하지 않는다**(`ls` 확인).
- **실패 시 행동 규약**: 툴체인 조합이 §2.4와 다르게 실패하면 **다른 조합을 탐색하지 말고** 실패 로그 전문(`--stacktrace` 포함)을 로그에 남기고 FAIL 반환한다. §2.4는 실측된 조합이므로 여기서의 실패는 환경 차이를 뜻하며, 임의 탐색은 V9 위반이다.

---

### Step 1 — GeckoView 도입과 렌더링 성립 (REQ-001)

- **대상 파일**: `gradle/libs.versions.toml`, `build.gradle.kts`(저장소), `settings.gradle.kts`(`dependencyResolutionManagement`에 `https://maven.mozilla.org/maven2/`), `app/build.gradle.kts`(`abiFilters`), `app/src/main/java/com/example/geckoviewtest/App.kt`, `app/src/main/AndroidManifest.xml`(`android:name=".App"`), `app/src/main/res/layout/activity_main.xml`(`GeckoView` 위젯), `app/src/main/java/com/example/geckoviewtest/MainActivity.kt`, `app/src/debug/AndroidManifest.xml`
- **변경 내용**:
  - GeckoView 의존성 추가. **`ndk { abiFilters += listOf("arm64-v8a") }`를 같은 커밋에 넣는다** — 빠뜨리면 3 ABI가 **비압축 합계 483.1 MiB**로 들어가 APK가 500 MB에 근접한다(§0.3).
  - `App.kt`: `GeckoRuntime`을 `by lazy`로 프로세스당 1회 생성(D-04). debug에서 `consoleOutput(true)` + `remoteDebuggingEnabled(true)`(requirements §2.12 — JS 실행 세계가 3곳이라 안 켜면 원인 분리가 불가능하다).
  - `MainActivity`: `GeckoSession` 생성 → `open(runtime)` → `geckoView.setSession(session)` → `about:blank` 로드.
  - `app/src/debug/AndroidManifest.xml`에 `usesCleartextTraffic="true"`(§2.8 대비 선반영).
- **검증 게이트**:
  - **AC-001-1**: `adb exec-out screencap`으로 받은 스크린샷이 흰/검은 단색이 아니다.
  - **AC-001-2**: `adb shell ps -A | grep com.example.geckoviewtest` 결과에 `:tab_` 또는 `:gpu_` 접미사 자식 프로세스가 **1개 이상** 있다. (WebView로는 나올 수 없는 신호. AC-001-1과 독립적으로 깨진다 — 런타임이 떠도 렌더가 실패할 수 있고, 그 반대도 가능하다.)
  - **AC-001-3**: `grep -r "android.webkit.WebView" app/src` 결과 **0건**.
  - **G1-abi (`abiFilters` 실효 — rev.2에서 판정 방법 교체, C-1)**: **크기가 아니라 APK 내 ABI 목록으로 판정한다.**
    ```
    unzip -l app/build/outputs/apk/debug/app-debug.apk | grep "lib/" \
      | awk -F'lib/' '{split($2,a,"/"); print a[1]}' | sort -u
    # 기대 출력: arm64-v8a   (이 한 줄만)
    ```
    **`arm64-v8a` 외의 ABI가 한 줄이라도 나오면 `abiFilters` 미적용이다.** 출력을 로그에 남긴다.
- **Step 1 산출 기록 항목** (게이트 아님 — 판정하지 않고 숫자만 남긴다):
  - **APK 크기**: `ls -l`로 바이트 수를 기록한다. **U-10 해소용 실측치이며 합격/불합격 판정에 쓰지 않는다.**
  - **설치 시간**: `time adb install -r`의 실측치를 기록한다.

> **[C-1 — rev.1의 150 MB 규칙을 폐기했다. 이것이 rev.2의 가장 중요한 정정이다.]**
> rev.1은 *"APK가 150 MB를 넘으면 `abiFilters`가 적용되지 않은 것이다 … 다음 Step으로 넘어가지 않는다"*로 못 박았다. **이 규칙은 올바른 빌드를 실패로 판정한다.**
> - **`abiFilters = ["arm64-v8a"]`가 정상 적용된 debug APK가 198,587,715 B = 189.4 MiB다** [확인 — evaluator 실측, evaluation.md §0.2 ❶].
> - 원인: **AGP는 minSdk ≥ 23에서 네이티브 `.so`를 비압축(Stored)으로 패키징한다**(시스템이 직접 mmap 하도록). APK 안의 `libxul.so`가 152,735,464 B, 압축률 0%다. requirements §2.2의 "85~95 MB" 추정은 **AAR 내부의 압축 크기**를 더한 값이라 실제의 약 1/2이다(§0.3).
> - **크기는 대리 신호이고 ABI 목록이 직접 신호다**(V1). rev.1은 U-10에 *"추정치를 문서에 굳히지 말 것(V5)"*이라 적고도 **바로 그 추정치 위에 게이트 임계값을 세웠다.** 자기모순이었다.
> - **크기 기반 보조 확인이 꼭 필요하면 임계값은 250 MB다** — 3 ABI 비압축 합계가 483.1 MiB이므로 미적용 시 확실히 넘는다. **189 MiB대는 정상이다.**

- **실패 시 행동 규약**: G1-abi에 `arm64-v8a` 외의 ABI가 나오면 `app/build.gradle.kts`의 `ndk { abiFilters }` 블록이 `defaultConfig` 안에 있는지, `./gradlew :app:dependencies`에 다른 네이티브 의존성이 끼어들었는지를 확인한다. **APK 크기가 189 MiB대인 것 자체는 실패가 아니므로 그것을 이유로 정지하지 마라.**

---

### Step 2 — 【최우선 스파이크 1】확장 설치 + 확장 페이지 로드 + 3단 왕복

> **여기서 막히면 REQ-003·004·005·006·007이 동시에 죽고 §2.7.4 폴백으로 전환해 파일 구성이 바뀐다.** UI를 다 만든 뒤에 발견하면 되돌릴 여지가 없다.

- **대상 파일**: `app/src/main/assets/messaging/manifest.json`, `.../background.js`, `.../index.html`, `.../bridge-client.js`, `app/src/main/java/com/example/geckoviewtest/App.kt`, `.../MainActivity.kt`, `.../bridge/NativeBridgeHandler.kt`, `.../gecko/GeckoResultExt.kt`
- **변경 내용 (최소 구현)**:
  - `manifest.json`: **MV2**(§2.5 결정). `id`·`version` 필수. 권한 `geckoViewAddons` + `nativeMessaging`. **`nativeMessagingFromContent`는 선언하지 않는다**(§2.10 — 본 설계에 불필요하고, 있으면 통신 경로를 오독하게 만든다). `web_accessible_resources`에 `index.html`·`page-bridge.js` 등록.
  - `App.kt`/`MainActivity`: `runtime.webExtensionController.ensureBuiltIn("resource://android/assets/messaging/", "<확장 id>")` → resolve된 `extension.metaData.baseUrl` + `"index.html"`을 `session.loadUri(...)`.
  - `extension.setMessageDelegate(NativeBridgeHandler, "browser")`.
  - `background.js`: `runtime.onMessage` → `sendNativeMessage("browser", JSON.stringify(req))` → 응답을 그대로 반환.
  - **`background.js` 상단에 §2.10의 WHY를 고정 주석으로 남긴다**: "content script에서 `sendNativeMessage`를 직접 부르면 mozilla/geckoview#220의 결함 경로에 들어간다. 이 파일을 거치는 구조는 우회가 아니라 필수다. '한 단계 줄이자'는 리팩터링이 곧바로 버그가 된다."
  - **`resource://android/assets/index.html`을 콘텐츠 페이지로 여는 시도를 하지 않는다.** 결정적 근거는 크래시(#199)가 아니라 **매치 패턴 제약**(`content_scripts.matches`는 `resource://`를 지원하지 않고 넣으면 manifest가 거부된다)이다 — 이 WHY를 `MainActivity`의 로드 지점 주석에 남긴다.
- **검증 게이트 (전부 AND)**:
  - **G2-a (오리진)**: 콜드 스타트 3초 내 스크린샷에 `index.html`의 버튼이 보이고, `about:neterror`나 "파일을 찾을 수 없음"이 **아니다**. (AC-007-1·2)
  - **G2-b (JS 실행)**: 페이지 로드 시 JS가 채우는 요소(브리지 준비 상태 표시)가 화면에 나타난다. (AC-007-3 — 정적 HTML만 그려지고 JS가 죽은 상태를 걸러낸다)
  - **G2-c (왕복)**: `getVersionName` 버튼 → 결과 영역에 버전 문자열이 표시되고, 그 값이 `adb shell dumpsys package com.example.geckoviewtest | grep versionName`과 **문자 단위로 일치**한다. (AC-004-1·2)
  - **G2-e (MV2)**: `manifest_version: 2`로 확장이 설치되었는지. 거부되면 MV3로 전환하고 그 사실을 기록한다.
- **Step 2 산출 기록 항목** (rev.2에서 게이트에서 분리 — C-4. 판정하지 않고 사실만 남긴다):
  - **wire 포맷 판정 (구 G2-d)**: §3.1의 U-04 해소 — 문자열 payload가 그대로 통과했는지, `{"json":...}` 래핑이 필요했는지를 **로그에 단정적으로 기록**한다.
  - **분리 이유**: 술어가 "기록한다"라 **무엇을 기록하든 참이 되므로 AND 결합의 항으로서 없는 것과 같다**(V1). 다만 **거짓 그린을 만들지는 않는다** — wire 포맷이 실제로 틀렸다면 **G2-c가 먼저 깨진다.** 결함이 아니라 분류 오류였다. **기록 요구 자체는 그대로 유지한다**(U-04의 판정 근거).
- **실패 시 행동 규약 (폴백 발동점 1)**:
  - **G2-a 실패**(`moz-extension://` 최상위 네비게이션 거부) → **§2.7.4 폴백 1안**이 필요한 상황이다. **developer는 전환을 임의로 진행하지 않는다** — 아래 §9의 **폴백 발동 절차**를 따라 FAIL 반환한다.
  - **G2-c만 실패**(페이지는 뜨는데 왕복이 안 됨) → 원인 후보를 순서대로 격리한다: ① `nativeApp` 문자열 불일치(가장 흔하고 **무성 실패**한다) ② `geckoViewAddons` 권한 누락 ③ wire 포맷(위 기록 항목). `consoleOutput(true)`가 켜져 있어야 이 격리가 가능하다. **이것은 폴백이 아니라 배선 오류이므로 developer가 직접 고친다.**
  - 3회 시도 후에도 원인이 특정되지 않으면 **추측으로 코드를 넓히지 말고**(V9) 마지막 상태·원시 오류·logcat 구간을 로그에 남기고 FAIL 반환한다.

---

### Step 3 — 【최우선 스파이크 2】naver 이동과 확장 페이지 히스토리 잔존

> **확장 페이지가 세션 히스토리에 남지 않으면 AC-011-1이 원리적으로 불가능하고 §2.7.4 폴백이 필요하다.** REQ-011의 UI를 다듬기 전에 이 사실부터 확인한다.

- **대상 파일**: `app/src/main/java/com/example/geckoviewtest/MainActivity.kt`, `app/src/main/res/layout/activity_main.xml`
- **변경 내용 (최소 구현)**: 임시 버튼으로 `session.loadUri("http://naver.com")`. `OnBackPressedCallback(enabled = true)`를 등록하고 본문에서 무조건 `session.goBack()`. (상태 관리는 Step 7에서 정식화한다.)
- **검증 게이트 (전부 AND)**:
  - **G3-a**: naver 이동 후 뒤로가기 → **`index.html`이 다시 렌더되어 버튼들이 보인다**(스크린샷). 빈 화면·오류 페이지가 아니다. (AC-011-1)
  - **G3-b**: 복귀 **후** `getVersionName`이 다시 동작하고 값이 `dumpsys`와 일치한다. (AC-011-2 — 페이지만 그려지고 확장 메시징이 죽은 상태를 걸러낸다. **이것은 실제로 깨질 수 있는 지점이며 G3-a만으로는 잡히지 않는다.**)
  - **G3-c**: naver 렌더 자체가 성립한다 — 스크린샷에 네이버 콘텐츠 + 표시 URL/`onTitleChange`가 naver.com 도메인. logcat 해당 구간에 `NS_ERROR_`·`about:neterror` 진입 없음. (AC-008-1·2·3)
- **Step 3 산출 기록 항목** (rev.2에서 게이트에서 분리 — C-4):
  - **cleartext 판정 (구 G3-d)**: `http://naver.com`이 차단되는지 여부를 **기록한다**(U-08 해소). 차단되면 `GeckoRuntimeSettings.Builder.allowInsecureConnections(...)`를 조정한다. **https 리다이렉트는 실패가 아니다**(AC-008-2는 도메인 기준).
  - **분리 이유**: G2-d와 같다 — 술어가 "기록한다"라 항상 참이 되어 AND 항으로서 없는 것과 같다(V1). 실제 신호는 **G3-c가 담당한다**(cleartext가 차단되면 naver 렌더가 실패한다). 기록 요구는 유지.
- **실패 시 행동 규약 (폴백 발동점 2)**:
  - **G3-a 실패** → 확장 페이지가 히스토리에 남지 않는 것이므로 **§2.7.4 폴백 1안이 필요한 상황**이다. **developer는 전환을 임의로 진행하지 않는다** — §9의 **폴백 발동 절차**를 따라 FAIL 반환한다.
  - **G3-b만 실패** → 복귀 시 확장 재주입이 필요한 경우이므로 `NavigationDelegate.onLocationChange` 시점의 재바인딩을 검토하되, **REQ-011 범위 밖으로 넓히지 않는다.** 이것은 설계 변경이 아니라 배선 보완이므로 developer가 직접 고친다.

---

### Step 4 — 【최우선 스파이크 3】외부 사이트 5단 경로와 페이지 세계 주입 (REQ-010)

> **이 배치에서 거짓 그린 위험이 가장 높은 지점이다.** content script의 격리 세계 안에서만 브리지가 동작해도 겉보기에는 완벽히 성공으로 보인다(§2.6.1).

- **대상 파일**: `app/src/main/assets/messaging/manifest.json`, `.../content.js`, `.../page-bridge.js`, `.../background.js`
- **변경 내용**:
  - `manifest.json`에 `content_scripts` 추가: **`matches: ["http://*/*", "https://*/*"]`, `all_frames: false`**(§2.5 결정), host permission, `web_accessible_resources`에 `page-bridge.js`.
  - `content.js`(격리 세계): `page-bridge.js`를 `<script>` 태그로 문서에 삽입하고, `window.postMessage` ↔ `browser.runtime.sendMessage`를 §3.3 봉투로 중계한다.
  - `page-bridge.js`(**페이지 세계**): `window.NativeBridge.call`을 정의하고, 검증용 프로브를 실행한다 — `getVersionName` 호출 결과를 고정 id 배지 `#__bridge_probe`에 쓰고 `window.__bridgeProbeRanInPageWorld = true`를 세팅한다.
  - `content.js`는 **`window.wrappedJSObject.__bridgeProbeRanInPageWorld === true`일 때만** 배지에 `PAGE_WORLD` 문자열을 덧붙인다.
  - **`content.js` 상단에 §2.6.1의 WHY를 고정 주석으로 남긴다**: "content script에서 `window.NativeBridge = {...}`로 대입해도 Xray vision 때문에 페이지 세계 JS는 그것을 보지 못한다. `page-bridge.js`를 `web_accessible_resources`로 등록해 `<script>`로 주입하는 것은 우회가 아니라 유일한 방법이다."
  - **`background.js`에 §2.6의 P2 오리진 검사 훅 자리를 만든다** — 기본 분기 무조건 통과(H-1) + 한글 주석(H-3).
- **검증 게이트 (전부 AND — 하나라도 빠지면 검증이 무의미해진다)**:

> **[C-6 / H-2] 아래 G4-a~e는 P2 오리진 검사 훅이 `background.js`에 **존재하는 상태에서** 통과해야 한다.** 특히 **G4-d(제2 사이트)의 통과가 "훅이 REQ-010을 좁히지 않았다"는 증거**다. 훅을 넣은 뒤 제2 사이트에서 브리지가 죽으면 **훅을 즉시 제거하고** 그 사실을 로그에 남겨라 — 사용자 결정(A-08)이 P2 제안보다 우선한다.

  - **G4-a**: `http://naver.com` 로드 후 스크린샷 또는 `adb shell uiautomator dump`에 배지가 보이고 안에 버전 문자열이 있다. (AC-010-1)
  - **G4-b**: 그 값이 `dumpsys package ... | grep versionName`과 **문자 단위 일치**. (AC-010-2)
  - **G4-c**: **배지에 `PAGE_WORLD` 마커가 있다.** (AC-010-3) — **없으면 격리 세계에서만 동작한 것이므로 REQ-010 미충족이다.** 이 조건 없이는 G4-a·G4-b가 모두 참인데도 실제 웹페이지는 브리지를 못 쓰는 상태가 통과한다.
  - **G4-d**: `http://example.com` 등 **naver가 아닌 제2 사이트**에서도 G4-a~c가 성립한다. (AC-010-4 — `matches`가 naver에 하드코딩된 구현을 걸러낸다)
  - **G4-e (회귀)**: G4-a 상태에서 **네이버 콘텐츠 렌더가 여전히 정상**이다. (AC-010-6 — 주입 스크립트가 페이지를 망가뜨리지 않았음)
  - **G4-f (계약 동일성)**: `background.js`에서 요청을 처리하는 분기가 **송신자 종류에 따라 갈라지지 않는다**(코드 검사). (AC-003-3)
  - **AC-010-5(naver에서 `appFinish`)는 앱을 죽이므로 여기서 하지 않는다.** Step 9의 QA 시퀀스 맨 마지막에 실행한다.
- **실패 시 행동 규약**: G4-c만 실패(격리 세계에서는 동작) → §2.6.1 (나) `exportFunction()`/`cloneInto()`로 전환을 검토한다. 단 **Promise·객체의 Xray 처리가 [미확인]**이므로, 전환 시 반환값이 페이지 세계에서 정상적인 Promise인지를 별도 게이트로 추가한다. G4-a부터 실패 → `web_accessible_resources` 등록 누락 / `matches` 스킴 / content script 주입 시점(`document_idle` vs `document_start`)을 순서대로 격리한다.

> **경계**: Step 2의 통과는 Step 4를 보장하지 않는다. 전송 경로가 물리적으로 다른 파일(`bridge-client.js` vs `content.js`+`page-bridge.js`)로 갈라져 있다. **두 경로의 결과를 서로의 근거로 인용하지 않는다**(AC-006-4, requirements §7.4-11).

---

### Step 5 — 아키텍처 정착과 JVM 테스트 (Step 2~4 통과 후)

> 여기서부터 설계가 확정된 상태이므로 §2.1의 결정대로 구조를 세운다. **커버리지 70%는 이 Step에서 벌린다.**

- **대상 파일**: `app/src/main/java/com/example/geckoviewtest/bridge/BridgeProtocol.kt`, `.../bridge/BridgeDispatcher.kt`, `.../bridge/NativeBridgeHandler.kt`, `.../data/AppInfoRepository.kt`, `.../data/AppInfoRepositoryImpl.kt`, `.../MainViewModel.kt`, `.../MainUiState.kt`, `.../App.kt`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `app/src/test/java/com/example/geckoviewtest/bridge/BridgeProtocolTest.kt`, `.../bridge/BridgeDispatcherTest.kt`, `.../data/AppInfoRepositoryTest.kt`, `.../MainViewModelTest.kt`
- **변경 내용**:
  - **`BridgeProtocol.kt` — 순수 Kotlin.** `kotlinx.serialization`의 `@Serializable` data class로 §3.2 스키마 정의 + `parseRequest(json: String): Result<BridgeRequest>` + `encodeResult(...)`. `NATIVE_APP`·함수명·오류 코드 상수. **안드로이드 타입 0개.**
  - **`BridgeDispatcher.kt` — 순수 Kotlin.** `suspend fun handle(req: BridgeRequest): BridgeResult`. `getVersionName` → `AppInfoRepository`, `appFinish` → 주입받은 `BridgeHost`(순수 인터페이스) 호출. 미지원 이름 → `UNKNOWN_FUNCTION`.
  - **`NativeBridgeHandler.kt` — 얇은 어댑터.** `WebExtension.MessageDelegate` 구현. `message.toString()` → `BridgeProtocol` → `geckoResultOf { dispatcher.handle(...) }`. **로직을 두지 않는다**(20줄 내외).
  - **`AppInfoRepositoryImpl`** — D-02대로 `versionNameProvider: () -> String?`를 받고 null이면 `UNKNOWN` 폴백.
  - **`MainViewModel`** — `MainUiState` StateFlow + `Channel<MainUiEvent>`. 메서드: `onPageStart()`, `onPageStop(success)`, `onCanGoBackChanged(b)`, `onLocationChanged(url)`, `onNaverClicked()`, `onBackPressed()`, `onBridgeResult(...)`. Dispatcher 생성자 주입(D-11).
  - **`App.kt`** — `AppContainer`(수동 DI, §2.2) + `GeckoRuntime` lazy + Application 스코프 `CoroutineScope`(D-10). `PackageManager` 호출은 여기 한 곳.
  - **JaCoCo 설정** (§7.2에 검증된 스크립트).
- **검증 게이트**:
  - `./gradlew :app:jvmCoverageReport`가 성공하고 `app/build/reports/jacoco/jvmCoverageReport/jvmCoverageReport.xml`이 생성된다.
  - **§7.1 표의 각 클래스가 라인 커버리지 70% 이상**이다(XML의 `<class>` 단위 `LINE` counter로 판정).
  - **V2 — RED 확인**: 새로 만든 테스트 스위트마다 프로덕션 코드를 의도적으로 한 줄 깨뜨려 **해당 테스트가 실제로 실패하는지** 확인하고, 원복 후 다시 통과하는 것까지 **양쪽 결과를 로그에 기록**한다. 최소 4건: `BridgeProtocol`(스키마 필드명), `BridgeDispatcher`(`UNKNOWN_FUNCTION` 분기), `AppInfoRepositoryImpl`(null 폴백), `MainViewModel`(`isLoading` 전이).
  - `MainViewModel` 테스트는 `Dispatchers.setMain` + Turbine 패턴을 쓴다(`architecture.md`).
  - **테스트 클래스 KDoc에 "무엇을 보장하고 무엇은 보장하지 않는가"를 적는다**(`comment-style.md` T1). 특히 **"`index.html` 경로 통과가 외부 사이트 경로를 보장하지 않는다"**를 `BridgeProtocolTest`에 명시한다.
- **실패 시 행동 규약**: 특정 클래스가 70%에 못 미치면 **테스트를 늘리기 전에 그 클래스에 프레임워크 의존이 섞였는지부터 본다.** §7의 전제는 "순수 Kotlin으로 설계되어야만 성립한다"이므로, 미달은 대개 테스트 부족이 아니라 설계 오염의 신호다.

---

### Step 6 — UI 정식화: XML 레이아웃 · naver 버튼 · 로딩 UI (REQ-002·008·009)

- **대상 파일**: `app/src/main/res/layout/activity_main.xml`, `app/src/main/res/values/strings.xml`, `app/src/main/java/com/example/geckoviewtest/MainActivity.kt`, `.../gecko/AppProgressDelegate.kt`, `app/src/main/res/xml/network_security_config.xml`(필요 시)
- **변경 내용**: `GeckoView` + `ProgressBar`(또는 로딩 오버레이) + naver 버튼을 XML로 구성. Activity는 `repeatOnLifecycle(STARTED) { uiState.collect { render(it) } }` **단일 render 함수**로 바인딩(D-06). `AppProgressDelegate`는 `onPageStart`/`onPageStop(success)`를 **ViewModel에 전달만** 하고 View를 건드리지 않는다(D-07). UI 텍스트는 `strings.xml`(D-13).
- **검증 게이트 (전부 AND)**:
  - **AC-002-1**: `activity_main.xml`이 존재하고 Activity가 ViewBinding으로 실제 사용한다.
  - **AC-009-1**: naver 버튼 탭 **직후 1초 이내** 스크린샷에 로딩 UI가 **보인다**.
  - **AC-009-2 + AC-009-3**: 로드 완료 후 스크린샷에 로딩 UI가 **보이지 않으면서 동시에** 네이버 콘텐츠가 렌더돼 있다. (2번만 보면 "로딩 UI가 애초에 안 뜨는 구현"이 통과한다 — 1·2·3을 AND로 묶어야 의미가 생긴다.)
  - **AC-009-4 (실패 경로)**: `adb shell cmd connectivity airplane-mode enable` 상태에서 버튼을 누르면 로딩 UI가 뜬 뒤 **반드시 사라진다**(무한 로딩 금지). `onPageStop(success=false)` 경로가 실제로 UI를 내리는지 확인한다.
  - **⚠ 확인 직후 반드시 `airplane-mode disable`로 해제하고, 해제됐음을 조회로 확인한다.** 켜둔 채로 넘어가면 **이후 모든 네트워크 검증(G3-c, Step 4 전체, Step 9 회귀)이 거짓 레드가 되고 원인 추적에 시간을 통째로 날린다.** 이 명령이 이 기기에서 동작함은 evaluator가 확인했다.
  - **코드 검사**: `MainActivity`와 델리게이트에서 `ProgressBar.visibility`를 `render()` 밖에서 대입하는 곳이 **0건**이다(D-07).
- **실패 시 행동 규약**: AC-009-1이 재현되지 않으면 **비결정성으로 단정하지 말고 5회 반복 후 "N회 중 M회"로 보고한다**(V5). 관측 실패 시 마지막 `MainUiState` 값과 해당 구간 logcat을 함께 남긴다.

---

### Step 7 — 뒤로가기 정식화 (REQ-011)

- **대상 파일**: `app/src/main/java/com/example/geckoviewtest/gecko/AppNavigationDelegate.kt`, `.../MainViewModel.kt`, `.../MainActivity.kt`, `gradle/libs.versions.toml`, `app/build.gradle.kts`(`androidx.activity`), `app/src/test/java/com/example/geckoviewtest/MainViewModelTest.kt`
- **변경 내용**: D-08의 4단 배선. `androidx.activity:activity-ktx`를 버전 카탈로그에 추가(GeckoView가 끌고 오지 않는다).
- **검증 게이트 (AC-011-4가 핵심 — 두 케이스를 **둘 다** 관측해야 한다)**:
  - **AC-011-1**: index.html → naver → 뒤로가기 → index.html 재렌더(스크린샷).
  - **AC-011-2**: 복귀 후 `getVersionName`이 다시 동작하고 값이 `dumpsys`와 일치.
  - **AC-011-3**: index.html 상태(웹 히스토리 없음)에서 뒤로가기 → Activity 종료 AND 같은 구간 logcat에 `FATAL EXCEPTION` **없음**.
  - **AC-011-4**: 위 1과 3이 **둘 다** 관측된다. **한쪽만 보면 콜백이 항상 켜져 있거나 항상 꺼져 있는 구현을 잡지 못한다.**
  - JVM 테스트: `onCanGoBackChanged(true/false)` → `uiState.canGoBack` 전이, `onBackPressed()` → `MainUiEvent.NavigateBack` 방출.
- **실패 시 행동 규약**: 화면 회전이 섞이면 페이지가 리로드되어 원인이 혼동된다(스코프 아웃된 알려진 한계). **검증 중 회전을 시키지 않는다.**

---

### Step 8 — `appFinish` 정식화와 계약 오류 경로 (REQ-005·006)

- **대상 파일**: `app/src/main/java/com/example/geckoviewtest/MainViewModel.kt`, `.../MainActivity.kt`, `.../bridge/BridgeDispatcher.kt`, `app/src/main/assets/messaging/index.html`, `.../bridge-client.js`, `.../background.js`
- **변경 내용**: `appFinish` → `BridgeHost` → ViewModel → `MainUiEvent.Finish` → Activity `finish()`(D-03). `background.js`의 `ALLOWED_FUNCTIONS` 화이트리스트와 §3.4 주석 확정. `index.html`에 없는 함수명을 호출하는 검증용 버튼을 추가한다.
- **검증 게이트**:
  - **AC-005-1**: 버튼 탭 후 `adb shell dumpsys activity activities`에서 Activity가 사라진다.
  - **AC-005-2**: 같은 시점 logcat에 `FATAL EXCEPTION` / `Fatal signal` / `libc: Fatal`이 **없다**. (이 조건이 없으면 크래시로 죽은 것을 정상 종료로 오판한다.)
  - **AC-005-4**: 종료 판정 후 **최소 3초** logcat을 더 수집해 지연 크래시가 없음을 확인한다(V1의 "늦게 오는 오류").
  - **AC-005-3**: 재실행하면 REQ-007이 정상 동작한다(런타임/확장 상태 미오염).
  - **AC-006-2**: 없는 함수명 호출 시 Promise가 **reject**되고 페이지에서 `UNKNOWN_FUNCTION`과 사유 문자열을 읽을 수 있다.
  - **AC-006-3**: 네이티브에 함수를 하나 추가할 때 페이지에서 바꾸는 코드가 **함수명 문자열뿐**임을 코드 검사로 확인한다.

---

### Step 9 — 회귀 검증 · 역주입 실패 테스트 · 커버리지 (전 REQ)

- **대상 파일**: `app/src/main/assets/messaging/background.js`(일시 수정 후 원복), `app/src/test/**`, `app/src/androidTest/**`
- **변경 내용**: 계측 테스트가 필요하면 `app/src/androidTest/**`에 추가하되 **환경 한정 테스트에는 어노테이션 필터 + `Assume` 가드를 둘 다** 적용한다(V4). 어디서나 통과하는 테스트에 환경 한정 표식을 붙이지 않는다.
- **검증 게이트**:
  - **AC-003-2 (역주입 실패 테스트, V2)**: `background.js`의 `sendNativeMessage` 호출 한 줄을 주석 처리해 빌드·설치하면 REQ-004의 화면 결과가 **표시되지 않거나 타임아웃**된다. 원복 후 다시 성공한다. **두 결과를 모두 QA 로그에 기록한다.** 이 테스트가 없으면 "항상 초록인 장식"과 구분할 수 없다.
  - **AC-004-3**: `build.gradle.kts`의 `versionName`을 다른 값으로 바꿔 재빌드하면 화면 표시 값도 바뀐다. (하드코딩 상수를 반환하는 가짜 구현을 걸러낸다.) 확인 후 원복.
  - **전체 회귀**: Step 2·3·4·6·7의 게이트를 **순서대로 다시** 통과시킨다.
  - **커버리지**: `jvmCoverageReport` XML로 §7.1의 클래스별 70% 재확인.
  - **AC-010-5는 맨 마지막**: naver.com에서 `appFinish` 호출 → 앱이 실제로 종료된다 AND logcat에 `FATAL EXCEPTION` 없음. **이것은 결함이 아니라 사용자가 선택한 의도된 동작이며(requirements §5.1), 이 케이스의 통과가 A-08 결정이 실현됐다는 증거다.** 앱이 죽으므로 이 뒤에는 아무 케이스도 두지 않는다.
- **화이트리스트 대조 (impact-report risk_notes 1번)**: 가드는 Edit/Write에만 걸리고 **Bash로 만든 파일은 검사조차 받지 않는다.** developer는 종료 전 `find app gradle settings.gradle.kts build.gradle.kts -type f`로 실제 파일 트리를 뽑아 화이트리스트와 **직접 대조한 결과를 로그에 남긴다.**

---

## 5. 리스크 지점

| # | 리스크 | 발생 조건 | 완화 / 롤백 |
|:--|:--|:--|:--|
| R-01 | **`abiFilters` 미적용 시 APK가 500 MB에 근접한다** (rev.2에서 서술 정정 — C-1·C-2) | `app/build.gradle.kts`에 `abiFilters` 누락. AAR에 3 ABI가 동봉되고 **APK에서는 비압축(Stored)으로 들어가 합계 483.1 MiB**가 된다(§0.3). 기기 저장공간과 전송 시간이 실제 부담이 되는 지점이다 | Step 1에서 GeckoView 의존성과 `abiFilters`를 **같은 커밋에** 넣는다. **판정은 크기가 아니라 `G1-abi`의 ABI 목록으로 한다.** APK 크기는 기록만 하고 판정에 쓰지 않는다 |
| R-02 | **`JAVA_HOME`이 JDK 8이라 AGP가 뜨지 않는다** | 셸 환경변수 `JAVA_HOME` = `jdk1.8.0_333`(§0 실측). PATH 기본 java도 17이라 **JDK 21이 필요한 조합에서 어긋날 수 있다** | 모든 Gradle 호출에 `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` 명시. `gradle.properties`에 `org.gradle.java.home`을 박지 않는다(머신 고유 경로) |
| R-03 | **AGP·Gradle wrapper 조합 오선택** | 시스템 gradle이 9.3.1이라는 이유로 wrapper를 9.3.1로 맞추면 AGP 9.3.1이 `NoClassDefFoundError`로 죽는다 **[확인 — 실제 재현]** | §2.4의 **실측 조합**(AGP 8.13.2 / wrapper 8.14.5 / Kotlin 2.3.21)을 쓴다. wrapper는 `gradle wrapper --gradle-version 8.14.5` 산출물 |
| R-04 | **Kotlin stdlib 버전 충돌** | GeckoView가 `kotlin-stdlib 2.3.21`을 끌고 온다. 컴파일러가 그보다 낮으면 경고·비호환 (AGP 9 내장 Kotlin은 2.2.10 **[확인]**) | KGP를 **2.3.21로 명시**해 stdlib과 일치시킨다. `:app:dependencies`로 `kotlin-stdlib:2.3.21` 해석을 확인했다 **[확인]** |
| R-05 | **`nativeApp` 문자열 불일치 → 무성 실패** | `setMessageDelegate(d, "browser")`와 `sendNativeMessage("browser", …)`가 어긋남. **예외도 로그도 없다.** 컴파일러·린트가 못 잡는다 | §3.4대로 양쪽을 상수화하고 서로를 가리키는 주석을 단다. Step 2의 실패 격리 순서 첫 번째 항목으로 지정 |
| R-06 | **페이지 세계 격리로 인한 거짓 그린** | `content.js`에서 `window.NativeBridge`를 직접 대입 → 격리 세계에서만 동작. **겉보기에는 완벽한 성공** | `page-bridge.js`를 `web_accessible_resources`로 등록해 `<script>` 주입(§2.6.1 (가)). **G4-c(PAGE_WORLD 마커)를 게이트로 강제.** `content.js` 상단에 WHY 주석 고정 |
| R-07 | **`GeckoRuntime`을 Activity에서 생성 → 회전 시 크래시** | 프로세스당 1회 제약 위반. `App.kt`와 `AndroidManifest.xml`의 `android:name` 중 **한쪽만** 만들면 발생 | D-04. 두 파일을 **쌍으로** 처리한다(impact-report risk_notes) |
| R-08 | **확장 페이지 최상위 로드 / 히스토리 잔존 실패 → 파일 구성 변경** | `moz-extension://` 최상위 네비게이션 거부, 또는 히스토리에 안 남음. **둘 다 [미확인]** | Step 2·3을 최우선 스파이크로 배치. 실패 시 **developer는 임의 전환하지 말고 FAIL 반환** → planner 재실행 → **evaluator 재승인**(§9의 폴백 발동 절차, C-7). **폴백 1안 자체의 성립도 [미확인]이므로**(확장의 파일 접근 권한 부여 수단이 GeckoView에 있는지 불명) **검증된 탈출구가 없다** — 이것이 스파이크를 최우선에 두는 이유다 |
| R-09 | **두 전송 경로 중 한쪽만 검증 → 거짓 그린** | `index.html`만 테스트. 외부 사이트 경로(`content.js`+`page-bridge.js`)는 한 줄도 실행되지 않은 채 초록 | Step 2와 Step 4를 **별도 게이트**로 분리했다. AC-006-4대로 **"AC-010 실패 시 REQ-006도 미충족"**. 한쪽 결과를 다른 쪽 근거로 인용 금지 |
| R-10 | **#220 결함 경로로의 "단순화" 리팩터링** | 다음 개발자가 "content script에서 바로 `sendNativeMessage` 부르면 한 단계 줄겠네"라고 판단 | `background.js` 상단 WHY 주석 고정(Step 2). **주석이 유일한 방어 수단이다** — 코드로는 막을 수 없다 |
| R-11 | **커버리지 70% 미달** | `BridgeProtocol`·`BridgeDispatcher`·`MainViewModel`에 프레임워크 의존이 섞이면 원리적으로 불가능 | §7의 설계 제약(§3.1의 문자열 wire, D-02의 람다 seam, D-08의 이벤트 경유)이 전부 이 목표에 종속되어 있다. 미달은 테스트 부족이 아니라 **설계 오염의 신호**로 읽는다 |
| R-12 | **REQ-010 산출물이 JaCoCo 분모에 0 기여** | 전량 순수 JS(`content.js`·`page-bridge.js`) | coverage-reporter가 **회귀나 테스트 부실로 오판하지 않도록** §7.3에 경계를 명시했다 |
| R-13 | **`app/src/main/kotlin/`과 `java/` 병용** | 스캐폴딩 도구가 `kotlin/`을 만들면 발생. **evaluator가 hook 로직을 직접 실행해 `app/src/main/kotlin/**`가 ALLOW됨을 확인했다** — 가드는 이것을 막지 **못한다** | §2.3에서 `java/`로 확정. **자동 통제가 0이므로 Step 0 게이트의 `ls` 확인과 code-reviewer 대조가 유일한 방어다.** developer가 손으로 지켜야 한다 |
| R-14 | **Bash 생성 파일이 화이트리스트 검사를 받지 않는다** | 가드는 Edit/Write/NotebookEdit에만 걸린다. 스캐폴딩은 대부분 Bash | Step 9에서 실제 파일 트리를 화이트리스트와 직접 대조한 결과를 로그에 남긴다 |
| R-15 | **실기기가 API 33인데 targetSdk 36** | targetSdk 36의 동작 변경 중 API 33 기기에서 재현되지 않는 것이 있을 수 있다 **[미확인]** | 이번 스코프(웹뷰 렌더·메시징)에서는 문제되지 않을 것으로 보나 기록해 둔다. 이상 동작 시 이 항목을 먼저 의심 |
| R-16 | **에뮬레이터에서 앱이 뜨지 않는다** (rev.2에서 문구 정정 — C-5) | AAR에 **32비트 `x86`이 없다** [확인] — 32비트 x86 에뮬레이터는 어떤 설정으로도 불가. **`x86_64`는 AAR에 있으나**, 이 배치가 `abiFilters`를 **arm64-v8a 단일**로 좁히므로 그 설정에서는 x86_64 에뮬레이터에서도 뜨지 않는다 | **모든 검증은 실기기 `R3CN60L0QMT`(SM-G981N, arm64-v8a)에서 한다.** 에뮬레이터 검증을 계획에 넣지 않는다(V3). **"이 `abiFilters` 설정에서는 성립하지 않는다"가 정확한 서술이며, `abiFilters`에 `x86_64`를 넣으면 에뮬레이터도 가능하다** — 그렇게 하지 않는 이유는 원리적 불가가 아니라 **기기 1대·ABI 1종이 검증 루프에 유리**하기 때문이다 |

---

## 6. 승인 요청 — 새 라이브러리 · 빌드 설정 변경

### 6.1 새 라이브러리 (전부 `gradle/libs.versions.toml`에서 관리 — D-12)

| # | 아티팩트 | 버전 | 필요 REQ | 승인이 필요한 이유 |
|:--|:--|:--|:--|:--|
| L-01 | **`org.mozilla.geckoview:geckoview`** | `153.0.20260730155536` — **최신 stable이 아니라 "검증 완료된 고정 버전"이다** (C-8②). **[확인 — evaluator가 이 버전으로 `assembleDebug` 성공]** | REQ-001 전부 | **APK 189.4 MiB (198,587,715 B) — evaluator 실측.** AAR 229.5 MiB, 3 ABI 동봉(비압축 합계 483.1 MiB). MPL 2.0. **`abiFilters`와 함께여야 승인이다**(R-01). **`<release>`가 `153.0.20260803132010`으로 이동했으나 좇아 올리지 않는다** — A-12(동적 버전 금지) + 이 버전만이 실제로 빌드·검증됐다(§0.4) |
| L-02 | **Maven 저장소 `https://maven.mozilla.org/maven2/`** | — | REQ-001 | Google/Maven Central 밖의 새 저장소. `settings.gradle.kts`의 `dependencyResolutionManagement`에 추가 |
| L-03 | `androidx.activity:activity-ktx` | `1.13.0` (최신 stable **[확인]**) | REQ-011 | `OnBackPressedDispatcher`. GeckoView가 끌고 오지 않는다 |
| L-04 | `androidx.appcompat:appcompat` | `1.7.1` **[확인]** | REQ-002 | XML 테마·`AppCompatActivity` |
| L-05 | `androidx.lifecycle:lifecycle-viewmodel-ktx`, `lifecycle-runtime-ktx` | `2.11.0` **[확인]** | REQ-004·005·008·009·011 | ViewModel + `repeatOnLifecycle` |
| L-06 | `androidx.constraintlayout:constraintlayout` | `2.2.2` **[확인]** | REQ-002·008·009 | XML 레이아웃 |
| L-07 | **`org.jetbrains.kotlinx:kotlinx-serialization-json`** + 플러그인 `org.jetbrains.kotlin.plugin.serialization` | `1.9.0` / 플러그인은 Kotlin과 동일 `2.3.21` **[확인 — planner 빌드(GeckoView 미포함) + evaluator 빌드(GeckoView 포함)에서 각각 성공]** | REQ-003·006 | **§7의 커버리지 경로가 이 선택에 종속된다.** `org.json`은 안드로이드 전용이라 JVM 단위 테스트에서 stub(`UnsupportedOperationException`)이고, 쓰면 `BridgeProtocol`이 JVM 테스트 불가가 되어 70% 목표가 **산술적으로** 무너진다. 대안(Robolectric)은 아티팩트를 더 늘리고 테스트를 느리게 만든다. **evaluator가 이 조합으로 순수 Kotlin 클래스의 JVM LINE 100%를 실증했다.** 최신은 1.11.0이나 **실제로 빌드한 것은 1.9.0**이다(U-14) |
| L-08 | `junit:junit` | `4.13.2` **[확인]** | 테스트 | `architecture.md` 요구 |
| L-09 | `org.jetbrains.kotlinx:kotlinx-coroutines-test` | `1.11.0` **[확인 — 최신 stable]** | 테스트 | `architecture.md`가 명시적으로 요구 |
| L-10 | `app.cash.turbine:turbine` | `1.2.1` **[확인 — 최신 stable]** | 테스트 | `architecture.md`가 "Turbine(또는 동등)" 명시 |
| L-11 | JaCoCo (Gradle `jacoco` 플러그인) | toolVersion `0.8.15` **[확인 — 실제 리포트 생성]** | 커버리지 | §7.2. 라이브러리가 아닌 빌드 플러그인 |

> **Hilt는 요청하지 않는다** (§2.2). **NanoHTTPD 등 localhost 서버 의존성도 요청하지 않는다** — §2.7.4 폴백 3안이 발동할 때만 필요하며, 그 시점에 **별도 승인**을 받는다.

### 6.2 빌드 설정 변경 (`architecture.md` "빌드 설정 변경은 plan.md에 리스크 항목으로 명시된 경우만 허용")

| # | 설정 | 값 | 근거 / 리스크 |
|:--|:--|:--|:--|
| B-01 | **`minSdk`** | **26** | **선택이 아니라 하한.** AAR의 `<uses-sdk android:minSdkVersion="26" />`를 직접 추출해 확인 **[확인]**. 그 미만은 manifest merge 단계에서 빌드 실패 |
| B-02 | `compileSdk` / `targetSdk` | **36 / 36** | 사용자 확정. `androidx.core 1.18.0`(GeckoView 전이)이 높은 compileSdk를 요구. android-36 설치 확인 **[확인]**. 실기기는 API 33 → R-15 |
| B-03 | **AGP** | **8.13.2** | §2.4에서 실제 `assembleDebug`로 검증 **[확인]**. AGP 9는 R-04(stdlib 불일치)와 D-12(버전 카탈로그) 충돌로 배제 |
| B-04 | **Gradle wrapper** | **8.14.5** | §2.4 검증 **[확인]**. **시스템 gradle 9.3.1을 그대로 쓰지 않는다** — AGP 9.3.1+Gradle 9.3.1이 실제로 죽는 것을 재현했다(R-03) |
| B-05 | Kotlin (KGP) | **2.3.21** | GeckoView의 `kotlin-stdlib 2.3.21`과 일치 **[확인]** |
| B-06 | `compileOptions` / `jvmTarget` | Java **17** | Mozilla 공식 quick-start가 Java 17 호환 플래그를 요구 **[문서]** |
| B-07 | **`ndk { abiFilters += listOf("arm64-v8a") }`** | debug | **R-01.** 실기기가 arm64-v8a **[확인]**. 이 설정이 들어갈 파일은 `app/build.gradle.kts` 하나뿐 |
| B-08 | `buildFeatures.viewBinding` | `true` | REQ-002. **`buildFeatures.compose`는 설정하지 않는다**(AC-002-2) |
| B-09 | `usesCleartextTraffic="true"` | **debug 소스셋 한정** | REQ-008이 `http://naver.com`을 명시. Gecko가 Android network security config를 따르는지는 **[미확인]**이나 넣어서 무해하다. release는 스코프 아웃이므로 debug에만 넣는다 |
| B-10 | `enableUnitTestCoverage = true` + `jacoco` 플러그인 + 커스텀 `jvmCoverageReport` 태스크 | debug | §7.2 |
| B-11 | debug `GeckoRuntimeSettings`: `consoleOutput(true)`, `remoteDebuggingEnabled(true)` | debug | JS 실행 세계가 3곳(페이지/격리/background)이라 안 켜면 **어디서 죽었는지 분리 자체가 불가능**하다 |

---

## 7. 커버리지 70% 달성 경로 (클래스 단위)

**전제:** 계측 테스트는 JaCoCo에 집계되지 않고(V8), REQ-010 산출물은 전량 순수 JS라 분모에 0 기여하며, GeckoView는 실기기 없이 실행조차 안 된다. **따라서 70%는 JVM 테스트가 닿는 순수 Kotlin 레이어에서만 벌 수 있고, 그것은 설계가 그렇게 되어 있을 때만 성립한다.**

### 7.1 분모에 포함 — JVM 단위 테스트 대상

| 클래스 | 성격 | 무엇을 테스트하는가 | 목표 |
|:--|:--|:--|:--|
| `bridge/BridgeProtocol.kt` | **순수 Kotlin** (`kotlinx.serialization`만) | §3.2 스키마 직렬화/역직렬화, 필수 필드 누락 → `INVALID_REQUEST`, 잘못된 JSON → 예외 대신 실패 결과, 오류 응답 인코딩 | ≥90% |
| `bridge/BridgeDispatcher.kt` | **순수 Kotlin** (`AppInfoRepository`·`BridgeHost` 인터페이스만 의존) | `getVersionName` 위임, `appFinish` 위임, **미지원 함수명 → `UNKNOWN_FUNCTION`**, repository 예외 → `INTERNAL_ERROR` | ≥90% |
| `data/AppInfoRepositoryImpl.kt` | **순수 Kotlin** (D-02의 람다 seam 덕분) | provider가 값을 주면 그대로 반환, **null이면 `UNKNOWN` 폴백** | 100% |
| `MainViewModel.kt` | **순수 Kotlin** (Dispatcher 주입, Context 없음) | `onPageStart`→`isLoading=true`, `onPageStop(true/false)`→`false`(**둘 다**), `onCanGoBackChanged` 전이, `onNaverClicked`→`Navigate` 이벤트, `onBackPressed`→`NavigateBack`, `appFinish`→`Finish` 이벤트 | ≥85% |
| `MainUiState.kt` (state + event sealed class) | data class | 위 테스트가 전이적으로 커버 | ≥80% |

**이 표의 클래스들이 순수 Kotlin이 되도록 만든 설계 결정 3개** — 하나라도 무너지면 70%가 원리적으로 불가능해진다:
1. **§3.1 wire를 JSON *문자열*로** → `BridgeProtocol`이 `org.json`을 안 쓴다.
2. **D-02의 `versionNameProvider` 람다 seam** → `AppInfoRepositoryImpl`이 `Context`를 안 든다.
3. **D-08의 이벤트 경유 뒤로가기** → 뒤로가기 로직이 `OnBackPressedCallback` 없이 ViewModel에서 검증된다.

### 7.2 JaCoCo 설정 — **실제로 실행해 확인한 스크립트** (`app/build.gradle.kts`)

```kotlin
// android { ... } 안
buildTypes { debug { enableUnitTestCoverage = true } }
testCoverage { jacocoVersion = libs.versions.jacoco.get() }

// 최상위
jacoco { toolVersion = libs.versions.jacoco.get() }

val coverageExclusions = listOf(
    "com/example/geckoviewtest/App.class", "com/example/geckoviewtest/App$*.class",
    "com/example/geckoviewtest/MainActivity*.class",
    "com/example/geckoviewtest/gecko/**",
    "com/example/geckoviewtest/bridge/NativeBridgeHandler*.class",
    "**/BuildConfig.*", "**/R.class", "**/R$*.class", "**/databinding/**",
)
tasks.register<JacocoReport>("jvmCoverageReport") {
    dependsOn("testDebugUnitTest")
    reports { xml.required.set(true); html.required.set(true) }
    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) { exclude(coverageExclusions) }
    )
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(
        layout.buildDirectory.file("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
    )
}
```
**증거 출처 (rev.2에서 정정 — C-3):**

| 누가 | 무엇을 확인했나 |
|:--|:--|
| planner | 스크립트가 동작하고 `.exec` 경로가 맞으며 `gecko/**` 제외가 실효함. **단 GeckoView가 없는 프로젝트였고 `jacocoVersion`도 0.8.13이었다** — 즉 **승인 요청한 0.8.15 조합은 planner가 검증하지 않았다** |
| **evaluator** | **[확인 — evaluation.md §0.3]** **GeckoView를 포함하고 `jacocoVersion 0.8.15`인 프로젝트**에서 위 스크립트 원문으로 `./gradlew :app:jvmCoverageReport --rerun-tasks`(캐시 배제, V7) **성공**, XML 생성, **`gecko/**`·`MainActivity` 제외 실효**, `kotlinx-serialization` 기반 순수 Kotlin 클래스 **LINE 100%** |

**따라서 "이 JaCoCo 설정이 GeckoView가 들어간 상태에서도 동작한다"의 출처는 evaluator 실측이다.**

리포트 경로: `app/build/reports/jacoco/jvmCoverageReport/jvmCoverageReport.xml`

### 7.3 분모에서 제외 — 계측/수동 검증 영역 (경계선)

| 클래스/파일 | 왜 제외하는가 | 무엇으로 검증하는가 |
|:--|:--|:--|
| `App.kt` | `GeckoRuntime` 생성 + `PackageManager` 호출. 프레임워크 그 자체 | Step 1 게이트(AC-001-2 자식 프로세스) |
| `MainActivity.kt` | View 바인딩·세션 소유·`OnBackPressedCallback` | Step 6·7 실기기 게이트 |
| `gecko/**` (`GeckoResultExt`, `AppProgressDelegate`, `AppNavigationDelegate`) | GeckoView 콜백 인터페이스 구현. 실기기 없이 실행 불가 | Step 3·6·7 실기기 게이트 |
| `bridge/NativeBridgeHandler.kt` | `WebExtension.MessageDelegate` 어댑터. **로직을 두지 않기로 했으므로**(Step 5) 20줄 내외 | Step 2 게이트(3단 왕복) + AC-003-2 역주입 테스트 |
| **`assets/messaging/**` 전량 (JS/HTML)** | JaCoCo 대상이 아님 | Step 2·4 실기기 게이트 |

> **coverage-reporter에게**: 이 배치는 rev.2에서 작업량(파일 수)이 늘었는데 **커버리지 분모는 거의 늘지 않는다** — REQ-010 산출물이 전량 JS이기 때문이다. **이것을 회귀나 테스트 부실로 판정하지 말 것.** 제외 목록은 §7.2의 `coverageExclusions`에 코드로 박혀 있어 임의 확대가 diff에 드러난다.

---

## 8. [미확인] 목록 — developer가 검증할 지점

이 계획이 **확인하지 못한 것**이다. 추측으로 채우지 않았다. 각 항목에 검증 시점을 지정했다.

| # | [미확인] 항목 | 검증 시점 | 실패 시 영향 |
|:--|:--|:--|:--|
| U-01 | `moz-extension://` 최상위 네비게이션이 허용되는가 | **Step 2 / G2-a** | §2.7.4 폴백 1안 → **파일 구성 변경, 계획 재작성** |
| U-02 | `web_accessible_resources` 등록이 확장 페이지 로드에 필수인가 | Step 2 / G2-a | manifest 수정 |
| U-03 | 확장 페이지가 세션 히스토리에 남는가 | **Step 3 / G3-a** | AC-011-1 원리적 불가 → 폴백 |
| U-04 | native messaging이 최상위 payload로 **문자열**을 허용하는가 (§3.1) | **Step 2 산출 기록 항목** (C-4로 게이트에서 분리) | `{"json":"…"}` 래핑으로 전환. **§7의 커버리지 경로는 유지된다** |
| U-05 | GeckoView 153이 **MV2**를 여전히 수용하는가 | Step 2 / G2-e | MV3 전환 — `web_accessible_resources`가 객체 배열이 되고 background가 이벤트 페이지가 되어 **중계 수명주기 검증이 추가로 필요**해진다 |
| U-06 | `runtime.sendMessage` 왕복 / `window.postMessage` 중계 / 페이지 세계 주입 (5단 경로 전체) | **Step 4 / G4-a~c** | REQ-010 재설계 → §2.6.1 (나) `exportFunction` |
| U-07 | `exportFunction`의 Promise·객체 Xray 처리 | Step 4 폴백 시에만 | (나)안 자체의 성립 여부 |
| U-08 | Gecko가 Android `network_security_config`/`usesCleartextTraffic`을 따르는가 | **Step 3 산출 기록 항목** (C-4로 게이트에서 분리) | `allowInsecureConnections(...)` 조정 |
| U-09 | `GeckoRuntimeSettings.allowInsecureConnections`의 상수 전체 이름 (`ALLOW_ALL` 외) | Step 3 필요 시 | Javadoc 재확인 |
| ~~U-10~~ | ~~`abiFilters` 적용 시 실제 APK 크기와 설치 시간~~ → **해소됨 (C-1·C-2)** | — | **evaluator가 실측했다: APK 198,587,715 B = 189.4 MiB, `adb install -r` 6.872초.** requirements의 "85~95 MB"는 **AAR 내부 압축 크기를 더한 추정치라 실제의 약 1/2**이었다(§0.3). **rev.1은 이 추정치 위에 Step 1의 게이트 임계값(150 MB)을 세워 정상 빌드를 실패로 판정했다** — V5가 경고한 *"추정치는 실측치로 굳어버린다"*의 교과서적 사례다. Step 1은 이제 크기가 아니라 **ABI 목록**으로 판정한다. developer는 자기 환경의 숫자를 **기록만** 하면 된다 |
| U-11 | `compileSdk`의 정확한 하한 (`androidx.core 1.18.0` 요구) | Step 1 빌드 시 AGP 오류로 확정 | 36으로 시작하므로 실무상 무해 |
| U-12 | mozilla/geckoview#199(`resource://` 로드 크래시)가 153에서 수정됐는가 | **검증하지 않는다** | `resource://`를 콘텐츠 페이지로 열지 않기로 했으므로(결정적 근거는 매치 패턴 제약) 무관 |
| U-13 | targetSdk 36의 동작 변경이 API 33 기기에서 재현되는가 | 이상 동작 시 | R-15 |
| U-14 | `kotlinx-serialization-json` 최신 1.11.0의 동작 | — | 이 계획이 빌드한 것은 **1.9.0**이다. 올리려면 재검증할 것 |

---

## 9. 폴백 발동 조건과 **발동 절차** (§2.7.4)

### 9.1 발동 절차 — **developer는 폴백으로 임의 전환하지 않는다** (rev.2 신설 — C-7)

rev.1은 *"이 계획의 Step 2 이후를 재작성한다"*고만 적고 **누가 재작성하는지를 정하지 않았다.** 절차를 못 박는다.

**폴백 1안 또는 2안이 필요해지면:**

1. **developer는 전환을 시작하지 않는다.**
2. 발동 근거를 로그에 남긴다 — **실패한 게이트 ID, 원시 오류 문자열, 해당 구간 logcat, 마지막 관측 상태.**
3. **FAIL을 반환하고 멈춘다.**
4. **planner 재실행** → 계획 갱신 → **evaluator 재승인** → **developer 재착수.**

**왜 재승인이 필요한가 — 폴백 1·2안은 우회가 아니라 설계 변경이기 때문이다:**
- 폴백 1안: `extension.metaData.baseUrl` **사용 중단**, 최초 실행 시 **파일 복사 로직 신설**, `content_scripts.matches`에 `file:///*` **추가**.
- 폴백 2안: 상대 경로 리소스가 불가하므로 **`bridge-client.js`를 `index.html`에 인라인 병합** — §3.5의 파사드/어댑터 분리 구조가 바뀐다.
- **파일이 화이트리스트 안이라는 것은 범위 문제가 해결됐다는 뜻일 뿐 설계 승인을 대신하지 않는다.** rev.1은 폴백 3안에만 재승인을 붙였으나, 재승인이 필요한 진짜 이유는 새 의존성이 아니라 **설계 변경**이다. 그 기준이면 1·2안도 해당한다.

**예외 — 재승인 없이 developer가 직접 처리하는 것:** 배선 오류(`nativeApp` 문자열 불일치, 권한 누락, 주입 시점, wire 래핑 전환)는 설계 변경이 아니므로 그대로 고친다. **구분 기준: 이 계획에 적힌 구조가 바뀌는가.**

### 9.2 폴백 조건표

| 발동 조건 | 전환 대상 | 무엇이 바뀌는가 | **절차** |
|:--|:--|:--|:--|
| **G2-a 실패** (확장 페이지 최상위 로드 거부) 또는 **G3-a 실패** (히스토리에 안 남음) | **폴백 1안** — assets의 `index.html`을 앱 내부 저장소로 복사 → `file://` 로드 | `matches`에 `file:///*` 추가, 최초 실행 복사 로직 신설, `extension.metaData.baseUrl` 사용 중단. **[미확인]** 확장의 파일 접근 권한 부여 수단이 GeckoView에 있는지 — **즉 이 탈출구 자체가 검증되지 않았다** | **planner 재실행 + evaluator 재승인 필요** (§9.1) |
| 폴백 1안도 실패 | **폴백 2안** — `GeckoSession.Loader().data(html, "text/html")` | 상대 경로 리소스 불가 → **`bridge-client.js`를 `index.html`에 인라인 병합** | **planner 재실행 + evaluator 재승인 필요** (§9.1) |
| 폴백 2안도 실패 | **폴백 3안** — 앱 내부 localhost HTTP 서버 | 포트 노출 증가. 비권장 | **planner 재실행 + evaluator 재승인 필요** — 설계 변경 **및** 새 의존성 |
| **G4-c 실패** (PAGE_WORLD 마커 없음) | §2.6.1 **(나) `exportFunction`/`cloneInto`** | `page-bridge.js` 폐기, `content.js`가 함수를 페이지 스코프로 직접 export. **U-07이 새 게이트가 된다** | **planner 재실행 + evaluator 재승인 필요** — A-15 결정을 뒤집는다 |

**모든 폴백의 파일 경로는 `app/src/main/assets/**` 와 `app/src/main/java/com/example/geckoviewtest/**` 글롭 안이므로 화이트리스트를 벗어나지 않는다** (evaluator가 hook 로직 직접 실행으로 확인). 예외는 폴백 3안의 **새 의존성**이며, 이는 파일 경계가 아니라 승인 문제다. **다만 위 표대로 화이트리스트 통과 여부와 무관하게 네 건 모두 재승인 대상이다.**

---

## 10. 계획에서 제외한 것 (스코프 아웃)

requirements.md §6을 그대로 따른다. **관련 있어 보여도 이번에 하지 않는다.**

| 항목 | 이유 |
|:--|:--|
| release 서명 / R8 / AAB | 원문에 없음. `app/src/release/**`는 화이트리스트에도 **의도적으로 없다** — 여기서 막히는 것은 버그가 아니라 설계된 정지점이다(impact-report). 필요하면 우회하지 말고 impact-analyzer 재실행 요청 |
| 세션 상태 저장·복원, 화면 회전 대응 | 원문에 없음. **회전 시 페이지 리로드 가능성은 알려진 한계로 기록.** Step 7 검증 중 회전을 시키지 않는다 |
| 앞으로 가기 / 새로고침 / 주소 입력창 | 원문은 뒤로가기만 요청 |
| 다운로드·파일 업로드·권한 프롬프트(`PermissionDelegate`) | 원문에 없음. 미구현 시 해당 기능이 **조용히 실패**한다는 사실만 기록 |
| 팝업·새 창(`onNewSession`)·외부 앱 인텐트 | 원문에 없음. naver에서 새 창 링크를 누르면 아무 일도 안 일어날 수 있음 |
| 오류 페이지 커스터마이징, 오프라인 대응, i18n, 다크 모드, 태블릿 | 원문에 없음 |
| naver 이외 URL 이동 UI | 원문은 버튼 1개만 요구. AC-010-4의 제2 사이트는 **검증용**이며 UI 버튼이 아니어도 된다 |
| 앱→페이지 푸시(`connectNative`/`Port.postMessage`) | A-06. **구조만 열어두고 기능은 만들지 않는다** |
| Hilt 등 DI 프레임워크 | §2.2 |
| **오리진 허용목록의 기본 활성화** | §2.6 참조. 훅 **자리**는 만들지만(evaluator 승인) **기본 활성화하면 REQ-010(사용자 A-08 결정)을 뒤집는다.** 배제 대상은 "활성화"이지 훅의 존재가 아니다 |
| 에뮬레이터 검증 | R-16. **이 배치의 `abiFilters`(arm64-v8a 단일) 설정에서는 성립하지 않는다**(V3). *(rev.2 정정 — C-5: rev.1은 "원리적으로 성립하지 않는다"고 적었으나 **AAR에 `x86_64`는 있다.** 32비트 `x86`만 없다. `abiFilters`에 `x86_64`를 넣으면 에뮬레이터도 가능하며, 넣지 않는 이유는 **기기 1대·ABI 1종이 검증 루프에 유리**하기 때문이다)* |

---

## 11. developer(4단계) 인계 사항

### 11.1 evaluator 판정 결과 — **전건 승인됐다**

rev.1이 판정을 요청한 8개 항목은 **전부 승인**됐다(`evaluation.md` §6). **설계 논쟁은 끝났으므로 developer는 아래를 확정 사항으로 다루고 재검토하지 마라.**

| # | 항목 | 판정 |
|:--|:--|:--|
| 1 | **§2.1 D-01~D-13** (13건 전부) | **전건 승인.** D-02·D-03·D-08 모두 *"이름만 바꾼 우회가 아니라 실질 격리"*로 확인됨 |
| 2 | **§2.2 Hilt 미도입** | **승인** — `architecture.md`는 생성자 주입만 요구하고 DI 프레임워크를 요구하지 않는다 |
| 3 | **§2.4 툴체인** AGP 8.13.2 / Gradle 8.14.5 / Kotlin 2.3.21 | **승인** — evaluator가 GeckoView 포함 상태로 끝까지 빌드해 실증. *(AGP 9 배제 근거 1번(버전 카탈로그 조항)은 **논증이 약하다**고 지적받았다. 근거 2번(stdlib 2.2.10 ↔ 2.3.21 역전)이 단독으로 결정을 지탱한다)* |
| 4 | **§6.1 L-07** `kotlinx-serialization-json` | **승인** — 커버리지 종속 관계가 실재함을 실증 |
| 5 | **A-13** `matches` / `all_frames: false` | **승인** — 어떤 도메인도 제외하지 않으므로 축소가 아니다 |
| 6 | **A-14 MV2** | **승인** |
| 7 | **A-15** `web_accessible_resources` + `<script>` 주입 | **승인.** (나) `exportFunction`은 폴백으로만 유지 |
| 8 | **P2 오리진 검사 훅** | **조건부 승인 → §2.6에서 채택 확정.** 조건 H-1·H-2·H-3을 지켜라 |

**요구를 좁히지 않으면서 기본 적용하는 완화책 2개** (추가 비용 0): `background.js` 한 곳의 **함수 화이트리스트**(AC-006-2가 이미 요구하므로 비용 0이고, 함수 추가 지점이 모여 §5.1-4의 검토 트리거가 된다) + **`all_frames: false`**.

### 11.2 착수 전 반드시 읽을 것 — 실행 주의 사항

`evaluation.md` §9에서 흡수했다. **1·2·3·6·7은 이 계획 본문에 없던 새 항목이다.**

1. **Step 1에서 APK가 189 MiB대로 나오는 것은 정상이다.** rev.1의 150 MB 규칙은 **폐기됐다**(C-1). **`G1-abi`의 ABI 목록으로 판정하라.**
2. **모든 Gradle 호출 앞에 `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`을 붙여라.** 셸 `JAVA_HOME`이 JDK 8임을 planner·evaluator가 각각 확인했다. 빠뜨리면 AGP가 뜨지 않는다. **`gradle.properties`에 `org.gradle.java.home`을 박지 마라**(머신 고유 경로).
3. **`gradle wrapper --gradle-version 8.14.5`는 시스템 gradle 9.3.1로 정상 생성된다** [확인 — evaluator]. `gradle-wrapper.jar`를 손으로 만들지 마라 — 리뷰가 "내용 대조"가 아니라 **"출처 확인"** 으로 이뤄진다.
4. **툴체인·JaCoCo·의존성 조합은 evaluator가 끝까지 빌드해 검증했다**(§0.2). Step 0·1·5에서 이 조합과 다르게 실패하면 **다른 조합을 탐색하지 말고**(V9) 실패 로그 전문과 함께 FAIL 반환하라.
5. **`nativeApp` 문자열 불일치는 예외도 로그도 없이 실패한다.** Step 2 / G2-c 실패 시 격리 순서 **1번**이다.
6. **`adb shell pm list packages`는 이 기기에서 `--user 0` 없이 SecurityException을 던진다** (삼성 다중 사용자). 이 계획은 이 명령을 쓰지 않지만 **QA가 쓸 경우 `--user 0`을 붙여라.** 계획이 쓰는 나머지 명령(`dumpsys`·`ps`·`screencap`·`uiautomator dump`·`logcat`·`cmd connectivity`)은 **전부 이 기기에서 정상 동작함을 evaluator가 전수 확인**했고, 기기는 `mWakefulness=Awake`다(도즈로 인한 무더기 실패 위험 없음 — V3).
7. **AC-009-4에서 비행기모드를 켠 뒤 반드시 해제하라.** Step 6에 적혀 있으나 **놓치기 쉽고, 남아 있으면 이후 모든 네트워크 검증이 거짓 레드가 된다.**
8. **AC-010-5(naver에서 `appFinish`)는 앱을 죽인다.** Step 9 QA 시퀀스의 **맨 마지막에만** 실행하라.
9. **`app/src/main/kotlin/**`는 가드가 ALLOW한다** [확인 — evaluator가 hook 로직 직접 실행]. §2.3의 *"`java/`만 쓴다"*는 **가드가 아니라 네 손으로** 지켜야 한다. Step 0 게이트의 `ls` 확인을 생략하지 마라.
10. **Step 9의 화이트리스트 대조를 생략하지 마라.** 가드는 Edit/Write에만 걸리고 **Bash로 만든 파일(스캐폴딩 대부분)은 검사조차 받지 않는다.**
11. **주석 3건은 선택이 아니다** — `background.js`(#220 회피 구조), `content.js`(Xray/페이지 세계), `MainActivity` 로드 지점(`resource://` 배제 근거는 **크래시가 아니라 매치 패턴 제약**). **셋 다 "단순화" 리팩터링을 막는 유일한 방어 수단이며 code-reviewer가 대조한다.**
12. **커버리지 70% 미달 시 테스트부터 늘리지 마라.** §7.1 클래스에 프레임워크 의존이 섞였는지부터 보라 — 미달은 대개 **설계 오염의 신호**다(R-11). evaluator가 `kotlinx-serialization` 기반 순수 Kotlin 클래스의 JVM LINE 100%를 실증했으므로 **설계만 지키면 목표는 달성 가능하다.** *(§7.1 중 `MainViewModel` 85%가 가장 빡빡하다 — 메서드 7개 전부에 테스트가 필요하다. 미달이 나온다면 여기일 가능성이 높다고 evaluator가 지적했다.)*
13. **폴백 1·2안이 필요해지면 임의 전환하지 말고 FAIL 반환하라** — §9.1의 절차를 따른다(C-7).

### 11.3 code-reviewer(5단계) 인계

**evaluator가 재승인을 요구하지 않았다** — 조건 C-1~C-8은 전부 문서 정정이고 설계 변경이 없기 때문이다. 대신 **반영 여부를 code-reviewer가 5단계에서 대조**한다. 대조 지점:

| 조건 | 확인 방법 |
|:--|:--|
| C-1 | Step 1에 **크기 임계값으로 기능하는 판정 규칙이 없고**(폐기 사실을 설명하는 인용문은 있어도 된다), `G1-abi`의 `unzip -l` 명령과 기대 출력(`arm64-v8a` 한 줄)이 게이트로 들어가 있다 |
| C-2 | R-01에 "수 분대"·"마비" 서술이 **없고**, 근거가 "3 ABI 비압축 합계 483.1 MiB"로 되어 있다 |
| C-3 | "GeckoView를 포함한 `assembleDebug`가 성공했다"는 진술의 출처가 **evaluator 실측**으로 표시돼 있다(§0.2·§2.4·§7.2) |
| C-4 | Step 2·3의 "검증 게이트 (전부 AND)" 목록에 술어가 "기록한다"인 항목이 **0건** |
| C-5 | 에뮬레이터 배제 근거가 "이 `abiFilters` 설정에서는"으로 한정돼 있다(R-16, §10) |
| C-6 | `background.js`에서 훅의 기본 분기가 **무조건 통과**하는지 코드 검사 + **G4-d 통과 로그** + 한글 주석 존재 |
| C-7 | §9 표의 폴백 1·2안 행에 **"planner 재실행 + evaluator 재승인 필요"** 가 적혀 있다 |
| C-8 | "정정 ③"이 **철회**돼 있고, 문서 전체의 바이트 크기 단위가 **MiB로 통일**돼 있으며, L-01이 "최신 stable"이 아니라 **"검증 완료된 고정 버전"** 이다 |

### 11.4 이 계획이 지킨 경계 (변동 없음)

- 모든 파일 경로가 `impact-report.json` 화이트리스트 안이다 — **evaluator가 hook 로직을 직접 실행해 41건 ALLOW / 의도적 배제 2건 BLOCK을 확인했다.** 모듈 추가·`app/src/release/**` 사용을 계획하지 않았다.
- 사용자가 요구하지 않은 보안 장치를 **기본 활성으로 넣지 않았다.** P2 훅도 기본 "전체 허용"이다(H-1). REQ-010을 좁히지 않는다.
- requirements §6 스코프 아웃 항목을 끌어들이지 않았다.
- **코드를 작성하지 않았다.** 산출물은 이 문서와 작업 로그뿐이다. §7.2의 Gradle 스니펫은 검증된 계약으로서 실은 것이며, 프로젝트 디렉터리에는 어떤 파일도 만들지 않았다(검증은 스크래치패드에서 수행).

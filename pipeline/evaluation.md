# 계획 검증 (evaluator, 파이프라인 3단계)

- 판정 일시: 2026-08-05 07:58:23 (KST)
- 판정 대상: `pipeline/plan.md` (576줄, 초안)
- 대조 정본: `rules/architecture.md`. 병행: `verification-honesty.md`, `comment-style.md`, `scope-guard.md`, `work-logging.md`
- 참조: `pipeline/requirements.md` **rev.2**, `pipeline/impact-report.json`, 작업 로그 4건

---

## 판정

# APPROVED (조건부)

**조건 C-1 ~ C-5를 developer 착수 전에 반영해야 한다.** 모두 계획의 구조·설계를 바꾸지 않는 **수치·라벨 정정**이며, 각 조건은 developer가 명령 한 줄로 검증 가능한 형태로 적었다.

아키텍처 설계(D-01~D-13), 범위 준수, 게이트 설계, 커버리지 경로는 **재작업 없이 승인**한다. 이 판정문에 적힌 실측값은 전부 evaluator가 이 세션에서 직접 재측정한 것이다(V7).

> **왜 REJECTED가 아닌가:** 발견된 결함 3건은 모두 "이미 evaluator가 정확한 값을 측정해 넘겨줄 수 있는 수치"이거나 "증거 라벨의 과장"이다. planner를 다시 돌려 같은 빌드를 재실행시키는 것은 재작업 루프 낭비다. 설계를 뒤집는 지적은 하나도 없다.

---

## 0. evaluator 재측정 결과 (V7 — 인계값을 옮겨 적지 않았다)

### 0.1 planner §0과 **일치**한 항목

| 항목 | planner 값 | evaluator 재측정 | 방법 |
|:--|:--|:--|:--|
| `JAVA_HOME` 환경변수 | JDK 8 (`jdk1.8.0_333`) | **일치** | `echo $JAVA_HOME` |
| PATH 기본 `java` | 17.0.14 | **일치** | `env -u JAVA_HOME /usr/bin/java -version` |
| Android Studio JBR | 21.0.10 | **일치** | 직접 실행 |
| 시스템 gradle | 9.3.1 | **일치** | `gradle --version` |
| ANDROID_HOME / android-36 | 존재 | **일치** | `ls $ANDROID_HOME/platforms` |
| 실기기 | SM-G981N, sdk=33, arm64-v8a | **일치** (+ `mWakefulness=Awake` 확인) | `adb getprop`, `dumpsys power` |
| AAR 크기 | 240,695,932 bytes | **일치** | `curl -I` content-length |
| **AAR minSdk** | `minSdkVersion="26"` | **일치** | AAR의 AndroidManifest.xml을 HTTP Range로 추출·inflate 후 원문 확인 |
| AAR 엔트리 수 | 68 | **일치** | 중앙 디렉터리 파싱 |
| 병합 `<service>` 수 | 89 | **일치** (+ 병합된 매니페스트에서도 89 재확인) | 원문 grep, `merged_manifest` grep |
| 병합 권한 5종 | INTERNET 외 4 | **일치** (병합 결과에서도 동일) | 동일 |
| 동봉 ABI | arm64-v8a / armeabi-v7a / x86_64, **x86 없음** | **일치** | 중앙 디렉터리 |
| `glEsVersion` / `zygotePreloadName` | 0x00020000 / ZygotePreload | **일치** | 원문 grep |
| 라이브러리 11건 존재 | L-01~L-11 | **전부 존재(HTTP 200)** | 각 POM HEAD |

### 0.2 planner §0과 **불일치**한 항목 — 4건

#### ❶ [BLOCKING] APK 크기 — Step 1 게이트가 **올바른 빌드를 실패로 판정**한다

evaluator가 `abiFilters = ["arm64-v8a"]`를 적용해 실제로 빌드한 debug APK:

```
198,587,715 bytes = 198.6 MB(10^6) = 189.4 MiB
APK 내 ABI: arm64-v8a 단 하나 (unzip 확인, lib/ 엔트리 13개)
lib/arm64-v8a/libxul.so    152,735,464 B  Method=Stored (비압축 0%)
lib/arm64-v8a/libmozglue.so  1,324,248 B  Method=Stored
assets/omni.ja              14,922,046 B  Defl:N
```

**abiFilters는 정상 적용됐는데 APK가 198.6 MB다.** 원인: AGP는 minSdk ≥ 23에서 네이티브 `.so`를 **비압축(Stored)으로 패키징**한다(시스템이 직접 mmap 하도록). requirements.md §2.2의 "70.7 + 13.6 + α ≈ 85~95 MB" 추정은 **AAR 내부의 압축 크기를 더한 값**이라 실제의 약 1/2이다.

| | AAR 내부(압축) | APK 저장 형태(비압축) |
|:--|--:|--:|
| arm64-v8a | 70.7 MiB | **167.1 MiB** |
| armeabi-v7a | 68.2 MiB | 130.4 MiB |
| x86_64 | 75.6 MiB | 185.7 MiB |
| assets/ | 13.6 MiB | 14.2 MiB |
| 3 ABI 전부 | — | **483.1 MiB** |

**계획의 결함:** Step 1 "실패 시 행동 규약"이 *"APK가 **150 MB를 넘으면** `abiFilters`가 적용되지 않은 것이다 … 원인이 분명해질 때까지 **다음 Step으로 넘어가지 않는다**"* 라고 못 박았고, R-01도 *"150 MB 초과 시 다음 Step으로 넘어가지 않는다"*를 완화책으로 적었다. **정상 빌드가 198.6 MB이므로 developer는 Step 1에서 반드시 이 규칙에 걸려 파이프라인이 정지한다.** 거짓 그린의 반대편이지만 결과는 같다 — 게이트가 신호를 만들지 못한다(V3).

계획 스스로 U-10에 *"requirements의 '85~95 MB'는 추정치다. 추정치를 문서에 굳히지 말 것(V5)"*이라 적어 놓고, **바로 그 추정치 위에 Step 1의 임계값을 세웠다.** 자기모순이다.

#### ❷ [BLOCKING] R-01의 심각도 서술이 사실과 다르다 — 설치는 **6.9초**

```
$ time adb -s R3CN60L0QMT install -r app-debug.apk   # 198.6 MB
Performing Streamed Install
Success
                                            6.872 total
```

R-01·requirements §2.2·impact-report가 공통으로 주장한 *"adb install이 수 분대로 늘어나고 파이프라인 검증 루프가 사실상 마비된다"*는 **arm64-v8a 단일 APK에서는 성립하지 않는다.** 6.9초는 정상적인 개발 루프다.

**단, `abiFilters` 자체는 유지가 맞다.** 3 ABI 비압축 합계가 483 MiB이므로 미적용 시 APK가 500 MB에 근접하고 그때는 설치 시간·기기 저장공간이 실제 문제가 된다. **결정은 유지하되 근거와 임계값을 실측으로 교체하라.**

#### ❸ [MAJOR] §0·§2.4의 `[확인]` 라벨이 과장됐다 — **GeckoView를 패키징한 빌드는 없었다**

planner의 스크래치패드 산출물을 직접 열어 확인했다(같은 세션 디렉터리에 남아 있다):

| 증거 | 관측값 |
|:--|:--|
| `agp8/app/build/outputs/apk/debug/app-debug.apk` | **3,315,659 bytes**, `lib/` 엔트리 **0개** → GeckoView 네이티브 라이브러리 없음 |
| `agp8/app/build.gradle.kts` 수정 시각 | **14:14:51** — APK 생성 시각 **14:14:31**보다 **20초 뒤** |
| Gradle 캐시 `geckoview/153.0.20260730155536/` | `.pom` + `.module`만 존재. **`.aar` 없음** |
| `cov/app/build.gradle.kts` (JaCoCo·serialization 검증용) | GeckoView 의존성 **없음** |
| `agp8`의 `jacocoVersion` | **0.8.13** (계획이 승인 요청한 값은 0.8.15) |

즉 §2.4의 *"AGP 8.13.2 + Gradle 8.14.5 + Kotlin 2.3.21 + JDK 21 → **성공 [확인]** — `assembleDebug` APK 생성, `createDebugUnitTestCoverageReport` XML 생성, `kotlin-stdlib` 2.3.21로 해석"* 은 **서로 다른 세 프로젝트의 결과를 하나로 합쳐 쓴 문장**이다. GeckoView 좌표를 선언한 뒤 `assembleDebug`를 다시 돌린 적이 없고, 240 MB AAR의 다운로드·추출·매니페스트 병합(89개 service)·네이티브 패키징은 **한 번도 실행되지 않았다.** "빌드 성공"이 "GeckoView를 포함한 빌드 성공"의 **대리 신호**로 쓰였다(V1).

**evaluator가 그 빌드를 대신 수행했고 결론은 계획과 같다** — 아래 0.3 참조. 따라서 툴체인 결정 자체는 승인한다. 다만 **plan.md의 증거 라벨은 정정해야 한다**(조건 C-3). 그대로 두면 code-reviewer·QA·coverage-reporter가 검증되지 않은 전제를 `[확인]`으로 물려받는다.

#### ❹ [MINOR] "정정 ③"은 정정이 아니라 **단위 혼동**이다

planner 로그와 §0은 rev.2의 ABI 크기(70.7 / 68.2 / 75.6)가 자신의 측정치(74.2 / 71.5 / 79.2)와 *"수 MB씩 달랐다"*며 **인계 문서 오류로 기록**했다. 실제 바이트 수를 재측정한 결과:

```
arm64-v8a   74,150,236 B = 74.2 MB(10^6) = 70.7 MiB
armeabi-v7a 71,541,879 B = 71.5 MB(10^6) = 68.2 MiB
x86_64      79,234,693 B = 79.2 MB(10^6) = 75.6 MiB
```

**두 문서는 같은 바이트 수를 서로 다른 단위로 적은 것이다.** rev.2는 MiB, planner는 MB(10⁶). rev.2는 틀리지 않았다. 게다가 plan.md는 같은 문서 안에서 AAR 총 크기를 **229.6 MiB**로, ABI 크기를 **74.2 MB**로 단위를 섞어 쓴다. 판단에 영향은 없으나, **존재하지 않는 인계 오류를 기록에 남기는 것은 V7이 막으려는 것과 정확히 반대 방향**이다.

#### ❺ [MINOR·정보] GeckoView `<release>`가 판정 시점에 이동했다

```
현재 <release>  = 153.0.20260803132010   (lastUpdated 20260804123340)
계획이 고정한 값 = 153.0.20260730155536   (여전히 존재, HTTP 200, resolve·빌드 성공 확인)
```

planner 측정 시점(08-04 14:18 KST)에는 계획의 값이 최신이었다. **불일치는 planner의 오류가 아니라 드리프트다.**

**판정: 계획이 고정한 `153.0.20260730155536`을 그대로 쓴다.** A-12가 동적 버전을 금지하고, 이 버전이 evaluator가 실제로 빌드·검증한 유일한 버전이다. `<release>`를 좇아 올리면 검증되지 않은 버전으로 바뀐다. §2.1의 "최신 stable" 문구만 "검증 완료 고정 버전"으로 바꿔라.

### 0.3 evaluator가 **새로 수행**한 검증 — 툴체인 전체 조합 (planner가 하지 않은 빌드)

계획이 요구한 전 구성요소를 **한 프로젝트에** 넣고 실제로 빌드했다.
(AGP 8.13.2 / Gradle wrapper 8.14.5 / Kotlin 2.3.21 / JDK 21 / compileSdk 36 / minSdk 26 / targetSdk 36 / viewBinding / abiFilters arm64-v8a / GeckoView 153.0.20260730155536 / kotlinx-serialization-json 1.9.0 / appcompat 1.7.1 / activity-ktx 1.13.0 / lifecycle 2.11.0 / constraintlayout 2.2.2 / junit 4.13.2 / coroutines-test 1.11.0 / turbine 1.2.1 / JaCoCo 0.8.15 + §7.2 스크립트 원문)

| 검증 항목 | 결과 |
|:--|:--|
| `gradle wrapper --gradle-version 8.14.5` (시스템 gradle 9.3.1로 생성) | **성공** — `gradle-wrapper.jar` 46,175 B 생성 |
| `./gradlew :app:assembleDebug` | **BUILD SUCCESSFUL in 56s** (40 tasks executed) |
| APK 내 ABI | **arm64-v8a 단일** — `abiFilters` 실효 확인 |
| 매니페스트 병합 | `<service>` **89개**, 권한 5종 병합 확인 |
| `kotlin-stdlib` 해석 | **2.3.21** — GeckoView POM이 `kotlin-stdlib 2.3.21`을 **직접 선언**함을 POM 원문에서 확인 → **R-04·B-05·AGP 9 배제 근거 2번이 성립한다** |
| `androidx.core` 해석 | **1.18.0** (requirements §2.4와 일치, compileSdk 36 근거 유효) |
| `./gradlew :app:jvmCoverageReport --rerun-tasks` | **BUILD SUCCESSFUL** (`--rerun-tasks`로 캐시 배제 — V7) |
| 리포트 XML 생성 | `app/build/reports/jacoco/jvmCoverageReport/jvmCoverageReport.xml` **생성됨** |
| `coverageExclusions` 실효 | `gecko/**` **제외됨**, `MainActivity` **제외됨** |
| 순수 JVM 테스트로 `kotlinx.serialization` 파싱 | `BridgeProtocol` **LINE 2/2 = 100%** — **§3.1 + L-07 + §7의 커버리지 경로 전체가 실증됨** |

**§7.2의 JaCoCo 스크립트는 GeckoView가 들어간 상태에서도 그대로 동작한다.** 계획의 커버리지 경로는 실증된 전제 위에 서 있다.

### 0.4 검증 수단의 환경 성립성 (V3) — 실기기에서 전수 확인

| 계획이 쓰는 명령 | 결과 |
|:--|:--|
| `dumpsys package … \| grep versionName` (AC-004-2, G2-c, G4-b) | **OK** |
| `ps -A` (AC-001-2 Gecko 자식 프로세스) | **OK** |
| `dumpsys activity activities` (AC-005-1, Step 0) | **OK** (`topResumedActivity`) |
| `uiautomator dump` (G4-a) | **OK** |
| `exec-out screencap -p` (AC-001-1 외 다수) | **OK** (2,080,350 B PNG 수신) |
| `logcat -d` (AC-005-2·4, AC-008-3) | **OK** |
| `cmd connectivity airplane-mode` (AC-009-4) | **OK** (`disabled` 반환 — 조회만 수행, 토글은 하지 않음) |
| 기기 상태 | `mWakefulness=Awake` — 도즈로 인한 무더기 실패 위험 없음(V3) |
| `pm list packages` (계획 미사용) | **`--user 0` 없이는 SecurityException** (삼성 다중 사용자). QA가 쓸 경우 주의 |

**계획된 검증은 실기기 1대(SM-G981N)로 전부 수행 가능하다.**

---

## 1. `architecture.md` 조항별 판정

### 1.1 선결 쟁점 — "UI (Compose)"를 XML로 읽는 해석이 타당한가

**타당하다. 승인.**

`architecture.md`가 규율하는 것은 **의존 방향과 상태 소유권**이지 렌더링 기술이 아니다. 근거는 문서 자체에 있다 — "UI 레이어에서 Repository/DataSource 직접 호출 금지", "ViewModel에 Context/View/Compose 타입 주입 금지", "단일 UiState + StateFlow"는 모두 렌더링 기술과 무관하게 성립한다. Compose 고유 조항(`remember`, `LaunchedEffect`)만 XML 대응물로 치환하면 된다.

REQ-002는 사용자 명시 요구이므로 뒤집지 않는다. **판정 기준은 "XML 환경에서 각 원칙이 실질적으로 지켜지는가"이며, 아래에서 항목별로 대조했다.**

### 1.2 레이어 구조

| 조항 | 판정 | 근거 |
|:--|:--|:--|
| UI → ViewModel → Repository → DataSource 단방향 | **충족** (D-01) | Activity는 `MainViewModel`만 참조. `AppInfoRepository`를 UI에서 직접 부르지 않음 |
| UI에서 Repository 직접 호출 금지 | **충족** (D-01) | — |
| **ViewModel에 Context/View 주입 금지** | **충족** — 아래 상세 | D-02·D-05·D-08 |
| 도메인 로직 프레임워크 비의존 | **충족** | `BridgeProtocol`·`BridgeDispatcher` 순수 Kotlin (0.3에서 실증) |

**ViewModel 누출 3개 지점을 개별 판정한다 — "이름만 바꾼 우회"인지 실질 격리인지가 핵심이다.**

**① `getVersionName` → `PackageManager` (D-02) — 실질 격리다. 승인.**
`AppInfoRepositoryImpl`이 `Context`가 아니라 `versionNameProvider: () -> String?` 람다를 받는다. `PackageManager` 호출은 `App.kt`에만 존재한다.
- **우회가 아닌 근거:** 람다 seam은 타입 수준에서 안드로이드 의존을 **제거한다**. `Impl`의 시그니처에 안드로이드 타입이 한 개도 남지 않으므로 JVM 단위 테스트에서 그대로 인스턴스화된다. `Context`를 받아 놓고 "Application Context니까 괜찮다"고 넘어가는 흔한 우회와 다르다 — 그 경우 클래스는 여전히 JVM 테스트 불가이고 커버리지 분모에 0%로 얹힌다.
- 0.3에서 같은 패턴의 순수 Kotlin 클래스가 JVM 테스트로 100% 커버되는 것을 실증했다.

**② `appFinish` → Activity 종료 (D-03) — 실질 격리다. 승인.**
ViewModel은 `Channel<MainUiEvent>`에 `Finish`를 방출할 뿐 Activity를 모른다. `finish()` 호출은 Activity가 한다.
- **`Channel` vs `SharedFlow(replay=0)` 선택도 승인한다.** planner의 근거가 정확하다 — `SharedFlow(replay=0)`는 활성 수집자가 없으면 방출을 **버린다**. Activity가 STOPPED인 순간 도착한 `Finish`가 유실되면 "가끔 종료가 안 되는" 비결정적 결함이 되고, 이런 결함은 V5의 "N회 중 M회" 지옥으로 직행한다. `Channel(BUFFERED)` + `receiveAsFlow()`는 수집자가 붙을 때까지 버퍼링한다. **일회성 이벤트의 정석이다.**
- `architecture.md` "일회성 이벤트는 상태가 아닌 이벤트 채널로 분리" **문자 그대로 충족.**

**③ 뒤로가기 → `OnBackPressedCallback` (D-08) — 실질 격리다. 승인.**
4단 배선: `onCanGoBack` → `viewModel.onCanGoBackChanged(b)` → `MainUiState.canGoBack` → Activity의 `render()`가 `backCallback.isEnabled` 갱신 → 콜백 본문이 `viewModel.onBackPressed()` → ViewModel이 `NavigateBack` 방출 → Activity가 `session.goBack()`.
- **ViewModel에 androidx.activity 타입이 한 개도 없다.** 콜백 객체는 Activity가 소유·생성한다.
- `canGoBack=false`면 콜백이 비활성 → dispatcher 기본 동작으로 Activity 종료 → **AC-011-3이 별도 코드 없이 성립한다.** 이 점이 설계의 품질을 보여준다.
- 부수 효과로 뒤로가기 로직의 두 갈래(상태 전이 / 이벤트 방출)가 **모두 JVM 테스트 가능**해진다.

**④ `GeckoSession`/`GeckoRuntime` 소유권 (D-04·D-05) — 단방향을 깨지 않는다. 승인.**
- `GeckoRuntime` = Application 스코프 싱글턴. "프로세스당 1회" 제약 준수. Activity 생성 시 회전 크래시.
- `GeckoSession` = **Activity 소유.** ViewModel은 세션 객체를 보유하지 않고 `currentUrl`/`isLoading`/`canGoBack`만 다룬다.
- **방향성 검토:** 세션이 UI 레이어에 있으므로 "ViewModel → 세션 조작"이 직접 호출이 아니라 **이벤트(`NavigateBack`, `Navigate`)로 역류**한다. UI가 이벤트를 수신해 자기 소유 자원을 조작하는 형태이므로 **단방향 의존이 유지된다.** ViewModel이 세션을 들었다면 그것이야말로 D-02 위반이자 회전 시 뷰 누수다.

### 1.3 상태 관리

| 조항 | 판정 | 근거 |
|:--|:--|:--|
| 단일 `UiState` + `StateFlow` | **충족** (D-06) | `MainUiState(isLoading, currentUrl, canGoBack, bridgeReady, lastBridgeResult)` + `repeatOnLifecycle(STARTED)` → **단일 `render(state)`** |
| `remember`로 비즈니스 상태 보관 금지 (XML판) | **충족** (D-07) | Activity 필드/View 상태에 비즈니스 상태 금지 |
| **로딩 상태가 `ProgressBar.visibility`가 아니라 UiState에 있는가** | **충족** | 진실의 원천은 `MainUiState.isLoading`, `visibility`는 투영. `AppProgressDelegate`는 `viewModel.onPageStart()/onPageStop(success)`만 호출하고 View를 건드리지 않음 |
| 일회성 이벤트가 상태와 분리 | **충족** (D-03) | `MainUiEvent` Channel |

**D-07의 강제 수단이 특히 좋다:** Step 6 게이트에 *"`ProgressBar.visibility`를 `render()` 밖에서 대입하는 곳이 **0건**"*이라는 **코드 검사 조건**을 넣었다. REQ-009는 델리게이트에서 `visibility=VISIBLE`을 직접 때려버리기 가장 쉬운 지점인데, 이를 게이트로 잡는다. `render()` 함수가 **1개**여야 한다는 제약이 UDF를 구조적으로 강제한다.

### 1.4 비동기

| 조항 | 판정 | 근거 |
|:--|:--|:--|
| `GlobalScope` / `runBlocking` 금지 | **충족** (D-09·D-10) | 브리지용 스코프를 `App.kt`가 만든 Application 스코프 `SupervisorJob + Main.immediate`로 **생성자 주입**. `GlobalScope` 미사용 |
| **`GeckoResult.poll()`(블로킹) 배제** | **명시적으로 배제됨** (D-09) | *"`GeckoResult.poll()`(블로킹) 사용 금지"* 문자 그대로 기재 |
| `GeckoResult` ↔ 코루틴 어댑터 | **충족** | `suspendCancellableCoroutine` + `then/exceptionally`, 취소 시 `GeckoResult.cancel()`. **`gecko/GeckoResultExt.kt` 한 파일로 변환 지점 집중** |
| Dispatcher 주입 | **충족, 기준 이상** (D-11) | 생성자 주입 + **"기본값을 주지 않는다"** |

**D-11의 "기본값 금지"를 특별히 지지한다.** `dispatcher: CoroutineDispatcher = Dispatchers.Default`처럼 기본값을 주면 테스트가 주입을 잊어도 통과해버려 **주입 장치가 장식**이 된다. 기본값을 없애면 컴파일러가 주입을 강제한다. `architecture.md`의 "테스트에서 교체 가능해야 함"을 형식이 아니라 실질로 만족시키는 선택이다.

**D-10의 스코프 선택도 타당하다.** 브리지 요청은 Activity 수명과 무관하게 도착하므로 `viewModelScope`가 맞지 않고, 그렇다고 `GlobalScope`는 금지 사항이다. Application 스코프 스코프를 **컨테이너가 만들어 주입**하는 것이 유일하게 원칙을 지키는 해법이다.

### 1.5 의존성

| 조항 | 판정 |
|:--|:--|
| 새 라이브러리는 plan.md 명시 + evaluator 승인 | **충족** — §6.1에 11건 열거. 아래 §2에서 개별 판정 |
| **버전은 `libs.versions.toml`에서만** | **충족** (D-12) — AGP/Gradle/Kotlin/GeckoView/androidx/테스트/JaCoCo 전부 카탈로그. `build.gradle.kts`에 리터럴 버전 금지 |

**AGP 9 배제 근거 1번(`libs.versions.toml` 조항)의 논증 성립 여부 — 부분 성립.**
planner는 "AGP 9의 내장 Kotlin은 버전이 AGP에 암묵적으로 묶여 카탈로그에 나타나지 않으므로 D-12와 충돌한다"고 주장했다. **이 논증은 약하다.** `architecture.md`의 조항은 *"버전을 카탈로그에서 관리하라"*(= build.gradle.kts에 하드코딩 금지)이지 *"모든 버전이 독립적으로 선언 가능해야 한다"*가 아니다. AGP 내장 Kotlin은 "다른 곳에서 관리되는" 것이 아니라 "선언 대상이 아닌" 것이므로 조항 위반으로 보기 어렵다.

**그러나 배제 결정 자체는 승인한다** — 근거 2번이 단독으로 결정을 지탱하고, evaluator가 이를 실증했기 때문이다:
- GeckoView POM이 `kotlin-stdlib 2.3.21`을 **직접 선언**함을 POM 원문에서 확인.
- AGP 9의 내장 Kotlin 컴파일러는 2.2.10 → **컴파일러(2.2.10)가 런타임 stdlib(2.3.21)보다 낮은** 구성이 되며, 이는 Kotlin이 지원하지 않는 방향이다.
- AGP 8.13.2 + KGP 2.3.21에서는 `kotlin-stdlib` **2.3.21로 깔끔히 해석**됨을 `:app:dependencies`로 확인.

근거 3번(검증 사례가 얇다)은 판단의 문제이나, evaluator가 채택 조합을 **끝까지 빌드해 성공**시켰으므로 채택 조합의 안전성은 실증됐다.

### 1.6 금지 사항

| 조항 | 판정 |
|:--|:--|
| `!!` 남용 / 빈 catch | 계획 단계에서 위반 없음. code-reviewer 대상 |
| **UI 노출 텍스트는 `strings.xml`** | **충족** (D-13) — 아래 상세 |
| 기존 public API 변경 | 해당 없음(신규 프로젝트) |
| **빌드 설정 변경은 리스크 항목으로 명시** | **충족** — §6.2에 11건. 아래 §3에서 판정 |

**D-13 (웹 자산에 `strings.xml`을 적용하지 않음) — 승인.**
`architecture.md`의 "하드코딩된 문자열 리소스(UI 노출 텍스트는 `strings.xml`)"는 **Android 리소스 시스템에 대한 조항**이다. `strings.xml`은 `Resources`/`Context`를 통해 해석되며 `index.html`·`page-bridge.js`는 그 시스템 밖에 있다 — **웹 자산에 강제하면 적용할 기술적 수단 자체가 없고**, 억지로 하면 빌드 타임 문자열 치환 같은 것을 만들어야 해서 HTML/JS가 망가진다.
- 적용 범위를 **"Android UI 텍스트만"**(앱 이름, naver 버튼 라벨, 로딩 안내, 오류 토스트)으로 그은 것이 정확하다.
- 스코프 아웃에 i18n이 있으므로 웹 자산 다국어화 요구도 없다.
- **code-reviewer는 이 경계로 판정하라** — `index.html`의 버튼 텍스트가 하드코딩됐다는 지적은 **오지적**이다.

### 1.7 테스트 / 커버리지

| 조항 | 판정 |
|:--|:--|
| 신규 비즈니스 로직 단위 테스트 필수 | **충족** — §7.1의 5개 클래스 + Step 5 게이트 |
| JUnit + kotlinx-coroutines-test | **충족** (L-08·L-09) |
| ViewModel은 `Dispatchers.setMain` + Turbine | **충족** — Step 5 게이트에 명시 (L-10) |
| **변경 클래스 라인 커버리지 70%** | **달성 가능. 승인** — 아래 상세 |
| 전체 커버리지 베이스라인 대비 하락 금지 | 신규 프로젝트라 베이스라인 없음. §7.3이 coverage-reporter에게 경계를 인계 |

**커버리지 70%가 실제로 달성 가능한가 — 가능하다. evaluator가 실증했다.**

계획의 논리는 "70%는 테스트를 더 짜서 버는 것이 아니라, **분모에 들어가는 클래스가 순수 Kotlin일 때만 원리적으로 성립한다**"이며, 이것이 정확한 진단이다. 근거 3개를 개별 확인했다:

1. **§3.1 wire를 JSON *문자열*로** → `BridgeProtocol`이 `org.json`(안드로이드 전용, JVM 테스트에서 stub)을 안 쓴다. **evaluator가 `kotlinx-serialization`만으로 파싱하는 클래스를 순수 JVM 테스트로 LINE 100% 커버하는 것을 실측했다.**
2. **D-02 람다 seam** → `AppInfoRepositoryImpl`이 `Context`를 안 든다 → JVM 테스트 가능.
3. **D-08 이벤트 경유 뒤로가기** → 뒤로가기 로직이 `OnBackPressedCallback` 없이 ViewModel에서 검증된다.

**§7.1의 클래스 단위 경계도 현실적이다:**

| 클래스 | 목표 | evaluator 판정 |
|:--|:--|:--|
| `BridgeProtocol` | ≥90% | **타당** — 직렬화/역직렬화·필드 누락·잘못된 JSON·오류 인코딩은 전부 순수 함수. 실증 완료 |
| `BridgeDispatcher` | ≥90% | **타당** — 인터페이스 2개(`AppInfoRepository`·`BridgeHost`)에만 의존. `when` 분기 4개는 테스트가 쉽다 |
| `AppInfoRepositoryImpl` | 100% | **타당** — 람다 1개 + null 폴백. 실질 2줄 |
| `MainViewModel` | ≥85% | **타당하나 가장 빡빡하다** — 메서드 7개 전부에 테스트가 필요하다. Step 5 게이트가 이를 요구하므로 통과 가능 |
| `MainUiState` | ≥80% | **타당** — data class, 전이적 커버 |

**§7.3의 제외 목록도 정당하다** — `App.kt`(GeckoRuntime 생성), `MainActivity`(View 바인딩), `gecko/**`(GeckoView 콜백 구현), `NativeBridgeHandler`(20줄 어댑터)는 실기기 없이 실행 불가다. **제외가 `coverageExclusions`에 코드로 박혀 있어 임의 확대가 diff에 드러난다**는 점이 특히 좋다(evaluator가 제외 실효를 실측 확인).

**V8 준수 확인 — 계획은 "계측을 늘리면 수치가 오른다"고 가정하지 않는다.** §7 전제문·R-12·§7.3의 coverage-reporter 경고문 3곳에서 *"계측 테스트는 JaCoCo에 집계되지 않는다"*, *"REQ-010 산출물은 전량 JS라 분모에 0 기여"*를 명시하고, **커버리지를 순수 JVM 레이어에서만 벌도록 설계를 역산했다.** V8을 정확히 이해한 계획이다.

---

## 2. §6.1 새 라이브러리 11건 — 개별 판정

**전부 존재 확인(HTTP 200) 완료. 아래 판정은 evaluator의 실제 빌드 성공을 근거로 한다.**

| # | 아티팩트 | 판정 | 근거 |
|:--|:--|:--|:--|
| L-01 | `org.mozilla.geckoview:geckoview:153.0.20260730155536` | **승인** | REQ-001의 유일한 수단. WebView 금지가 요구사항. MPL 2.0(앱 배포에 문제 없음). **APK 198.6 MB는 evaluator 실측** — `abiFilters`와 함께여야 승인이다(조건 C-1) |
| L-02 | Maven 저장소 `https://maven.mozilla.org/maven2/` | **승인** | L-01의 유일한 배포처. `dependencyResolutionManagement`에 추가하는 방식이 정석 |
| L-03 | `androidx.activity:activity-ktx:1.13.0` | **승인** | `OnBackPressedDispatcher`(REQ-011). **참고:** `appcompat`이 전이로 끌고 오므로 컴파일은 되지만, 직접 쓰는 API는 직접 선언하는 것이 옳다 |
| L-04 | `androidx.appcompat:appcompat:1.7.1` | **승인** | XML 테마·`AppCompatActivity`(REQ-002) |
| L-05 | `androidx.lifecycle:lifecycle-{viewmodel,runtime}-ktx:2.11.0` | **승인** | ViewModel + `repeatOnLifecycle`(D-06) |
| L-06 | `androidx.constraintlayout:constraintlayout:2.2.2` | **승인** | XML 레이아웃 |
| **L-07** | **`kotlinx-serialization-json:1.9.0` + 플러그인 2.3.21** | **승인 — 아래 상세** | |
| L-08 | `junit:junit:4.13.2` | **승인** | `architecture.md`가 명시 요구 |
| L-09 | `kotlinx-coroutines-test:1.11.0` | **승인** | `architecture.md`가 명시 요구 |
| L-10 | `app.cash.turbine:turbine:1.2.1` | **승인** | `architecture.md`가 "Turbine(또는 동등)" 명시 |
| L-11 | JaCoCo 플러그인 `0.8.15` | **승인** | 커버리지 게이트의 수단. §7.2 스크립트 실효를 evaluator가 실측 |

### L-07 판정 — **승인. 승인 근거가 정확하다.**

planner는 이 도입을 "편해서"가 아니라 **"이것 없이는 커버리지 70%가 원리적으로 불가능하다"**로 논증했고, **그 종속 관계가 실재한다.**

- `org.json.JSONObject`는 안드로이드 SDK에 포함된 클래스이며 **JVM 단위 테스트에서는 메서드가 전부 `UnsupportedOperationException`을 던지는 stub**이다. `BridgeProtocol`이 이를 쓰면 그 클래스는 JVM 테스트로 한 줄도 커버할 수 없고, §7.1의 분모 5개 중 가장 큰 항목이 0%가 되어 **70%가 산술적으로 무너진다.**
- 대안인 Robolectric은 아티팩트를 더 늘리고(승인 대상 증가) 테스트를 현저히 느리게 만든다 — 229.6 MiB AAR로 이미 빌드 시간이 리스크인 배치에서 나쁜 교환이다.
- **evaluator 실증:** `kotlinx-serialization-json:1.9.0`을 쓴 `BridgeProtocol`이 순수 JVM 테스트에서 **LINE 100%** 로 계측됐다.
- 버전 선택도 정직하다 — 최신은 1.11.0이나 **실제로 빌드한 1.9.0을 요청**하고 U-14에 "올리려면 재검증" 을 남겼다. V5의 "측정 전 추정치를 쓰지 않는다"에 부합.

**직렬화 플러그인 버전을 Kotlin과 동일한 2.3.21로 묶은 것도 옳다** — 컴파일러 플러그인이므로 KGP와 버전이 어긋나면 빌드가 깨진다. evaluator 빌드에서 확인.

### GeckoView(L-01)의 APK 영향 — 승인하되 조건부

200 MB급 APK를 들이는 것은 중대한 결정이나, **REQ-001이 GeckoView를 명시하고 WebView를 금지**하므로 대안이 없다. ABI별 분리 아티팩트가 stable 채널에 존재하지 않는 것도 확인했다(`.module`의 변형은 api/runtime 2개뿐이며 둘 다 동일한 universal AAR을 가리킴). **`abiFilters`가 유일한 축소 수단이며 계획이 이를 Step 1의 같은 커밋에 넣도록 못 박은 것은 적절하다.**

---

## 3. §6.2 빌드 설정 변경 11건 — 리스크 항목 명시 여부

`architecture.md`: *"빌드 설정(AGP/Kotlin 버전, minSdk 등) 변경은 plan.md에 **리스크 항목으로 명시된 경우만** 허용"*

| # | 설정 | 리스크 명시 | 판정 |
|:--|:--|:--|:--|
| B-01 | `minSdk 26` | "선택이 아니라 하한" + AAR 실측 근거 | **승인** — evaluator가 AAR 매니페스트에서 직접 재확인 |
| B-02 | `compileSdk`/`targetSdk` 36 | R-15(실기기 API 33)와 연결 | **승인** — 사용자 확정 사항이며 `androidx.core 1.18.0` 요구와 정합 |
| B-03 | AGP 8.13.2 | R-03·R-04와 연결 | **승인** — evaluator 빌드 성공 |
| B-04 | Gradle wrapper 8.14.5 | R-03(시스템 gradle 9.3.1 함정) | **승인** — evaluator가 wrapper 생성·빌드 성공 |
| B-05 | Kotlin 2.3.21 | R-04(stdlib 충돌) | **승인** — stdlib 2.3.21 해석 실측 |
| B-06 | Java 17 compileOptions | Mozilla 공식 요구 [문서] | **승인** — evaluator 빌드에 반영해 성공 |
| **B-07** | **`abiFilters arm64-v8a`** | **R-01** | **승인하되 R-01 서술 정정 필요(C-1·C-2)** |
| B-08 | `viewBinding=true`, compose 미설정 | AC-002-2와 연결 | **승인** |
| B-09 | `usesCleartextTraffic` (debug 한정) | U-08과 연결 | **승인** — debug 소스셋 한정이 적절. release는 스코프 아웃 |
| B-10 | `enableUnitTestCoverage` + JaCoCo | §7.2 | **승인** — 스크립트 실효 실측 |
| B-11 | debug `consoleOutput`/`remoteDebuggingEnabled` | JS 세계 3곳 원인 분리 | **승인** — 오히려 **필수**. 없으면 5단 경로 디버깅이 불가능 |

**11건 전부 리스크 항목으로 연결되어 있다. 조항 충족.**

---

## 4. 검증 정직성 (verification-honesty.md) 판정

### V1 — 게이트가 대리 신호를 쓰는가

**전반적으로 매우 양호하다.** 게이트가 "델리게이트 호출됨"·"확장 설치됨"·"`goBack()` 호출됨" 같은 대리 신호를 쓰지 않고 **화면·`dumpsys`·`ps`·logcat으로 외부 관측 가능한 조건**으로 작성됐다. AND 결합된 조건들의 독립성도 대체로 확보돼 있다.

**Step 2 (G2-a~e) — 3개는 강하고 2개는 장식이다.**

| 조건 | 판정 |
|:--|:--|
| G2-a (버튼 보임 + `about:neterror` 아님) | **유효·독립** |
| G2-b (JS가 채우는 요소 표시) | **유효·독립** — 정적 HTML만 그려지고 JS가 죽은 상태를 걸러낸다. G2-a와 실제로 독립적으로 깨진다 |
| G2-c (버전 문자열 `dumpsys`와 문자 단위 일치) | **유효·독립·가장 강함** |
| **G2-d (wire 포맷 판정을 "로그에 단정적으로 기록한다")** | **무효 — 사실상 항상 참** |
| G2-e (MV2로 설치되었는지) | **약한 종속** — 확장이 설치 안 되면 G2-a·b·c가 먼저 깨진다 |

**G2-d는 게이트 조건이 아니라 기록 지시다.** "기록한다"는 무엇을 기록하든 충족되므로 AND 항으로서 **없는 것과 같다**(V1: *"하나가 사실상 항상 참이면 그 조건은 없는 것과 같다"*). **거짓 그린을 만들지는 않는다** — 실제 wire 포맷이 틀렸다면 G2-c가 먼저 깨지기 때문이다. 따라서 결함이 아니라 **분류 오류**다. 게이트 목록에서 빼고 "Step 2 산출 기록 항목"으로 옮겨라(C-4).

**Step 3 (G3-a~d) — 동일 패턴.**

| 조건 | 판정 |
|:--|:--|
| G3-a (뒤로가기 후 index.html 재렌더) | **유효·독립** |
| G3-b (복귀 **후** 브리지 재동작 + `dumpsys` 일치) | **유효·독립·핵심** — 계획이 *"G3-a만으로는 잡히지 않는다"*를 명시한 것이 정확하다. 페이지만 그려지고 확장 메시징이 죽은 상태는 실제로 발생 가능하며, 이 조건이 없으면 통과한다 |
| G3-c (naver 렌더 + 도메인 + `NS_ERROR_` 부재) | **유효·독립** |
| **G3-d (cleartext 차단 여부를 "기록한다")** | **무효 — G2-d와 동일한 기록 지시** |

**Step 4 (G4-a~f) — 이 계획에서 가장 잘 설계된 게이트다. 결함 없음.**

거짓 그린 최대 위험 지점(§2.6.1 페이지 세계 격리)을 **G4-c가 실제로 걸러낸다.**
- 메커니즘 검토: `page-bridge.js`가 **페이지 세계**에서 `window.__bridgeProbeRanInPageWorld = true`를 세팅하고, `content.js`(격리 세계)는 `window.wrappedJSObject.__bridgeProbeRanInPageWorld === true`일 때**만** 배지에 `PAGE_WORLD`를 덧붙인다.
- **판별력 검증:** 주입이 실패해 브리지가 격리 세계에만 존재하면 페이지 세계에 그 플래그가 없으므로 `wrappedJSObject` 조회가 `undefined`가 되어 마커가 붙지 않는다. 반대로 주입이 성공하면 마커가 붙는다. **두 상태를 실제로 구분한다.**
- 계획이 *"이 조건 없이는 G4-a·G4-b가 모두 참인데도 실제 웹페이지는 브리지를 못 쓰는 상태가 통과한다"*고 명시한 것이 정확하다. **거짓 그린을 정면으로 겨냥한 조건이며 유효하다.**
- G4-d(제2 사이트)는 `matches` 하드코딩을, G4-e(회귀)는 주입이 페이지를 망가뜨리는 것을, G4-f(코드 검사)는 송신자별 분기를 각각 독립적으로 잡는다. **6개 조건이 서로 다른 실패 양식을 겨냥한다.**

**전송 경로 2종의 상호 인용 금지 — 준수한다.**
- Step 2(확장 페이지 3단)와 Step 4(외부 사이트 5단)를 **별도 Step·별도 게이트**로 물리적으로 분리했다.
- Step 4 말미에 *"Step 2의 통과는 Step 4를 보장하지 않는다. 전송 경로가 물리적으로 다른 파일로 갈라져 있다. 두 경로의 결과를 서로의 근거로 인용하지 않는다"*를 **경계 문구로 명시**했다.
- R-09가 같은 내용을 리스크로 중복 고정했고, AC-006-4("AC-010 실패 시 REQ-006도 미충족")를 인용한다.
- **한쪽 통과를 다른 쪽 근거로 쓰지 않는다. 조항 충족.**

**늦게 오는 오류 (V1 후단) — 준수.** Step 8이 AC-005-4로 *"종료 판정 후 **최소 3초** logcat을 더 수집"*을 게이트에 넣었다. 성공 판정 직후 종료해 지연 크래시를 놓치는 것을 막는다.

### V2 — 새 검증이 실패를 잡는지 증명하는가

**충족. 기준 이상이다.**
- Step 5 게이트: 새 테스트 스위트마다 프로덕션 코드를 **의도적으로 깨뜨려 RED 확인 후 원복**, **양쪽 결과를 로그에 기록**. 최소 4건을 구체적으로 지정(`BridgeProtocol` 스키마 필드명 / `BridgeDispatcher`의 `UNKNOWN_FUNCTION` 분기 / `AppInfoRepositoryImpl` null 폴백 / `MainViewModel` `isLoading` 전이).
- Step 9의 **AC-003-2 역주입 실패 테스트**: `background.js`의 `sendNativeMessage` 한 줄을 주석 처리 → 화면 결과가 표시되지 않거나 타임아웃 → 원복 후 재성공, **두 결과 모두 기록**. 브리지 전체 경로에 대한 V2 적용이며 **"항상 초록인 장식"과 구분하는 유일한 수단**이다.
- AC-004-3(`versionName` 변경 시 표시값도 변경)은 **하드코딩 상수를 반환하는 가짜 구현**을 겨냥한다.

### V3 — 환경이 신호를 만들 수 있는가

**충족.**
- **에뮬레이터 배제 근거는 타당하다.** AAR에 32비트 `x86`이 없음을 evaluator가 재확인했고, `abiFilters`를 arm64-v8a로 좁히면 x86_64 에뮬레이터에서도 앱이 뜨지 않는다. *"성립 불가한 검증을 배치하면 '이건 원래 빨간 거야'가 된다"*는 인용이 V3의 취지 그대로다.
  - **다만 문구는 과장이다** — "원리적으로 성립하지 않는다"가 아니라 "이 `abiFilters` 설정에서는 성립하지 않는다"가 정확하다. x86_64를 포함시키면 에뮬레이터도 가능하다. **결정은 승인**(기기 1대·ABI 1종이 검증 루프에 유리)**하되 문구를 정정하라**(C-5).
- **실기기 1대로 계획된 검증이 전부 가능함을 evaluator가 전수 확인했다** (§0.4). `screencap`·`uiautomator dump`·`dumpsys`·`ps`·`logcat`·`cmd connectivity` 모두 동작하고, 기기가 `Awake` 상태다.

### V5 — 비결정성을 횟수와 함께 보고하는가

**충족.** Step 6이 AC-009-1 미재현 시 *"비결정성으로 단정하지 말고 **5회 반복 후 'N회 중 M회'로 보고**"*를 규정했다. U-10에서 추정치("85~95 MB")를 실측으로 대체하도록 지시한 것도 V5의 *"측정 전 추정치를 문서에 쓰지 않는다"*에 부합한다 — **다만 계획 자신이 Step 1 임계값에서 이 원칙을 어겼다**(C-1).

### V7 — 인계값 재측정

**계획은 재측정을 수행했고 실제로 인계 오류 2건(JAVA_HOME, PATH java)을 잡았다** — evaluator가 둘 다 확인했다. **그러나 §0의 `[확인]` 라벨 일부가 실제 수행 범위를 넘어선다**(§0.2 ❸) — 이것이 이번 판정의 주요 지적이다. 또한 "정정 ③"은 정정이 아니었다(§0.2 ❹).

### V8 — 도구가 조용히 건너뛰는 곳

**충족.** 계측 테스트가 JaCoCo에 집계되지 않는 사실을 §7 전제·R-12·§7.3 3곳에서 인식하고 **커버리지 경로를 JVM 레이어로 역산**했다. Step 9가 *"Bash로 만든 파일은 화이트리스트 검사조차 받지 않는다"*(impact-report risk_notes 1번)를 흡수해 **종료 전 실제 파일 트리 대조**를 게이트로 넣은 것도 V8의 "도구가 침묵하는 곳" 대응이다. `gradle-wrapper.jar`를 "내용 대조가 아니라 출처 확인"으로 다루기로 한 것도 적절하다.

### V9 — 범위 밖 결함을 넓히지 않는가

**충족.** Step 0(*"다른 조합을 탐색하지 말고 FAIL 반환"*), Step 2(*"3회 시도 후에도 원인 미특정 시 추측으로 코드를 넓히지 말고 FAIL 반환"*), Step 3(*"REQ-011 범위 밖으로 넓히지 않는다"*)에 명시적 정지 규약이 있다.

---

## 5. 범위 (scope-guard.md · V9) 판정

### 5.1 화이트리스트 전수 대조 — **위반 0건. 직접 실행해 확인했다.**

`guard-impact-scope.py`의 실제 로직에 계획의 모든 파일 경로를 통과시켰다(hook을 실제로 실행 — V7).

**ALLOW 41건** — Step 0~9의 대상 파일 전부:
루트 빌드 스크립트 6종 · `gradle/**` 3종 · `gradlew`/`gradlew.bat` · `app/build.gradle.kts` · `proguard-rules.pro` · `AndroidManifest.xml` · `.gitignore` · `res/**`(layout·values·xml·mipmap) · `src/debug/**` · Kotlin 12종(`MainActivity`·`App`·`MainViewModel`·`MainUiState`·`bridge/**` 3종·`data/**` 2종·`gecko/**` 3종) · `assets/messaging/**` 6종 · `src/test/**` 4종 · `src/androidTest/**`

**BLOCK 2건** — 계획이 **의도적으로 배제한 것들**이 정확히 차단된다:
- `app/src/release/AndroidManifest.xml` → **BLOCK** (§10이 "설계된 정지점"이라 밝힌 그대로)
- `core/build.gradle.kts` → **BLOCK** (§2.3이 모듈 분리를 배제한 근거 그대로)

**§11의 자기 보고("모든 파일 경로가 화이트리스트 안에 있다")가 사실임을 확인했다.**

> **참고 — 가드가 막지 못하는 것:** `app/src/main/kotlin/**`는 화이트리스트에 있어 **ALLOW된다.** 즉 §2.3의 "`java/`로 확정, `kotlin/`은 만들지 않는다"는 **가드로 강제되지 않는다**(R-13이 이미 지적). Step 0 게이트의 `ls` 확인과 code-reviewer 대조가 유일한 방어다. 계획이 이를 정확히 인식하고 있다.

### 5.2 모듈 추가 / `app/src/release/**`

**계획하지 않았다.** §2.3(단일 `app` 모듈), §10(release 서명·R8·AAB 스코프 아웃). 5.1에서 둘 다 실제로 차단됨을 확인.

### 5.3 requirements.md §6 스코프 아웃 항목을 끌어들였는가

**끌어들이지 않았다.** §10이 §6을 항목별로 그대로 승계한다 — release 빌드, 세션 상태 저장·복원/회전 대응, 앞으로가기·새로고침·주소창, 다운로드·업로드·`PermissionDelegate`, 팝업·`onNewSession`·외부 인텐트, 오류 페이지·오프라인·i18n·다크모드·태블릿, naver 이외 URL UI, 앱→페이지 푸시(`connectNative`), Hilt. **누락·추가 없음.**

미구현의 귀결을 기록만 하고 구현하지 않는 태도도 옳다(예: *"`PermissionDelegate` 미구현 시 해당 기능이 조용히 실패한다는 사실만 기록"*).

### 5.4 REQ-010을 사용자가 요구하지 않은 보안 장치로 좁혔는가 — **좁히지 않았다**

| 장치 | 판정 |
|:--|:--|
| `content_scripts.matches: ["http://*/*","https://*/*"]` | **REQ-010을 좁히지 않는다** — §6 판정 참조 |
| `all_frames: false` | **좁히지 않는다** — §6 판정 참조 |
| `background.js` 함수 화이트리스트 | **좁히지 않는다** — AC-006-2가 *이미* 미지원 함수명의 reject를 요구한다. 화이트리스트는 그 요구의 구현이지 추가 제약이 아니다. 요구된 두 함수는 모든 사이트에서 동작한다 |
| **오리진 검사 훅 (P2 제안)** | **조건부 승인** — §6 판정 참조 |

---

## 6. planner가 판정을 요청한 8개 항목 — 개별 판정

| # | 항목 | 판정 |
|:--|:--|:--|
| 1 | **§2.1 D-01~D-13** (13건 전부) | **전건 승인** |
| 2 | **§2.2 Hilt 미도입** (A-09) | **승인** |
| 3 | **§2.4 툴체인** AGP 8.13.2 / Gradle 8.14.5 / Kotlin 2.3.21 | **승인** (근거 1번은 약하나 2번이 지탱, evaluator 빌드로 실증) |
| 4 | **§6.1 L-07** `kotlinx-serialization-json` | **승인** |
| 5 | **A-13** `matches` / `all_frames` | **승인** |
| 6 | **A-14** MV2 | **승인** |
| 7 | **A-15** `web_accessible_resources` + `<script>` 주입 | **승인** |
| 8 | **P2 오리진 검사 훅** | **조건부 승인** (조건 C-6) |

### 1. D-01~D-13 — 전건 승인

§1.2~§1.6에서 항목별로 대조했다. 특별히 요청된 4건:

- **D-02 (람다 seam)** — **승인.** 이름만 바꾼 우회가 아니다. `Impl`의 시그니처에서 안드로이드 타입이 **완전히 사라져** JVM 테스트가 가능해진다(§1.2 ①).
- **D-03 (`Channel` vs `SharedFlow`)** — **승인.** `SharedFlow(replay=0)`의 이벤트 유실 위험을 정확히 짚었다. 일회성 이벤트에는 `Channel`이 맞다(§1.2 ②).
- **D-08 (`OnBackPressedCallback` 소유)** — **승인.** ViewModel에 androidx.activity 타입이 0개이고, `canGoBack=false` 시 AC-011-3이 별도 코드 없이 성립한다(§1.2 ③).
- **D-13 (`strings.xml` 미적용)** — **승인.** 조항은 Android 리소스 시스템에 대한 것이며 웹 자산에는 적용 수단 자체가 없다(§1.6).

### 2. Hilt 미도입 — 승인

`architecture.md`는 **"Dispatcher는 주입"·"생성자 주입"만 요구하고 DI 프레임워크를 요구하지 않는다.** 수동 DI로 원칙이 전부 충족되며, 이는 §1.4에서 확인했다(특히 D-11의 "기본값 금지"가 프레임워크 없이도 주입을 강제한다).

부수 근거도 타당하다 — 주입 대상 5개는 손익분기점 아래이고, Hilt는 승인 대상 아티팩트를 3개 늘리며 KSP 라운드가 빌드 루프에 얹힌다(evaluator 실측: 클린 `assembleDebug` 56초, GeckoView AAR 처리 포함). **수동 DI 배선이 `App.kt` 한 파일에 보이는 것이 `comment-style.md`의 "Android 초보 개발자" 독자 기준에 유리하다**는 논거도 이 프로젝트에 한해 설득력이 있다.

### 3. 툴체인 — 승인 (evaluator가 전 조합을 직접 빌드해 실증)

§0.3의 표가 근거다. AGP 9 배제 근거 3개 중 **2번이 결정적이고 evaluator가 실증했다**(GeckoView POM의 `kotlin-stdlib 2.3.21` 직접 선언 확인 → AGP 9의 내장 Kotlin 2.2.10과 역전). 1번은 논증이 약하나 결정을 바꾸지 않는다(§1.5).

**R-03의 "시스템 gradle 9.3.1을 그대로 wrapper로 쓰면 안 된다"는 실질적 수확이다.** evaluator는 반대 방향으로 확인했다 — **시스템 gradle 9.3.1로 8.14.5 wrapper를 생성하는 것은 정상 동작한다.** 계획의 지시(`gradle wrapper --gradle-version 8.14.5`)가 그대로 성립한다.

### 4. L-07 — 승인

§2의 L-07 항목 참조. **커버리지 목표와의 종속 관계가 실재하며 evaluator가 실증했다.**

### 5. A-13 (`matches: ["http://*/*","https://*/*"]`, `all_frames: false`) — 승인

**`matches` — 축소가 아니라 스킴 정리라는 planner의 논증을 인정한다.**
- **어떤 도메인도 제외하지 않는다.** 사용자 결정(A-08)은 "외부 사이트에서도 브리지 호출 가능"이며, `http`/`https` 두 스킴이 "웹사이트"의 전부다.
- `<all_urls>`가 추가로 덮는 것은 `ws`·`wss`·`ftp`·`data`·`file`이다. 이 중 **`data:`와 `file:`은 오리진 의미가 특수해 브리지 주입 동작이 불명확**하고([미확인]), `ws`/`wss`는 문서가 아니라 소켓이라 content script 주입 대상이 아니다. **사용자가 "외부 사이트"라 말했을 때 `data:` URI를 의도했다고 보기 어렵다.**
- REQ-008(naver)과 AC-010-4(제2 사이트)를 모두 덮는다. §2.8이 지적한 http/https 리다이렉트 문제도 두 스킴을 함께 넣어 해소된다.

**`all_frames: false` — 요구 축소가 아니다.**
- manifest **기본값**을 명시적으로 유지하는 것이며, 사용자가 `all_frames`에 대해 어떤 요구도 하지 않았다.
- 사용자가 말한 "외부 사이트"는 **방문한 사이트**를 뜻한다고 읽는 것이 자연스럽다. 서드파티 광고 iframe에까지 브리지(= `appFinish` 포함)를 넣는 것은 사용자가 요구한 것이 아니며, 넣지 않는다고 REQ-010의 어떤 AC도 깨지지 않는다(AC-010-1~6은 모두 최상위 문서 기준).
- **`true`로 바꿔야 할 근거가 요구사항 어디에도 없다.**

### 6. A-14 MV2 — 승인

- **근거가 타당하다:** 영속 background page라 메시지 중계의 수명주기 문제가 없다. MV3의 이벤트 페이지는 종료 후 첫 메시지의 지연·유실을 **별도로 검증해야 하는데**, 이 배치는 이미 5단 경로 전체가 [미확인]이라 검증 부담을 더 얹을 여유가 없다는 판단이 옳다.
- MV2에서 `web_accessible_resources`가 단순 문자열 배열이라 A-15 구현이 단순해지는 부수 효과도 실질적이다.
- **U-05(GeckoView 153이 MV2를 수용하는가)를 Step 2/G2-e에 배치**해 조기 확인하도록 한 것이 적절하다. 거부되면 MV3 전환이고, 그 경우 추가 검증 항목이 무엇인지도 U-05에 적혀 있다.
- 스코프 아웃 항목(팝업·다운로드 등)이 없어 MV3의 이점이 이 배치에서 발생하지 않는다.

### 7. A-15 페이지 세계 노출 방식 — 승인

**(가) `web_accessible_resources` + `<script>` 주입을 승인한다.**
- **표준 WebExtension 기법이며 Xray 우회 트릭이 없다.** 주입된 스크립트는 페이지 세계에서 그냥 실행되므로 `window.NativeBridge`의 정의·호출·Promise 반환이 **일반 JS 의미론 그대로** 동작한다.
- (나) `exportFunction`/`cloneInto`는 Firefox 전용 API이고 **인자·반환이 Xray 경계를 넘어와 Promise·객체 처리가 까다롭다**([미확인] U-07). 5단 경로 전체가 이미 미검증인 상황에서 검증되지 않은 API를 핵심 경로에 놓는 것은 리스크 집중이다.
- (다)는 외부 사이트에 우리 코드가 없으므로 REQ-010을 원리적으로 충족할 수 없다 — 배제가 옳다.
- **(나)를 폴백으로 남겨둔 것도 적절하다**(§9). G4-c 실패 시 전환 경로가 있고, 전환 시 U-07을 새 게이트로 추가하도록 명시했다.

### 8. P2 오리진 검사 훅 — **조건부 승인 (조건 C-6)**

**채택을 승인한다.** 판단 근거:
- **REQ-010을 침해하지 않는다.** 훅은 **자리만** 만들고 기본값이 "전체 허용"이므로 동작은 사용자 결정(A-08) 그대로다.
- requirements §6의 스코프 아웃 항목은 *"오리진 허용목록의 **기본 활성화**"*이지 훅의 존재 자체가 아니다. **기본 비활성이면 스코프 아웃을 침범하지 않는다.**
- **실질적 가치가 있다.** §5.1-4가 지적한 *"앞으로 브리지에 함수를 추가하면 추가되는 즉시 모든 웹사이트에 열린다 — 가장 큰 장기 리스크"*에 대해, 훅이 **검토 지점을 코드에 고정**한다. 함수 화이트리스트와 같은 위치에 모여 있으면 다음 개발자가 그 트레이드오프를 마주치게 된다.
- 비용이 사실상 0이다(분기 1개 + 주석).

**단, 아래 C-6을 지키지 않으면 사용자 결정을 뒤집는 것이 된다.**

---

## 7. 실행 가능성 판정

### 7.1 Step 순서가 컴파일 의존성상 성립하는가 — **성립한다**

| 전이 | 검토 |
|:--|:--|
| Step 0 → 1 | 스캐폴딩 후 GeckoView 추가. **evaluator가 이 순서를 그대로 재현해 성공** |
| Step 1 → 2 | 확장 설치에 `GeckoRuntime`(Step 1의 `App.kt`) 필요 ✓ |
| Step 2 → 3 | naver 이동에 `GeckoSession`(Step 1·2) 필요 ✓ |
| Step 3 → 4 | content script 경로가 background.js(Step 2)를 전제 ✓ |
| Step 4 → 5 | 스파이크 코드를 정식 구조로 이관. **Step 2가 `NativeBridgeHandler`에 임시 로직을 두고 Step 5가 `BridgeProtocol`/`BridgeDispatcher`로 추출**하는 순서가 명시돼 있다 ✓ |
| Step 5 → 6 | `repeatOnLifecycle`에 필요한 L-05가 Step 5 대상 파일(`libs.versions.toml`·`app/build.gradle.kts`)에 포함 ✓ |
| Step 6 → 7 | D-08 배선이 Step 5의 `MainViewModel`을 전제 ✓ |

**한 가지 관찰(차단 아님):** Step 3이 `OnBackPressedCallback`을 쓰지만 대상 파일에 `libs.versions.toml`/`app/build.gradle.kts`가 없고, L-03(`activity-ktx`)은 Step 7에서 추가된다. **`appcompat`(L-04)이 `androidx.activity`를 전이로 끌고 오므로 컴파일은 된다** — evaluator가 `appcompat 1.7.1`만으로 빌드되는 것을 확인했다. Step 7에서 명시 선언을 추가하는 순서가 맞다.

### 7.2 폴백 발동 시 실제로 복구 가능한가 — **부분적으로 미정의. 조건 C-7**

**§9의 폴백 조건과 각 Step의 실패 규약은 서로 모순되지 않는다.** 발동 조건(G2-a·G3-a·G4-c)과 전환 대상이 일관되게 대응한다. 폴백 경로가 화이트리스트 안이라는 §9의 주장도 §5.1에서 확인했다(`assets/**`·`java/**` 글롭 내부).

**그러나 절차가 미정의다.** Step 2 실패 규약은 *"파일 구성이 바뀌므로 **이 계획의 Step 2 이후를 재작성**하고 그 사실을 로그에 남긴다"*고만 적었다. **누가 재작성하는가가 정해져 있지 않다.**
- developer는 `plan.md`의 작성 주체가 아니다. 계획 재작성은 planner의 산출물이고, 변경된 계획은 **evaluator 승인 없이 착수할 수 없다**(이 게이트의 존재 이유).
- 폴백 1안은 단순 우회가 아니라 **설계 변경**이다 — `extension.metaData.baseUrl` 사용 중단, 최초 실행 시 파일 복사 로직 신설, `matches`에 `file:///*` 추가. 게다가 *"확장의 파일 접근 권한 부여 수단이 GeckoView에 있는지"*가 **[미확인]**이라 폴백 자체의 성립도 보장되지 않는다.
- 계획은 폴백 3안(localhost 서버)만 "evaluator 재승인 필요"로 적었는데, **재승인이 필요한 진짜 이유는 새 의존성이 아니라 설계 변경**이다. 그 기준이면 1안·2안도 해당한다.

→ **C-7로 절차를 명시하게 한다.** 계획을 폐기할 사유는 아니다.

### 7.3 [미확인] 14건의 배치 — **적절하다**

**설계를 뒤집을 수 있는 항목이 전부 앞쪽에 있다:**

| 항목 | 검증 시점 | 실패 시 파장 | 배치 판정 |
|:--|:--|:--|:--|
| U-01 `moz-extension://` 최상위 로드 | **Step 2** | 파일 구성 변경 | **적절 — 가능한 가장 이른 시점** |
| U-03 확장 페이지 히스토리 잔존 | **Step 3** | AC-011-1 원리적 불가 | **적절** |
| U-06 5단 경로 전체 | **Step 4** | REQ-010 재설계 | **적절** |
| U-04 wire 문자열 payload | **Step 2** | 래핑 전환(커버리지 경로 유지) | **적절** |
| U-05 MV2 수용 | **Step 2** | MV3 전환 | **적절** |
| U-02 `web_accessible_resources` 필수 여부 | Step 2 | manifest 수정 | 적절 |
| U-08 cleartext | Step 3 | 설정 조정 | 적절 |
| U-10 APK 크기 | Step 1 | — | **evaluator가 선해결(198.6 MB)** |
| U-11 compileSdk 하한 | Step 1 | 무해 | 적절 |
| U-07 `exportFunction` Xray | Step 4 폴백 시에만 | (나)안 성립 여부 | 적절 — 폴백 전용 |
| U-09 `allowInsecureConnections` 상수명 | Step 3 필요 시 | 경미 | 적절 |
| U-12 #199 크래시 | **검증하지 않음** | 무관 | **적절** — `resource://`를 콘텐츠 페이지로 열지 않기로 했고, 그 근거가 크래시가 아니라 **매치 패턴 제약**이므로 검증 불필요 |
| U-13 targetSdk 36 @ API 33 | 이상 동작 시 | R-15 | 적절 |
| U-14 serialization 1.11.0 | — | 올릴 때만 | 적절 |

**설계를 뒤집을 수 있는 [미확인]이 뒤쪽 Step에 남아 있지 않다.** Step 2·3·4를 스파이크로 앞세운 배치가 이 원칙을 구조적으로 보장한다. **계획의 가장 큰 강점이다.**

---

## 8. 승인 조건 (developer 착수 전 반영)

각 조건은 **developer가 명령으로 검증 가능한 형태**다.

### C-1 [필수] Step 1 · R-01의 APK 크기 임계값을 실측값으로 교체하라

- **삭제할 문장:** Step 1 실패 규약의 *"APK가 150 MB를 넘으면 `abiFilters`가 적용되지 않은 것이다"*, R-01 완화책의 *"150 MB 초과 시 다음 Step으로 넘어가지 않는다"*.
- **대체할 판정 방법 — 크기가 아니라 ABI 구성으로 판정한다:**
  ```
  unzip -l app/build/outputs/apk/debug/app-debug.apk | grep "lib/" \
    | awk -F'lib/' '{split($2,a,"/"); print a[1]}' | sort -u
  # 기대 출력: arm64-v8a  (이 한 줄만)
  ```
  **`arm64-v8a` 외의 ABI가 한 줄이라도 나오면 `abiFilters` 미적용이다.** 크기는 대리 신호이고 ABI 목록이 직접 신호다(V1).
- **참고 실측값(evaluator, 동일 조합):** `abiFilters` 정상 적용 시 **198,587,715 B (189.4 MiB)**. 크기 기반 보조 확인이 필요하면 **250 MB 초과 시에만** 이상으로 판정하라(3 ABI 비압축 합계가 483 MiB이므로 미적용 시 확실히 넘는다).
- **검증:** Step 1 게이트에서 위 명령 출력이 `arm64-v8a` 한 줄임을 로그에 남긴다.

### C-2 [필수] R-01의 심각도 서술을 실측으로 정정하라

- **정정 대상:** *"adb install이 수 분대가 되어 developer의 빌드·검증 루프가 사실상 마비된다"*.
- **실측:** 198.6 MB APK의 `adb install -r` = **6.872초** (SM-G981N, USB). **마비되지 않는다.**
- **유지할 결론:** `abiFilters`는 그대로 적용한다. 근거를 *"설치가 수 분대가 된다"*에서 ***"3 ABI 비압축 합계 483 MiB로 APK가 500 MB에 근접해 기기 저장공간과 전송 시간이 실제 부담이 된다"***로 교체하라.
- **검증:** Step 1에서 `time adb install -r` 실측치를 로그에 숫자로 남긴다(U-10과 통합).

### C-3 [필수] §0·§2.4의 `[확인]` 라벨을 실제 수행 범위에 맞게 정정하라

plan.md가 다음을 반영해야 한다. **이 정정 없이는 하위 에이전트가 검증되지 않은 전제를 `[확인]`으로 물려받는다.**

- §2.4의 "AGP 8.13.2 … 성공 [확인]" 행에 **planner의 검증 범위가 GeckoView를 패키징하지 않은 빌드였다**는 사실을 명시하고,
- **evaluator가 전 구성요소를 한 프로젝트에 넣어 `assembleDebug` BUILD SUCCESSFUL(56s), `jvmCoverageReport` 성공, `kotlin-stdlib 2.3.21` 해석, 매니페스트 89 service 병합, APK 내 arm64-v8a 단일을 확인했다**는 사실로 `[확인]` 근거를 교체하라(본 문서 §0.3 인용).
- §7.2의 "[확인]" 역시 **GeckoView 부재 프로젝트(`jacocoVersion 0.8.13`)에서 수행됐다**는 점을 밝히고, evaluator가 **GeckoView 포함·0.8.15 조합에서 재확인**했음을 근거로 갱신하라.
- **검증:** 정정 후 plan.md에 "GeckoView를 포함한 `assembleDebug`가 성공했다"는 진술의 출처가 evaluator 실측으로 표시되어 있을 것.

### C-4 [권고] G2-d · G3-d를 게이트 조건에서 기록 항목으로 옮겨라

- 두 항목은 *"기록한다"*가 술어이므로 무엇을 기록하든 참이 되어, AND 결합의 항으로서 **없는 것과 같다**(V1).
- **거짓 그린을 만들지는 않는다**(G2-c·G3-c가 실제 신호를 담당). 따라서 **게이트 목록에서 빼고 "Step 산출 기록 항목"으로 분류만 바꾸면 된다.** 기록 자체는 그대로 요구하라 — U-04·U-08의 판정 근거이므로 가치가 있다.
- **검증:** Step 2·3의 "검증 게이트 (전부 AND)" 목록에 술어가 "기록한다"인 항목이 0건.

### C-5 [권고] 에뮬레이터 배제 문구를 정정하라

- *"원리적으로 성립하지 않는다"* → ***"이 배치의 `abiFilters`(arm64-v8a 단일) 설정에서는 성립하지 않는다"***.
- AAR에는 `x86_64`가 있으므로(32비트 `x86`만 없음) `abiFilters`에 `x86_64`를 넣으면 에뮬레이터도 가능하다. **결정(에뮬레이터 미사용)은 승인하되 근거를 정확히 적어라** — 과장된 불가능 주장은 나중에 누군가 반증하면 그 옆의 옳은 판단까지 의심받는다.

### C-6 [필수] P2 오리진 검사 훅 채택 조건

채택을 승인하되 **아래 3개를 모두 지켜야 한다. 하나라도 어기면 사용자 결정(A-08) 위반이다.**

1. **기본값은 "전체 허용"으로 출고한다.** 훅을 켠 상태로 출고하는 것은 금지.
2. **Step 4의 G4-a~e가 훅이 존재하는 상태에서 통과해야 한다.** 특히 **G4-d(naver가 아닌 제2 사이트)** 통과가 "훅이 REQ-010을 좁히지 않았다"는 증거다. 훅을 넣은 뒤 제2 사이트에서 브리지가 죽으면 즉시 제거하라.
3. **훅 위 주석에 "기본 비활성이며, 켜는 것은 사용자 결정 사항이다(requirements §5.1, A-08)"를 한글로 명시한다.**
- **검증:** `background.js`에서 훅의 기본 분기가 무조건 통과하는지 코드 검사 + G4-d 통과 로그.

### C-7 [필수] 폴백 1·2안 발동 시의 절차를 명시하라

- Step 2·3의 *"이 계획의 Step 2 이후를 재작성"*이 **누구의 작업인지**를 적어라.
- **요구하는 절차:** 폴백 1안 또는 2안이 발동하면 developer는 **전환을 임의로 진행하지 말고**, 발동 근거(실패한 게이트·원시 오류·logcat)를 로그에 남기고 **FAIL 반환** → **planner 재실행으로 계획 갱신 → evaluator 재승인 → developer 재착수**.
  - 근거: 폴백 1·2안은 우회가 아니라 **설계 변경**이다(`extension.metaData.baseUrl` 폐기, 파일 복사 로직 신설, `matches` 변경, 2안은 `bridge-client.js` 인라인 병합). 파일이 화이트리스트 안이라는 것은 **범위 문제가 해결됐다는 뜻일 뿐 설계 승인을 대신하지 않는다.**
  - 계획이 폴백 3안에만 붙인 "evaluator 재승인"을 **1·2안으로 확장**하는 것이다.
- **검증:** §9 표의 폴백 1·2안 행에 "planner 재실행 + evaluator 재승인 필요"가 적혀 있을 것.

### C-8 [권고] 사실 정정 2건

1. **§0의 "정정 ③"을 철회하라.** rev.2의 ABI 크기(70.7/68.2/75.6 **MiB**)와 planner의 값(74.2/71.5/79.2 **MB**)은 **같은 바이트 수의 다른 단위 표기**다(74,150,236 / 71,541,879 / 79,234,693 B). rev.2는 틀리지 않았다. **plan.md 전체에서 단위를 하나로 통일하라**(현재 AAR 총량은 MiB, ABI별은 MB로 섞여 있다).
2. **§2.1의 GeckoView 버전 문구.** 현재 `<release>`는 `153.0.20260803132010`으로 이동했다. **계획이 고정한 `153.0.20260730155536`을 그대로 유지하되**(A-12 동적 버전 금지 + evaluator가 실제 빌드한 유일한 버전), "최신 stable"이라는 표현을 **"검증 완료된 고정 버전"**으로 바꿔라. `<release>`를 좇아 올리지 마라.

---

## 9. developer(4단계) 인계 주의 사항

1. **Step 1에서 APK가 198 MB대로 나오는 것은 정상이다.** 계획 원문의 150 MB 규칙은 폐기됐다(C-1). ABI 목록으로 판정하라.
2. **모든 Gradle 호출 앞에 `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`을 붙여라.** 셸 `JAVA_HOME`이 JDK 8이라는 사실을 evaluator가 재확인했다. 빠뜨리면 AGP가 뜨지 않는다.
3. **`gradle.properties`에 `org.gradle.java.home`을 박지 마라**(머신 고유 경로). 계획 R-02의 지시가 옳다.
4. **툴체인·JaCoCo·의존성 조합은 evaluator가 끝까지 빌드해 검증했다**(§0.3). Step 0·1·5에서 이 조합과 다르게 실패하면 **다른 조합을 탐색하지 말고**(V9) 실패 로그 전문과 함께 FAIL 반환하라 — 계획 Step 0의 규약 그대로다.
5. **`gradle wrapper --gradle-version 8.14.5`는 시스템 gradle 9.3.1로 정상 생성된다**(evaluator 확인). `gradle-wrapper.jar`를 손으로 만들지 마라.
6. **`adb shell pm list packages`는 이 기기에서 `--user 0` 없이는 SecurityException을 던진다**(삼성 다중 사용자). 계획은 이 명령을 쓰지 않지만 QA가 쓸 경우 `--user 0`을 붙여라. **다른 검증 명령(`dumpsys`·`ps`·`screencap`·`uiautomator dump`·`logcat`·`cmd connectivity`)은 전부 정상 동작함을 evaluator가 확인했다.**
7. **AC-009-4에서 비행기모드를 켠 뒤 반드시 해제하라.** 계획에 적혀 있으나 놓치기 쉽고, 남아 있으면 이후 모든 네트워크 검증이 거짓 레드가 된다.
8. **AC-010-5(naver에서 `appFinish`)는 앱을 죽인다. Step 9 QA 시퀀스의 맨 마지막에만 실행하라.**
9. **Step 9의 화이트리스트 대조를 생략하지 마라.** 가드는 Edit/Write에만 걸리고 **Bash로 만든 파일(스캐폴딩 대부분)은 검사조차 받지 않는다.** evaluator가 hook 로직을 직접 실행해 이 사실을 확인했다 — `app/src/main/kotlin/**`도 **ALLOW된다**. §2.3의 "`java/`만 쓴다"는 가드가 아니라 **네 손으로** 지켜야 한다.
10. **`nativeApp` 문자열 불일치는 예외도 로그도 없이 실패한다.** Step 2/G2-c 실패 시 격리 순서 1번이다.
11. **주석 3건은 선택이 아니다** — `background.js`(#220 회피 구조), `content.js`(Xray/페이지 세계), `MainActivity` 로드 지점(`resource://` 배제 근거는 크래시가 아니라 매치 패턴 제약). **셋 다 "단순화" 리팩터링을 막는 유일한 방어 수단이며 code-reviewer가 대조한다.**
12. **커버리지 70% 미달 시 테스트부터 늘리지 마라.** §7.1 클래스에 프레임워크 의존이 섞였는지부터 보라 — 미달은 대개 설계 오염의 신호다(R-11). evaluator가 `kotlinx-serialization` 기반 순수 Kotlin 클래스의 JVM 100% 커버를 실증했으므로 **설계만 지키면 목표는 달성 가능하다.**
13. **폴백 1·2안이 필요해지면 임의 전환하지 말고 FAIL 반환하라**(C-7).

---

## 10. 판정 요약

| 영역 | 판정 |
|:--|:--|
| `architecture.md` 레이어 구조 (D-01·02·04·05·08) | **충족** — ViewModel에 Context/View/androidx 타입 누출 0건, 우회 아님 |
| `architecture.md` 상태 관리 (D-03·06·07) | **충족** — 단일 UiState + 단일 render, 로딩은 UiState가 원천, 일회성 이벤트 분리 |
| `architecture.md` 비동기 (D-09·10·11) | **충족, 기준 이상** — `poll()` 명시 배제, Dispatcher 기본값 금지 |
| `architecture.md` 의존성 (D-12, §6.1) | **충족** — 11건 전건 승인, 버전 카탈로그 일원화 |
| `architecture.md` 금지 사항 (D-13, §6.2) | **충족** — 빌드 설정 11건 전부 리스크 항목과 연결 |
| `architecture.md` 테스트·커버리지 (§7) | **충족** — 70% 달성 경로를 evaluator가 실증 |
| `verification-honesty.md` V1 | **대체로 충족** — Step 4는 모범적. G2-d·G3-d는 장식(C-4) |
| V2 / V3 / V5 / V8 / V9 | **충족** |
| **V7** | **부분 미충족** — `[확인]` 라벨 과장(C-3), 허위 정정 1건(C-8) |
| `scope-guard.md` | **충족** — hook 실행 대조 결과 위반 0건 |
| 실행 가능성 (Step 순서·[미확인] 배치) | **충족** |
| 실행 가능성 (폴백 절차) | **부분 미충족** — 재작성 주체 미정의(C-7) |

**최종 판정: APPROVED (조건 C-1 ~ C-8, 이 중 C-1·C-2·C-3·C-6·C-7이 필수)**

Developer는 planner가 C-1~C-8을 plan.md에 반영한 뒤 착수한다. 조건은 전부 문서 정정이며 **설계 변경을 요구하지 않으므로 evaluator 재승인은 불필요하다** — 반영 여부는 code-reviewer가 5단계에서 대조한다.

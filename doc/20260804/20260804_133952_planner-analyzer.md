# planner-analyzer 작업 로그

- **일시**: 2026-08-04 13:39:52 (KST)
- **작업**: `initRequire.md`(GeckoView 기반 Android 앱 요구사항 9줄)를 분석해 REQ-ID 기반 요구사항 스펙 작성 + GeckoView 도입 실측 조사
- **입력**:
  - `/Users/appdevloperteam/Documents/ClaudeWorkspace/GeckoViewTest/initRequire.md`
  - 오케스트레이터(team-lead) 전달 프롬프트
  - `/Users/appdevloperteam/.claude/plugins/cache/android-pipeline-marketplace/android-pipeline/1.0.0/rules/{architecture,comment-style,scope-guard,verification-honesty,work-logging}.md`
  - 스킬 `android-pipeline:requirements-analysis`
- **산출물**:
  - `/Users/appdevloperteam/Documents/ClaudeWorkspace/GeckoViewTest/pipeline/requirements.md`
  - `/Users/appdevloperteam/Documents/ClaudeWorkspace/GeckoViewTest/doc/20260804/20260804_133952_planner-analyzer.md` (본 문서)

## 수행 내용

### 1. 프로젝트 상태 확인
대상 디렉터리는 빈 상태(`.claude/`, `.gitignore`, `initRequire.md`, 빈 `pipeline/`·`doc/`). Android 프로젝트가 존재하지 않으므로 이번 파이프라인은 **신규 스캐폴딩을 포함**한다. 이 사실을 requirements.md 상단과 §7.1에 명시했다 — impact-analyzer가 "기존 코드 영향 추적"이 아니라 "생성할 파일 화이트리스트 확정"을 해야 하기 때문이다.

### 2. 요구사항 분해
원문 9줄을 REQ-001~009로 1:1 분해하고, 파생 안전 요건 REQ-010(브리지 오리진 제한)을 추가했다. 각 REQ에 `verification-honesty.md` V1을 따른 수용 기준을 붙였다. 대리 신호 배제 원칙을 실제로 적용한 예:

- REQ-004(getVersionName): "onMessage가 호출됨"이 아니라 ① 화면에 값이 표시됨 ② `adb shell dumpsys package`의 versionName과 문자 일치 ③ `versionName`을 바꿔 재빌드하면 표시 값도 바뀜(하드코딩 가짜 구현 배제) — 세 조건이 독립적으로 깨진다.
- REQ-005(appFinish): Activity 소멸만으로는 크래시와 구분되지 않으므로 `FATAL EXCEPTION` 부재를 AND 조건으로 넣고, V1의 "늦게 오는 오류" 조항에 따라 판정 후 3초 추가 logcat 수집을 명시했다.
- REQ-009(로딩 UI): "로드 후 안 보임"만 검증하면 **로딩 UI를 아예 안 띄우는 구현이 통과**한다. 탭 직후 보임 + 완료 후 사라짐 + 동시에 콘텐츠 렌더됨을 AND로 묶고, 네트워크 차단 시 무한 로딩 금지(`onPageStop(success=false)` 경로)를 별도 조건으로 넣었다.
- REQ-003(채널): V2에 따라 background.js의 전송 한 줄을 주석 처리해 RED가 되는지 확인하는 역주입 테스트를 수용 기준에 포함했다.

### 3. GeckoView 실측 조사
추측을 배제하기 위해 문서 검색뿐 아니라 **아티팩트를 직접 조회**했다.

- `maven-metadata.xml` 직접 `curl` → 최신 stable **`153.0.20260730155536`** 확정.
- **AAR의 ZIP 중앙 디렉터리를 HTTP Range 요청으로 파싱**(전체 240MB 다운로드 없이). 얻은 실측값:
  - `AndroidManifest.xml`의 **`minSdkVersion="26"`** → 앱 minSdk 하한이 선택 사항이 아님을 확정
  - 동봉 ABI는 `arm64-v8a`/`armeabi-v7a`/`x86_64` **3종뿐, `x86` 없음** → 32비트 x86 에뮬레이터 불가
  - jni/ 합계 214.5MB, assets 13.6MB, classes.jar 1.4MB → APK 크기 리스크의 정량 근거
  - 병합되는 권한/`<uses-feature glEsVersion>`/80여 개 `<service>`(tab0~39, isolatedTab0~39, gpu, rdd 등) 목록 → REQ-001 수용 기준(AC-001-2, Gecko 자식 프로세스 존재)의 근거가 여기서 나왔다
- `.pom` 직접 조회 → 전이 의존성 전체(androidx.core 1.18.0, media3 1.10.1, play-services-fido 등) 확인. compileSdk 36 권고의 근거.
- `.module`(Gradle metadata) 조회 → 변형이 api/runtime 2개뿐이고 둘 다 동일한 universal AAR을 가리킴 → **ABI별 분리 좌표가 없음**을 확인. APK 축소는 앱 모듈의 `abiFilters`/`splits`로만 가능.
- Javadoc/firefox-source-docs에서 실제 API 이름 확인: `installBuiltIn(String)`, `ensureBuiltIn(String, String)` 시그니처와 반환형 `GeckoResult<WebExtension>`, `setMessageDelegate(delegate, nativeApp)`, `MessageDelegate.onMessage(String, Object, MessageSender)`, `onConnect(Port)`, `Port.postMessage`, `WebExtension.MetaData.baseUrl`, `GeckoRuntimeSettings.Builder.allowInsecureConnections(int)`, `ProgressDelegate.onPageStart/onPageStop/onProgressChange`.
- 확인된 것/문서상인 것/미확인을 **[확인]/[문서]/[미확인] 3등급으로 전부 표기**했다(§0 표기 규약).

### 4. 핵심 발견 — index.html을 `resource://android/assets/`로 열면 안 된다
오케스트레이터가 "`resource://android/assets/` 스킴으로 assets의 index.html 로드가 가능한지" 확인을 요청했다. 조사 결과 **두 층위로 나뉜다**:

- **확장 설치 경로로서는 지원됨**(`installBuiltIn`이 이 스킴만 허용). 즉 `assets/messaging/`에 확장을 두는 것은 정상.
- **콘텐츠 페이지로 여는 것은 부적합.** 결정적 근거는 크래시 보고(#199, GV109)가 아니라 **원리적 제약**이다: WebExtension `content_scripts.matches`가 지원하는 스킴은 http/https/ws/wss/ftp/data/file뿐이고 `<all_urls>`도 이 집합이다. `resource://`는 매치 패턴으로 쓸 수 없으며 manifest가 거부된다. 따라서 `resource://` 페이지에는 content script를 주입할 수단이 아예 없고 → 페이지가 background.js와 대화할 통로가 없어 → **REQ-003·006·007이 동시에 불가능해진다.**

권고 설계로 **index.html을 확장 자체의 페이지로 만들고 `extension.metaData.baseUrl`로 로드**하는 방안을 제시했다. 이 설계는 (a) 확장 오리진 페이지라 content script 없이 `browser.runtime.sendMessage`로 background.js와 직결되고, (b) content script 관련 미해결 결함(#220)을 우회하며, (c) 브리지가 확장 오리진에만 존재하므로 **REQ-010(외부 사이트 차단)이 설계상 자동 충족**된다. 실패 시 대안 3가지도 우선순위와 함께 기록했다.

### 5. XML View vs architecture.md 충돌 정리
`architecture.md`는 "UI (Compose)" 기준이나 REQ-002는 XML을 명시한다. **요구사항을 바꾸지 않고** 원칙별 대응표를 만들어 evaluator가 판정할 수 있게 남겼다. 최대 쟁점은 "ViewModel에 Context 주입 금지" — `getVersionName`은 PackageManager를, `appFinish`는 Activity를 필요로 한다. 제안한 해법은 ① `AppInfoRepository`로 Context를 격리 ② `appFinish`는 상태가 아닌 일회성 이벤트(Channel)로 방출하고 Activity가 `finish()` 실행. 그 외 `GeckoRuntime` 소유권(프로세스당 1회 제약 → Application 스코프), `remember` 금지 조항의 XML 대응(로딩 상태를 `ProgressBar.visibility`가 아닌 UiState에 두기), `GeckoResult` ↔ 코루틴 어댑터를 정리했다.

### 6. 환경 재측정 (V7)
오케스트레이터가 전달한 환경값을 옮겨 적지 않고 전부 재측정했다. JDK 21.0.10, ANDROID_HOME, platforms(android-36 존재 확인), build-tools, gradle 9.3.1, 실기기 `R3CN60L0QMT`/SM-G981N/`ro.build.version.sdk=33`/`ro.product.cpu.abi=arm64-v8a` 모두 **전달값과 일치**했다. 불일치 항목 없음.

## 결정 사항

| 지점 | 선택 | 이유 |
|:--|:--|:--|
| 우선순위 부여 | REQ-001~009 전부 P1 | 스킬 기본값은 "명시 없으면 P2"지만, 9개가 원문 9문장에 1:1 대응하므로 전부 P2로 내리면 변별력이 0이 된다. 파생 요건만 P2로 구분 |
| index.html 로드 방식 | 확장 페이지(`metaData.baseUrl`) | `resource://`는 content script 매치 불가라는 **원리적 제약**. 크래시 보고는 부차적 근거 |
| 브리지 노출 범위 | 확장 오리진 한정, `<all_urls>` content script 미사용 | `<all_urls>`면 임의 사이트가 `appFinish`를 호출 가능. 원문에 없는 요건이지만 방치하면 명백한 결함이라 REQ-010으로 명문화하고 사용자 통보 대상으로 표시 |
| 통신 방향 | 1차는 페이지→앱 요청/응답만 | 구체 기능 2개가 모두 요청/응답형. `connectNative`는 구조만 열어두고 미구현 (A-06으로 확인 요청) |
| L6("App 과 웹 통신을 위한 함수 제공") 해석 | L3의 상위 표현 = 함수 API 계약(REQ-006) | GeckoView에는 `addJavascriptInterface` 상당 API가 없어 WebExtension 경로가 유일. 두 벌의 브리지를 만들 이유가 없음. 다만 사용자 의도가 다를 수 있어 A-05로 확인 요청 |
| ABI | debug에 `abiFilters=["arm64-v8a"]` | 미적용 시 APK 200MB+ → 설치 수 분 → 검증 루프 마비. 실기기가 arm64-v8a임을 실측 |
| minSdk | **26** (선택지 없음) | AAR AndroidManifest 실측값. 미만이면 manifest merge 실패 |
| GeckoView 버전 | `153.0.20260730155536` 고정 | 동적 버전(`+`)은 재현성 파괴 |
| DI | Hilt 없이 수동 DI 제안 | 화면 1개 규모. 라이브러리 승인 항목을 줄임 (evaluator 판정 대상) |
| AAR 조사 방법 | 전체 다운로드 대신 HTTP Range로 ZIP 중앙 디렉터리 파싱 | 240MB 다운로드는 시간 초과. 첫 시도(`zipfile` + 스트리밍 어댑터)는 range 요청이 과다해 5분 타임아웃 → 중앙 디렉터리를 수동 파싱하는 방식으로 교체해 성공 |

## 이슈 / 리스크

**다음 단계(impact-analyzer / planner / evaluator)로 넘기는 경고:**

1. **`resource://android/assets/index.html` 직접 로드 금지** — 근거를 plan.md에 그대로 옮겨야 한다. 문서화되지 않으면 developer가 "더 간단한 방법"으로 되돌리고, 그때는 왜 안 되는지 다시 조사해야 한다. comment-style 규칙 4의 non-obvious WHY 주석 대상이기도 하다.
2. **`abiFilters` 미적용 시 APK 200MB+** — plan.md 리스크 항목 필수.
3. **content script를 네이티브 통신 1차 경로로 쓰지 말 것** — mozilla/geckoview#220 미해결 보고.
4. **`GeckoRuntime`은 프로세스당 1회 초기화** — Activity에서 생성하면 회전 시 크래시 가능.
5. **`nativeApp` 문자열 불일치는 조용히 실패한다** — `setMessageDelegate(d, "browser")` ↔ `sendNativeMessage("browser", ...)`. 오류 메시지 없이 아무 일도 안 일어나므로 디버깅 난이도가 매우 높다.
6. **커버리지 70% 달성 경로가 설계에 종속된다** — 계측 테스트는 JaCoCo에 집계되지 않고(V8), GeckoView는 실기기 없이 실행조차 안 된다. `BridgeProtocol`·`AppInfoRepository`·`MainViewModel`을 프레임워크 비의존으로 설계해야만 목표가 성립한다. **plan 단계에서 확정하지 않으면 커버리지 게이트에서 막힌 뒤 되돌아와야 한다.**
7. **assets(JS/HTML)와 Kotlin이 하나의 메시지 스키마로 결합** — impact-report.json에서 한쪽만 허용하면 브리지가 반드시 깨진다. `risk_notes`에 명시 요망.
8. **AGP/Gradle wrapper 버전 조합 미확정** — compileSdk 36 + JDK 21 전제. 시스템 gradle이 9.3.1이라는 사실이 wrapper를 9로 맞춰야 한다는 뜻은 아니다. AGP를 먼저 고르고 wrapper를 생성할 것.
9. **화면 회전 시 페이지 리로드 가능성** — 세션 상태 저장·복원을 스코프 아웃했으므로 알려진 한계로 기록.
10. **naver.com 이동 후 복귀 수단이 없다**(A-10, 스코프 아웃) — QA가 REQ-004·005를 재검증하려면 매번 앱을 재시작해야 한다. 뒤로가기 추가 여부는 사용자 확인 대상.

**차단당한 시도**: 없음. AAR 전체 다운로드 시도가 1회 타임아웃됐으나 Range 파싱으로 우회해 목표 데이터를 모두 확보했다.

**미확인으로 남긴 것**(추측으로 채우지 않음): compileSdk의 정확한 하한, `HttpsOnlyMode` 상수 전체 이름, GeckoView가 앱 시작 최상위 네비게이션으로 `moz-extension://`를 허용하는지, `web_accessible_resources` 등록 필수 여부, `resource://` 크래시(#199)가 153에서 수정됐는지, abiFilters 적용 시 실제 APK 크기, Gecko가 Android network security config를 따르는지.

## 판정

**PASS** — `pipeline/requirements.md` 생성 완료. REQ-001~010 전부에 독립적으로 깨질 수 있는 수용 기준을 부여했고, GeckoView 도입 조사는 문서 인용이 아니라 아티팩트 실측(maven-metadata / POM / .module / AAR 중앙 디렉터리·AndroidManifest)으로 뒷받침했다. XML vs architecture.md 충돌은 임의 해결하지 않고 원칙별 대응표 형태로 evaluator 판정에 넘겼다. 사용자 확인이 필요한 항목 3건(A-05, A-08, A-10)과 통보 필요 항목 2건(A-04, A-11)은 명시적으로 분리했다.

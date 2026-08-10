# GeckoViewTest

Mozilla **GeckoView**(Firefox 엔진)를 XML View 기반으로 띄우고, **WebExtension의 `background.js`를 경유해 웹 페이지 ↔ 안드로이드 네이티브** 사이에 함수를 호출할 수 있게 만든 예제 앱이다.

요구사항 원문은 [`initRequire.md`](initRequire.md), 상세 스펙은 [`doc/archive/batch-01-geckoview/requirements.md`](doc/archive/batch-01-geckoview/requirements.md)(rev.3)에 있다.

## 무엇을 하는 앱인가

- 앱을 켜면 내장 `index.html`이 GeckoView로 렌더된다
- 그 페이지의 버튼으로 **`getVersionName`**(앱 버전 조회)과 **`appFinish`**(앱 종료)를 호출할 수 있다
- **naver.com 같은 외부 사이트에서도** 같은 브리지가 동작한다 — 페이지의 실제 JS 스코프에 `window.NativeBridge`가 주입된다
- 페이지 이동 중에는 로딩 UI가 뜨고, 끝나면 사라진다
- naver.com에서 뒤로가기를 누르면 `index.html`로 돌아오고, 브리지도 다시 동작한다

---

## 1. 요구 환경

| 항목 | 값 | 비고 |
|:--|:--|:--|
| JDK | **21** (Android Studio 내장 JBR) | 셸의 `JAVA_HOME`이 JDK 8이면 AGP가 뜨지 않는다 — 아래 참조 |
| Android SDK | compileSdk **36** 설치 필요 | |
| minSdk | **26** | GeckoView AAR의 하한이라 낮출 수 없다 |
| targetSdk | 36 | |
| 기기 | **arm64-v8a 실기기** | APK가 `arm64-v8a` 단일로 빌드된다 (아래 참조) |
| 툴체인 | AGP 8.13.2 / Gradle 8.14.5 / Kotlin 2.3.21 | 검증된 조합. 임의로 올리지 말 것 |

### ⚠️ `JAVA_HOME`을 반드시 지정할 것

이 머신의 셸 `JAVA_HOME`은 JDK 8을 가리킨다. **아무 설정 없이 `./gradlew`를 실행하면 AGP가 뜨지 않는다.**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

이 문서의 모든 Gradle 명령은 위 `JAVA_HOME`이 설정돼 있다고 가정한다. 한 번만 쓰려면 명령 앞에 붙이면 된다:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug
```

> `gradle.properties`에 `org.gradle.java.home`을 넣지 말 것 — 머신 고유 절대경로라 다른 환경에서 깨진다.

### ⚠️ 에뮬레이터에서는 실행되지 않는다

APK가 `abiFilters = ["arm64-v8a"]`로 빌드되므로 **x86_64 에뮬레이터에서는 앱이 뜨지 않는다.**

GeckoView AAR 자체에는 `x86_64`가 들어 있으므로(32비트 `x86`만 없음) 에뮬레이터가 꼭 필요하면 `app/build.gradle.kts`의 `abiFilters`에 `"x86_64"`를 추가하면 된다. 다만 **APK가 크게 늘어난다** — 아래 참조.

### ⚠️ APK가 약 190 MiB인 것은 정상이다

| 구성 | APK 크기 |
|:--|--:|
| **arm64-v8a 단일 (현재 설정)** | **198.6 MB (189.4 MiB)** |
| 3개 ABI 전부 | 약 483 MiB |

AGP는 minSdk ≥ 23에서 네이티브 `.so`를 **비압축(Stored)으로** 패키징한다(시스템이 직접 mmap 하도록). GeckoView의 `libxul.so` 하나가 152 MB다.

**크기로 `abiFilters` 적용 여부를 판정하지 말고 ABI 목록으로 판정할 것:**

```bash
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep "lib/" \
  | awk -F'lib/' '{split($2,a,"/"); print a[1]}' | sort -u
# 기대 출력: arm64-v8a  (이 한 줄만)
```

설치 시간은 걱정하지 않아도 된다 — 실측 **약 6.9초**(USB).

---

## 2. 빌드 · 실행

```bash
# 빌드
./gradlew :app:assembleDebug

# 기기에 설치 + 실행
./gradlew :app:installDebug
adb shell am start -n com.example.geckoviewtest/.MainActivity

# 한 번에
./gradlew :app:installDebug && adb shell am start -n com.example.geckoviewtest/.MainActivity
```

기기가 여러 대면 `adb -s <시리얼>`로 지정한다. 연결 확인:

```bash
adb devices -l
```

### 로그 보기

debug 빌드에서는 웹 페이지의 `console.log`가 logcat으로 나온다(`consoleOutput(BuildConfig.DEBUG)`).

```bash
# 앱 + Gecko 관련 로그만
adb logcat -v time | grep -Ei "geckoviewtest|GeckoConsole|NativeBridge|GeckoView:"

# JS 오류만
adb logcat -v time | grep "JavaScript Error"
```

> **브리지가 동작하지 않을 때 이 로그가 없으면 원인 분리가 불가능하다.** JS 오류는 화면에 아무것도 남기지 않고 조용히 죽기 때문이다.

### PC Firefox로 원격 디버깅

debug 빌드는 `remoteDebuggingEnabled(BuildConfig.DEBUG)`가 켜져 있다. PC Firefox에서 `about:debugging` → **This Firefox** 옆의 **Setup** → USB 기기 연결로 페이지 콘솔에 직접 접근할 수 있다.

---

## 3. 앱 화면

앱을 켜면 나오는 화면은 두 부분이다.

```
┌─────────────────────────────┐
│  [ NAVER.COM 이동 ]         │  ← 안드로이드 버튼 (XML 레이아웃)
│                             │     문자열 리소스는 "naver.com 이동"이고
│                             │     Material 테마가 대문자로 표시한다
├─────────────────────────────┤
│  네이티브 브리지 테스트      │
│  브리지 상태: READY          │  ← 여기가 (JS 미실행)이면 JS가 죽은 것
│                             │
│  [ getVersionName 호출 ]    │  ← 웹 페이지(index.html)의 버튼
│  [ 없는 함수 호출 ]          │
│  [ appFinish 호출 (앱 종료) ]│
│                             │
│  결과가 여기에 표시된다       │
└─────────────────────────────┘
        (로딩 중이면 "불러오는 중…" 표시)
```

- **`NAVER.COM 이동`** 버튼만 안드로이드 XML 위젯이고, 나머지는 GeckoView가 렌더한 웹 페이지다
- **`브리지 상태`** 는 JS가 채운다. `(JS 미실행)`이 그대로면 페이지 스크립트가 실행되지 않은 것이다

naver.com으로 이동하면 페이지 최상단에 **검정 배경 + 초록 글씨 배지**가 붙는다:

```
versionName = 1.0.0 [PAGE_WORLD]
```

이 배지는 외부 사이트 브리지가 **페이지 세계에서** 동작함을 보여주는 검증 장치다. 콘텐츠 렌더 실패가 아니다.

---

## 4. 웹 페이지에서 브리지 쓰는 법

### API

페이지 JS에서 `window.NativeBridge.call()` 하나만 쓰면 된다. 함수별 배선 없이 **함수명 문자열만 바꾸면 된다.**

```js
// 앱 버전 조회
NativeBridge.call('getVersionName')
  .then(v => console.log('버전:', v))     // "1.0.0"
  .catch(e => console.error(e.code, e.message));

// 앱 종료
NativeBridge.call('appFinish');

// 인자를 넘길 때 (현재 제공 함수는 인자를 쓰지 않는다)
NativeBridge.call('someFunc', { key: 'value' });
```

- 성공하면 **값으로 resolve**, 실패하면 **Error로 reject**된다
- reject된 Error에는 `code` 속성이 붙는다 — `UNKNOWN_FUNCTION` / `INVALID_REQUEST` / `INTERNAL_ERROR`
- **`appFinish`의 resolve에 의존하지 말 것** — 프로세스가 죽는 중이라 응답이 도착하지 않을 수 있다

### 제공 함수

| 함수명 | 반환 | 설명 |
|:--|:--|:--|
| `getVersionName` | `string` | `PackageManager`로 조회한 앱 `versionName` |
| `appFinish` | `true` | Activity 종료 요청 |

### 어디서 쓸 수 있나

| 페이지 | 사용 가능 | 경로 |
|:--|:--:|:--|
| 내장 `index.html` (확장 페이지) | O | 3단: `bridge-client.js` → `background.js` → 네이티브 |
| **모든 http/https 사이트** | **O** | 5단: 페이지 → `page-bridge.js` → `content.js` → `background.js` → 네이티브 |

> **보안 주의**: 브리지는 **모든 http/https 사이트에 열려 있다.** 사용자가 명시적으로 선택한 설계다(`doc/archive/batch-01-geckoview/requirements.md` §5.1). 임의의 웹사이트가 `appFinish`로 앱을 종료시킬 수 있고, MITM으로 주입된 스크립트도 같은 권한을 갖는다.
>
> 제한이 필요해지면 `background.js`의 `isOriginAllowed()` 훅이 자리만 잡혀 있다. **현재는 기본값이 "전체 허용"이며, 켜는 것은 별도 결정 사항이다.**

### 메시지 스키마 (JSON)

```jsonc
// 요청 (페이지 → background.js → 네이티브)
{ "id": "<고유 문자열>", "type": "call", "name": "<함수명>", "payload": { } }

// 응답 (성공)
{ "id": "<요청 id>", "type": "result", "ok": true,  "value": <임의 JSON> }

// 응답 (실패)
{ "id": "<요청 id>", "type": "result", "ok": false, "error": { "code": "...", "message": "..." } }
```

---

## 5. 브리지 함수 추가하기

**두 곳을 고쳐야 하고, 순서가 중요하다.**

1. **먼저 JS** — `app/src/main/assets/messaging/background.js`의 `ALLOWED_FUNCTIONS` 배열에 함수명 추가
2. **그다음 Kotlin** — `bridge/BridgeDispatcher.kt`의 `when` 분기 추가 + `BridgeProtocol.kt`에 `FN_*` 상수 추가

순서가 중요한 이유는 **한쪽만 고쳤을 때의 결과가 비대칭**이기 때문이다:

| 한쪽만 고친 경우 | 결과 |
|:--|:--|
| JS에만 추가 | 네이티브가 `UNKNOWN_FUNCTION`으로 거절 → **시끄럽게 실패 (안전)** |
| Kotlin에만 추가 | JS 화이트리스트가 먼저 막아 아무 일도 안 일어남 → **조용히 실패** |

추가 후 `BridgeWireContractTest`가 양쪽 집합 일치를 자동 검증한다(상수를 리플렉션으로 수집하므로 테스트를 고칠 필요는 없다).

---

## 6. 프로젝트 구조

```
app/src/main/
├── java/com/example/geckoviewtest/
│   ├── App.kt                     GeckoRuntime 싱글턴 (프로세스당 1회) + 수동 DI 컨테이너
│   ├── MainActivity.kt            XML 바인딩, GeckoSession 소유, 확장 설치/로드
│   ├── MainViewModel.kt           UiState(StateFlow) + 일회성 이벤트(Channel)
│   ├── MainUiState.kt             isLoading / canGoBack / currentUrl
│   ├── bridge/
│   │   ├── BridgeProtocol.kt      ★ 메시지 스키마·상수 (순수 Kotlin, JS와 글자 단위로 맞아야 함)
│   │   ├── BridgeDispatcher.kt    ★ 함수명 → 처리 분배 (순수 Kotlin)
│   │   └── NativeBridgeHandler.kt GeckoView WebExtension.MessageDelegate 구현
│   ├── data/
│   │   ├── AppInfoRepository.kt   인터페이스 (순수 Kotlin)
│   │   └── AppInfoRepositoryImpl.kt  PackageManager 접근을 여기에 가둔다
│   └── gecko/
│       ├── AppProgressDelegate.kt    onPageStart/Stop → 로딩 상태
│       ├── AppNavigationDelegate.kt  onCanGoBack → 뒤로가기 활성 여부
│       └── GeckoResultExt.kt         GeckoResult → 코루틴 어댑터
│
├── assets/messaging/              ★ WebExtension (Kotlin과 하나의 스키마로 결합)
│   ├── manifest.json              MV2, matches, web_accessible_resources
│   ├── background.js              ★ 네이티브 메시징 중계 + 함수 화이트리스트 + 오리진 훅
│   ├── content.js                 외부 사이트: 격리 세계 ↔ 페이지 세계 중계
│   ├── page-bridge.js             외부 사이트: 페이지 세계에 window.NativeBridge 주입
│   ├── bridge-client.js           확장 페이지용 어댑터
│   ├── index.html                 내장 테스트 페이지
│   └── index-page.js              index.html의 스크립트 (CSP 때문에 외부 파일 필수)
│
└── res/layout/activity_main.xml   GeckoView + 버튼 + ProgressBar
```

**★ 표시된 파일은 서로 결합돼 있다.** `BridgeProtocol.kt`의 `NATIVE_APP`·`FN_*` 상수와 `background.js`의 `NATIVE_APP`·`ALLOWED_FUNCTIONS`가 글자 단위로 맞아야 하며, **어긋나면 예외도 로그도 없이 조용히 실패한다.**

### 읽기 전에 알아둘 설계 제약

각 파일 상단 주석에 근거가 적혀 있다. 특히 다음 넷은 **"단순화" 리팩터링을 막는 유일한 방어 수단**이다:

| 파일 | 무엇을 막는가 |
|:--|:--|
| `background.js` 상단 | content script에서 `sendNativeMessage`를 **직접** 부르면 `mozilla/geckoview#220` 결함 경로에 들어간다. 이 파일을 거치는 구조는 우회가 아니라 필수 |
| `content.js` 상단 | content script는 페이지와 **JS 스코프가 격리**돼 있다(Xray vision). `window.X = ...`로 대입해도 페이지 JS는 볼 수 없다 |
| `MainActivity.kt` 로드 지점 | `resource://android/assets/index.html`을 콘텐츠 페이지로 열면 안 되는 이유는 크래시가 아니라 **`content_scripts.matches`가 `resource://`를 지원하지 않는 매치 패턴 제약**이다 |
| `index-page.js` 상단 | 확장 페이지에는 기본 CSP `script-src 'self'`가 걸려 **인라인 `<script>`가 실행되지 않는다** |

---

## 7. 자동 테스트

```bash
# 단위 테스트 (48건)
./gradlew :app:testDebugUnitTest

# 커버리지 리포트 생성
./gradlew :app:jvmCoverageReport
```

리포트 위치:
- HTML — `app/build/reports/jacoco/jvmCoverageReport/html/index.html`
- XML — `app/build/reports/jacoco/jvmCoverageReport/jvmCoverageReport.xml`

현재 수치:

| 지표 | 값 |
|:--|:--|
| 테스트 | **48건** / 실패 0 / skip 0 / `@Ignore` 0 |
| LINE | 98/98 = **100%** |
| BRANCH | 18/18 = **100%** |
| CLASS | 14/14 |

### ⚠️ 반드시 `--rerun-tasks`로 돌릴 것

**알려진 문제**: 계약 테스트 15건이 `src/main/assets/messaging/`을 직접 읽는데, **그 경로가 Gradle 태스크 입력으로 선언돼 있지 않다.** 따라서 **자산(JS/JSON)만 수정하면 테스트가 `UP-TO-DATE`로 건너뛰어진다** — 자산이 깨져도 초록이 남는다.

```bash
# 자산을 고친 뒤에는 반드시 이렇게
./gradlew :app:testDebugUnitTest --rerun-tasks
```

근본 해결은 `app/build.gradle.kts`의 테스트 태스크에 `inputs.dir("src/main/assets/messaging")`을 선언하는 것이다 (후속 과제 F-1).

### 커버리지에서 제외된 것

`app/build.gradle.kts`의 `coverageExclusions` 참조. 제외 기준은 **"테스트하기 귀찮아서가 아니라 JVM 테스트로 실행 자체가 불가능한가"** 다 — `App`, `AppContainer`, `MainActivity`, `gecko/**`, `NativeBridgeHandler`가 해당한다.

> **100%를 액면 그대로 읽지 말 것.** JS 자산 전량은 JaCoCo 분모에 **0 기여**한다. 계측 테스트도 집계되지 않는다. 즉 이 수치는 **JVM 테스트가 닿는 Kotlin 레이어**에 한정된 것이다.

---

## 8. 수동 QA 절차

자동 테스트로 덮이지 않는 영역(GeckoView 렌더링, 실제 브리지 왕복, 페이지 세계 주입)은 **실기기에서 수동으로 확인해야 한다.**

### 8.0 준비

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ADB=~/Library/Android/sdk/platform-tools/adb
export PKG=com.example.geckoviewtest

# 기기 확인 + API 버전 기록 (AC-011-3 판정에 필요)
$ADB devices -l
$ADB shell getprop ro.build.version.sdk        # 이 값을 반드시 기록할 것
$ADB shell getprop ro.product.cpu.abi          # arm64-v8a 여야 함

# 화면을 켜둔다 (도즈 상태면 Activity가 뜨지 않아 무더기로 실패한다)
$ADB shell input keyevent KEYCODE_WAKEUP

# 설치
./gradlew :app:installDebug
```

> ⚠️ **기기에 `kr.co.chunjae.android.geckoviewtestapp`라는 이름이 비슷한 무관한 앱이 있을 수 있다.** 반드시 `com.example.geckoviewtest`로 정확히 필터할 것.
>
> ⚠️ `$ADB shell pm list packages`는 이 기기에서 **`--user 0` 없이 SecurityException**을 던진다.

### 8.1 검증 순서 — 반드시 지킬 것

**`appFinish` 검증은 앱을 죽이므로 맨 마지막에 한다.** 중간에 넣으면 이후 케이스가 무더기로 실패하고 원인 분리가 어려워진다.

```
① 렌더링 → ② 내장 브리지 → ③ naver 이동 + 로딩 → ④ 페이지 세계 브리지
  → ⑤ 뒤로가기 → ⑥ 오류 경로 → ⑦ appFinish (마지막)
```

---

### ① GeckoView 렌더링 확인

```bash
$ADB shell am force-stop $PKG
$ADB shell am start -n $PKG/.MainActivity
sleep 3
$ADB exec-out screencap -p > /tmp/01_launch.png
```

**합격 조건 (전부 AND)**
- 스크린샷에 `index.html`의 버튼 3개 + `NAVER.COM 이동` 버튼이 보인다. 흰 화면·검은 화면이 아니다
- 화면의 **`브리지 상태: READY`** — `(JS 미실행)`이면 JS가 죽은 것이다
- Gecko 자식 프로세스가 떠 있다:

```bash
$ADB shell ps -A | grep $PKG
# 기대: :gpu_..., :tab_... 등 접미사가 붙은 자식 프로세스가 1개 이상
# (WebView로는 절대 나올 수 없는 신호다)
```

> **오독 주의**: `uiautomator dump`의 접근성 트리는 GeckoView를 `class="android.webkit.WebView"`로 보고한다. 이는 GeckoView의 **접근성 호환 매핑**이며 WebView 사용이 아니다.

---

### ② 내장 페이지 브리지 왕복 (`getVersionName`)

화면의 **`getVersionName 호출`** 버튼을 탭한 뒤:

```bash
$ADB exec-out screencap -p > /tmp/02_version.png

# 기준값
$ADB shell dumpsys package $PKG | grep versionName
```

**합격 조건**
- 결과 영역에 `versionName = 1.0.0` 같은 값이 표시된다 (빈 문자열·`undefined`·`null` 아님)
- 그 값이 `dumpsys`의 `versionName=`과 **문자 단위로 일치**한다

**하드코딩 판별 (선택, 더 강한 검증)**

`app/build.gradle.kts`의 `versionName`을 `"7.7.7-probe"`로 바꿔 재빌드·재설치한 뒤 화면 값도 같이 바뀌는지 확인한다. **확인 후 반드시 원복할 것.**

---

### ③ naver.com 이동 + 로딩 UI

**`NAVER.COM 이동`** 버튼을 탭하고 **즉시** 덤프한다.

```bash
# adb 왕복 지연을 없애려면 기기 안에서 연속 실행하는 편이 낫다
$ADB shell "input tap <버튼좌표>; for i in 1 2 3 4; do uiautomator dump /sdcard/d\$i.xml; done"
$ADB shell 'grep -l "불러오는 중" /sdcard/d*.xml'
```

**합격 조건 (전부 AND)**
- 탭 직후 샘플에 **`불러오는 중…`이 보인다**
- 로드 완료 후 샘플에는 **없다**
- **같은 덤프에서 동시에** 네이버 콘텐츠가 렌더돼 있다 (로딩만 사라지고 빈 화면인 상태를 걸러낸다)

```bash
$ADB exec-out screencap -p > /tmp/03_naver.png

# 도메인 확인 (https 리다이렉트는 실패가 아니다)
$ADB logcat -d | grep -E "GeckoView:(PageStart|LocationChange)" | tail -5
# 기대: uri=http://naver.com/ → uri=https://m.naver.com/

# 오류 부재
$ADB logcat -d | grep -E "NS_ERROR_|about:neterror"
# 기대: 0건
```

**무한 로딩 금지 확인 (네트워크 실패 경로)**

```bash
$ADB shell svc wifi disable && $ADB shell svc data disable
# → naver 버튼 탭 → 여러 번 덤프해서 "불러오는 중"이 남아 있지 않은지 확인

# ⚠️ 반드시 복구할 것 — 안 하면 이후 모든 네트워크 검증이 거짓 실패가 된다
$ADB shell svc wifi enable && $ADB shell svc data enable
$ADB shell settings get global airplane_mode_on   # 0 이어야 함
```

> 실패 경로는 `PageStart` → `PageStop`이 **약 220ms**로 매우 짧아 `uiautomator dump`(1주기 ~700ms)로는 로딩 UI 등장 순간을 포착하기 어렵다. **포착 실패를 결함으로 해석하지 말 것** — 계측 한계다. 반드시 **"N회 중 M회"** 형태로 기록한다.

---

### ④ 외부 사이트 페이지 세계 브리지 ★ 가장 중요

**여기가 이 앱에서 거짓 그린 위험이 가장 높은 지점이다.** content script는 페이지와 격리된 별도 JS 세계에서 돌기 때문에, **격리 세계 안에서만 동작해도 겉보기에는 완벽히 성공으로 보인다.**

naver.com이 뜬 상태에서:

```bash
$ADB exec-out screencap -p > /tmp/04_badge.png
```

**합격 조건 (전부 AND)**
- 페이지 최상단 배지에 값이 보인다
- 그 값이 `dumpsys`의 `versionName`과 일치한다
- **배지 문구에 `[PAGE_WORLD]` 마커가 있다** ← 이 조건이 핵심이다

`[PAGE_WORLD]` 마커가 없으면 **격리 세계에서만 동작하는 것**이며, 실제 웹페이지 JS는 브리지를 쓸 수 없는 상태다. 배지가 보인다는 사실만으로 통과 처리하면 안 된다.

**더 강한 검증 (원격 디버깅으로 페이지 스코프 직접 확인)**

debug 빌드는 `remoteDebuggingEnabled`가 켜져 있어 프로덕션 코드를 고치지 않고 페이지 세계를 직접 관측할 수 있다. PC Firefox `about:debugging`에서 해당 탭에 접속해 콘솔에서:

```js
window.__bridgeProbeRanInPageWorld   // true
typeof window.NativeBridge           // "object"
typeof window.NativeBridge.call      // "function"

// 살아 있는 5단 왕복 (과거 결과를 읽는 게 아니라 새 요청을 발행한다)
await window.NativeBridge.call('getVersionName')   // "1.0.0"
```

**제2 사이트 확인**: 브리지가 naver.com에만 하드코딩되지 않았는지 보려면 다른 사이트에서도 배지가 뜨는지 확인한다. 앱에 URL 입력 UI가 없으므로 `MainActivity.kt`의 `NAVER_URL` 상수를 `http://example.com`으로 일시 변경해 재빌드하면 된다. **확인 후 반드시 원복할 것.**

> 자동 대체 수단: `ExtensionManifestTest`가 `content_scripts.matches == ["http://*/*","https://*/*"]`를 매 빌드 검증하므로, 도메인 하드코딩은 상시 자동 차단된다.

---

### ⑤ 뒤로가기 복귀

naver.com이 뜬 상태에서:

```bash
$ADB shell input keyevent KEYCODE_BACK
sleep 2
$ADB exec-out screencap -p > /tmp/05_back.png
```

**합격 조건 (전부 AND)**
- `index.html`이 다시 렌더된다 (버튼 3개 재등장). 빈 화면·오류 페이지가 아니다
- **복귀 후 `getVersionName`이 다시 동작한다** ← 페이지만 그려지고 확장 메시징이 죽은 상태를 걸러낸다

**웹 히스토리가 없을 때의 뒤로가기 — 기기 API에 따라 결과가 갈린다**

```bash
# index.html 상태에서 뒤로가기
$ADB shell input keyevent KEYCODE_BACK
$ADB shell dumpsys activity activities | grep -A3 "MainActivity"
```

| 기기 API | 기대 동작 | 관측 |
|:--|:--|:--|
| **31 이상** | 태스크가 백그라운드로 이동 (홈 버튼과 비슷) | `state=STOPPED` **`finishing=false`**, 태스크 `visible=false` |
| **30 이하** | Activity 종료 | ActivityRecord 소멸 |

**반드시 기기 API 버전과 함께 기록할 것.** `finishing=false`를 결함으로 오판하지 말 것 — API 31+ 의 플랫폼 표준 동작이다.

---

### ⑥ 오류 경로

화면의 **`없는 함수 호출`** 버튼을 탭한다.

**합격 조건**
- 결과 영역에 `오류: UNKNOWN_FUNCTION / 브리지에 없는 함수명이다: ...` 처럼 **사유를 읽을 수 있는** 메시지가 나온다
- 앱이 죽지 않는다

> **문구 주의**: 실기기에서 보이는 것은 **JS 화이트리스트**의 `브리지에 없는 함수명이다:` 다. Kotlin `BridgeDispatcher`의 `지원하지 않는 함수명이다:` 가 **아니다.** JS가 먼저 막으므로 Kotlin 분기는 실기기에서 도달하지 않으며 **단위 테스트로만 덮인다** — 설계상 의도된 이중 방어다.

---

### ⑦ `appFinish` — 맨 마지막에

```bash
# 종료 전 상태
$ADB shell dumpsys activity activities | grep "MainActivity"
```

화면의 **`appFinish 호출`** 버튼을 탭한 뒤:

```bash
sleep 2
$ADB shell dumpsys activity activities | grep "MainActivity"
# 기대: ActivityRecord 자체가 사라진다 (0건)

# 늦게 도착하는 오류까지 잡기 위해 3초 더 수집한다
sleep 3
$ADB logcat -d | grep -E "FATAL EXCEPTION|Fatal signal|libc: Fatal"
# 기대: 0건
```

**합격 조건 (전부 AND)**
- `Hist #0: ActivityRecord{… MainActivity}` **레코드 자체가 소멸**한다
- 치명 오류 0건 (**크래시로 인한 소멸과 구분하기 위해 필수**)
- 재실행하면 정상 동작한다 (런타임·확장 상태 미오염)

> ⚠️ **`dumpsys` grep 건수만으로 판정하지 말 것.** `finishing=`/`state=` 필드를 봐야 하고, 확실한 종료 신호는 **레코드 자체가 사라지는 것**이다. 이 함정에 이 프로젝트에서 두 명이 걸렸다.

**외부 사이트에서의 `appFinish`** — naver.com 페이지 세계에서 `NativeBridge.call('appFinish')`를 호출해도 같은 결과가 나와야 한다. 이것이 "브리지가 외부 사이트에 열려 있다"는 설계가 실제로 구현됐다는 증거다.

---

### 8.2 QA 체크리스트

| # | 항목 | 판정 신호 | 결과 |
|:--|:--|:--|:--|
| 1 | GeckoView 렌더링 | 스크린샷 + Gecko 자식 프로세스 ≥1 | ☐ |
| 2 | JS 실행 | `브리지 상태: READY` | ☐ |
| 3 | 내장 브리지 왕복 | 화면 값 == `dumpsys versionName` | ☐ |
| 4 | naver 이동 | 콘텐츠 렌더 + 도메인 naver.com + `NS_ERROR_` 0건 | ☐ |
| 5 | 로딩 UI 등장 | 탭 직후 덤프에 `불러오는 중` | ☐ |
| 6 | 로딩 UI 소멸 | 완료 후 0건 **AND** 콘텐츠 렌더 | ☐ |
| 7 | 무한 로딩 금지 | 네트워크 차단 시에도 로딩 잔존 0건 | ☐ |
| 8 | **페이지 세계 브리지** | 배지에 **`[PAGE_WORLD]`** 마커 | ☐ |
| 9 | 뒤로가기 복귀 | `index.html` 재렌더 **AND** 브리지 재동작 | ☐ |
| 10 | 뒤로가기 기본 동작 | 기기 API 기록 + `finishing=` 확인 | ☐ |
| 11 | 오류 경로 | 사유 판독 가능 + 앱 생존 | ☐ |
| 12 | `appFinish` | ActivityRecord 소멸 **AND** 치명 오류 0건 | ☐ |

**기기 정리**: 검증이 끝나면 비행기모드/네트워크가 원복됐는지 확인한다.

```bash
$ADB shell settings get global airplane_mode_on   # 0
$ADB shell dumpsys connectivity | grep "Active default network"
```

### 8.3 QA 기록 원칙

- **"통과"만 적지 말고 무엇을 관찰했는지 적을 것.** 값·스크린샷 경로·logcat 구간을 남긴다
- **관측하지 못한 것은 "통과"로 적지 말고 "관측하지 않음 + 사유"로 적을 것**
- **비결정적 현상은 "N회 중 M회"로만 보고할 것.** 3회 실행으로 "해결됨"을 판정하지 않는다
- **재현 실패를 결함 부재로 해석하지 말 것**

---

## 9. 트러블슈팅

| 증상 | 원인 / 확인 |
|:--|:--|
| `./gradlew`에서 AGP 오류 | `JAVA_HOME`이 JDK 21인지 확인 (셸 기본값은 JDK 8) |
| 에뮬레이터에서 앱이 안 뜸 | `abiFilters`가 `arm64-v8a` 단일이다. 실기기를 쓰거나 `x86_64` 추가 |
| **브리지가 아무 반응 없음** | ①`BridgeProtocol.NATIVE_APP`과 `background.js`의 `NATIVE_APP` 문자열 일치 확인 — **어긋나면 예외도 로그도 없이 조용히 실패한다.** ②`manifest.json`의 `geckoViewAddons`·`nativeMessaging` 권한 ③logcat의 JS 오류 |
| `브리지 상태: (JS 미실행)` | 확장 페이지 CSP 위반. **인라인 `<script>`는 실행되지 않는다** — 외부 파일로 분리할 것. logcat에 `Content-Security-Policy: ... blocked an inline script` |
| 외부 사이트 배지에 `[PAGE_WORLD]` 없음 | `page-bridge.js`가 `web_accessible_resources`에 등록됐는지 확인. 없으면 격리 세계에서만 동작한다 |
| 자산을 고쳤는데 테스트 결과가 그대로 | `UP-TO-DATE`로 건너뛴 것. `--rerun-tasks`로 강제 실행 |
| `pm list packages` SecurityException | `--user 0` 추가 |
| 접근성 트리에 `android.webkit.WebView` | GeckoView의 호환 매핑. WebView 사용이 아니다 |

---

## 10. 알려진 제약 · 후속 과제

### 이번 범위에서 제외한 것

세션 상태 저장·복원(화면 회전 시 페이지가 리로드될 수 있음), 다운로드·파일 업로드, 권한 프롬프트(`PermissionDelegate` 미구현 — 페이지가 알림/위치를 요청하면 조용히 실패), 팝업·새 창(`onNewSession` — **네이버의 외부 링크를 탭해도 이동하지 않는다**), 주소 입력창, release 빌드·서명·R8, 다국어·다크모드.

### 후속 과제

| ID | 내용 | 심각도 |
|:--|:--|:--|
| **F-1** | 테스트 태스크에 `inputs.dir("src/main/assets/messaging")` 선언 | **잠재적 거짓 그린** — 자산이 깨져도 초록이 남는다 |
| **F-5** | **git 저장소가 아니다** — diff 기반 검증과 원복 보증이 불가능 | 높음 |
| F-3 | `NativeBridgeHandler`(커버리지 구조적 예외)의 보상 통제 | 다음에 이 파일을 수정할 때까지 |
| F-2 | `AppBridgeHost`가 원칙이 아니라 파일 위치로 커버리지 제외됨 | 낮음 (포함해도 96.08%) |
| F-4 | 베이스라인을 `coverage-baseline.json`으로 명문화 | 낮음 |
| — | 확장 설치 실패 시 사용자 대면 처리 (현재는 미처리 코루틴 예외로 앱이 죽는다) | 다음 배치 |
| — | 검증 프로브 배지를 debug 빌드로 게이팅 | **release 도입 전 필수** |

---

## 11. 문서

이 프로젝트는 7단계 파이프라인(요구사항 분석 → 영향 분석 → 계획 → 검증 → 구현 → 리뷰 → QA → 커버리지)으로 만들어졌다. 판단 근거가 모두 문서로 남아 있다.

| 문서 | 내용 |
|:--|:--|
| [`doc/archive/batch-01-geckoview/requirements.md`](doc/archive/batch-01-geckoview/requirements.md) | REQ-001~011과 수용 기준(AC-*). **rev.3이 최신** |
| [`doc/archive/batch-01-geckoview/plan.md`](doc/archive/batch-01-geckoview/plan.md) | 설계 결정(D-01~D-13), 브리지 계약, 작업 순서와 게이트 |
| [`doc/archive/batch-01-geckoview/evaluation.md`](doc/archive/batch-01-geckoview/evaluation.md) | 아키텍처 검증 판정 |
| [`doc/archive/batch-01-geckoview/review.md`](doc/archive/batch-01-geckoview/review.md) | 코드 리뷰 |
| [`doc/archive/batch-01-geckoview/qa-report.md`](doc/archive/batch-01-geckoview/qa-report.md) | **AC별 실기기 관측값** — 수동 QA의 상세 근거 |
| [`doc/archive/batch-01-geckoview/coverage-report.md`](doc/archive/batch-01-geckoview/coverage-report.md) | 커버리지 판정과 사각지대 분석 |
| `doc/<날짜>/` | 각 단계 작업 로그 13개 |

**GeckoView 관련 의사결정의 근거**(왜 `resource://`를 쓰지 않는지, 왜 MV2인지, 왜 `abiFilters`가 필요한지 등)는 `requirements.md` §2와 `plan.md` §2에 실측값과 함께 정리돼 있다.

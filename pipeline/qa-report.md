# QA 리포트 (파이프라인 6단계)

- **일시**: 2026-08-06 08:54 (KST) — 측정 시각은 각 절에 병기
- **대상**: `pipeline/review.md` rev.2(APPROVED) 시점의 구현
- **판정 기준**: **`pipeline/requirements.md` rev.3** (rev.2 문언으로 판정하지 않았다 — AC-011-3·AC-002-2가 rev.3에서 정정됐다)
- **입력**: `requirements.md` rev.3, `plan.md` rev.2, `evaluation.md`, `review.md` rev.1 §9 + rev.2 §17, `impact-report.json`, 규칙 5종
- **검증 환경**: 실기기 `R3CN60L0QMT`(SM-G981N), JDK = Android Studio JBR 21
- **원칙**: 테스트 코드만 작성했다. **프로덕션 코드·빌드 스크립트는 한 줄도 수정하지 않았다**(RED 실험의 일시 변경은 전부 원복하고 sha256으로 확인했다 — §3).

> **진행 메모**: 이 리포트는 단계별로 나누어 기록했다. §1~§5(베이스라인·미커버 분기·추가 테스트·RED 증명·커버리지 재측정)가
> 먼저 확정됐고, §6(실기기 AC 검증) 이후가 그다음에 채워졌다.

---

## 1. 베이스라인 재측정 (V7 — 인계값을 옮겨 적지 않았다)

인계받은 값(31건 / LINE 98/98 / BRANCH 16/18 / CLASS 14)을 인용하지 않고 **직접 강제 실행해 재측정**했다.

```
JAVA_HOME=<AS JBR> ./gradlew :app:jvmCoverageReport --rerun-tasks
→ BUILD SUCCESSFUL, 29 actionable tasks: 29 executed (up-to-date 0건)
```

| 항목 | 인계값 | **내 재측정값** | 판정 |
|:--|:--|:--|:--|
| 테스트 케이스 | 31건 | **31건 / 실패 0 / skip 0** | 일치 |
| LINE | 98/98 = 100.0% | **98/98 = 100.0%** | 일치 |
| BRANCH | 16/18 = 88.9% | **16/18 = 88.9%** | 일치 |
| CLASS | 14 | **14/14 = 100.0%** | 일치 |
| 70% 미달 클래스 | 0건 | **0건** | 일치 |

**인계값에 오류 없음.** 캐시된 `up-to-date`를 근거로 쓰지 않았다.

---

## 2. BRANCH 미커버 2개의 정체 — **도달 가능한 실제 분기였다. 테스트로 덮었다.**

JaCoCo XML을 직접 파싱해 미커버 분기의 위치를 특정했다(추정하지 않았다).

```
BridgeProtocol.kt line 67  missedBranches=2  coveredBranches=2
```

**정체**: `BridgeProtocol.kt:67`

```kotlin
require(request.name.isNotBlank()) { "name이 비어 있다" }
```

- 바로 위 두 줄(`:65` id 검사, `:66` type 검사)은 **양쪽 분기가 다 덮여 있었다** — 각각 `id가 비어 있으면 실패로 처리한다`, `type이 call이 아니면 실패로 처리한다` 케이스가 있었기 때문이다.
- **`name`이 비어 있는 요청만 케이스가 없었다.** 즉 Kotlin이 생성한 도달 불가능한 방어 코드가 **아니라**, 페이지가 `{"id":"x","type":"call","name":""}`를 보내면 실제로 타는 경로였다.
- 방치했을 때의 증상: 빈 이름이 디스패처까지 내려가 `지원하지 않는 함수명이다: ` 라는 **이름이 빠진 사유**가 페이지로 돌아간다. 무엇을 잘못 불렀는지 알 수 없는 응답이 된다.

**처리**: `BridgeProtocolTest`에 `name이 비어 있으면 실패로 처리한다` 추가 → **BRANCH 18/18 = 100.0%**.

---

## 3. 추가한 테스트 — 17건 (31 → 48)

**프로덕션 코드는 수정하지 않았다.** 전부 `app/src/test/**`(화이트리스트 `allowed_globs` 내부)다.

### 3.1 신규 스위트 ① `BridgeWireContractTest` (7건) — 코틀린↔JS 계약 대조

**무엇을 보장하는가**: 브리지 계약은 서로 다른 두 언어의 파일에 나뉘어 있고 어긋나도 **컴파일러도 린트도 침묵한다**. impact-report.json이 `risk_notes`에서 최우선으로 경고한 결합면이며, 지금까지 **자동 통제가 0이었다.** 이 스위트가 그 결합을 처음으로 자동 검증한다.

| # | 테스트 | 무엇을 보장하는가 |
|:--|:--|:--|
| W-1 | `코틀린이 아는 함수명은 모두 background_js 화이트리스트에도 있다` | `BridgeProtocol`의 `FN_*` 상수를 **리플렉션으로** 모아 JS 목록과 대조. 상수가 늘면 테스트가 저절로 따라간다. **이 방향이 어긋나면 조용히 실패한다**(JS가 먼저 막아 logcat에도 안 남는다) |
| W-2 | `background_js가 허용한 함수명은 모두 디스패처가 실제로 처리한다` | 텍스트 비교가 아니라 **진짜 `BridgeDispatcher`에 태워** `UNKNOWN_FUNCTION`이 나오는지로 판정. 상수만 대조하면 "상수는 있는데 `when` 분기가 없는" 상태를 놓친다 |
| W-3 | `nativeApp 식별자가 코틀린과 background_js에서 같다` | 어긋나면 GeckoView가 델리게이트를 못 찾아 **예외도 로그도 없이** 아무 일도 안 일어난다(risk_notes의 "무성 실패") |
| W-4 | `ErrorCode의 모든 오류 코드가 background_js에도 존재한다` | 오류 코드는 닫힌 집합이고 페이지가 이 값으로 분기한다. 한쪽에만 늘리면 페이지 분기가 죽은 코드가 된다. `ErrorCode` 상수도 리플렉션으로 수집 |
| W-5 | `background_js는 송신자 종류로 요청 처리를 가르지 않는다` | **AC-003-3.** `sender` 속성 접근이 0건임을 확인. 갈라지는 순간 REQ-006의 "동일 계약"이 이름뿐이 된다 |
| W-6 | `오리진 훅은 전체 허용인 채로 출고된다` | **plan.md H-1 / requirements §5.1.** 훅을 켠 채 출고하면 사용자 결정 A-08을 코드가 조용히 뒤집는다. code-reviewer가 수동으로 보던 항목을 자동화했다 |
| W-7 | `background_js는 네이티브로 객체가 아니라 JSON 문자열을 보낸다` | plan.md §3.1/§7.1. 객체 직송으로 되돌리면 코틀린이 `org.json`에 묶여 **커버리지 경로가 통째로 무너지는데**, JS 파일만 봐서는 그 사실이 드러나지 않는다 |

**보장하지 않는 것(스위트 KDoc에 명시)**: 이것은 **텍스트 대조이지 JS 실행이 아니다.** 목록이 일치해도 5단 경로가 살아 있다는 뜻이 아니다. **이 스위트가 초록인 것을 외부 사이트 브리지의 근거로 쓰면 안 된다**(AC-006-4).

### 3.2 신규 스위트 ② `ExtensionManifestTest` (8건) — manifest.json 구조 불변식

**무엇을 보장하는가**: `manifest.json`은 코드가 아니라 설정이라 컴파일러가 한 글자도 봐주지 않는다. 그런데 여기서 한 줄이 빠지면 브리지는 **깨지지 않고 반쪽만 동작한다.**

| # | 테스트 | 무엇을 보장하는가 |
|:--|:--|:--|
| M-1 | `page-bridge_js가 web_accessible_resources에 등록돼 있다` | **거짓 그린 최대 위험(§2.6.1).** 빠지면 격리 세계에서는 브리지가 완벽히 동작해 AC-010-1·2가 통과하고, **AC-010-3 하나만 깨진다.** 사람이 그것을 놓치면 그대로 초록이 된다 |
| M-2 | `manifest_version은 2다` | MV3로 올리면 `web_accessible_resources`가 **문자열 배열에서 객체 배열로 바뀌어** M-1의 파싱이 먼저 깨진다. 원인을 엉뚱한 곳에서 찾게 된다(A-14) |
| M-3 | `네이티브 메시징에 필요한 두 권한이 선언돼 있다` | `geckoViewAddons` 없으면 `sendNativeMessage`가 아예 존재하지 않는다 |
| M-4 | `nativeMessagingFromContent 권한은 선언하지 않는다` | rev.2 정정 사항. 선언해 두면 다음 사람이 "직접 불러도 되는구나"로 읽어 **mozilla/geckoview#220의 결함 경로로 되돌린다** |
| M-5 | `content script는 http와 https 전체에 주입된다` | A-13. 도메인 축소가 들어오면 REQ-010이 좁아진다 |
| M-6 | `content script를 서드파티 iframe에까지 넣지 않는다` | §5.1 완화책(`all_frames: false`) |
| M-7 | `빌트인 확장 설치에 필요한 id와 version이 있다` | 둘 중 하나만 없어도 `ensureBuiltIn`이 확장을 설치하지 못한다 |
| M-8 | `background script로 background_js를 등록한다` | 파일명이 어긋나면 background가 아예 안 뜬다 |

**보장하지 않는 것(KDoc에 명시)**: manifest가 형식상 올바르다는 것이지 **GeckoView가 이 확장을 실제로 설치한다는 뜻이 아니다.** `ensureBuiltIn` 성공·content script 실제 주입·페이지 세계 실행은 전부 실기기에서만 확인된다.

### 3.3 기존 스위트 보강 (2건)

| # | 파일 | 테스트 | 무엇을 보장하는가 |
|:--|:--|:--|:--|
| P-1 | `BridgeProtocolTest` | `name이 비어 있으면 실패로 처리한다` | §2의 미커버 분기 2개를 덮는다 |
| D-1 | `BridgeDispatcherTest` | `코루틴 취소는 오류 결과로 바뀌지 않고 취소인 채로 전파된다` | 취소를 `BridgeResult.Failure`로 바꾸면 취소가 위로 전파되지 않아 스코프가 정리되지 않는다. **범위 한계를 케이스 주석에 명시했다 — §4.2 참조** |

### 3.4 함께 처리한 비차단 지적 (review.md rev.2 §16)

- **MINOR-10 해소**: `BridgeDispatcherTest`의 클래스 KDoc이 *"예외가 오류 응답으로 바뀌는지"* 라고 과잉 단정해 바로 아래 케이스 주석과 상충하던 것을 *"삼켜지지 않고 전파되는지"* 로 고쳤다. 미보장 범위(`INTERNAL_ERROR` 변환은 `NativeBridgeHandler`의 몫)도 KDoc에 옮겨 적었다.
- MINOR-11(프로덕션 주석의 "정정 대기 중" 표현)은 **프로덕션 파일이므로 손대지 않았다.** developer 몫으로 남긴다.

---

## 4. V2 — 새 테스트가 "실패를 잡는다"는 증명 (17건 전건)

**17건 전부를 의도적으로 깨뜨려 RED를 관측하고 원복했다.** 각 실험은 **하나의 테스트만** 빨갛게 만들도록 설계했다(부수 실패 = collateral 열이 전건 0건이다 — 그래야 "이 테스트가 이 회귀를 잡는다"가 성립한다).

**중요 — 실험 방법의 함정을 먼저 처리했다**: 자산 파일만 바꾸면 `:app:testDebugUnitTest`가 **UP-TO-DATE로 건너뛴다**(§7 참조). 그래서 모든 RED 실험을 **`--rerun`으로 강제 실행**했다. 이 처리를 안 했다면 17건 전부 "초록인데 실험은 했다"는 거짓 기록이 됐을 것이다.

| # | 테스트 | 깨뜨린 방법 | 관측 | 부수 실패 | 원복 |
|:--|:--|:--|:--|:--|:--|
| P-1 | `name이 비어 있으면…` | `BridgeProtocol.kt:67`의 `require` 한 줄 삭제 | **RED** | 0건 | sha256 일치 |
| D-1 | `코루틴 취소는…전파된다` | `BridgeDispatcher`의 `getVersionName` 분기가 **`CancellationException`만** 삼켜 `Failure`로 바꾸게 변경 | **RED** | 0건 | sha256 일치 |
| W-1 | `코틀린이 아는 함수명은…` | JS 화이트리스트에서 `appFinish` 제거(코틀린에만 존재하는 상태) | **RED** | 0건 | sha256 일치 |
| W-2 | `background_js가 허용한 함수명은…` | JS에만 미구현 함수명 `getBatteryLevel` 추가 | **RED** | 0건 | sha256 일치 |
| W-3 | `nativeApp 식별자가…` | JS `NATIVE_APP`을 `"browserX"`로 변경 | **RED** | 0건 | sha256 일치 |
| W-4 | `ErrorCode의 모든 오류 코드가…` | JS의 `INTERNAL_ERROR` → `INTERNAL_FAILURE` | **RED** | 0건 | sha256 일치 |
| W-5 | `송신자 종류로 요청 처리를 가르지 않는다` | 리스너에 `sender.envType === "content_child"` 분기 추가 | **RED** | 0건 | sha256 일치 |
| W-6 | `오리진 훅은 전체 허용인 채로 출고된다` | `isOriginAllowed`를 `return false;`로(훅을 켠 상태) | **RED** | 0건 | sha256 일치 |
| W-7 | `네이티브로…JSON 문자열을 보낸다` | `JSON.stringify(request)` → `request`(객체 직송) | **RED** | 0건 | sha256 일치 |
| M-1 | `page-bridge_js가 web_accessible_resources에…` | 배열에서 `"page-bridge.js"` 제거 | **RED** | 0건 | sha256 일치 |
| M-2 | `manifest_version은 2다` | `"manifest_version": 3`으로 변경 | **RED** | 0건 | sha256 일치 |
| M-3 | `네이티브 메시징에 필요한 두 권한이…` | `geckoViewAddons` 권한 제거 | **RED** | 0건 | sha256 일치 |
| M-4 | `nativeMessagingFromContent 권한은…` | 그 권한을 추가 | **RED** | 0건 | sha256 일치 |
| M-5 | `content script는 http와 https 전체에…` | `matches`에서 `http://*/*` 제거 | **RED** | 0건 | sha256 일치 |
| M-6 | `서드파티 iframe에까지 넣지 않는다` | `"all_frames": true` | **RED** | 0건 | sha256 일치 |
| M-8 | `background script로 background_js를…` | `"scripts": ["bg.js"]` | **RED** | 0건 | sha256 일치 |
| M-7 | `빌트인 확장 설치에 필요한 id와 version이…` | 확장 `id`를 빈 문자열로 | **RED** | 0건 | sha256 일치 |

### 4.1 원복 전수 확인

실험 대상 4개 파일 전부 **실험 전 sha256과 일치**한다(백업본에서 복원 후 해시 대조).

```
5b31489df0326aa7215290165d09a7469b15003233c7a22ec3d81993aacae613  assets/messaging/background.js
726037e656276a3744c69dce74de1a6dfc9cf6f64476a963f34885608f86a090  assets/messaging/manifest.json
92817d2711d9e6cf43e1394d0ce04f1b57705a28c615d7c6f542c727304ffca9  bridge/BridgeProtocol.kt
9caadb542535c4a313764545d2db77caa1b19bd1f40d891b31224939f983b58d  bridge/BridgeDispatcher.kt
```

백업 파일(`*.bak`/`*.orig`)은 **프로젝트 밖 스크래치패드에만** 두었고 프로젝트 트리에 남기지 않았다.

### 4.2 D-1의 범위 한계 — 대리 신호로 쓰지 않기 위해 명시한다 (V1)

`NativeBridgeHandler`에 이번 배치에서 추가된 `catch (CancellationException) { throw e }`는 **D-1이 검증하지 못한다.**

- 이유: `NativeBridgeHandler`는 `android.util.Log`(JVM 단위 테스트에서 stub → 호출 즉시 예외)와 `GeckoResult`/`WebExtension.MessageSender`에 묶여 있어 **JVM에서 `onMessage`를 호출하는 것 자체가 불가능**하다. 커버리지 제외 대상인 것도 같은 이유다.
- D-1이 고정하는 것은 **한 계층 아래인 `BridgeDispatcher` 경계**다. 같은 취지의 방어지만 **같은 코드가 아니다.**
- 이 한계를 테스트 케이스 주석에 그대로 적어 두었다 — *"여기가 초록이어도 그쪽 catch 순서는 검증되지 않았다."*
- `NativeBridgeHandler`의 실제 검증 수단은 §5의 실기기 3단·5단 왕복이며, 그것도 **취소 경로를 직접 태우지는 못한다**(§7의 미자동화 항목).

---

## 5. 커버리지 재측정 (`clean` + `--rerun-tasks`)

```
JAVA_HOME=<AS JBR> ./gradlew clean :app:jvmCoverageReport --rerun-tasks
→ BUILD SUCCESSFUL   (^e: 0건, ^w: 0건)
```

| 항목 | 베이스라인(내 §1 재측정) | **추가 후** | 변화 |
|:--|:--|:--|:--|
| 테스트 케이스 | 31건 / 실패 0 / skip 0 | **48건 / 실패 0 / skip 0** | **+17** |
| LINE | 98/98 = 100.0% | **98/98 = 100.0%** | 유지 (회귀 없음) |
| **BRANCH** | 16/18 = 88.9% | **18/18 = 100.0%** | **+11.1%p** |
| CLASS | 14/14 = 100.0% | **14/14 = 100.0%** | 유지 |
| 70% 미달 클래스 | 0건 | **0건** | 유지 |
| 미커버 분기 | 2개 | **0개** | 해소 |

스위트별 케이스 수: `MainViewModelTest` 12 · `BridgeProtocolTest` **11**(+1) · `BridgeDispatcherTest` **7**(+1) · `BridgeWireContractTest` **7**(신규) · `ExtensionManifestTest` **8**(신규) · `AppInfoRepositoryTest` 3.

**LINE이 안 오른 것은 부실이 아니다.** 추가한 17건 중 15건은 **JS/JSON 자산을 대조**하는 테스트라 JaCoCo 분모에 원리적으로 기여하지 않는다(V8). 분모는 그대로인데 **BRANCH가 미커버 0으로 닫혔다**는 것이 이번 배치의 수치상 성과다.


---

## 6. 실기기 수용 기준 검증 (requirements.md **rev.3** 기준)

- **검증 기기**: `R3CN60L0QMT` = SM-G981N, **`ro.build.version.sdk = 33`** (Android 13), `ro.product.cpu.abi = arm64-v8a`
  → **AC-011-3 (d)의 기록 의무 이행. API 31+ 갈래를 적용한다.**
- 검증 시작 시 `airplane_mode_on = 0`, 화면 `mWakefulness=Awake`(V3 — 도즈로 인한 거짓 레드 배제)
- 설치 패키지: `com.example.geckoviewtest`로 **정확히 필터**했다. 기기에 있는 `kr.co.chunjae.android.geckoviewtestapp`는 무관한 앱이며 `dumpsys` 출력에서 실제로 함께 나오는 것을 확인했다(트랩 #9 실재 확인).

### 6.1 판정 요약

| REQ | 판정 | 비고 |
|:--|:--|:--|
| REQ-001 GeckoView 렌더링 | **PASS** | |
| REQ-002 XML View | **PASS** | dexdump 기준 |
| REQ-003 브리지 채널 | **PASS** | AC-003-2는 재수행 안 함(§6.3) |
| REQ-004 getVersionName | **PASS** | AC-004-3은 재수행 안 함(§6.3) |
| REQ-005 appFinish | **PASS** | |
| REQ-006 브리지 API 계약 | **PASS** | 두 경로 각각 검증 |
| REQ-007 index.html 로드 | **PASS** | |
| REQ-008 naver 이동 | **PASS** | |
| REQ-009 로딩 UI | **PASS** | AC-009-4 관측 범위는 §6.2 참조 |
| REQ-010 외부 사이트 브리지 | **PASS** | AC-010-4는 재수행 안 함(§6.3) |
| REQ-011 뒤로가기 | **PASS** | |

### 6.2 AC별 관측값

**REQ-001**
| AC | 판정 | 관측값 |
|:--|:--|:--|
| AC-001-1 | PASS | 스크린샷(1080×2400)에 네이버 콘텐츠가 이미지·텍스트까지 완전히 렌더. 흰/검은 화면 아님. `index.html`도 버튼 3개 + 결과 영역이 렌더 |
| AC-001-2 | PASS | `:gpu_disable_art_image_`, `:tab_disable_art_image_28`, `:tab_disable_art_image_29`, `:crashhelper_disable_art_image_` — **자식 프로세스 4개**. WebView로는 나올 수 없는 신호 |
| AC-001-3 | PASS | `android.webkit` grep 히트 **1건은 `activity_main.xml:5`의 설명 주석**이고 실제 사용 0건(`import android.webkit`/`<WebView` 0건). **설치 APK의 dex 7개 전수 조사에서 `android/webkit/WebView` 정의 클래스 0개** |

> **오독 주의(신규 발견)**: `uiautomator dump`의 접근성 트리는 GeckoView를 **`class="android.webkit.WebView"`** 로 보고한다. 이는 GeckoView의 접근성 호환 매핑이며 **AC-001-3 위반이 아니다.** dex·소스 양쪽에서 실사용 0건임을 확인했다. 다음 검증자가 이 문자열만 보고 위반으로 오판하기 쉽다.

**REQ-002**
| AC | 판정 | 관측값 |
|:--|:--|:--|
| AC-002-1 | PASS | `app/src/main/res/layout/activity_main.xml` 존재. `MainActivity.kt:81-82` `ActivityMainBinding.inflate` + `setContentView(binding.root)` |
| AC-002-2 | PASS | **기기에 실제로 설치된 APK를 `adb pull`로 받아**(198,619,378 B) dex 7개를 `dexdump` 전수 조사. `androidx/compose` 정의 클래스 **정확히 6개**: `runtime/Immutable`·`runtime/Stable`·`runtime/StableMarker`·`runtime/annotation/FrequentlyChangingValue`·`runtime/annotation/RememberInComposition`·`runtime/annotation/R`. **`androidx/compose/(ui\|foundation\|material)/` = 0건.** `buildFeatures.compose` 미설정, Compose 컴파일러 플러그인 미적용. rev.3의 기대 출력과 **정확히 일치** |

> `:app:dependencies | grep compose`는 판정에 쓰지 않았다(rev.3 지시).

**REQ-003 / REQ-004**
| AC | 판정 | 관측값 |
|:--|:--|:--|
| AC-003-1 | PASS | REQ-004·REQ-005 왕복이 실기기에서 **둘 다** 성공(아래) |
| AC-003-2 | **재수행 안 함** | §6.3 |
| AC-003-3 | PASS | `background.js`에 송신자별 분기 없음. **이번 배치에서 자동 테스트로 승격**(W-5: `sender` 속성 접근 0건) |
| AC-004-1 | PASS | 확장 페이지 버튼 탭 → 결과 영역 `versionName = 1.0.0` (빈 문자열·undefined·null 아님) |
| AC-004-2 | PASS | `dumpsys package … versionName=1.0.0`과 **문자 단위 일치** |
| AC-004-3 | **재수행 안 함** | §6.3 |

**REQ-005 (appFinish — 확장 페이지 경로)**
| AC | 판정 | 관측값 |
|:--|:--|:--|
| AC-005-1 | PASS | 탭 전 `Hist #0: ActivityRecord{… MainActivity}` **1건** → 탭 후 **0건**(전체 dumpsys에서 `com.example.geckoviewtest/.MainActivity` 언급 자체가 0). 단순 grep 건수가 아니라 **레코드 소멸**로 판정했다 |
| AC-005-2 | PASS | `FATAL EXCEPTION`/`Fatal signal`/`libc: Fatal` **0건** |
| AC-005-3 | PASS | 재실행 `Status: ok, TotalTime: 120ms` → `index.html` 렌더 + `브리지 상태: READY` + `getVersionName` 왕복 재성공. 런타임/확장 상태 미오염 |
| AC-005-4 | PASS | 종료 판정 후 **3초 추가 수집**에도 치명 오류 0건 |

**REQ-006 (두 경로를 각각 검증했다 — 한쪽을 다른 쪽 근거로 쓰지 않았다)**
| AC | 판정 | 관측값 |
|:--|:--|:--|
| AC-006-1 | PASS | 확장 페이지에서 `NativeBridge.call('getVersionName')` → 값 표시(Promise resolve). 전역 콜백 변수 방식 아님 |
| AC-006-2 | PASS | 확장 페이지: `오류: UNKNOWN_FUNCTION / 브리지에 없는 함수명이다: thisFunctionDoesNotExist`. **외부 사이트 페이지 세계에서도** `REJ:Error: 브리지에 없는 함수명이다: 노노존재하지않는함수` — 양쪽 다 사유 판독 가능 |
| AC-006-3 | PASS | `bridge-client.js:34`·`page-bridge.js:63` 모두 **범용 `call(name, payload)` 파사드**이고 함수별 배선 코드가 없다. 함수명 문자열은 호출 지점과 `background.js` 화이트리스트에만 등장 |
| AC-006-4 | PASS | **AC-010이 통과했다**(아래). 따라서 REQ-006 충족 |

> **[확인 — 인계 사항 재현]** AC-006-2의 실기기 문구는 **JS 화이트리스트**의 `브리지에 없는 함수명이다:`이고 Kotlin `BridgeDispatcher`의 `지원하지 않는 함수명이다:`가 **아니다.** JS가 먼저 막으므로 Kotlin 분기는 실기기에서 도달하지 않으며 **단위 테스트로만 덮인다**(설계상 의도된 이중 방어). review.md §9-3의 인계를 실측으로 재확인했다.

**REQ-007 / REQ-008**
| AC | 판정 | 관측값 |
|:--|:--|:--|
| AC-007-1 | PASS | 강제 종료 후 콜드 스타트(`TotalTime: 316ms`) → 3초 내 버튼 3개 + `NAVER.COM 이동` 표시 |
| AC-007-2 | PASS | `about:neterror`·"파일을 찾을 수 없음" 없음. 정상 콘텐츠 |
| AC-007-3 | PASS | `브리지 상태:` 값이 정적 초기값 `(JS 미실행)`이 아니라 **`READY`** — JS가 실제로 실행돼 채웠다 |
| AC-008-1 | PASS | 스크린샷에 네이버 콘텐츠 렌더(검색창·메뉴·뉴스 카드·광고 이미지) |
| AC-008-2 | PASS | `GeckoView:PageStart uri=http://naver.com/` → `GeckoView:LocationChange uri=https://m.naver.com/`. **naver.com 도메인**(https 리다이렉트는 실패가 아니다 — §2.8) |
| AC-008-3 | PASS | 해당 구간 logcat `NS_ERROR_`·`about:neterror` **0건** |

**REQ-009**
| AC | 판정 | 관측값 |
|:--|:--|:--|
| AC-009-1 | PASS | 탭과 덤프를 **기기 안에서 연속 실행**(adb 왕복 지연 제거). 콜드 캐시 로드에서 연속 샘플 **1·2·3에 `불러오는 중…` 표시**, 샘플 4에서 사라짐 |
| AC-009-2 | PASS | 로드 완료 상태에서 `불러오는 중` **0건** |
| AC-009-3 | PASS | **같은 덤프에서 동시에** 로딩 문구 0건 AND 네이버 콘텐츠 렌더 AND 배지 존재 |
| AC-009-4 | **PASS (관측 범위 명시)** | 아래 |

**AC-009-4 상세 — 두 반쪽을 구분해 적는다 (V1·V5)**

- **"반드시 사라진다"(무한 로딩 금지) — 직접 관측, PASS.** wifi/데이터를 끈 상태(`Active default network: none`)에서 naver 탭 → 직후 연속 8샘플 + 5초 후 1샘플 + 재시도 5회×6샘플 = **총 39개 샘플 전부 로딩 문구 0건.** 로딩이 걸려 남아 있는 상태는 없다.
- **"로딩 UI가 뜬 뒤" — 실패 경로에서는 직접 포착하지 못했다. 5회 중 0회.**
  - 원인을 추정이 아니라 **측정**했다: `GeckoView:PageStart 09:03:26.996` → `GeckoView:PageStop uri=null 09:03:27.217` = **221 ms**. `uiautomator dump` 1회가 약 700 ms 이상이라 원리적으로 이 창을 잡을 수 없다.
  - **이것은 앱 결함 신호가 아니라 계측 한계다**(V3). 네트워크가 아예 없어 즉시 실패하므로 창이 좁다.
  - 성공 경로에서는 로딩 UI 등장이 **직접 관측**됐다(AC-009-1). 실패 경로에서 등장 자체가 없다는 증거는 없다.
  - 보조 근거(대리 신호이므로 판정 근거가 아니라 정황으로만 적는다): `onPageStop`이 실제로 발생했고, `MainViewModelTest`의 `onPageStop은 실패했을 때도 로딩을 끈다`가 `success=false` → `isLoading=false` 전이를 고정하며, code-reviewer가 D-07로 `render()`가 `ProgressBar` 가시성의 유일한 대입 지점임을 확인했다.

**REQ-010 — 이번 검증에서 가장 강한 증거를 얻은 영역**

`remoteDebuggingEnabled(BuildConfig.DEBUG)`가 켜져 있어(`App.kt:53`) 기기의 `@com.example.geckoviewtest/firefox-debugger-socket`에 접속해 **naver.com 페이지의 콘텐츠 스코프에서 직접 JS를 평가**했다. **프로덕션 코드를 고치지 않고** 페이지 세계를 관측한 경로다.

| AC | 판정 | 관측값 |
|:--|:--|:--|
| AC-010-1 | PASS | 배지 `versionName = 1.0.0 [PAGE_WORLD]` — 스크린샷·접근성 덤프·DOM 조회 3중 확인 |
| AC-010-2 | PASS | `1.0.0` = `dumpsys package … versionName=1.0.0` **문자 단위 일치** |
| AC-010-3 | PASS | **`[PAGE_WORLD]` 마커 존재.** 추가로 페이지 스코프에서 `window.__bridgeProbeRanInPageWorld` → **`true`**, `typeof window.NativeBridge` → **`object`**, `typeof window.NativeBridge.call` → **`function`**. 격리 세계 전용 동작이 아님이 직접 확인됐다 |
| AC-010-4 | **재수행 안 함** | §6.3 |
| AC-010-5 | PASS | naver.com 페이지 세계에서 `window.NativeBridge.call('appFinish')` → **`Hist #0` ActivityRecord 1건 → 0건(소멸)**, 이후 3초 추가 수집에도 치명 오류 **0건**. **사용자 결정 A-08이 실현됐다는 증거** |
| AC-010-6 | PASS | 배지가 떠 있는 **같은 화면**에서 네이버 콘텐츠가 정상 렌더(AC-008-1 유지) |

> **[신규 관측 — 인계값이 아니다]** 페이지 세계에서 **살아 있는 5단 왕복**을 직접 태웠다:
> `window.NativeBridge.call('getVersionName')` → **`OK:"1.0.0"`** (Promise resolve).
> 이것은 배지에 남은 과거 결과를 읽은 것이 아니라 **새로 발행한 요청의 응답**이다.
> 같은 경로에서 미지원 함수명은 **`REJ:Error: 브리지에 없는 함수명이다: …`** 로 reject됐다.
> 즉 외부 사이트 경로의 성공·실패 두 갈래가 모두 실측됐다.

> **트랩 #6 확인**: 프로브 배지는 고정 검정 배경 + 초록 글씨로 페이지 최상단을 덮는다. 스크린샷에서 이것은 **콘텐츠 렌더 실패가 아니다.**

**REQ-011**
| AC | 판정 | 관측값 |
|:--|:--|:--|
| AC-011-1 | PASS | naver → `KEYCODE_BACK` → `index.html` 재렌더(제목·버튼 3개·`NAVER.COM 이동` 전부). 빈 화면·오류 페이지 아님 |
| AC-011-2 | PASS | 복귀 **후** `getVersionName` 재호출 → `versionName = 1.0.0`, `dumpsys`와 일치(AC-004-2 재충족). 세션 히스토리 복원 후에도 브리지 생존 |
| AC-011-3 | PASS | **(a) API 31+ 갈래**: `index.html`에서 뒤로가기 → ActivityRecord **존재하되** `state=STOPPED stopped=true **finishing=false**`, 태스크 `visible=false visibleRequested=false`, 프로세스 4개 생존 = **태스크 백그라운드 이동**. **(b)** 치명 오류 0건. **(c) 대조**: `appFinish` 경로에서는 같은 레코드가 **완전히 소멸**(AC-005-1) — 두 결과가 구분되므로 `finishing=false`가 "애초에 실행되지 않음"이 아님이 증명된다. **(d)** `ro.build.version.sdk = 33` 기록 |
| AC-011-4 | PASS | **두 조작의 결과가 서로 다르다**(rev.3 판정 기준): naver 뒤로가기 = `index.html` 복귀 + **`state=RESUMED`** + 태스크 `visible=true`(포그라운드 유지) / index 뒤로가기 = 포그라운드 이탈 + **`state=STOPPED finishing=false`**. 두 결과가 같아지지 않았으므로 `canGoBack` 배선이 살아 있다 |

> **API 30 이하 갈래는 검증하지 못했다** — 해당 실기기가 없다. `minSdk = 26`이라 지원 범위 안에 있는 동작이며, **rev.3 (a)의 두 갈래 중 하나는 미실측**임을 명시한다.

### 6.3 재수행하지 않은 수용 기준 3건 — 사유와 대체 근거

세 건 모두 **프로덕션 파일을 일시 수정하고 재빌드해야만** 관측 가능하다. 이번 단계의 지시가 *"프로덕션 코드를 수정하지 마라"* 이므로 수행하지 않았다. **"통과했다"고 적지 않는다.**

| AC | 무엇이 필요한가 | 재수행 안 한 사유 | 대체 근거 (판정 대용이 아님) |
|:--|:--|:--|:--|
| **AC-003-2** (역주입 실패 테스트) | `background.js`의 `sendNativeMessage` 한 줄 주석 처리 → 재빌드 → 화면 미표시/타임아웃 확인 → 원복 | 프로덕션 자산 수정 + 재빌드 필요 | developer 수행, code-reviewer가 원복을 내용 대조로 확인(review.md §6). **이번에 내가 수행한 17건의 RED 실험이 같은 성격의 실패 주입**이며, 그중 W-7은 `sendNativeMessage` 호출 형태가 바뀌면 RED가 됨을 실증했다 |
| **AC-004-3** (하드코딩 판별) | `versionName`을 다른 값으로 바꿔 재빌드 → 표시 값도 바뀌는지 | 빌드 스크립트 수정 + 재빌드/재설치 필요 | developer가 `7.7.7-probe`로 수행(review.md §6). `AppInfoRepositoryTest`의 `값을 물어볼 때마다 provider를 다시 호출한다`가 캐싱을 배제하고, `App.kt:113-129`의 `PackageManager` 조회가 유일한 값 출처다 |
| **AC-010-4** (제2 외부 사이트) | naver.com이 아닌 외부 사이트 로드 | **앱에 URL 입력 UI가 없고 `AndroidManifest`에 `VIEW` 인텐트 필터가 없다**(MAIN/LAUNCHER만). 네이버 페이지의 외부 링크를 탭해도 `onNewSession`(§6 스코프 아웃)이라 이동하지 않는 것을 **실측 확인**했다. 남은 방법은 `NAVER_URL` 상수 일시 변경뿐 | developer가 example.com으로 수행(review.md §6). **더 나은 대체**: 이번에 추가한 **M-5가 `content_scripts.matches == ["http://*/*","https://*/*"]`를 매 빌드 자동 검증**한다 — AC-010-4가 잡으려던 위험(“matches가 naver 도메인에 하드코딩된 구현”)을 **상시 자동으로** 차단한다 |

---

## 7. 자동화한 것 / 여전히 수동인 것

| 영역 | 자동 | 수동·계측 | 근거 |
|:--|:--:|:--:|:--|
| 브리지 wire 스키마(직렬화·파싱·오류 코드) | **O** | | `BridgeProtocolTest` 11건 |
| 함수 디스패치·미지원 함수명·예외/취소 전파 | **O** | | `BridgeDispatcherTest` 7건 |
| **코틀린↔JS 계약 일치(함수명·식별자·오류코드·전송 형태)** | **O (신규)** | | `BridgeWireContractTest` 7건 |
| **manifest.json 구조 불변식(페이지 세계 주입 전제 포함)** | **O (신규)** | | `ExtensionManifestTest` 8건 |
| 화면 상태 전이·일회성 이벤트 | **O** | | `MainViewModelTest` 12건 |
| 버전 폴백 | **O** | | `AppInfoRepositoryTest` 3건 |
| GeckoView 실제 렌더·Gecko 프로세스 기동 | | **수동** | 실기기 외 방법 없음 |
| 페이지 세계 주입·5단 왕복 실제 동작 | | **수동** | 이번엔 원격 디버깅으로 **정밀도를 올렸다**(§6.2 REQ-010) |
| 뒤로가기 플랫폼 기본 동작(API 분기) | | **수동** | `dumpsys` 관측 |
| 로딩 UI 가시성 | | **수동** | 접근성 덤프 |
| `NativeBridgeHandler`의 `catch` 순서 | | **불가** | §4.2 — JVM 호출 자체가 불가, 계측으로도 취소 경로를 태우기 어렵다 |
| `background.js`·`content.js`·`page-bridge.js`의 **런타임 동작** | | **수동** | JS 실행 환경이 없다(§7.1) |

### 7.1 JS 자산을 더 자동화하지 못한 이유

REQ-010 산출물은 전량 순수 JS다. 이번에 **정적 계약 대조는 자동화했지만**(W-1~W-7, M-1~M-8) **런타임 동작은 자동화하지 않았다.** 무리해서 만들지 않은 이유:

- JS를 실행하려면 Node/Jest 등 **JS 툴체인과 `browser.*` API 목(mock)** 이 필요하다. 이는 plan.md §6.1의 승인된 의존성 11건(L-01~L-11) **밖의 새 의존성**이며 architecture.md가 evaluator 승인을 요구한다. **V9(범위 확대 금지)에 걸린다.**
- 목을 씌운 JS 테스트는 `browser.runtime.sendNativeMessage`의 **실제 GeckoView 구현**을 대체하지 못한다. mozilla/geckoview#220이 정확히 "같은 코드가 background에서는 되고 content script에서는 안 되는" 결함이라는 점에서, 목 기반 초록은 이 영역에서 **특히 신뢰도가 낮다.**
- 대신 **깨졌을 때 조용히 실패하는 지점**(함수명 집합·`nativeApp` 문자열·`web_accessible_resources`·`matches`·`all_frames`·MV 버전)을 전부 정적 테스트로 덮었다. 이것이 자동화로 실제 가치를 낼 수 있는 경계였다.

### 7.2 계측 테스트(`androidTest`)를 추가하지 않은 이유 — V4·V9에 따른 판단

**추가하지 않았다.** `app/src/androidTest/`는 여전히 없다.

1. **승인되지 않은 의존성이 필요하다.** `app/build.gradle.kts`에 `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"`가 선언돼 있지만 **`androidTestImplementation` 의존성이 하나도 없다.** 러너를 붙이려면 `androidx.test:runner`·`androidx.test.ext:junit`을 `libs.versions.toml`과 `app/build.gradle.kts`에 추가해야 하는데, plan.md §6.1의 승인 목록(L-01~L-11)에 **없다.** architecture.md는 *"새 라이브러리 추가는 plan.md에 명시되고 Evaluator 승인을 받은 경우만 허용"*이라고 못 박는다 → **evaluator 재승인 없이는 넣을 수 없다(V9).**
2. **V4를 지킬 수 없다.** 환경 한정 테스트는 어노테이션 필터 + `Assume` 가드를 **둘 다** 요구하고, 필터가 실제로 작동하는지 **케이스 수 차이로 확인**해야 한다. 이 프로젝트에는 CI도 에뮬레이터 경로도 없고(GeckoView는 x86 미동봉이라 32비트 에뮬레이터 자체가 불가), **필터의 실효를 증명할 대조군이 없다.** 증명하지 못하는 표식을 붙이면 그것이 바로 V4가 경고하는 거짓 그린이다.
3. **이번 배치에서 계측이 벌 수 있는 것이 적다.** 검증 대상 대부분이 GeckoView 렌더·페이지 세계인데, 이번에 **원격 디버깅으로 페이지 세계 JS를 직접 평가**해 계측 테스트보다 오히려 정밀한 관측을 얻었다(§6.2).

**다음 배치로 넘기는 실행 가능한 과제는 §9-4에 적었다.**

### 7.3 **[신규 발견] 자산만 바꾸면 단위 테스트가 UP-TO-DATE로 건너뛴다** — 내 테스트 자체의 거짓 그린 위험

새로 만든 15건(W-*, M-*)은 `src/main/assets/messaging/**`를 **직접 파일로 읽는다.** 그런데 이 경로는 `:app:testDebugUnitTest`의 **선언된 입력이 아니다.** 실측:

```
./gradlew :app:testDebugUnitTest            → 실행됨, BUILD SUCCESSFUL
(background.js만 수정)
./gradlew :app:testDebugUnitTest            → Task :app:testDebugUnitTest UP-TO-DATE   ← 검증이 통째로 건너뛰어진다
```

- **영향 범위**: 로컬 증분 빌드에 한정된다. 클린 체크아웃·CI·`--rerun-tasks`에서는 항상 실행된다. 이 리포트의 모든 수치는 `--rerun-tasks`로 측정했고, **17건의 RED 실험도 전부 `--rerun`으로 강제 실행**했다(안 했으면 17건 전부 거짓 기록이 됐다).
- **고치지 않은 이유**: 해결책이 `app/build.gradle.kts`의 테스트 태스크에 `inputs.dir("src/main/assets/messaging")`를 **한 줄 추가**하는 것인데, 이는 빌드 스크립트 수정이라 이번 단계의 금지 범위다. **§9-4에 실행 가능한 형태로 넘긴다.**

---

## 8. 발견했으나 손대지 않은 것 (V9)

| # | 내용 | 처리 |
|:--|:--|:--|
| 1 | **§7.3의 UP-TO-DATE 건너뜀** | 빌드 스크립트 수정이 필요 → developer 후속 과제(§9-4). 이번 리포트의 측정은 전부 강제 실행으로 회피했다 |
| 2 | MINOR-11 — `MainActivity.kt:58-59`·`AppNavigationDelegate.kt:25`의 *"문언 정정 대기 중"* 표현이 낡음(rev.3에서 정정 완료) | **프로덕션 주석이라 손대지 않았다.** developer 몫 |
| 3 | 접근성 트리가 GeckoView를 `android.webkit.WebView`로 보고 | **결함 아님.** 오독 방지용으로 §6.2에 기록만 했다 |
| 4 | 네이버 외부 링크 탭이 아무 동작도 하지 않음(`onNewSession` 미구현) | requirements §6 **스코프 아웃 명시 항목.** 결함으로 올리지 않는다. 다만 AC-010-4 재수행을 막는 실제 원인이므로 §6.3에 기록 |
| 5 | `GeckoConsole: Permission error: No listener for GeckoView:ContentPermission` | `PermissionDelegate` 미구현. requirements §6 스코프 아웃 → **결함 아님, 후속 과제 유지** |
| 6 | MINOR-1(`ensureBuiltIn` 실패 시 미처리 코루틴 예외), MINOR-8(프로브 배지 always-on) | **이월 2건. 이번 배치 결함으로 올리지 않는다**(V9) |

**프로덕션 결함은 발견되지 않았다.** 위 6건 중 결함은 0건이며, 1번은 내가 추가한 테스트의 실행 보장에 관한 빌드 설정 사항이다.

---

## 9. coverage-reporter(7단계) 인계 사항

1. **새 커버리지 기준선**: **LINE 98/98 = 100.0%, BRANCH 18/18 = 100.0%, CLASS 14/14 = 100.0%, 테스트 48건 / 실패 0 / skip 0.** `clean` + `--rerun-tasks`로 측정한 값이다. 이전 기준선(BRANCH 16/18 = 88.9%)에서 **BRANCH가 개선**됐고 LINE·CLASS는 동일하다. **회귀 0건.**
2. **LINE이 안 오른 것을 테스트 부실로 판정하지 말 것.** 추가한 17건 중 **15건이 JS/JSON 자산을 대조하는 테스트**라 JaCoCo 분모에 원리적으로 기여하지 않는다(V8). 분모(98 라인)는 그대로이고 분자도 그대로다 — **정상이다.**
3. **`coverageExclusions`는 손대지 않았다.** `app/build.gradle.kts`는 한 글자도 수정하지 않았으므로 제외 범위 확대가 없다. diff로 확인 가능하다.
4. **[실행 가능한 후속 과제 — developer]** `app/build.gradle.kts`의 `testDebugUnitTest`에 자산 디렉터리를 입력으로 선언할 것:
   ```kotlin
   tasks.named<Test>("testDebugUnitTest") {
       // W-*/M-* 테스트가 이 디렉터리를 직접 읽는다. 입력으로 선언하지 않으면
       // 자산만 고쳤을 때 태스크가 UP-TO-DATE로 건너뛰어 검증이 조용히 사라진다.
       inputs.dir("src/main/assets/messaging").withPathSensitivity(PathSensitivity.RELATIVE)
   }
   ```
   **무엇이 깨지는가**: 넣지 않으면 로컬 증분 빌드에서 계약 위반이 감지되지 않는다. **이번에 못 한 이유**: 빌드 스크립트 수정이 6단계 금지 범위다.
5. **[실행 가능한 후속 과제 — 계측 테스트 도입 시]** `androidTest`를 넣으려면 **먼저 evaluator 승인**이 필요하다(`androidx.test:runner`, `androidx.test.ext:junit` — plan.md §6.1 승인 목록 밖). 승인 후에는 V4대로 **어노테이션 필터 + `Assume` 가드를 둘 다** 넣고, **필터 유무에 따른 케이스 수 차이로 필터가 실제로 작동함을 증명**해야 한다. 증명 수단이 없으면 표식을 붙이지 말 것.
6. **[유지] 미실측 갈래 1건**: AC-011-3의 **API 30 이하 동작**(Activity 종료)은 해당 기기가 없어 검증하지 못했다. `minSdk = 26`이라 지원 범위 안이다.
7. **기기 반납 상태**: `R3CN60L0QMT`(API 33)에 APK 설치 유지, **`index.html`을 띄운 채 포그라운드**, `airplane_mode_on = 0`, wifi 켜짐, 기본 네트워크 활성. `adb forward`는 **전부 해제**했다.
8. **프로덕션 무결성**: RED 실험으로 건드린 4개 파일 전부 **sha256이 실험 전과 동일**하다. 임시 코드·백업 파일 잔존 **0건**. **`background.js`는 mtime만 갱신됐고 내용은 동일하다** — 트랩 #10이 이번에도 재현됐으니 **mtime으로 변경 파일을 세지 말 것**(§10).

---

## 10. 최종 판정: **PASS**

- **프로덕션 결함 0건.** REQ-001~011 전부 rev.3 기준 충족.
- **테스트 결함 0건.** 48건 전건 통과, skip 0, `@Ignore` 0건(V6 — 알려진 결함을 초록으로 덮은 곳이 없다).
- 미충족으로 남은 것은 **재수행하지 않은 수용 기준 3건**(AC-003-2·AC-004-3·AC-010-4)과 **미실측 갈래 1건**(AC-011-3의 API 30 이하)이며, **전부 "통과"가 아니라 "이번 단계가 관측하지 않았다"로 기록**했다(§6.3, §9-6). 셋 다 앞 단계에서 수행됐고 출처를 명시했다.

### 변경 파일 (내용 기준)

```
app/src/test/java/com/example/geckoviewtest/bridge/BridgeWireContractTest.kt   (신규)
app/src/test/java/com/example/geckoviewtest/bridge/ExtensionManifestTest.kt    (신규)
app/src/test/java/com/example/geckoviewtest/bridge/BridgeProtocolTest.kt       (케이스 1건 추가)
app/src/test/java/com/example/geckoviewtest/bridge/BridgeDispatcherTest.kt     (케이스 1건 추가 + MINOR-10 KDoc 정정)
```

전부 `impact-report.json`의 `allowed_globs`(`app/src/test/**`) 안이다. **화이트리스트 위반 0건.**
`app/src/main/assets/messaging/background.js`는 mtime만 바뀌었고 **내용은 동일**하다(§9-8).

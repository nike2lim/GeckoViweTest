# 요구사항 분석: GeckoView 기반 웹뷰 앱 (신규 프로젝트)

- 분석 일시: 2026-08-04 13:25:24 (KST)
- **개정 일시: 2026-08-04 13:55:06 (rev.2) / 2026-08-05 13:38:42 (KST) — rev.3**
- 입력 소스: `/Users/appdevloperteam/Documents/ClaudeWorkspace/GeckoViewTest/initRequire.md` (전문 9줄) + 오케스트레이터 전달 프롬프트 + **사용자 확인 답변 4건(rev.2)**
- 대상 프로젝트: `/Users/appdevloperteam/Documents/ClaudeWorkspace/GeckoViewTest`
- **프로젝트 상태: 빈 디렉터리.** `.claude/`, `.gitignore`, `initRequire.md`, `pipeline/`, `doc/`만 존재. Android 프로젝트 자체가 없으므로 이번 작업은 **신규 프로젝트 스캐폴딩을 포함**한다.

---

## 개정 이력

### rev.3 — 2026-08-05 13:38:42 (구현 후 실측으로 **수용 기준 문언 정정**)

**성격: 요구사항이 바뀐 것이 아니다.** REQ 자체는 rev.2와 동일하며, **수용 기준 2건의 문언이 실제 플랫폼·빌드 현실을 반영하지 못한 것**을 구현·검증 후에 바로잡은 것이다. 두 건 모두 5단계 code-reviewer가 **"수용"(구현 변경 불요)** 으로 판정했고, **설계·의존성 변경이 없으므로 evaluator 재승인은 불요**하다(review.md §1, §8).

| 대상 | rev.2 문언 | rev.3 문언 | 정정 사유 |
|:--|:--|:--|:--|
| **AC-011-3** | "뒤로가기를 누르면 **Activity가 종료된다**" | "콜백이 비활성이 되어 **플랫폼 기본 동작이 수행된다** — API 31+ 는 태스크 백그라운드 이동(`finishing=false` + 태스크 `visible=false`), API 30 이하는 Activity 종료" + **`appFinish` 대조 조건** + **기기 API 기록 의무** | API 31부터 루트 런처 Activity의 뒤로가기 기본 동작이 `finish()`에서 태스크 백그라운드 이동으로 바뀌었다 **[문서 — Android 12 Behavior changes: all apps]**. rev.2 문언은 이 변경을 반영하지 못했다. **구현 결함이 아니다** |
| **AC-002-2** | "`androidx.compose.*` 아티팩트가 **0개**" + 판정 명령 `:app:dependencies` | "**Compose UI 툴킷**(ui/foundation/material/runtime 본체)이 0개" + **애너테이션 전용 아티팩트는 예외** + 판정 명령을 **`dexdump`** 로 교체 | 애너테이션 전용 아티팩트가 androidx 전이 의존(`activity-ktx` → `navigationevent` → `compose.runtime:runtime-annotation`)으로 들어오는 현실을 rev.2 문언이 반영하지 못했다. **REQ-002의 의도(Compose를 UI로 쓰지 않는다)는 완전히 충족** |

**함께 손본 직결 서술 3건**
1. **REQ-011 본문** — "뒤로가기는 통상대로 **앱을 종료한다**" → "통상대로(=**플랫폼 기본 동작으로**) 앱에서 벗어난다". 사용자 요구인 "통상대로"는 그대로 두고, API 33에서 "통상"이 곧 태스크 백그라운드 이동이라는 사실과 합치시켰다. **요구 범위는 바뀌지 않는다.**
2. **AC-011-4** — 판정 근거를 "종료 여부"에서 **"두 조작의 결과가 서로 다르다"**(naver 뒤로가기 = 복귀 + `RESUMED` / index 뒤로가기 = 포그라운드 이탈)로 바꿨다. AC-011-3이 더 이상 "종료"를 단정하지 않으므로 그것에 기대던 판정 문장을 함께 고쳤다.
3. **REQ-011의 [미확인] 항목 해소** — rev.2가 "최우선 실기기 확인"으로 올렸던 *"확장 페이지가 GeckoSession 히스토리에 남는가"* 는 **해소됐다**(남는다. AC-011-1·2 성립, §2.7.4 폴백 미발동). 해소된 항목을 [미확인]으로 남겨두면 후속 단계가 존재하지 않는 리스크를 계속 안고 간다.

**출처 구분 (V7)**
- **planner-analyzer 직접 재측정 [확인]**: `dexdump`(build-tools 36.0.0)로 `app-debug.apk`(198,619,378 B)의 dex 7개 전수 조사 → `androidx/compose` 정의 클래스 **정확히 6개**, 전부 애너테이션 + R, `androidx/compose/(ui|foundation|material)/` **0건**. `app/build.gradle.kts`의 `minSdk = 26` / `targetSdk = 36` / `buildFeatures.compose` 미설정. 기기 `ro.build.version.sdk = 33`.
- **문서로 확인 [문서]**: Android 12(API 31) 뒤로가기 동작 변경 — 적용 대상이 `ACTION_MAIN`+`CATEGORY_LAUNCHER` 루트 Activity이고, **targetSdk가 아니라 기기 OS 버전으로 적용**된다는 점을 공식 문서에서 확인했다. 이 사실이 AC-011-3 (d)의 "기기 API를 기록하라"는 요구의 근거다.
- **재현하지 않고 인용 — 출처 명시**: 실기기 조작 관측값(실험 A `state=STOPPED finishing=false` / 실험 B `state=RESUMED` / 실험 C ActivityRecord 소멸)은 **code-reviewer 실측(review.md §1①, SM-G981N / API 33)** 이다. planner-analyzer는 기기를 조작하지 않았다. 리뷰어가 반납한 기기 상태를 보존하기 위함이며, 재현이 필요하면 QA(6단계)가 수행한다.

### rev.2 — 2026-08-04 13:55:06 (사용자 확인 답변 반영)

**impact-analyzer 주의: rev.1과 rev.2는 브리지 노출 범위가 정반대다.** rev.1 기준으로 만든 산출물이 있으면 폐기할 것.

| 항목 | rev.1 (초판) | rev.2 (현재) | 사유 |
|:--|:--|:--|:--|
| **REQ-010** | 브리지를 신뢰 오리진에만 노출, 외부 사이트 **차단** (P2) | **반전.** 외부 웹사이트에서도 브리지 함수가 **동작한다** (P1) | **사용자가 A-08에서 "외부 사이트에서도 호출 가능"을 명시적으로 선택.** planner-analyzer의 권고와 반대 방향이며, 그 트레이드오프는 §5.1에 명문화 |
| **REQ-011** | 없음 (뒤로가기는 §6 스코프 아웃) | **신규 추가** (P1) — naver 이동 후 index.html 복귀 | 사용자가 A-10에서 스코프 아웃 해제 |
| REQ-006 | 브리지 함수 API 계약 | 유지 + **"확장 페이지와 외부 사이트 양쪽에서 동일 계약"** 을 명시 | A-08 반전으로 클라이언트가 2종이 됨 |
| A-02/A-03 | 제안값 (확인 권장) | **확정** — 패키지 `com.example.geckoviewtest`, 앱명 `GeckoViewTest`, minSdk 26, target/compileSdk 36 | 사용자 승인 |
| A-05 | 확인 필요 | **확정** — L6은 L3의 상위 표현. 별도 JS 인터페이스를 두 벌 만들지 않음 | 사용자가 제안 채택 |
| A-08 | 차단 가정 | **반전 확정** | 위와 동일 |
| A-10 | 스코프 아웃 | **해제** → REQ-011 | 위와 동일 |
| §2.7 index.html 오리진 | 확장 페이지 권고 | **재판정 후 유지** (§2.7.3에 재판정 근거 신설) | A-08 반전으로 content script가 필수 경로가 되어 재검토가 필요했음 |
| §2.6 | — | **§2.6.1 신설**: 페이지 세계 ↔ content script 격리(Xray vision) 제약. **rev.1에 없던 신규 제약이며 REQ-006/010의 구현 방식을 결정한다** | A-08 반전으로 처음 관련성이 생김 |
| §2.10 | — | **신설**: mozilla/geckoview#220 정밀 재조사 결과 (rev.1의 경고 범위를 **축소 정정**) | 아래 참조 |

**rev.2에서 정정된 rev.1의 서술 (2건)**

1. **#220의 적용 범위 — rev.1이 과잉 경고했다.** rev.1은 "content script를 네이티브 통신 1차 경로로 쓰지 말 것"이라 적었으나, 이슈 본문을 직접 확인한 결과 깨지는 것은 **content script가 `browser.runtime.sendNativeMessage`를 직접 호출하는 경우**에 한정된다(§2.10). 본 설계는 content script → `runtime.sendMessage` → **background.js** → `sendNativeMessage` 경로이므로 **#220의 결함 경로를 지나지 않는다.** 요구사항 원문이 "background.js를 사용한 통신"을 명시한 덕분에 우연히 회피된다. 다만 content script 경로 전체가 미검증인 것은 사실이므로 **최우선 검증 대상**이라는 결론 자체는 유지한다.
2. **`nativeMessagingFromContent` 권한은 불필요하다.** rev.1은 세 권한을 나란히 나열했으나, 이 권한은 content script가 네이티브 API를 **직접** 호출할 때만 필요하다. 본 설계에서는 선언하지 않는다 — 불필요한 권한을 넣으면 실제 통신 경로를 오독하게 만든다.

### rev.1 — 2026-08-04 13:25:24 (초판)
원문 9줄 분해, REQ-001~010 정의, GeckoView 아티팩트 실측 조사, XML vs architecture.md 충돌 정리.

---

## 0. 표기 규약

이 문서의 모든 사실은 아래 세 등급 중 하나로 표시한다. **[확인]이 아닌 것을 구현 근거로 쓰지 말 것** (verification-honesty V7).

| 표기 | 의미 |
|:--|:--|
| **[확인]** | 이 세션에서 직접 측정·조회해 검증함. 측정 방법을 병기. |
| **[문서]** | Mozilla 공식 문서/Javadoc/MDN에 명시됨. 실기기 재현은 안 함. |
| **[미확인]** | 추정 또는 2차 출처. 구현 전 반드시 검증 필요. |

---

## 1. 기능 요구사항

우선순위는 원문에 명시가 없다. 아래 원칙으로 부여했다(§5 가정 A-01 참조):
- **P1** = 이 항목이 빠지면 원문 문장 하나 또는 사용자 명시 결정이 통째로 미충족
- **P2** = 원문 문장의 부분 충족 또는 품질 요건

| ID | 요구사항 | 유형 | 우선순위 | 출처 근거 |
|:--|:--|:--|:--|:--|
| REQ-001 | 앱 실행 시 GeckoView(Gecko 엔진)로 웹 콘텐츠가 화면에 렌더링된다. WebView(`android.webkit`)를 사용하지 않는다. | 신규 | P1 | initRequire.md L1 |
| REQ-002 | 화면 UI가 XML 레이아웃으로 구성된다. Jetpack Compose를 사용하지 않는다. | 신규 | P1 | L2 |
| REQ-003 | 앱에 내장된 WebExtension의 `background.js`와 Android 네이티브 코드 사이에 메시지 채널이 수립되고, 그 채널을 통해 실제 데이터가 왕복한다. | 신규 | P1 | L3 |
| REQ-004 | 웹 페이지가 `getVersionName`을 호출하면 `PackageManager`로 조회한 앱 버전 문자열이 페이지로 반환되어 화면에 표시된다. | 신규 | P1 | L4 |
| REQ-005 | 웹 페이지가 `appFinish`를 호출하면 현재 Activity가 정상 종료된다. | 신규 | P1 | L5 |
| REQ-006 | 웹 페이지 JS에서 호출할 수 있는 브리지 함수 API가 제공된다 — 함수명·인자·반환(Promise) 계약이 정의되어 있고, **내장 `index.html`과 외부 사이트가 동일한 계약을 사용**하며, 새 네이티브 기능을 추가할 때 페이지 쪽 코드 형태가 바뀌지 않는다. | 신규 | P1 | L6 (해석 확정 — §5 A-05) |
| REQ-007 | 앱 실행 직후 별도 조작 없이 앱에 내장된 `index.html`이 로드되고, REQ-004·REQ-005를 각각 실행하는 UI와 결과 표시 영역이 보인다. | 신규 | P1 | L7 |
| REQ-008 | 화면에 버튼이 있고, 누르면 `http://naver.com`으로 이동해 네이버 콘텐츠가 렌더링된다. | 신규 | P1 | L8 |
| REQ-009 | 페이지 로드가 시작되면 로딩 UI가 화면에 나타나고, 로드가 끝나면(성공/실패 모두) 사라진다. | 신규 | P1 | L9 |
| **REQ-010** | **외부 웹사이트(naver.com 등 앱이 소유하지 않은 페이지)의 페이지 세계 JS에서도 브리지 함수가 동작한다.** | 신규 | **P1** | **원문에 없음 — 사용자 A-08 결정 (rev.2에서 rev.1 대비 반전)** |
| **REQ-011** | **naver.com으로 이동한 뒤 뒤로가기로 `index.html`에 복귀할 수 있고, 복귀 후 브리지가 다시 동작한다. 웹 히스토리가 없으면 뒤로가기는 통상대로(=플랫폼 기본 동작으로) 앱에서 벗어난다.** | 신규 | **P1** | **원문에 없음 — 사용자 A-10 결정 (rev.2 신규, rev.3에서 "앱을 종료한다" → "플랫폼 기본 동작으로 앱에서 벗어난다"로 문언 정정)** |

### 1.1 수용 기준 (Acceptance Criteria)

**verification-honesty V1 준수 원칙:** "델리게이트가 호출됨", "확장이 설치됨", "onPageStart가 왔음", "`goBack()`이 호출됨"은 **대리 신호이므로 수용 기준이 될 수 없다.** 아래 기준은 모두 (a) 사람이 화면에서 보거나 adb로 외부에서 관측 가능하고, (b) AND로 묶인 각 조건이 독립적으로 깨질 수 있도록 작성했다.

---

**REQ-001 — GeckoView 렌더링**
- AC-001-1: 앱 실행 후 스크린샷에 웹 콘텐츠(텍스트/버튼)가 실제로 그려져 있다. 흰 화면·검은 화면이 아니다.
- AC-001-2: `adb shell ps -A | grep com.example.geckoviewtest`에 `:tab_...` 또는 `:gpu_...` 접미사가 붙은 Gecko 자식 프로세스가 **1개 이상** 존재한다. (Gecko 멀티프로세스가 실제로 떴다는 뜻 — WebView로는 절대 나올 수 없는 신호)
- AC-001-3: 앱 소스 전체 grep 결과 `android.webkit.WebView` 사용처가 0건이다.
- AC-001-1과 AC-001-2는 독립적으로 깨진다: 런타임이 떠도 렌더가 안 될 수 있고(1 실패), 캐싱된 화면만 있고 프로세스가 죽었을 수 있다(2 실패).

**REQ-002 — XML View**
- AC-002-1: `app/src/main/res/layout/` 아래 화면 레이아웃 XML이 존재하고, Activity가 그 레이아웃을 실제로 사용한다(ViewBinding 또는 `setContentView`).
- AC-002-2 **(rev.3 정정 — 구현 후 실측 반영)**: **Compose UI 툴킷**이 앱에 들어오지 않는다. 세 조건의 AND로 판정한다.
  - (a) APK의 모든 dex에 `androidx/compose/ui/`·`foundation/`·`material/` 및 `compose.runtime` **본체** 클래스가 **0개**다.
  - (b) `buildFeatures.compose`가 설정되지 않았다.
  - (c) Compose 컴파일러 플러그인이 적용되지 않았다.
- **예외 — androidx 전이 의존으로 들어오는 애너테이션 전용 아티팩트(`androidx.compose.runtime:runtime-annotation`)는 위반이 아니다.** 유입 경로는 `androidx.activity:activity-ktx:1.13.0` → `androidx.navigationevent:1.0.0` → `androidx.compose.runtime:runtime-annotation:1.9.0`이며, 승인된 의존성(L-03)을 바꾸지 않고는 제거할 수 없다.
- **판정 명령 (QA는 이것을 쓸 것):**
  ```bash
  # APK를 풀고 각 dex를 전수 조사
  for f in classes*.dex; do dexdump "$f" | grep "Class descriptor" | grep androidx/compose; done
  ```
  결과가 **애너테이션 타입 + 리소스 R 6개**(`runtime/Immutable`, `runtime/Stable`, `runtime/StableMarker`, `runtime/annotation/FrequentlyChangingValue`, `runtime/annotation/RememberInComposition`, `runtime/annotation/R`)이고 그 밖의 클래스가 0개면 **PASS**.
- **`./gradlew :app:dependencies | grep compose`를 판정에 쓰지 말 것** — 애너테이션 전용 아티팩트까지 잡혀 **항상 1건 이상이 나온다.** rev.2의 "아티팩트 0개" 문언이 이 현실을 반영하지 못했다.
- **[확인 — planner-analyzer 재측정 2026-08-05 13:38]** `build-tools/36.0.0/dexdump`로 `app-debug.apk`(198,619,378 B)의 dex 7개를 전수 조사한 결과 `androidx/compose` 정의 클래스는 **정확히 6개**이고 전부 위 목록과 일치했으며, `androidx/compose/(ui|foundation|material)/` 는 **0건**이었다. `app/build.gradle.kts:75`에 `buildFeatures.compose` 미설정이 주석과 함께 확인된다.

**REQ-003 — background.js ↔ 네이티브 채널**
- AC-003-1: REQ-004와 REQ-005의 왕복이 실기기에서 모두 성공한다. (채널의 존재 자체가 아니라 **채널로 실제 데이터가 오간 결과**로 판정)
- AC-003-2: **역주입 실패 테스트(V2)** — `background.js`의 native 메시지 전송 한 줄을 의도적으로 주석 처리해 빌드하면 REQ-004의 화면 결과가 **표시되지 않거나 타임아웃**된다. 원복 후 다시 성공한다. 두 결과를 QA 로그에 기록한다. 이 테스트를 하지 않으면 "항상 초록인 장식"과 구분할 수 없다.
- AC-003-3: 두 클라이언트 경로(확장 페이지 / 외부 사이트)가 **background.js에 동일한 wire 스키마**를 보낸다 — `background.js`에서 요청을 처리하는 분기가 **송신자 종류에 따라 갈라지지 않는다**(코드 검사). 갈라지면 REQ-006의 "동일 계약"이 이름뿐이 된다.

**REQ-004 — getVersionName**
- AC-004-1: `index.html`의 버튼을 누르면 화면 결과 영역에 버전 문자열이 표시된다(빈 문자열·`undefined`·`null` 아님).
- AC-004-2: 그 표시 값이 `adb shell dumpsys package com.example.geckoviewtest | grep versionName`의 값과 **문자 단위로 일치**한다.
- AC-004-3: `build.gradle.kts`의 `versionName`을 다른 값으로 바꿔 재빌드하면 AC-004-1의 표시 값도 그에 맞게 바뀐다. (하드코딩된 상수를 반환하는 가짜 구현을 걸러내는 조건)
- 세 조건은 독립적이다: 하드코딩이면 3만 깨지고, 오탈자/인코딩 문제면 2만 깨진다.

**REQ-005 — appFinish**
- AC-005-1: 버튼을 누른 뒤 `adb shell dumpsys activity activities`에서 해당 Activity가 사라진다.
- AC-005-2: 같은 시점의 `adb logcat`에 `FATAL EXCEPTION` / `Fatal signal` / `libc: Fatal`이 **없다**. (크래시로 죽어도 AC-005-1은 참이 되므로, 이 조건이 없으면 크래시를 정상 종료로 오판한다)
- AC-005-3: 종료 직후 앱을 다시 실행하면 REQ-007이 정상 동작한다(런타임/확장 상태가 오염되지 않음).
- AC-005-4: V1의 "늦게 오는 오류" 조항 — 종료 판정 후 **최소 3초** logcat을 더 수집해 지연 크래시가 없음을 확인한다.

**REQ-006 — 브리지 함수 API 계약**
- AC-006-1: `index.html`(확장 페이지)에서 단일 진입 함수 `window.NativeBridge.call('getVersionName')`를 호출하면 **Promise가 resolve**되어 값이 돌아온다. 콜백 전역 변수 방식이 아니다.
- AC-006-2: 존재하지 않는 함수명을 호출하면 Promise가 **reject**되고 페이지에서 오류 사유를 읽을 수 있다. (성공 경로만 있는 구현을 걸러냄)
- AC-006-3: 네이티브 쪽에 함수를 하나 추가할 때 페이지 쪽에서 바꾸는 코드는 **함수명 문자열뿐**이다(브리지 배선 코드 수정 불필요).
- AC-006-4: **외부 사이트에서도 같은 호출 형태가 성립한다** — REQ-010의 AC로 검증한다. AC-006-1만 통과하고 AC-010이 실패하면 REQ-006은 **미충족**이다(전송 경로가 2종이므로 한쪽 통과가 다른 쪽을 보장하지 않는다 — §2.7.3).

**REQ-007 — index.html 로드**
- AC-007-1: 콜드 스타트(앱 강제 종료 후 실행) 후 3초 내 스크린샷에 `index.html`의 버튼들이 보인다.
- AC-007-2: 그 화면이 오류 페이지(`about:neterror`, "파일을 찾을 수 없음")가 **아니다**.
- AC-007-3: 페이지의 JS가 실제로 실행된다 — 페이지 로드 시점에 JS가 채우는 요소(예: 현재 시각, 브리지 준비 상태)가 표시된다. (정적 HTML만 그려지고 JS가 죽은 상태를 걸러냄)

**REQ-008 — naver.com 이동**
- AC-008-1: 버튼 탭 후 GeckoView 영역 스크린샷에 네이버 콘텐츠가 렌더링된다.
- AC-008-2: `GeckoSession.ContentDelegate.onTitleChange` 또는 화면 표시 URL이 `naver.com` 도메인을 가리킨다.
- AC-008-3: logcat에 해당 로드 구간의 네트워크 오류(`NS_ERROR_`)나 `about:neterror` 진입이 없다.
- AC-008-1과 AC-008-2는 독립: 리다이렉트만 되고 렌더가 실패할 수 있다.

**REQ-009 — 로딩 UI**
- AC-009-1: REQ-008 버튼 탭 **직후(≤1초)** 스크린샷에 로딩 UI가 **보인다**.
- AC-009-2: 로드 완료 후 스크린샷에 로딩 UI가 **보이지 않는다**.
- AC-009-3: AC-009-2와 **동시에** 네이버 콘텐츠가 렌더링돼 있다. (로딩 UI가 그냥 안 뜨는 구현은 AC-009-2만으로는 통과해버린다 — 1·2·3을 AND로 묶어야 의미가 생긴다)
- AC-009-4: **실패 경로** — 네트워크를 끄고(비행기모드 등) 버튼을 누르면 로딩 UI가 뜬 뒤 **반드시 사라진다**(무한 로딩 금지). `ProgressDelegate.onPageStop(success=false)` 경로가 실제로 UI를 내리는지 확인.

**REQ-010 — 외부 사이트 브리지 (rev.2 반전)**

> **이 REQ의 수용 기준은 이 문서에서 거짓 그린 위험이 가장 높은 지점이다.** content script는 페이지와 **격리된 세계(isolated world)** 에서 돈다(§2.6.1). 격리 세계 안에서만 브리지가 동작해도 "브리지가 열렸다"고 보이지만, **페이지 세계의 JS는 여전히 브리지를 못 쓴다.** 그래서 "페이지 세계에서 실행됐음"을 별도 조건으로 못 박는다.

검증 장치(프로덕션 코드에 포함, debug 빌드 한정 권장):
확장이 페이지에 **페이지 세계 프로브 스크립트**를 주입한다. 프로브는 ① 페이지 세계에서 `window.NativeBridge.call('getVersionName')`을 호출하고 ② 결과를 페이지 DOM의 고정 id 배지(`#__bridge_probe`)에 쓰며 ③ 자신이 페이지 세계임을 나타내는 마커를 함께 쓴다. content script는 `window.wrappedJSObject.__bridgeProbeRanInPageWorld`가 `true`일 때만 배지에 `PAGE_WORLD` 문자열을 붙인다.

- AC-010-1: `http://naver.com` 로드 후 스크린샷(또는 `adb shell uiautomator dump` 텍스트)에 배지가 보이고, 그 안에 버전 문자열이 들어 있다.
- AC-010-2: 그 값이 `adb shell dumpsys package com.example.geckoviewtest | grep versionName`과 **문자 단위로 일치**한다.
- AC-010-3: **배지에 `PAGE_WORLD` 마커가 있다.** 없으면 격리 세계에서만 동작한 것이므로 **REQ-010 미충족**이다. (이 조건이 없으면 AC-010-1·2가 모두 참인데도 실제 웹페이지는 브리지를 못 쓰는 상태를 통과시킨다)
- AC-010-4: **naver.com이 아닌 다른 외부 사이트 1곳**(예: `http://example.com`)에서도 AC-010-1~3이 성립한다. (`matches`가 naver 도메인에 하드코딩된 구현을 걸러냄)
- AC-010-5: naver.com에서 `appFinish`를 호출하면 **실제로 앱이 종료된다** — REQ-005의 AC-005-2(크래시 부재)도 동일 적용. **이것은 결함이 아니라 사용자가 선택한 의도된 동작이며**(§5.1), 이 케이스가 통과하는 것이 A-08 결정이 실현됐다는 증거다. 앱이 죽으므로 QA 시퀀스의 **맨 마지막**에 실행한다.
- AC-010-6: 브리지 주입이 REQ-008을 깨지 않는다 — AC-010-1 상태에서 AC-008-1(네이버 콘텐츠 렌더)이 여전히 참이다. (주입 스크립트가 페이지를 망가뜨리는 회귀 차단)

**REQ-011 — 뒤로가기 (rev.2 신규)**

구현 범위: `OnBackPressedDispatcher`에 `OnBackPressedCallback`을 등록하고, `NavigationDelegate.onCanGoBack(session, canGoBack)` **[확인 — Javadoc 시그니처 확인]** 으로 받은 값을 UiState에 반영해 콜백의 `isEnabled`를 갱신한다. 콜백이 활성일 때 `session.goBack()` **[확인 — `@AnyThread public void goBack()`]**, 비활성이면 dispatcher가 기본 동작(Activity 종료)을 수행한다.

- AC-011-1: `index.html` → naver 이동 → 뒤로가기 → **`index.html`이 다시 렌더**되어 버튼들이 보인다(스크린샷). 빈 화면·오류 페이지가 아니다.
- AC-011-2: 복귀 **후** `getVersionName` 버튼을 누르면 값이 다시 표시되고 AC-004-2를 만족한다. (페이지만 그려지고 확장 메시징이 죽은 상태를 걸러냄 — 세션 히스토리 복원 후 브리지 생존 여부는 실제로 깨질 수 있는 지점이다)
- AC-011-3 **(rev.3 정정 — 구현 후 실측 반영)**: `index.html` 상태(웹 히스토리 없음)에서 뒤로가기를 누르면 콜백이 비활성이 되어 **플랫폼 기본 동작이 수행된다.** 우리 코드는 여기서 끝나며, 그 기본 동작은 **기기 OS 버전에 따라 갈린다.** 아래 네 조건의 AND로 판정한다.
  - (a) **API 31+ 기기**: `adb shell dumpsys activity activities`에서 `MainActivity`의 ActivityRecord가 **존재하되** `finishing=false`이고, 해당 태스크가 `visible=false`다(태스크 백그라운드 이동).
    **API 30 이하 기기(minSdk 26이므로 지원 범위 안이다)**: Activity가 종료되어 ActivityRecord가 소멸한다.
  - (b) 같은 구간 logcat에 `FATAL EXCEPTION` / `Fatal signal`이 없다.
  - (c) **대조 — `appFinish`(명시적 `finish()`) 경로에서는 `MainActivity` ActivityRecord가 실제로 소멸한다**(AC-005-1과 동일 관측). 이 대조가 없으면 `finishing=false`만으로는 "종료되지 않음"과 "애초에 실행되지 않음"을 구분할 수 없다.
  - (d) **판정 시 기기의 `ro.build.version.sdk`를 함께 기록한다.** 기록이 없으면 (a)의 어느 갈래를 적용했는지 사후에 검증할 수 없다.
- **왜 정정했는가 [문서 — Android Developers "Behavior changes: all apps" (Android 12)]**: API 31부터 **`ACTION_MAIN` + `CATEGORY_LAUNCHER`를 선언한 루트 Activity**의 뒤로가기 기본 동작이 `finish()`에서 **태스크 백그라운드 이동**으로 바뀌었다. 이 변경은 **기기 OS 버전으로 적용되며 targetSdk와 무관**하다 — 그래서 (d)에서 targetSdk가 아니라 기기 API를 기록하라고 요구한다. 같은 문서가 커스텀 뒤로가기에 **AndroidX Activity API 사용을 권장**하며 "가로채는 컴포넌트가 없으면 시스템 기본 동작으로 자동 위임된다"고 명시하는데, 현 구현의 `OnBackPressedCallback.isEnabled = false`가 정확히 그 경로다. **즉 rev.2의 "Activity가 종료된다"는 문언이 플랫폼 현실을 반영하지 못한 것이며, 구현 결함이 아니다.**
- AC-011-4: AC-011-1과 AC-011-3이 **둘 다** 관측된다. 한쪽만 보면 콜백이 항상 켜져 있거나 항상 꺼져 있는 구현을 잡지 못한다 — 두 케이스가 함께 있어야 `canGoBack` 상태 관리가 실제로 동작한다는 뜻이 된다. **(rev.3 보강)** 판정은 "종료 여부"가 아니라 **두 조작의 결과가 서로 다르다**는 것으로 한다: naver에서 뒤로가기는 `index.html` 복귀 + 포그라운드 유지(`state=RESUMED`), `index.html`에서 뒤로가기는 포그라운드 이탈(API 31+ 기준 `state=STOPPED`). 두 결과가 같아지면 `canGoBack` 배선이 죽은 것이다.
- **[확인 — code-reviewer 실측 (review.md §1①, 실기기 SM-G981N / API 33)]** 확장 페이지(`moz-extension://`)는 GeckoSession 히스토리에 정상적으로 남는다 — naver 이동 후 뒤로가기로 `index.html`이 재렌더되고 브리지가 재동작함이 관측됐다(AC-011-1·AC-011-2 성립). rev.2가 최우선 확인 대상으로 올렸던 **[미확인] 항목은 해소됐고 §2.7.4 폴백은 발동하지 않았다.** planner-analyzer는 이 관측을 직접 재현하지 않았다.

---

## 2. GeckoView 도입 조사 결과

### 2.1 좌표·채널·버전 — **[확인]**

`curl https://maven.mozilla.org/maven2/org/mozilla/geckoview/geckoview/maven-metadata.xml` 직접 조회 결과:

| 항목 | 값 |
|:--|:--|
| 저장소 URL | `https://maven.mozilla.org/maven2/` |
| groupId | `org.mozilla.geckoview` |
| stable artifactId | `geckoview` |
| beta / nightly artifactId | `geckoview-beta` / `geckoview-nightly` **[문서]** |
| **최신 stable 버전** | **`153.0.20260730155536`** (`<release>` 태그, `lastUpdated=20260731092255`) |
| 라이선스 | MPL 2.0 |

```kotlin
maven { url = uri("https://maven.mozilla.org/maven2/") }
// ...
implementation("org.mozilla.geckoview:geckoview:153.0.20260730155536")
```

> 버전은 Firefox 릴리스 트레인을 따라 수 주 단위로 바뀐다. 구현 시점에 위 URL을 **다시 조회**해 `<release>` 값을 쓸 것. 이 문서의 값은 2026-08-04 기준이다.

### 2.2 minSdk / ABI / APK 크기 — **[확인]**

AAR(`geckoview-153.0.20260730155536.aar`)의 ZIP 중앙 디렉터리를 HTTP Range 요청으로 직접 파싱해 얻은 값이다(전체 다운로드 없이 실측). 총 68개 엔트리, **AAR 크기 240,695,932 bytes (≈229.6 MiB)**.

| 항목 | 실측값 | 의미 |
|:--|:--|:--|
| **`<uses-sdk android:minSdkVersion="26" />`** | AAR AndroidManifest.xml에 명시 | **앱 minSdk는 26 이상이어야 한다.** 그 미만은 manifest merge 단계에서 빌드 실패. |
| 동봉 ABI | `arm64-v8a`, `armeabi-v7a`, `x86_64` **3종만** | **`x86`(32비트) 없음** → 32비트 x86 에뮬레이터에서 실행 불가. 실기기(arm64-v8a)와 x86_64 에뮬레이터만 가능. |
| ABI별 네이티브 크기(압축 기준) | arm64-v8a 70.7 MB / armeabi-v7a 68.2 MB / x86_64 75.6 MB | jni/ 합계 214.5 MB |
| `assets/` | 13.6 MB (omni.ja 등) | ABI와 무관하게 항상 포함 |
| `classes.jar` | 1.4 MB | Java/Kotlin API 표면 |
| ABI별 분리 아티팩트 | **없음.** `.module`(Gradle metadata) 변형은 api/runtime 2개뿐이며 둘 다 동일한 단일 universal AAR을 가리킨다 | 과거의 `geckoview-arm64-v8a` 식 좌표는 현재 stable 채널에 없음 → **APK 축소는 앱 모듈에서 직접 해야 한다** |

**APK 크기 대응 (필수):** 아무 설정 없이 빌드하면 3개 ABI가 모두 들어가 APK가 **200 MB를 훌쩍 넘긴다.** 실기기 `SM-G981N`은 arm64-v8a이므로:
```kotlin
// app/build.gradle.kts — 개발/검증용
defaultConfig {
    ndk { abiFilters += listOf("arm64-v8a") }   // 또는 splits.abi 로 ABI별 APK 분리
}
```
예상 APK ≈ 70.7 + 13.6 + α ≈ **85~95 MB [미확인 — 실측 필요]**. abiFilters를 넣지 않으면 adb install 시간이 수 분대로 늘어나고 파이프라인 검증 루프가 사실상 마비된다. **plan.md의 리스크 항목으로 반드시 올릴 것.**

### 2.3 AAR이 자동 병합하는 Manifest 내용 — **[확인]**

AAR AndroidManifest.xml 실측:
- **권한**: `INTERNET`, `ACCESS_NETWORK_STATE`, `WAKE_LOCK`, `MODIFY_AUDIO_SETTINGS`, `HIGH_SAMPLING_RATE_SENSORS` — **앱 manifest에 따로 안 써도 병합된다.**
- **`<uses-feature android:glEsVersion="0x00020000" android:required="true" />`** — OpenGL ES 2.0 필수.
- **`<queries>`** — `ACTION_VIEW` 인텐트 조회용.
- **`<application android:zygotePreloadName="org.mozilla.gecko.process.ZygotePreload">`** — 앱 `<application>` 태그와 병합된다.
- **서비스 다수**: `MediaManager`, `CrashHelper`, `GeckoChildProcessServices$tab0..tab39`, `$isolatedTab0..39`, `$gpu`, `$rdd`, `$socket`, `$gmplugin`, `$utility`, `$zygoteTab` — 총 80개 이상의 `<service>`가 병합되어 `:tab_disable_art_image_N` 등 별도 프로세스로 뜬다. **AC-001-2의 판정 근거가 여기서 나온다.**

### 2.4 전이 의존성 — **[확인]** (POM 직접 조회)

`kotlin-stdlib 2.3.21`, `androidx.annotation 1.10.0`, `androidx.annotation-experimental 1.6.0`, `androidx.collection 1.6.0`, **`androidx.core 1.18.0`**, `androidx.lifecycle-common 2.10.0`, `androidx.lifecycle-process 2.10.0`, `com.google.android.gms:play-services-fido 21.3.0`, `org.yaml:snakeyaml 2.2`, `androidx.media3-{common,datasource,decoder,exoplayer,exoplayer-hls} 1.10.1`.

파급 효과:
- `androidx.core 1.18.0`은 높은 `compileSdk`를 요구한다 → **compileSdk 36 확정**(A-03, 설치된 platform android-36 존재 **[확인]**). 정확한 하한은 빌드 시 AGP 오류로 확정 **[미확인]**.
- Kotlin stdlib 2.3.21이 딸려온다 → 프로젝트 Kotlin 버전을 그보다 낮게 잡으면 충돌 가능 **[미확인]**.
- `play-services-fido` 때문에 `google()` 저장소 필요.
- **rev.2 추가**: REQ-011의 `OnBackPressedDispatcher`는 `androidx.activity`가 필요하다. GeckoView가 끌고 오지 않으므로 **명시적으로 추가**해야 한다(`androidx.activity:activity-ktx`). 버전 카탈로그 등재 대상.

### 2.5 Java 17 요구 — **[문서]**

Mozilla 공식 quick-start: *"GeckoView uses some Java 17 APIs, it requires these compatibility flags"*
```kotlin
compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
```
같은 문서에 **"GeckoRuntime can only be initialized once per process"** 명시 — 런타임은 Application 스코프 싱글턴이어야 한다.

### 2.6 WebExtension 기반 앱↔웹 통신 메커니즘 — **[문서]** (Javadoc + firefox-source-docs 원문 확인)

**Android → 확장 설치**
```java
@NonNull @HandlerThread GeckoResult<WebExtension> installBuiltIn(@NonNull String uri)
@NonNull @HandlerThread GeckoResult<WebExtension> ensureBuiltIn(@NonNull String uri, @Nullable String id)
@NonNull @HandlerThread GeckoResult<List<WebExtension>> list()
@NonNull @HandlerThread GeckoResult<Void> uninstall(@NonNull WebExtension extension)
```
- `uri`는 **`resource://android/` 로 시작해야 한다**. 예: `"resource://android/assets/messaging/"`
- **`ensureBuiltIn`을 쓸 것.** 확장은 앱 재시작 후에도 유지되므로 매번 재설치하면 느리다.
- 빌트인 확장은 **서명 불필요**하고 네이티브 메시징 권한을 가진다. manifest.json에 **`id`와 `version`이 반드시 있어야 한다** **[문서]**.

**manifest.json 권한 (rev.2에서 정정)**
| 권한 | 필요 여부 | 역할 |
|:--|:--|:--|
| `geckoViewAddons` | **필수** | 네이티브 메시징 API 자체를 열어준다. 없으면 `sendNativeMessage`가 존재하지 않는다. |
| `nativeMessaging` | **필수** | background script의 네이티브 통신 허용 |
| `nativeMessagingFromContent` | **불필요 — 선언하지 않는다** | content script가 네이티브 API를 **직접** 호출할 때만 필요. 본 설계는 content script → background.js 중계이므로 해당 없음 (§2.10) |
| `<all_urls>` (host permission) | **필수** | content script 주입 대상 (§2.9) |

**단발성 메시지 (확장 → 앱)**
- background.js: `browser.runtime.sendNativeMessage("browser", message)`
- Android: `extension.setMessageDelegate(delegate, "browser")` +
  `GeckoResult<Object> onMessage(String nativeApp, Object message, WebExtension.MessageSender sender)`
- **`nativeApp` 문자열이 양쪽에서 일치해야 한다.** GeckoView는 native manifest 파일을 쓰지 않고, `setMessageDelegate`의 두 번째 인자가 곧 앱 식별자다. 공식 예제는 `"browser"`. **불일치 시 오류 없이 조용히 아무 일도 안 일어난다.**
- `onMessage`의 반환 `GeckoResult`가 resolve되면 그 값이 확장 쪽 Promise 결과가 된다 → **REQ-004의 요청/응답 왕복이 이 한 쌍으로 끝난다.**

**연결형 메시지 (앱이 먼저 말하는 방향)**
- 확장: `browser.runtime.connectNative("browser")` / Android: `MessageDelegate.onConnect(WebExtension.Port)` → `port.setDelegate(...)`, `port.postMessage(...)`
- **이번 스코프에서는 쓰지 않는다**(§5 A-06).

**background script vs content script 델리게이트 등록 위치**
- `WebExtension.setMessageDelegate(...)` → **background script**의 메시지를 받는다. **본 설계가 쓰는 것은 이쪽 하나뿐이다.**
- content script의 메시지는 세션 단위 등록이 필요하다: `session.getWebExtensionController().setMessageDelegate(...)` **[확인 — `@UiThread @NonNull WebExtension.SessionController getWebExtensionController()`]**. **본 설계에서는 사용하지 않는다.**

### 2.6.1 페이지 세계(page world) ↔ content script 격리 — **rev.2 신설. REQ-006/010의 구현 방식을 결정하는 제약**

**[문서 — MDN "Share objects with page scripts", Firefox Script Security]**

content script는 페이지와 DOM은 공유하지만 **JS 스코프는 격리**되어 있고, Firefox는 여기에 **Xray vision**을 적용한다. 실무상 결과:

- content script에서 `window.NativeBridge = {...}` 로 대입해도 **페이지 세계의 JS는 그것을 볼 수 없다.** Xray 때문에 content script가 페이지 window에 함수를 실어 보낼 수 없다 — **`exportFunction()`이 존재하는 이유가 바로 이것이다.**
- content script가 페이지가 만든 값을 읽으려면 `window.wrappedJSObject.foo`를 써야 한다(단, 언랩된 객체는 페이지가 재정의했을 수 있어 신뢰 불가).

**따라서 "웹 페이지 JS에서 호출 가능한 브리지 함수"(REQ-006)를 외부 사이트에 제공하려면 아래 중 하나가 반드시 필요하다:**

| 방식 | 내용 | 평가 |
|:--|:--|:--|
| **(가) 페이지 세계 스크립트 주입** | `web_accessible_resources`에 등록한 JS를 content script가 `<script>` 태그로 페이지에 삽입 → 그 스크립트는 **페이지 세계에서 실행**되며 `window.NativeBridge`를 정의한다. 페이지 세계 ↔ content script는 `window.postMessage`로 통신 | **권고.** 표준 WebExtension 기법이고 Xray 우회 트릭이 없어 동작이 예측 가능. `web_accessible_resources` 등록이 필요 |
| (나) `exportFunction()` / `cloneInto()` | content script가 함수를 페이지 스코프로 직접 export (Firefox 전용 API) | 코드는 짧지만 **인자·반환이 Xray로 넘어와** Promise·객체 처리가 까다롭다 **[미확인]**. GeckoView는 Firefox 엔진이므로 사용 자체는 가능 |
| (다) 페이지가 먼저 `window.postMessage` | 페이지 쪽에 우리 코드가 있어야 성립 | **외부 사이트에는 불가.** REQ-010을 충족할 수 없다 |

**(가) 채택 시 최종 데이터 흐름 (외부 사이트):**
```
페이지 세계 JS (window.NativeBridge.call)
  └─ window.postMessage ─▶ content script (격리 세계)
       └─ browser.runtime.sendMessage ─▶ background.js
            └─ browser.runtime.sendNativeMessage("browser", …) ─▶ Android MessageDelegate.onMessage
                 └─ GeckoResult resolve ─▶ (역순으로 되돌아가 페이지의 Promise가 resolve)
```
**확장 페이지(index.html)는 이 중 앞 두 단계가 없다** — `browser.runtime.sendMessage`부터 시작한다. 이것이 §2.7.3에서 말하는 "전송 경로 2종"이다.

> **주의 (거짓 그린):** 위 흐름에서 `window.postMessage` 구간이 빠져도 **content script 안에서는 브리지가 완벽히 동작한다.** 그래서 AC-010-3(PAGE_WORLD 마커)이 없으면 검증이 통과해버린다.

### 2.7 index.html을 어디서 열 것인가

#### 2.7.1 `resource://android/assets/index.html` 직접 로드는 배제 — 근거 유지 (rev.1과 동일, A-08 반전과 무관하게 유효)

**(a) 확장 설치 경로로서의 `resource://android/assets/` — [문서] 지원됨.** `installBuiltIn`/`ensureBuiltIn`이 이 스킴만 허용한다. `app/src/main/assets/messaging/` 배치는 정상 설계다.

**(b) 콘텐츠 페이지로 여는 것은 부적합.**
1. **[확인된 원리적 제약 — 결정적]** WebExtension `content_scripts.matches`가 지원하는 스킴은 `http`, `https`, `ws`, `wss`, `ftp`, `data`, `file` 뿐이고 `<all_urls>`도 이 집합이다 **[문서 — MDN Match patterns]**. **`resource://`는 매치 패턴으로 쓸 수 없고, 넣으면 manifest가 거부된다.**
2. **[미확인 / 리스크]** mozilla/geckoview#199: `resource://android/assets/start.html` 로드가 앱을 **크래시**시킨다는 보고(GeckoView 109 stable). 153에서 수정됐는지 확인되지 않았다.

#### 2.7.2 채택 설계 — 확장 페이지로 로드

- `index.html`을 확장 폴더(`assets/messaging/`) 안에 두고 `manifest.json`의 `web_accessible_resources`에 등록한다.
- `ensureBuiltIn(...)`의 `GeckoResult<WebExtension>`이 resolve되면 `extension.metaData.baseUrl` **[확인 — Javadoc에 `final String baseUrl`, "Root URL for this extension's pages"]** 를 읽어 `session.loadUri(baseUrl + "index.html")` 로 로드한다.
- 이 페이지는 **확장 오리진(`moz-extension://<uuid>/`)의 확장 페이지**이므로 content script 없이 `browser.runtime.sendMessage()`로 background.js와 직접 대화한다.
- **[미확인 — 실기기 확인 필요]** GeckoView가 앱 시작 최상위 네비게이션으로 `moz-extension://` URL을 여는 것을 허용하는지, `web_accessible_resources` 등록이 필수인지. 실패 시 §2.7.4 폴백.

#### 2.7.3 A-08 반전에 따른 재판정 — **결론: 확장 페이지 방식을 유지한다**

A-08이 반전되어 content script가 **우회 대상이 아니라 필수 경로**가 되었으므로, "index.html도 content script 경로로 통일하는 편이 단순하지 않은가"를 재검토했다.

**통일안(index.html을 `file://` 또는 `http://127.0.0.1`로 옮겨 content script 하나로 처리)을 채택하지 않는 이유:**

1. **리스크 분산이 깨진다 (결정적).** 확장 페이지 경로는 content script·페이지 세계 격리·#220과 **완전히 독립**이다. 외부 사이트 경로가 막혀도 REQ-004·005·007은 살아남는다. 통일하면 **단일 경로가 막히는 순간 REQ 7개가 동시에 죽는다.**
2. **통일안의 두 후보가 각각 [미확인] 리스크를 안는다.** `file://` 은 확장의 파일 접근 권한 부여 수단이 GeckoView에 있는지 불확실하고 **[미확인]**, localhost 서버는 의존성(NanoHTTPD 등)과 포트 노출이 늘어 architecture.md의 "새 라이브러리는 승인 필요" 항목을 추가로 소모한다.
3. **REQ-011(뒤로가기)이 통일안에서 더 불리하다.** 확장 페이지든 file이든 히스토리 잔존 여부는 **[미확인]**이지만, 통일안은 여기에 파일 복사·서버 수명주기 문제가 겹친다.
4. **`resource://` 배제 근거는 그대로 유효**하므로, 통일안이라 해도 assets에서 직접 열 수는 없다 — 즉 통일안은 "간단해지는" 게 아니라 **파일 복사 또는 서버라는 새 작업이 추가되는** 안이다.

**통일은 전송 계층이 아니라 계약 계층에서 확보한다:**
- 두 경로가 **동일한 파사드**를 노출한다: `window.NativeBridge.call(name, payload) → Promise`
- 두 경로가 **동일한 wire 스키마**로 background.js에 도달한다(AC-003-3이 이를 강제).
- 클라이언트 JS는 파사드 1개 + 전송 어댑터 2개(`runtime.sendMessage` / `window.postMessage`)로 나눈다. **background.js 이후는 완전히 동일한 코드다.**

**대가 — 정직하게 기록:** 전송 경로가 2종이므로 **`index.html`에서만 테스트하면 외부 사이트 경로에 대한 거짓 그린이 된다.** 그래서 AC-006-4가 "AC-010이 실패하면 REQ-006도 미충족"이라고 못 박고 있다. QA는 두 경로를 **각각** 검증해야 하며, 한쪽 결과를 다른 쪽 근거로 인용해서는 안 된다.

#### 2.7.4 폴백 (§2.7.2 또는 REQ-011이 실기기에서 실패할 경우, 우선순위 순)
1. assets의 `index.html`을 최초 실행 시 앱 내부 저장소로 복사하고 `file://` 로드 + content script `matches: ["file:///*"]` **[미확인 — 파일 접근 허용 필요 가능성]**
2. `GeckoSession.Loader().data(html, "text/html")` 문자열 로드. 상대 경로 리소스 불가
3. 앱 내부 localhost HTTP 서버. **의존성 증가·포트 노출로 비권장**

### 2.8 `http://naver.com` (cleartext) — **[미확인, 검증 항목]**

- Android의 `android:usesCleartextTraffic` / `network_security_config`는 **Android 네트워크 스택**에 적용된다. Gecko는 자체 스택(necko)을 쓰므로 적용 대상인지 **확인되지 않았다.** 넣어도 무해하므로 **debug 빌드에 `usesCleartextTraffic="true"`를 넣고 시작**한다.
- **[확인 — Javadoc]** Gecko 자체의 HTTPS 강제는 `GeckoRuntimeSettings.Builder.allowInsecureConnections(int level)`로 제어. `ALLOW_ALL` 상수 존재 **[문서]**, 나머지 상수명 **[미확인]**.
- naver.com은 https로 리다이렉트될 가능성이 높다. **AC-008-2는 도메인 기준 판정**이므로 https 리다이렉트는 실패가 아니다.
- **rev.2 추가:** REQ-010의 AC-010-4에서 두 번째 외부 사이트를 고를 때, http/https 어느 쪽이든 `matches`에 걸리도록 `["http://*/*", "https://*/*"]`를 쓰면 이 문제가 사라진다(§2.9).

### 2.9 `content_scripts.matches` 범위 — **evaluator 판정 항목**

사용자 답변은 "외부 사이트에서도 브리지 호출 가능"이다. **특정 도메인 한정으로 축소하지 않는다.** 두 선택지:

| 선택지 | 커버 범위 | 평가 |
|:--|:--|:--|
| `["<all_urls>"]` | http, https, ws, wss, ftp, data, file **[문서]** | 사용자 요구의 **가장 문자 그대로의 해석**. `data:`/`file:`은 오리진 의미가 특수해 브리지 주입 시 동작이 불명확 **[미확인]** |
| **`["http://*/*", "https://*/*"]`** (권고) | 실제 웹사이트 전부 | 실무상 "외부 사이트"를 빠짐없이 덮으면서 `data:`/`ftp:`의 불명확한 경계를 피한다. REQ-008(naver)·AC-010-4(제2 사이트)를 모두 충족 |

**축소가 아니라 스킴 정리다** — 어떤 도메인도 제외하지 않는다. 다만 판정은 evaluator에 넘긴다.

**`all_frames`**: manifest 기본값 `false`(최상위 문서에만 주입)를 **명시적으로 유지**한다. 서드파티 광고 iframe까지 브리지가 들어가는 것은 사용자가 요구한 범위가 아니며, 요구를 좁히지도 않는다(사용자가 말한 "외부 사이트"는 방문한 사이트를 뜻한다). 이 판단도 evaluator 확인 대상.

### 2.10 mozilla/geckoview#220 정밀 재조사 — **rev.1의 경고 범위를 정정**

이슈 본문을 직접 확인한 결과 **[확인 — 이슈 페이지 원문]**:
- 깨지는 코드: `let response = await chrome.runtime.sendNativeMessage("browser", {...})` — **content script가 네이티브 API를 직접 호출**하는 경우
- 같은 코드가 **background script에서는 정상 동작**한다고 보고자가 명시
- GeckoView 113.0.20230504192738에서 보고, **이슈는 열린 상태**, 메인테이너 코멘트·워크어라운드 없음
- 보고자는 `nativeMessaging`, `nativeMessagingFromContent`, `geckoViewAddons`를 모두 선언했음에도 실패

**본 설계에 대한 결론:** 본 설계의 content script는 `browser.runtime.sendMessage`(확장 내부 메시징)만 쓰고 네이티브 API는 background.js에서만 호출한다. **따라서 #220의 결함 경로를 지나지 않는다.** 요구사항 원문이 "background.js를 사용한 통신"을 명시한 덕분에 우연히 회피된 구조다 — **이 사실을 `background.js` 상단 주석에 남길 것.** 남기지 않으면 다음 개발자가 "content script에서 바로 보내면 한 단계 줄겠네"라며 결함 경로로 되돌린다.

**단, 경계할 것:** #220이 회피된다고 해서 content script 경로 전체가 검증된 것은 아니다. `runtime.sendMessage` 왕복, 페이지 세계 주입, `web_accessible_resources` 동작은 모두 **[미확인]**이며 §7.4의 최우선 검증 대상이다.

### 2.11 manifest_version 선택 — **[미확인] / plan 단계 결정**

| | MV2 | MV3 |
|:--|:--|:--|
| `web_accessible_resources` | 문자열 배열 `["injected.js"]` | 객체 배열 `[{"resources":[…],"matches":[…]}]` |
| host permission | `permissions`에 포함 | `host_permissions`로 분리 |
| background | 영속 background page | 이벤트 페이지(종료될 수 있음) |

**권고: MV2로 시작.** background가 영속이라 메시지 중계 수명주기 문제가 없고, Mozilla의 GeckoView 예제가 MV2 형태다 **[미확인 — 예제의 MV 버전을 직접 확인하지 못함]**. MV3를 쓸 경우 이벤트 페이지가 종료된 뒤 첫 메시지의 지연/유실 여부를 별도 검증해야 한다.

### 2.12 디버깅 수단 — **[문서]**
`GeckoRuntimeSettings.Builder.consoleOutput(true)`로 웹 콘솔 메시지를 logcat으로 뺀다. `remoteDebuggingEnabled(true)`로 원격 디버깅. **debug 빌드에서 반드시 켤 것** — 안 켜면 페이지 세계/격리 세계/background 3곳의 JS 오류가 통째로 안 보여서 원인 분리가 불가능하다. rev.2에서 통신 경로가 5단계로 늘었으므로 중요도가 더 올라갔다.

---

## 3. XML View vs `architecture.md` 레이어 원칙 — 충돌 정리 (evaluator 판정 대상)

`architecture.md`는 "**UI (Compose)** → ViewModel → Repository → DataSource"로 쓰여 있으나, **REQ-002는 XML View를 명시**한다. **사용자 요구가 우선이므로 요구사항을 Compose로 바꾸지 않는다.** 아래는 XML+ViewBinding 환경에서 각 원칙을 어떻게 지킬 것인가의 쟁점이다. **결론은 evaluator가 내린다.**

| architecture.md 원칙 | XML 환경에서의 해석 (제안) | 쟁점 / 판정 필요 |
|:--|:--|:--|
| "UI (Compose) → ViewModel → ..." | "UI(Activity/Fragment + XML)"로 읽는다. 레이어 방향 원칙은 그대로 유지 | 문구만 Compose이고 의도는 레이어 방향이므로 **충족 가능** |
| UI가 Repository/DataSource 직접 호출 금지 | Activity는 ViewModel만 참조 | — |
| **ViewModel에 Context/View 주입 금지** | **최대 쟁점.** `getVersionName`은 `PackageManager`(=Context)가, `appFinish`는 Activity가 필요 | **제안:** ① `AppInfoRepository` 인터페이스(순수 Kotlin)를 두고 Context 의존을 `Impl`에 가둔다(Application Context) ② `appFinish`는 **일회성 이벤트**로 ViewModel이 `Channel<UiEvent>`에 `Finish`를 방출, Activity가 수신해 `finish()` 호출 |
| **GeckoRuntime/GeckoSession 소유자** | `GeckoRuntime`은 프로세스당 1회 제약 **[문서]**, `GeckoSession`은 View와 결합 | **제안:** `GeckoRuntime`은 Application 스코프 싱글턴, `GeckoSession`은 Activity/View 계층 소유. ViewModel은 세션 객체를 보유하지 않고 URL과 로딩 여부만 UiState로 표현 |
| 단일 `UiState` + `StateFlow` | 유지. `MainUiState(isLoading, currentUrl, canGoBack, bridgeReady, lastBridgeResult)` | XML에서는 `repeatOnLifecycle(STARTED) { uiState.collect { render(it) } }`로 단일 `render()`에 바인딩. Compose의 recomposition 대신 **명시적 render 함수 1개**가 UDF의 XML 대응물 |
| "`remember`로 비즈니스 상태 보관 금지" | XML판: **Activity 필드/View 상태에 비즈니스 상태 보관 금지.** 로딩 여부의 진실의 원천은 UiState이고 `ProgressBar.visibility`는 그 투영 | REQ-009 구현이 이 원칙을 어기기 쉬운 지점(델리게이트에서 직접 `visibility=VISIBLE`). **code-reviewer 중점 확인** |
| 일회성 이벤트를 상태와 분리 | `appFinish`, 오류 토스트. `Channel`/`SharedFlow(replay=0)` | — |
| **(rev.2) 뒤로가기 상태** | `onCanGoBack(session, canGoBack)` **[확인]** → ViewModel → `MainUiState.canGoBack` → `OnBackPressedCallback.isEnabled` | **쟁점:** `OnBackPressedCallback`은 androidx.activity 타입이라 ViewModel이 직접 들면 안 된다. Activity가 UiState를 관찰해 `isEnabled`만 갱신하고, 뒤로가기 발생 시 ViewModel에 의도를 알리는 방향이 레이어 원칙에 부합. **evaluator 판정** |
| `viewModelScope`만, `GlobalScope`/`runBlocking` 금지 | GeckoView 콜백은 UI 스레드 콜백이지 코루틴이 아니다 | **제안:** `GeckoResult<T>` → `suspendCancellableCoroutine` 어댑터 확장 함수를 두고 그 안에서만 변환. `GeckoResult.poll()`(블로킹) 금지 |
| Dispatcher 주입 | 유지. 생성자 주입 | GeckoView API가 `@UiThread`/`@AnyThread`/`@HandlerThread`로 갈리므로 **스레드 계약을 문서화** |
| 버전은 `gradle/libs.versions.toml`에서만 | 신규 프로젝트이므로 처음부터 버전 카탈로그. GeckoView·androidx.activity 포함 | — |
| 새 라이브러리는 plan.md 명시 + evaluator 승인 | **GeckoView**(APK 200MB+ 영향), **androidx.activity**(REQ-011), DI 라이브러리 도입 여부 | §5 A-09 |
| UI 노출 텍스트는 `strings.xml` | Android UI 텍스트는 `strings.xml`. **`index.html`·주입 스크립트 내부 텍스트는 웹 리소스이므로 대상 외** | evaluator 판정 — 과잉 적용하면 HTML/JS가 망가진다 |

**추가 쟁점 — 테스트 가능성:** GeckoView는 실기기/에뮬레이터 없이는 아무것도 실행되지 않는다. 순수 JVM 단위 테스트로 검증 가능한 것은 **ViewModel의 상태 전이(로딩·canGoBack)**, **`AppInfoRepository` 계약**, **브리지 메시지의 직렬화/역직렬화(JSON 파싱, 미지원 함수명 → 오류 응답)** 정도다. 나머지(REQ-001·007·008·009·010·011의 렌더링·네비게이션)는 계측 테스트 또는 수동 실기기 검증이다. §4의 커버리지 목표는 이 경계를 전제로 한다.

---

## 4. 비기능 요구사항

원문(`initRequire.md`)에 비기능 명시는 **없다.** 아래는 파이프라인 규칙 문서에서 오는 제약이다.

| 항목 | 값 | 출처 |
|:--|:--|:--|
| 커버리지 | **변경 클래스 라인 커버리지 70% 이상**, 전체는 베이스라인 대비 하락 금지 | `architecture.md` |
| 커버리지 계측 주의 | **계측 테스트는 JaCoCo에 집계되지 않는다**(`.ec` 미수집). GeckoView 특성상 검증의 다수가 계측/수동이 되므로 70%는 **JVM 테스트가 닿는 레이어에서 벌어야 한다.** 계측을 늘려도 수치는 안 오른다 — "테스트 부실"로 오판 금지 | `verification-honesty.md` V8 |
| 단위 테스트 | 신규/변경 비즈니스 로직 필수. JUnit + kotlinx-coroutines-test. ViewModel은 `Dispatchers.setMain` + Turbine(또는 동등) | `architecture.md` |
| 새 검증의 RED 확인 | 새로 만든 테스트는 **의도적으로 깨뜨려 RED 확인 후 원복**하고 로그에 기록 | `verification-honesty.md` V2 |
| 환경 한정 테스트 | 실기기 전용 테스트는 **어노테이션 필터 + `Assume` 가드를 둘 다** 적용 | `verification-honesty.md` V4 |
| 주석 | **한글**. 파일/클래스 상단 KDoc 필수. Android 개념 첫 등장 시 한 줄 설명. 독자는 **Android 초보 개발자** | `comment-style.md` |
| 주석 — 이 프로젝트 특수 | GeckoView·WebExtension·native messaging·`GeckoResult`·`resource://android/`·`ensureBuiltIn`·**page world vs isolated world·Xray vision**은 초보자가 검색 없이 못 따라온다. 첫 등장 지점에 설명 필수. **non-obvious WHY로 반드시 남길 것 3건**: ① 왜 `resource://`로 index.html을 안 여는가(§2.7.1) ② 왜 content script에서 바로 `sendNativeMessage`를 안 하는가(§2.10) ③ 왜 `window.NativeBridge`를 content script에서 직접 대입하지 않고 스크립트를 주입하는가(§2.6.1). **셋 다 없으면 다음 개발자가 "단순화"하며 되돌린다** | `comment-style.md` 규칙 3·4 |
| 테스트 주석 | 클래스 KDoc에 **"무엇을 보장하고 무엇은 보장하지 않는가"**. 특히 **"index.html 경로 통과가 외부 사이트 경로를 보장하지 않는다"**를 명시 | `comment-style.md` T1 |
| 스코프 가드 | 화이트리스트 밖 수정은 hook이 차단. 신규 프로젝트라 `allowed_globs` 설계가 중요 | `scope-guard.md` |
| 지원 OS | **minSdk 26**(AAR 하한, 실측), **targetSdk 36 / compileSdk 36** (A-03 확정) |
| **보안 (rev.2)** | **외부 사이트 브리지 노출은 사용자가 선택한 트레이드오프다** — §5.1 참조. 완화책(함수 화이트리스트, `all_frames:false`)은 요구를 좁히지 않는 범위에서만 적용 | 사용자 A-08 결정 |
| 접근성/성능/오프라인 | 원문 명시 없음 |

---

## 5. 모호점 및 가정

| # | 모호점 | 결론 | 상태 |
|:--|:--|:--|:--|
| A-01 | 우선순위 미명시 | REQ-001~009 및 사용자 결정 기반 REQ-010·011은 전부 **P1**. 원문 문장 또는 명시적 사용자 결정에 1:1 대응하므로 P2로 내리면 변별력이 0이 된다 | 확정 |
| A-02 | 패키지명 | **`com.example.geckoviewtest`** | **rev.2 확정 (사용자)** |
| A-03 | 앱 이름 / SDK 버전 | 앱 이름 **`GeckoViewTest`**, **minSdk 26**(AAR 하한, 선택 불가), **targetSdk 36**, **compileSdk 36** | **rev.2 확정 (사용자)** |
| A-04 | index.html 오리진 | **확장 페이지**(`extension.metaData.baseUrl` + `web_accessible_resources`). A-08 반전 후 §2.7.3에서 **재판정했고 결론 유지** | **rev.2 재확인** |
| A-05 | L6이 L3과 별개 요구인가 | **별개가 아니다.** L6은 L3의 상위 표현 = 채널 위에 올린 함수 API 계약(REQ-006). **별도 JS 인터페이스를 두 벌 만들지 않는다.** GeckoView에는 `addJavascriptInterface` 상당 API가 없어 WebExtension 경로가 유일하다는 점을 사용자에게 안내하고 동의받음 | **rev.2 확정 (사용자)** |
| A-06 | 통신 방향 | 1차 구현은 **페이지→앱 요청/응답**(`sendNativeMessage` + `onMessage` 반환). 앱→페이지 푸시(`connectNative`/`Port.postMessage`)는 **구조만 열어두고 기능은 만들지 않는다** | 가정 유지 (미질의) |
| A-07 | cleartext / HTTPS 업그레이드 | debug에 `usesCleartextTraffic="true"` 선반영. Gecko 쪽은 기본으로 시작하고 차단되면 `allowInsecureConnections(ALLOW_ALL)`. AC-008-2는 도메인 기준이므로 https 리다이렉트는 실패가 아님 | 가정 유지 |
| **A-08** | **브리지의 보안 경계** | **사용자 결정: 외부 사이트에서도 브리지 호출 가능.** rev.1의 차단 가정을 반전. → REQ-010 반전, §5.1에 트레이드오프 명문화 | **rev.2 반전 확정 (사용자)** |
| A-09 | DI 프레임워크 | 화면 1개·의존성 4~5개 규모라 **Hilt 없이 수동 DI**(Application이 컨테이너 + `ViewModelProvider.Factory`) 제안 | **evaluator 판정** |
| **A-10** | **뒤로가기** | **사용자 결정: 포함.** 스코프 아웃 해제 → **REQ-011**. `OnBackPressedDispatcher` + `session.goBack()` + `onCanGoBack` 상태 관리. 웹 히스토리 없으면 **통상대로 Activity 종료** | **rev.2 확정 (사용자)** |
| A-11 | APK 크기 / ABI | debug에 `abiFilters=["arm64-v8a"]`. 실기기 `SM-G981N`이 arm64-v8a임을 실측. **x86_64 에뮬레이터에서는 앱이 안 뜬다** | 가정 유지 (통보 완료) |
| A-12 | GeckoView 버전 고정 | `153.0.20260730155536` 고정. 동적 버전(`+`) 금지 | 가정 유지 |
| **A-13** | **`content_scripts.matches` 범위** (rev.2 신규) | `["http://*/*", "https://*/*"]` 권고, `<all_urls>`가 대안. **도메인 축소는 하지 않는다.** `all_frames`는 기본값 `false` 유지 | **evaluator 판정** (§2.9) |
| **A-14** | **manifest_version** (rev.2 신규) | **MV2 권고**(영속 background로 중계 수명주기 문제 없음). MV3 선택 시 이벤트 페이지 종료 후 첫 메시지 지연/유실 검증 필요 | **plan 단계 결정** (§2.11) |
| **A-15** | **페이지 세계 노출 방식** (rev.2 신규) | **(가) `web_accessible_resources` + 페이지 세계 스크립트 주입** 권고. (나) `exportFunction`은 Xray 처리 복잡도로 차선 | **evaluator 판정** (§2.6.1) |

### 5.1 A-08 결정의 보안 트레이드오프 — 명문화 (rev.2 신설)

**사용자는 "외부 사이트에서도 브리지 호출 가능"을 명시적으로 선택했다.** 이 문서는 그 결정을 되돌리려 하지 않는다. 다만 **결정의 귀결을 정확히 기록**해 둔다 — 나중에 "왜 브리지가 전 사이트에 열려 있지?"라는 질문이 나왔을 때의 근거다.

**귀결:**
1. `content_scripts.matches` 범위 안의 **임의의 웹사이트**가 `appFinish`를 호출해 **앱을 종료시킬 수 있다.**
2. 동일하게 `getVersionName`으로 **앱 버전 정보를 얻을 수 있다**(취약 버전 식별에 쓰일 수 있는 정보).
3. `http://` 사이트는 전송 구간이 평문이므로 **중간자(MITM)가 주입한 스크립트도** 위 두 가지를 할 수 있다. REQ-008이 `http://naver.com`을 명시하므로 이 경로는 실제로 존재한다.
4. 앞으로 브리지에 함수를 추가하면 **추가되는 즉시 모든 웹사이트에 열린다.** 이것이 가장 큰 장기 리스크다.

**요구를 좁히지 않는 완화책 (기본 적용 권고):**
- **호출 가능 함수 화이트리스트를 `background.js` 한 곳에서 관리**하고, 목록에 없는 이름은 reject한다. AC-006-2가 이미 이 동작을 요구하고 있으므로 **추가 비용이 없다.** 함수 추가 지점이 한 곳으로 모여 위 4번의 검토 트리거가 된다.
- **`all_frames: false`**(manifest 기본값) 유지 → 서드파티 광고 iframe은 제외. 사용자가 말한 "외부 사이트"는 방문한 사이트를 뜻하므로 요구 축소가 아니다(§2.9).
- **브리지 함수 추가 시 재검토 필수**를 `background.js` 화이트리스트 위 주석으로 못 박는다.

**별도 P2 제안 — 기본 비활성, evaluator 판정 (사용자가 요구하지 않았으므로 임의로 넣지 않는다):**
- 오리진 허용목록(allowlist) 스위치: `background.js`에 오리진 검사 훅 자리만 만들어 두고 **기본은 "전체 허용"**(=사용자 결정 그대로)으로 둔다. 나중에 민감 기능이 추가될 때 한 줄로 켤 수 있다.
- **이 제안을 채택하더라도 기본 동작은 REQ-010을 그대로 충족해야 한다.** 훅을 켠 상태로 출고하면 사용자 결정을 뒤집는 것이므로 금지.

---

## 6. 스코프 아웃

원문에 없고 사용자가 요청하지도 않아 이번 작업에서 제외한다.

| 항목 | 근거 |
|:--|:--|
| ~~뒤로가기~~ | **rev.2에서 제거 — REQ-011로 승격됨 (사용자 A-10 결정)** |
| 앞으로 가기, 새로고침, 주소 입력창 | 원문에 없음. 뒤로가기만 요청됨 |
| 탭/멀티 세션, 세션 상태 저장·복원(회전/프로세스 사망) | 원문에 없음. **화면 회전 시 페이지가 리로드될 수 있음** — 알려진 한계로 기록 |
| 다운로드, 파일 업로드, 권한 프롬프트(카메라/마이크/위치) 처리 | 원문에 없음. `PermissionDelegate` 미구현 시 해당 기능이 조용히 실패한다 |
| 팝업/새 창(`onNewSession`), 외부 앱 인텐트 처리 | 원문에 없음. naver.com에서 새 창 링크를 누르면 아무 일도 안 일어날 수 있음 |
| 오류 페이지 커스터마이징, 오프라인 대응 | 원문에 없음 |
| release 서명, ProGuard/R8, AAB 구성 | 원문에 없음. **release 빌드는 이번에 만들지 않는다** |
| 다국어(i18n), 다크 모드, 태블릿 대응 | 원문에 없음 |
| naver.com 이외 URL 이동 UI | 원문은 naver.com 버튼 1개만 요구. (단 AC-010-4의 제2 사이트는 **검증용**이며 UI 버튼이 아니어도 된다) |
| 브리지 호출 인증·서명, 오리진 허용목록의 **기본 활성화** | §5.1 — 사용자가 요구하지 않았고, 기본 활성화하면 A-08 결정을 뒤집는다 |

---

## 7. Impact Analyzer 전달 사항

### 7.1 특수 상황: 신규 프로젝트

**추적할 기존 코드가 없다.** "영향 범위 분석"이 아니라 **생성할 파일 목록을 화이트리스트로 확정**하는 작업이다. 패키지는 `com.example.geckoviewtest`로 **확정**되었다(A-02).

```
settings.gradle.kts, build.gradle.kts, gradle.properties
gradle/libs.versions.toml, gradle/wrapper/**, gradlew, gradlew.bat
app/build.gradle.kts, app/proguard-rules.pro
app/src/main/AndroidManifest.xml
app/src/main/java/com/example/geckoviewtest/**
app/src/main/res/**
app/src/main/assets/**            ← WebExtension 일체 (manifest.json, background.js, content.js, page-bridge.js, index.html, bridge-client.js)
app/src/test/**                   ← QA용, scope-guard 규칙 3에 따라 처음부터 포함
app/src/androidTest/**            ← 동일
```

**주의:** `app/src/main/assets/**`는 **웹 자산(JS/HTML)** 이고 나머지는 Kotlin이다. 두 세계가 **하나의 메시지 스키마로 결합**되어 있으므로 한쪽만 수정 허용되면 브리지가 반드시 깨진다. **rev.2에서는 자산 쪽 파일이 2개(background.js, index.html)에서 5개로 늘었다** — `risk_notes`에 명시할 것.

### 7.2 예상 구성요소 (rev.2 갱신 — 신규 항목에 ★)

| 구성요소 | 역할 | 관련 REQ |
|:--|:--|:--|
| `App`(Application) | `GeckoRuntime` 싱글턴 생성 — **프로세스당 1회 제약**. 수동 DI 컨테이너 | 001 |
| `MainActivity` | XML 바인딩, `GeckoSession` 소유, ViewModel 관찰, `finish()` 수신, ★`OnBackPressedCallback` 등록 | 001,002,005,007,008,009,★011 |
| `activity_main.xml` | `GeckoView` + 로딩 UI + naver 버튼 | 002,008,009 |
| `MainViewModel` | `MainUiState`(StateFlow) + 일회성 이벤트 Channel. ★`canGoBack` 상태 | 004,005,008,009,★011 |
| `AppInfoRepository` (+`Impl`) | `PackageManager` 격리 — ViewModel에서 Context를 몰아내는 장치 | 004 |
| `NativeBridgeHandler` | `WebExtension.MessageDelegate` 구현. 메시지 → 함수 디스패치 → `GeckoResult` 응답 | 003,004,005,006 |
| `BridgeProtocol` (순수 Kotlin) | 요청/응답 스키마 + 미지원 함수명 오류. **JVM 테스트로 커버리지를 버는 핵심 지점** | 006 |
| ★`AppNavigationDelegate` | `NavigationDelegate` 구현 — `onCanGoBack`/`onLocationChange`를 ViewModel로 전달 | ★011,008 |
| `GeckoResultExt.kt` | `GeckoResult<T>` ↔ 코루틴 어댑터 | 003 |
| `assets/messaging/manifest.json` | `geckoViewAddons`+`nativeMessaging` 권한(★`nativeMessagingFromContent` 제외), ★`content_scripts`, ★`web_accessible_resources`, `id`/`version` | 003,007,★010 |
| `assets/messaging/background.js` | **두 클라이언트 경로의 합류점.** 함수 화이트리스트 관리 + `sendNativeMessage` 중계 | 003,006,★010 |
| ★`assets/messaging/content.js` | content script(격리 세계). 페이지 세계 스크립트를 `<script>`로 주입하고 `window.postMessage` ↔ `runtime.sendMessage` 중계 | ★010 |
| ★`assets/messaging/page-bridge.js` | **페이지 세계**에서 실행. `window.NativeBridge` 정의 + 검증용 프로브 배지. `web_accessible_resources` 등록 필수 | ★010,006 |
| `assets/messaging/index.html` | 테스트 UI(버튼 2개 + 결과 영역). 확장 페이지 | 006,007 |
| ★`assets/messaging/bridge-client.js` | 파사드 `window.NativeBridge.call()` — 확장 페이지용 전송 어댑터(`runtime.sendMessage`) | 006,007 |

### 7.3 REQ별 작업 성격

| REQ | 성격 |
|:--|:--|
| 001,002 | 빌드 설정 + 스캐폴딩 |
| 003,006 | **Kotlin↔JS 프로토콜 설계.** 양쪽 동시 수정 필수. 회귀 위험 최고 |
| 004 | 로직 + Context 격리. **JVM 단위 테스트 가능** |
| 005 | 일회성 이벤트 전달. **ViewModel 이벤트 방출까지 JVM 테스트 가능**, `finish()` 실행은 계측/수동 |
| 007,008 | UI 배선 + 네비게이션. 수동/계측 검증 |
| 009 | 상태 관리(로딩) — **UiState 위반이 나오기 가장 쉬운 지점** |
| **010** | **순수 JS 작업(content.js + page-bridge.js) + manifest.** Kotlin 변경 거의 없음. **JVM 테스트로 커버 불가 — 전량 실기기 검증.** 이 문서에서 **미확인 요소가 가장 많은 REQ** |
| **011** | Delegate → ViewModel → Activity 배선. **`canGoBack` 상태 전이는 JVM 테스트 가능**, 실제 복귀는 실기기 |

### 7.4 다음 단계 경고 (rev.2 갱신 — 우선순위 순)

**［검증 순서 지정］developer는 아래 1·2를 다른 어떤 기능보다 먼저 실기기로 확인하고 결과를 로그에 남긴 뒤 나머지에 착수할 것.** 여기서 막히면 REQ 다수가 동시에 흔들리는데, 마지막에 발견하면 되돌릴 여지가 없다.

1. **［최우선］외부 사이트 브리지 경로의 실기기 스파이크.** `content.js` 주입 → `page-bridge.js`가 **페이지 세계**에서 실행 → `window.postMessage` → `runtime.sendMessage` → `background.js` → `sendNativeMessage` 5단 왕복이 naver.com에서 실제로 도는지. **§2.6.1의 페이지 세계 격리가 이 경로의 유일한 실패 지점이며 rev.1에는 없던 제약이다.** 최소 구현(버전 문자열 하나 왕복)으로 먼저 뚫을 것.
   - **주의:** content script의 격리 세계에서만 테스트하면 통과한다. **AC-010-3(PAGE_WORLD 마커)까지 확인해야 스파이크가 끝난 것이다.**
2. **［최우선］확장 페이지 로드와 히스토리 잔존.** `extension.metaData.baseUrl + "index.html"` 최상위 로드가 되는지(§2.7.2 [미확인]), 그리고 naver 이동 후 뒤로가기로 **되돌아오는지**(REQ-011 [미확인]). 둘 다 실패하면 §2.7.4 폴백으로 전환해야 하며 파일 구성이 바뀐다.
3. **`resource://android/assets/index.html`로 페이지를 열려는 시도를 막아라.** 결정적 근거는 크래시(#199)가 아니라 **매치 패턴 제약**(§2.7.1). plan.md와 코드 주석 양쪽에 남길 것.
4. **`abiFilters` 없이 빌드하지 마라.** APK 200MB+ → 설치 수 분. plan.md 리스크 항목 필수.
5. **#220은 회피되지만 회피 구조를 주석으로 고정하라.** content script에서 `sendNativeMessage`를 직접 부르면 결함 경로에 들어간다(§2.10). "한 단계 줄이자"는 리팩터링이 곧바로 버그다.
6. **`GeckoRuntime`은 프로세스당 1회.** Activity에서 만들면 회전 시 크래시 가능. Application 스코프 필수.
7. **`nativeApp` 문자열 일치**(`setMessageDelegate(d,"browser")` ↔ `sendNativeMessage("browser",…)`). 불일치 시 오류 없이 조용히 실패한다.
8. **`consoleOutput(true)`를 debug에 켜라.** rev.2에서 JS 실행 세계가 3곳(페이지/격리/background)으로 늘었다. 안 켜면 어디서 죽었는지 분리 불가.
9. **커버리지 70%는 설계 종속이다.** `BridgeProtocol`·`AppInfoRepository`·`MainViewModel`(로딩·canGoBack 상태 전이)을 프레임워크 비의존으로 설계해야 성립한다. **REQ-010은 순수 JS라 JaCoCo에 전혀 기여하지 않으므로**, rev.2에서 작업량은 늘었는데 커버리지 분모는 거의 안 늘어난다 — coverage-reporter가 이를 회귀로 오판하지 말 것.
10. **QA 시퀀스 주의:** AC-010-5(naver.com에서 `appFinish`)는 앱을 죽인다. **반드시 맨 마지막에 실행**할 것.
11. **두 전송 경로의 결과를 서로의 근거로 쓰지 마라.** index.html 통과가 외부 사이트를 보장하지 않는다(§2.7.3). AC-006-4가 이를 명시한다.
12. **AGP/Gradle wrapper 조합 미확정.** compileSdk 36 + JDK 21 전제. 시스템 gradle 9.3.1 존재가 wrapper를 9로 맞추라는 뜻은 아니다 — AGP를 먼저 고르고 wrapper를 생성할 것.
13. **화면 회전 시 페이지 리로드 가능성**(스코프 아웃, 알려진 한계). REQ-011 검증 중 회전이 섞이면 원인 혼동 가능.

### 7.5 빌드 환경 (rev.1에서 재측정 — verification-honesty V7)

| 항목 | 실측값 |
|:--|:--|
| JAVA_HOME | `/Applications/Android Studio.app/Contents/jbr/Contents/Home` → **OpenJDK 21.0.10** |
| PATH 기본 java | 1.8.0_333 — **AGP 실행 불가.** 모든 gradle 호출에 위 JAVA_HOME 지정 필수 |
| ANDROID_HOME | `/Users/appdevloperteam/Library/Android/sdk` |
| platforms | 19, 28, 29, 30, 31, 33, 34, 35, 36, 36.1, 37.0 (**android-36 존재 → compileSdk 36 가능**) |
| build-tools | 28.0.3 ~ 36.0.0 (33.0.1, 33.0.2, 34.0.0, 35.0.0, 36.0.0 포함) |
| 시스템 gradle | `/opt/homebrew/bin/gradle` — **9.3.1**. 프로젝트는 **wrapper 사용 권장** |
| 실기기 | `R3CN60L0QMT` = **SM-G981N**, `ro.build.version.sdk=33`, `ro.product.cpu.abi=arm64-v8a` |
| adb | `/Users/appdevloperteam/Library/Android/sdk/platform-tools/adb` |

**주의:** 실기기가 API 33인데 targetSdk는 36이다. targetSdk 36의 동작 변경 중 API 33 기기에서 재현되지 않는 것이 있을 수 있다 **[미확인]** — 이번 스코프에서는 문제되지 않을 것으로 보나 기록해 둔다.

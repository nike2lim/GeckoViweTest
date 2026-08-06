# planner-analyzer 작업 로그 (rev.2 개정)

- **일시**: 2026-08-04 13:55:06 (KST)
- **작업**: 사용자 확인 답변 4건(A-02/A-03, A-05, A-08, A-10)을 반영해 `pipeline/requirements.md`를 rev.2로 개정
- **입력**:
  - `pipeline/requirements.md` rev.1 (본 에이전트의 직전 실행 산출물)
  - 오케스트레이터(team-lead) 전달 사용자 답변 4건
  - `/Users/appdevloperteam/.claude/plugins/cache/android-pipeline-marketplace/android-pipeline/1.0.0/rules/{architecture,comment-style,scope-guard,verification-honesty,work-logging}.md` (rev.1 실행에서 이미 반영)
- **산출물**:
  - `/Users/appdevloperteam/Documents/ClaudeWorkspace/GeckoViewTest/pipeline/requirements.md` (**개정** — rev.2)
  - `/Users/appdevloperteam/Documents/ClaudeWorkspace/GeckoViewTest/doc/20260804/20260804_135506_planner-analyzer.md` (본 문서, 신규)
- **직전 로그**: `doc/20260804/20260804_133952_planner-analyzer.md` (rev.1) — 수정하지 않았다(work-logging.md "실행 1회당 새 파일 1개")

## 수행 내용

### 1. 개정 이력 신설
문서 상단에 개정 이력 표를 넣고 **"rev.1과 rev.2는 브리지 노출 범위가 정반대"** 임을 impact-analyzer용 경고로 명시했다. 항목별로 rev.1 값 / rev.2 값 / 변경 사유를 나란히 적어 구판 기준 산출물을 식별할 수 있게 했다.

### 2. 사용자 답변 4건 반영
- **A-02/A-03 확정** — 패키지 `com.example.geckoviewtest`, 앱명 `GeckoViewTest`, minSdk 26, target/compileSdk 36. §5 표에서 "확인 권장" → "rev.2 확정"으로 상태 전환하고, AC-001-2·AC-004-2·§7.1의 경로/명령에 실제 패키지명을 반영했다.
- **A-05 확정** — L6은 L3의 상위 표현. REQ-006 서술에 "내장 index.html과 외부 사이트가 동일한 계약을 사용"을 추가했다.
- **A-10 → REQ-011 신규** — §6 스코프 아웃 표에서 뒤로가기 행을 제거(취소선 + 승격 사유 표기)하고 REQ-011을 신설했다.
- **A-08 반전** — REQ-010을 삭제하지 않고 **반전**시켰다(P2 차단 → P1 허용). 이력에 반전 사실과 사유를 남겨 "왜 브리지가 전 사이트에 열려 있지?"라는 후속 질문의 근거가 되게 했다.

### 3. A-08 반전에 따른 재조사 — 이번 개정의 실질
A-08이 반전되면 content script가 우회 대상에서 **필수 경로**로 바뀐다. 오케스트레이터는 이를 근거로 "#220이 정면 리스크가 된다"고 지시했으나, 인계값을 그대로 옮기지 않고(V7) 이슈 본문을 직접 확인했다. 그 결과 **두 건의 정정과 한 건의 신규 제약**이 나왔다.

**정정 ① — #220의 적용 범위 (rev.1의 과잉 경고를 축소)**
이슈 본문 확인 결과 깨지는 것은 `content script가 chrome.runtime.sendNativeMessage를 직접 호출`하는 경우에 한정되고, 보고자 스스로 "background script에서는 동작한다"고 적었다. 본 설계는 content script → `runtime.sendMessage` → background.js → `sendNativeMessage` 이므로 **결함 경로를 지나지 않는다.** 요구사항 원문이 "background.js를 사용한 통신"을 명시한 덕분에 우연히 회피된 구조여서, 이 사실을 `background.js` 주석으로 고정하라는 지시를 §2.10과 §4·§7.4에 넣었다("한 단계 줄이자"는 리팩터링이 곧바로 버그가 되는 지점).

**정정 ② — `nativeMessagingFromContent` 권한 불필요**
rev.1은 세 권한을 나란히 나열했으나, 이 권한은 content script가 네이티브 API를 직접 부를 때만 필요하다. 본 설계에서는 선언하지 않기로 하고 §2.6 권한 표에 "불필요 — 선언하지 않는다"를 사유와 함께 명시했다. 불필요한 권한이 있으면 실제 통신 경로를 오독하게 만든다.

**신규 제약 — 페이지 세계 ↔ content script 격리 (§2.6.1 신설, rev.1에 없던 내용)**
MDN·Firefox Script Security 확인 결과, content script는 페이지와 **JS 스코프가 격리**되어 있고 Firefox는 **Xray vision**을 적용한다. 따라서 content script에서 `window.NativeBridge = {...}`로 대입해도 **페이지 세계 JS는 그것을 보지 못한다** — `exportFunction()`이 존재하는 이유가 바로 이것이다. 이것은 REQ-006("웹 페이지 JS에서 호출 가능한 브리지 함수")을 외부 사이트에 제공하는 방식을 결정하는 제약이며, A-08이 반전되기 전에는 관련성이 없었다. 세 가지 방식((가) `web_accessible_resources` + 페이지 세계 스크립트 주입 / (나) `exportFunction`·`cloneInto` / (다) 페이지가 먼저 `postMessage`)을 비교해 (가)를 권고하고 A-15로 evaluator 판정에 넘겼다.

### 4. index.html 오리진 재판정 (§2.7.3 신설)
지시대로 A-04를 재검토했다. **결론은 확장 페이지 방식 유지**이며, 근거를 네 가지로 적었다. 핵심은 **리스크 분산**이다 — 확장 페이지 경로는 content script·페이지 세계 격리·#220과 완전히 독립이므로 외부 사이트 경로가 막혀도 REQ-004·005·007이 살아남는다. 통일하면 단일 경로가 막히는 순간 REQ 7개가 동시에 죽는다. 또한 통일안(file:// / localhost)은 "간단해지는" 안이 아니라 파일 복사 또는 서버라는 새 작업이 추가되는 안이라는 점도 적었다(`resource://` 배제 근거는 여전히 유효하므로 assets에서 직접 열 수는 없다).

통일은 전송 계층이 아니라 **계약 계층**에서 확보하기로 했다: 동일 파사드(`window.NativeBridge.call`) + 동일 wire 스키마 + 전송 어댑터만 2종. AC-003-3에 "background.js의 처리 분기가 송신자 종류에 따라 갈라지지 않는다"를 넣어 이를 강제했다.

**대가를 숨기지 않았다**: 전송 경로가 2종이므로 index.html에서만 테스트하면 외부 사이트에 대한 거짓 그린이 된다. AC-006-4에 "AC-010이 실패하면 REQ-006도 미충족"을 못 박고, §7.4에 "두 경로의 결과를 서로의 근거로 쓰지 마라"를 넣었다.

### 5. REQ-010 수용 기준 설계 — 거짓 그린 방지가 핵심
오케스트레이터가 "여기서 대리 신호를 쓰면 최악의 거짓 그린"이라 지적한 지점이다. §2.6.1의 격리 구조상 **content script의 격리 세계 안에서만 브리지가 동작해도 겉보기에는 성공으로 보인다.** 이를 잡기 위해 검증 장치를 설계했다:

페이지 세계 프로브 스크립트가 ① 페이지 세계에서 `window.NativeBridge.call('getVersionName')`을 호출하고 ② 결과를 페이지 DOM의 고정 id 배지에 쓰며 ③ `window.__bridgeProbeRanInPageWorld = true`를 세팅하면, content script가 `window.wrappedJSObject.__bridgeProbeRanInPageWorld`를 읽어 true일 때만 배지에 `PAGE_WORLD` 마커를 붙인다.

- AC-010-1(배지에 값 표시) + AC-010-2(dumpsys versionName과 문자 일치) + **AC-010-3(PAGE_WORLD 마커)** 를 AND로 묶었다. 마커 조건이 없으면 1·2가 모두 참인데도 실제 웹페이지는 브리지를 못 쓰는 상태를 통과시킨다.
- AC-010-4로 naver 외 제2 사이트를 요구해 `matches` 도메인 하드코딩을 배제했다.
- AC-010-6으로 "브리지 주입이 naver 렌더링을 깨지 않을 것"을 회귀 조건으로 추가했다.

### 6. REQ-011 수용 기준
"`goBack()`이 호출됨"을 배제하고 관측 가능한 조건으로 구성했다. AC-011-2(복귀 후 `getVersionName`이 **다시** 동작)가 핵심이다 — 페이지만 그려지고 확장 메시징이 죽은 상태는 실제로 깨질 수 있는 지점이고, AC-011-1(렌더)만으로는 잡히지 않는다. AC-011-4로 "히스토리 있음/없음 두 케이스가 모두 관측될 것"을 요구해 콜백이 항상 켜져 있거나 항상 꺼져 있는 구현을 배제했다.

### 7. API 시그니처 재확인 (V7)
rev.2에서 새로 인용하는 API는 추측하지 않고 Javadoc에서 확인했다: `NavigationDelegate.onCanGoBack(@NonNull GeckoSession, boolean)`, `onCanGoForward`, `onLocationChange(session, url, perms, hasUserGesture)`, `onLoadError`, `@AnyThread public void goBack()` (및 `goBack(boolean userInteraction)` 오버로드), `@UiThread @NonNull WebExtension.SessionController getWebExtensionController()`. 전부 [확인]으로 표기했다. 반대로 `HttpsOnlyMode` 상수 전체 이름, MV2/MV3 중 GeckoView 예제가 쓰는 버전, `exportFunction`의 Promise 처리 동작은 확인하지 못해 [미확인]으로 남겼다.

### 8. 부수 반영
- §2.4에 `androidx.activity` 추가 필요를 기록(REQ-011의 `OnBackPressedDispatcher`는 GeckoView가 끌고 오지 않는다).
- §2.9 신설 — `content_scripts.matches` 범위. `["http://*/*","https://*/*"]` 권고, `<all_urls>` 대안. **도메인 축소는 하지 않음**을 명시하고 A-13으로 evaluator 판정에 넘겼다. `all_frames: false`(기본값) 유지도 함께.
- §2.11 신설 — manifest_version 선택(A-14, MV2 권고).
- §3 아키텍처 표에 뒤로가기 상태 관리 행 추가. `OnBackPressedCallback`은 androidx 타입이라 ViewModel이 직접 들면 안 된다는 쟁점을 evaluator 판정으로 남겼다.
- §4 주석 규칙에 **반드시 남겨야 할 non-obvious WHY 3건**을 열거(resource:// 배제 / content script 직접 native 호출 금지 / window.NativeBridge 직접 대입 불가). 셋 다 없으면 다음 개발자가 "단순화"하며 되돌린다.
- §7.2에 신규 파일 4개(`content.js`, `page-bridge.js`, `bridge-client.js`, `AppNavigationDelegate`)를 ★로 표시해 추가. 자산 파일이 2개 → 5개로 늘었음을 §7.1 risk_notes 지시에 반영.
- §7.4를 우선순위 순으로 재배열하고 **1·2번은 다른 기능보다 먼저 실기기 스파이크로 확인 후 나머지 착수**라는 순서 지정을 넣었다.
- 개정 시각을 처음에 추정값(14:02:31)으로 적었다가 `date` 실행 결과(13:55:06)로 정정했다. work-logging.md의 "타임스탬프는 반드시 `date` 실행 결과 사용" 규칙 위반이었다.

## 결정 사항

| 지점 | 선택 | 이유 |
|:--|:--|:--|
| REQ-010 처리 | 삭제가 아닌 **반전**, 이력에 사유 보존 | 지시대로. 나중에 보안 질문이 나왔을 때 "누가 언제 왜 결정했는가"가 남아야 함 |
| index.html 오리진 | **확장 페이지 유지**(재판정 후) | 리스크 분산이 결정적. 통일하면 단일 실패점이 REQ 7개를 동시에 죽인다 |
| 통일 방식 | 전송 계층이 아닌 **계약 계층**에서 통일 | 파사드+wire 스키마 동일, 어댑터만 2종. AC-003-3이 강제 |
| #220 경고 | rev.1의 경고를 **축소 정정**하되 검증 우선순위는 유지 | 이슈 본문상 결함은 content script 직접 호출 한정. 다만 경로 전체가 미검증인 것은 사실 |
| `nativeMessagingFromContent` | **선언하지 않음** | 본 설계에 불필요. 있으면 통신 경로를 오독하게 만든다 |
| 페이지 세계 노출 방식 | **(가) web_accessible_resources + 스크립트 주입** 권고 | 표준 기법이고 Xray 우회 트릭이 없어 동작이 예측 가능. `exportFunction`은 Promise/객체의 Xray 처리가 [미확인] |
| `matches` 범위 | `["http://*/*","https://*/*"]` 권고, `<all_urls>` 병기 | 도메인은 하나도 제외하지 않으므로 **축소가 아니라 스킴 정리**. 판정은 evaluator |
| `all_frames` | 기본값 `false` 유지 | 서드파티 광고 iframe은 사용자가 말한 "외부 사이트"가 아님. 요구 축소가 아님 |
| 보안 완화책 | 함수 화이트리스트 + `all_frames:false`만 기본 적용 | 둘 다 요구를 좁히지 않고 추가 비용이 없다(화이트리스트는 AC-006-2가 이미 요구) |
| 오리진 허용목록 | **별도 P2 제안으로 분리, 기본 비활성** | 사용자가 요구하지 않았다. 지시대로 임의로 넣지 않고 evaluator 판정으로 넘김. "훅을 켠 채 출고 금지"를 명시 |
| REQ-010 검증 장치 | 페이지 세계 프로브 + `PAGE_WORLD` 마커 | 격리 세계 통과를 페이지 세계 성공으로 오판하는 것이 이 REQ의 유일한 거짓 그린 경로 |

## 이슈 / 리스크

**A-08 결정으로 새로 생긴 리스크**

1. **［보안, 사용자 수용됨］임의 웹사이트가 `appFinish`로 앱을 종료시키고 `getVersionName`으로 버전을 얻을 수 있다.** `http://naver.com`이 요구사항이므로 **MITM이 주입한 스크립트도 동일한 권한을 갖는다.** 장기적으로 가장 큰 리스크는 "앞으로 추가되는 브리지 함수가 추가되는 즉시 전 웹사이트에 열린다"는 점이다. §5.1에 명문화했다. 되돌리자고 설득하지 않았다.
2. **［기술, 최우선 검증］페이지 세계 격리(§2.6.1).** rev.1에는 존재하지 않던 제약이며 REQ-010의 유일한 실패 지점이다. content script 격리 세계에서만 동작하는 상태는 겉보기에 성공이라 **검증 설계가 없으면 반드시 놓친다.**
3. **［기술, 최우선 검증］5단 통신 경로 전체가 미검증.** `runtime.sendMessage` 왕복, `web_accessible_resources` 주입, `window.postMessage` 중계 모두 [미확인]. #220이 회피된다고 해서 경로가 검증된 것은 아니다.
4. **［검증］전송 경로 2종으로 인한 거짓 그린 위험.** index.html 통과가 외부 사이트를 보장하지 않는다. AC-006-4·§7.4-11로 방어했으나, QA가 이를 무시하고 한쪽만 돌리면 그대로 뚫린다.
5. **［커버리지］REQ-010은 순수 JS라 JaCoCo에 전혀 기여하지 않는다.** rev.2에서 작업량은 늘었는데 커버리지 분모는 거의 안 늘어난다. coverage-reporter가 이를 회귀나 테스트 부실로 오판하지 않도록 §7.4-9에 적었다.
6. **［QA 절차］AC-010-5(naver.com에서 `appFinish`)는 앱을 죽인다.** 시퀀스 맨 마지막에 실행해야 한다. 중간에 넣으면 이후 케이스가 무더기로 실패하고 원인 분리가 어려워진다.

**REQ-011로 새로 생긴 리스크**

7. **확장 페이지가 세션 히스토리에 남는지 [미확인].** 남지 않으면 AC-011-1이 원리적으로 불가능하고 §2.7.4 폴백으로 전환해야 하며 파일 구성이 바뀐다. §7.4-2에 최우선 검증으로 지정했다.
8. **`OnBackPressedCallback`의 소유 레이어가 미결.** androidx.activity 타입이라 ViewModel이 직접 들면 architecture.md 위반이다. §3에 쟁점으로 남겨 evaluator 판정에 넘겼다.

**rev.1에서 이월된 리스크 (해소되지 않음)**: `resource://` 배제 근거 유지, `abiFilters` 미적용 시 APK 200MB+, `GeckoRuntime` 프로세스당 1회, `nativeApp` 문자열 불일치의 무성 실패, AGP/wrapper 조합 미확정, 회전 시 페이지 리로드 가능성.

**차단당한 시도**: 없음.

**오케스트레이터 지시 중 사실관계를 정정한 것 1건**: "#220이 정면 리스크로 바뀐다"는 지시는 이슈 본문 확인 결과 부분적으로만 성립한다(결함은 content script 직접 native 호출 한정, 본 설계는 해당 없음). 다만 "content script 경로를 최우선 실기기 검증 대상으로 지정하라"는 지시의 결론 자체는 타당하므로 그대로 반영했다.

**미확인으로 남긴 것**(추측으로 채우지 않음): 페이지 세계 주입·`window.postMessage` 중계의 실제 동작, `moz-extension://` 최상위 네비게이션 허용 여부와 `web_accessible_resources` 필수 여부, 확장 페이지의 히스토리 잔존, `exportFunction`의 Promise/객체 Xray 처리, MV2/MV3 중 GeckoView 예제가 쓰는 버전, `HttpsOnlyMode` 상수 전체 이름, compileSdk 정확한 하한, abiFilters 적용 시 실제 APK 크기, Gecko가 Android network security config를 따르는지, `file://` content script 주입에 파일 접근 허용이 필요한지, targetSdk 36의 동작 변경이 API 33 기기에서 재현되는지.

## 판정

**PASS** — `pipeline/requirements.md` rev.2 개정 완료. 사용자 결정 4건을 모두 반영했고, A-08 반전은 설득 시도 없이 수용하되 트레이드오프를 §5.1에 명문화했다. 반전의 파급으로 새로 관련성이 생긴 제약(페이지 세계 격리)을 직접 조사해 §2.6.1로 신설했고, 그 과정에서 rev.1의 서술 2건(#220 경고 범위, `nativeMessagingFromContent` 필요성)을 근거와 함께 정정했다. REQ-010·011의 수용 기준은 대리 신호를 배제하고 격리 세계 통과를 페이지 세계 성공으로 오판하지 않도록 `PAGE_WORLD` 마커 조건을 넣었다. 개정 시각은 `date` 실행 결과로 정정했다.

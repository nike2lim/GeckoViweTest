# planner-analyzer 작업 로그 (rev.3 개정)

- **일시**: 2026-08-05 13:40:39 (KST)
- **작업**: code-reviewer 판정에 따라 `pipeline/requirements.md`의 수용 기준 2건(AC-011-3, AC-002-2) 문언을 실측에 맞게 정정 — rev.3
- **입력**:
  - `pipeline/review.md` §1(①·②), §8, §9 — code-reviewer 판정문
  - `doc/20260805/20260805_133319_code-reviewer.md` — code-reviewer 작업 로그
  - `pipeline/requirements.md` rev.2 (본 에이전트의 직전 산출물)
  - 오케스트레이터 전달 정정 지시
- **산출물**:
  - `/Users/appdevloperteam/Documents/ClaudeWorkspace/GeckoViewTest/pipeline/requirements.md` (**개정** — rev.3)
  - `/Users/appdevloperteam/Documents/ClaudeWorkspace/GeckoViewTest/doc/20260805/20260805_134039_planner-analyzer.md` (본 문서, 신규)
- **기존 로그 미수정**: `20260804_133952_planner-analyzer.md`(rev.1), `20260804_135506_planner-analyzer.md`(rev.2) 손대지 않음 (work-logging.md "실행 1회당 새 파일 1개")

## 수행 내용

### 0. 이 개정의 성격

**요구사항이 바뀐 것이 아니다.** REQ-001~011은 rev.2와 동일하다. 구현·검증이 끝난 뒤 **수용 기준 2건의 문언이 실제 플랫폼·빌드 현실을 반영하지 못했음**이 드러나 기준 쪽을 바로잡은 것이다. 두 건 모두 code-reviewer가 **"수용"(구현 변경 불요)** 으로 판정했고 설계·의존성 변경이 없어 **evaluator 재승인은 불요**다. 이 성격을 개정 이력 첫 문단에 명시했다.

### 1. 인용 전 재확인 (V7)

오케스트레이터가 "리뷰어 관측값을 그대로 옮겨 적지 말라"고 지시했고, 이는 rev.2 개정 때 내가 지적받았던 지점이다. **재확인 가능한 것과 불가능한 것을 나눠 처리했다.**

**직접 재측정한 것 [확인]**
- `dexdump`(build-tools 36.0.0)로 `app-debug.apk`(198,619,378 B)의 dex 7개를 전수 조사했다. 결과: `androidx/compose` 정의 클래스 **정확히 6개** — `runtime/Immutable`, `runtime/Stable`, `runtime/StableMarker`, `runtime/annotation/FrequentlyChangingValue`, `runtime/annotation/RememberInComposition`, `runtime/annotation/R`. 별도로 `androidx/compose/(ui|foundation|material)/` 패턴을 검색해 **0건**임을 확인했다. **리뷰어 보고와 클래스 목록까지 정확히 일치한다.**
- `app/build.gradle.kts` — `minSdk = 26`(24~26행에 GeckoView AAR 하한이라는 근거 주석 포함), `targetSdk = 36`, `compileSdk = 36`, `versionName = "1.0.0"`, 75행에 `buildFeatures.compose` 미설정이 주석과 함께 확인됨.
- 기기 `ro.build.version.sdk = 33`.

**문서로 확인한 것 [문서] — 리뷰어가 근거로 든 플랫폼 동작을 1차 출처에서 확인**
- Android 12(API 31) "Behavior changes: all apps" — 루트 런처 Activity의 뒤로가기 기본 동작이 `finish()`에서 태스크 백그라운드 이동으로 변경. 여기서 **리뷰어 판정문에 없던 두 가지를 추가로 확인**했고 정정 문언에 반영했다:
  1. **적용 대상이 `ACTION_MAIN` + `CATEGORY_LAUNCHER`를 선언한 루트 Activity로 한정**된다. 그 외 Activity는 종전대로 `finish()`된다.
  2. **이 변경은 targetSdk가 아니라 기기 OS 버전으로 적용**된다("all apps"). → 그래서 AC-011-3 (d)를 "targetSdk"가 아니라 **"기기의 `ro.build.version.sdk`를 기록하라"** 로 못 박았다. 이 구분이 없으면 QA가 targetSdk 36만 보고 갈래를 잘못 판정할 수 있다.
  3. 같은 문서가 커스텀 뒤로가기에 **AndroidX Activity API 사용을 권장**하며 "가로채는 컴포넌트가 없으면 시스템 기본 동작으로 자동 위임된다"고 명시한다. 현 구현의 `OnBackPressedCallback.isEnabled = false`가 정확히 그 경로다 — **구현이 플랫폼 권장을 따르고 있다는 근거**이므로 정정 문언에 넣었다.

**재현하지 않고 출처를 명시해 인용한 것**
- 실기기 조작 관측값(실험 A `state=STOPPED finishing=false` / B `state=RESUMED` / C ActivityRecord 소멸)은 **code-reviewer 실측(review.md §1①, SM-G981N / API 33)** 으로 출처를 박아 인용했다. 재현하지 않은 이유는 두 가지다: ① 리뷰어가 기기 상태를 정리해 반납했고(앱 설치·정상 종료·비행기모드 해제) 요구사항 문서 작성을 위해 그 상태를 흐트러뜨릴 이유가 없다 ② 실기기 재현은 QA(6단계)의 역할이며, 정정된 기준 자체가 QA가 재현할 판정 절차를 담고 있다. **"planner-analyzer는 기기를 조작하지 않았다"를 문서에 명시**했다.

### 2. AC-002-2 정정

문언을 "아티팩트 0개"에서 **"Compose UI 툴킷이 앱에 들어오지 않는다"** 로 바꾸고 (a) dex에 `compose/ui`·`foundation`·`material`·`runtime` 본체 0개 (b) `buildFeatures.compose` 미설정 (c) Compose 컴파일러 미적용의 AND로 재구성했다. 애너테이션 전용 아티팩트를 예외로 명시하고 유입 경로(`activity-ktx:1.13.0` → `navigationevent:1.0.0` → `compose.runtime:runtime-annotation:1.9.0`)를 함께 적었다.

**정정의 핵심은 판정 명령 교체다.** rev.2는 `./gradlew :app:dependencies`를 판정 수단으로 지정했는데, 이 명령은 애너테이션 전용 아티팩트까지 잡아 **항상 1건 이상이 나온다** — 즉 rev.2 기준은 무엇을 해도 통과할 수 없는 기준이었다. `dexdump` 기반으로 바꾸고, **"`:app:dependencies` grep을 판정에 쓰지 말 것"** 을 경고로 명시했다. 통과 조건(6개, 전부 애너테이션 + R)도 클래스 이름까지 적어 QA가 판단에 재량을 쓰지 않게 했다.

### 3. AC-011-3 정정

대리 신호를 피하라는 지시(V1)에 따라 **"콜백이 비활성이 되었다"를 판정 조건으로 쓰지 않았다.** 그것은 코드 내부 상태이지 관측 가능한 결과가 아니다. 대신 `dumpsys`로 외부에서 볼 수 있는 값으로 4조건 AND를 구성했다:

- (a) **API 31+**: ActivityRecord가 **존재하되** `finishing=false`이고 태스크가 `visible=false` / **API 30 이하**: ActivityRecord 소멸
- (b) logcat에 `FATAL EXCEPTION` / `Fatal signal` 없음
- (c) **대조 — `appFinish` 경로에서는 ActivityRecord가 실제로 소멸**
- (d) **기기 `ro.build.version.sdk` 기록 의무**

**(c)를 넣은 이유가 이 정정에서 가장 중요하다.** 지시에도 있었듯 `finishing=false`만으로는 **"종료되지 않았다"와 "애초에 실행되지 않았다"를 구분할 수 없다.** 앱이 아예 안 떴어도 `finishing=false`는 관측되지 않거나 오해될 수 있다. `appFinish`가 같은 기기에서 실제로 ActivityRecord를 소멸시킨다는 대조가 있어야 (a)의 "살아 있음"이 의미를 갖는다. 리뷰어의 실험 C가 이미 이 대조를 수행했고, 그 구조를 수용 기준으로 승격시킨 것이다.

(d)를 넣은 이유는 **minSdk가 26이라 두 갈래가 모두 지원 범위 안**이기 때문이다. 기기 API 기록이 없으면 어느 갈래로 판정했는지 사후 검증이 불가능하다.

### 4. 함께 손본 직결 서술 3건

지시 범위("AC-011-3·AC-002-2 두 건과 그에 직결된 서술")를 넘지 않는 선에서 세 곳을 함께 고쳤다. 셋 다 **정정된 두 AC에 직접 의존하던 문장**이다.

1. **REQ-011 본문** — "뒤로가기는 통상대로 **앱을 종료한다**" → "통상대로(=**플랫폼 기본 동작으로**) 앱에서 벗어난다". 사용자 요구어 "통상대로"는 그대로 두었다. 오케스트레이터 지적대로 API 33에서 "통상"이 곧 태스크 백그라운드 이동이므로 원문과 오히려 합치하며, 문제는 뒤에 붙은 "앱을 종료한다"라는 단정뿐이었다. **요구 범위는 바뀌지 않는다.**
2. **AC-011-4** — 판정 근거를 "종료 여부"에서 **"두 조작의 결과가 서로 다르다"**로 바꿨다. AC-011-3이 더는 "종료"를 단정하지 않으므로, 그것에 기대던 AC-011-4의 판정 문장이 공중에 뜨기 때문이다. 새 문언은 naver 뒤로가기(복귀 + `RESUMED`)와 index 뒤로가기(포그라운드 이탈)가 **갈린다**는 것을 조건으로 삼는다 — 리뷰어가 "실험 A와 B가 갈린다는 것이 결정적"이라 판정한 논리와 같다. 두 결과가 같아지면 `canGoBack` 배선이 죽은 것이다.
3. **REQ-011의 [미확인] 항목 해소** — rev.2가 "최우선 실기기 확인"으로 올렸던 *"확장 페이지가 GeckoSession 히스토리에 남는가"* 를 **해소 상태로 갱신**했다(남는다, §2.7.4 폴백 미발동). 해소된 항목을 [미확인]으로 방치하면 후속 단계가 존재하지 않는 리스크를 계속 안고 간다. 출처는 code-reviewer 실측으로 명시했다.

### 5. 잔존 참조 전수 점검

정정 후 `AC-002-2` / `AC-011-3` / `AC-011-4` / "앱을 종료" / "Activity가 종료" / "compose"를 문서 전체에서 grep해 **구 문언에 기대는 서술이 남아 있지 않음**을 확인했다. §5.1의 *"임의의 웹사이트가 `appFinish`를 호출해 앱을 종료시킬 수 있다"* 는 그대로 두었다 — `appFinish`는 명시적 `finish()`라 실제로 종료되며(실험 C) 이번 정정 대상이 아니다.

## 결정 사항

| 지점 | 선택 | 이유 |
|:--|:--|:--|
| 실기기 실험 재현 여부 | **재현하지 않고 출처 명시 인용** | 리뷰어가 정리해 반납한 기기 상태를 보존. 실기기 재현은 QA 역할이며 정정된 기준이 그 절차를 담고 있다. 대신 재현 없이 인용한다는 사실과 출처를 문서에 박았다 |
| dex 검증 | **직접 재실행** | 기기 조작이 아니라 로컬 아티팩트 조사라 부작용이 없고, AC 문언에 클래스 이름 6개를 박으려면 내가 본 값이어야 한다 |
| API 31 동작 변경 | **리뷰어 근거를 그대로 쓰지 않고 공식 문서에서 재확인** | 그 결과 리뷰어 판정문에 없던 3가지(적용 대상 한정 / targetSdk 무관 / AndroidX 권장 경로)를 추가로 확보해 AC (d)와 정정 근거에 반영 |
| AC-011-3의 판정 신호 | `dumpsys`의 `finishing=` / `state=` / 태스크 `visible=` | V1 — "콜백이 비활성"은 코드 내부 상태라 대리 신호다. 외부 관측 가능한 값으로만 구성 |
| `appFinish` 대조 조건 | **AND로 유지** | `finishing=false`만으로는 "종료 안 됨"과 "실행 안 됨"을 구분 못 한다. 대조가 있어야 (a)가 의미를 갖는다 |
| 기기 API 기록 | **의무 조건 (d)로 승격** | minSdk 26이라 두 갈래가 모두 지원 범위. 기록 없으면 사후 검증 불가 |
| AC-002-2 판정 수단 | **`dexdump`로 교체 + `:app:dependencies` 금지 명시** | 후자는 애너테이션 아티팩트를 잡아 **항상 실패**한다. 수단을 안 바꾸면 기준만 고쳐도 여전히 통과 불가 |
| REQ-011 본문 | "통상대로"는 유지, "앱을 종료한다"만 정정 | 사용자 요구어를 보존하면서 사실과 합치. 요구 범위 불변 |
| [미확인] 해소 항목 | **갱신** | 지시 범위 밖으로 볼 여지가 있으나, 같은 실험 세트가 직접 반증한 서술이고 방치하면 허위 리스크가 후속 단계로 전파된다 |

## 이슈 / 리스크

**QA(6단계)가 이 정정으로 달라지는 절차 — 3건**

1. **AC-011-3 판정 시 기기 `ro.build.version.sdk` 기록이 의무다.** API 31+ 와 API 30 이하는 **기대 결과가 서로 반대**다. 현 기기는 API 33이므로 "태스크 백그라운드 이동" 갈래를 적용해야 하며, ActivityRecord 소멸을 기대하면 정상 동작을 FAIL로 오판한다. **targetSdk(36)가 아니라 기기 OS 버전으로 갈린다** — 이 구분을 놓치면 갈래 선택 자체가 틀린다.
2. **`dumpsys` 판정은 grep 건수가 아니라 `finishing=` / `state=` 필드로 하라.** 단순 건수로는 STOPPED와 소멸을 구분하지 못한다. 그리고 **`appFinish` 대조를 반드시 같은 회차에 수행**해야 (a)가 의미를 갖는다.
3. **AC-002-2는 `dexdump`로만 판정하라.** `./gradlew :app:dependencies | grep compose`는 애너테이션 전용 아티팩트를 잡아 **항상 1건 이상**이 나오며, 이 명령으로는 어떤 구현도 통과할 수 없다. 통과 조건은 "6개, 전부 애너테이션 + R, 그 밖 0개"다.

**리스크**

4. **AC-002-2의 "6개"는 의존성 버전에 종속된 매직 상수다.** `androidx.activity`나 `navigationevent` 버전이 오르면 애너테이션 목록이 바뀔 수 있다. **개수가 아니라 "비(非)애너테이션 클래스 0개"가 본질**이며, AC 문언도 그 순서로 적었다(개수는 현재 스냅샷). 향후 6이 7이 되어도 전부 애너테이션이면 PASS다 — QA가 개수만 보고 FAIL 처리하지 않도록 주의.
5. **AC-011-3의 API 30 이하 갈래는 실측되지 않았다.** 현재 가용 기기가 API 33뿐이라 "API 30 이하에서 Activity가 종료된다"는 **[문서] 근거의 추론**이다. 이 갈래를 실제로 검증하려면 API 26~30 기기/에뮬레이터가 필요하다. QA가 이 갈래를 "검증했다"고 보고해서는 안 된다 — **미검증임을 명시**해야 한다(V4·V5).
6. **code-reviewer가 남긴 MAJOR-1(주석 2곳)은 이 정정으로 해소되지 않는다.** `MainActivity.kt:39-44`와 `AppNavigationDelegate.kt:11-12`의 주석 수정은 developer 소관이며 별개 작업이다. 문서만 고치고 코드 주석을 두면 **코드와 문서가 반대로 말하는 상태**가 되므로, developer 재작업이 완료되어야 정합이 맞는다.
7. **review.md의 나머지 MINOR 8건은 이번 정정 범위 밖**이다. 특히 MINOR-1(확장 설치 실패 시 크래시)·MINOR-8(프로브가 release에도 실행)은 리뷰어가 V9에 따라 다음 배치로 넘기라고 명시했다. 요구사항 문서에 새 REQ로 추가하지 않았다 — 지시 범위를 넘기 때문이며, 후속 배치 착수 시 별도 요구사항으로 올려야 한다.

**차단당한 시도**: 없음.

**요구사항을 새로 만들지 않았음**: REQ 개수·범위·우선순위는 rev.2와 동일하다. 정정은 AC 문언 2건 + 직결 서술 3건에 국한했다.

## 판정

**PASS** — `pipeline/requirements.md` rev.3 개정 완료. AC-011-3·AC-002-2 두 건의 문언을 실측·문서에 맞게 정정하고, 개정 이력에 **"요구가 바뀐 것이 아니라 구현 후 실측으로 기준을 정정한 것"** 이라는 성격과 rev.2/rev.3 문언 대조표를 남겼다. V7에 따라 dex 검증은 직접 재실행하고 플랫폼 동작 변경은 공식 문서에서 재확인했으며(그 과정에서 targetSdk 무관·적용 대상 한정 등 판정문에 없던 사실 3건을 추가 확보), 재현하지 않은 실기기 관측값은 **code-reviewer 실측(review.md §1①)** 으로 출처를 명시했다. 정정된 기준은 V1에 따라 코드 내부 상태("콜백이 비활성")가 아닌 `dumpsys` 관측값으로 구성했고, `finishing=false`가 단독으로는 무의미하므로 `appFinish` 대조 조건을 AND로 유지했다.

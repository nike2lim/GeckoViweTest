/*
 * background.js — 확장의 background script.
 *
 * ┌───────────────────────────────────────────────────────────────────────────┐
 * │ 이 파일을 거치는 구조는 "우회"가 아니라 **필수**다. 지우거나 건너뛰지 말 것.  │
 * └───────────────────────────────────────────────────────────────────────────┘
 *
 * content script에서 `browser.runtime.sendNativeMessage(...)`를 **직접** 부르면
 * mozilla/geckoview#220의 결함 경로에 들어간다 — 같은 코드가 background script에서는
 * 정상 동작하는데 content script에서는 실패한다고 이슈 보고자가 명시했고,
 * 이슈는 아직 열린 상태이며 워크어라운드도 없다.
 *
 * 즉 "content script에서 바로 네이티브로 보내면 한 단계 줄겠네"라는 리팩터링은
 * **곧바로 버그가 된다.** 네이티브 API 호출은 이 파일에서만 한다.
 * 코드로는 이것을 막을 수 없고 이 주석이 유일한 방어 수단이다(plan.md R-10).
 *
 * 데이터 흐름에서의 위치:
 *   확장 페이지(index.html):  bridge-client.js ──runtime.sendMessage──▶ 이 파일 ──▶ 네이티브
 *   외부 사이트(naver 등):    page-bridge.js ──postMessage──▶ content.js ──runtime.sendMessage──▶ 이 파일 ──▶ 네이티브
 *
 * **두 경로는 이 파일에 도달할 때 완전히 같은 형태의 요청을 보낸다.**
 * 그래서 아래 처리 코드에는 "누가 보냈는지"로 갈라지는 분기가 없다(AC-003-3).
 */

// 이 문자열은 BridgeProtocol.kt의 NATIVE_APP 상수와 **반드시 같아야 한다**
// (app/src/main/java/com/example/geckoviewtest/bridge/BridgeProtocol.kt).
// 어긋나면 예외도 로그도 없이 아무 일도 일어나지 않는다 — 디버깅이 가장 어려운 종류의 실패다.
const NATIVE_APP = "browser";

/*
 * 브리지가 열어주는 함수 목록.
 *
 * ⚠ 여기에 함수를 추가하면 그 즉시 content_scripts.matches 범위의 **모든 웹사이트**에 열린다.
 *   현재 matches는 http/https 전체이므로 "방문하는 모든 사이트"라는 뜻이다.
 *   추가하기 전에 requirements.md §5.1의 트레이드오프를 다시 읽을 것.
 *
 * 코틀린 쪽 BridgeDispatcher의 when 분기와 이 배열은 쌍이다. 고칠 때는 **이 파일을 먼저** 고친다
 * (JS에만 있으면 네이티브가 UNKNOWN_FUNCTION으로 시끄럽게 거절하지만,
 *  코틀린에만 있으면 이 배열이 먼저 막아 조용히 실패하기 때문이다).
 */
const ALLOWED_FUNCTIONS = ["getVersionName", "appFinish"];

/*
 * [오리진 검사 훅 — 기본 비활성]
 *
 * 이 훅은 "앞으로 브리지 함수를 추가할 때 어떤 사이트까지 열어줄 것인가"를
 * 반드시 마주치게 하려고 만든 **자리**다. 지금은 아무것도 막지 않는다.
 *
 * 기본값은 "전체 허용"이며 이것을 켜는 것은 사용자 결정 사항이다
 * (requirements.md §5.1, 가정 A-08 — 사용자가 "외부 사이트에서도 브리지가 동작해야 한다"고 결정했다).
 * 켠 상태로 출고하면 REQ-010을 뒤집는 것이므로 금지다(plan.md H-1).
 *
 * 켜고 싶다면: 아래 return true를 허용 목록 검사로 바꾸면 된다.
 */
function isOriginAllowed(sender) {
  // 기본 분기는 무조건 통과한다. 사용자가 요구한 범위를 좁히지 않기 위해서다.
  return true;
}

/**
 * 요청 하나를 네이티브로 넘기고 응답을 돌려준다.
 *
 * @param {object} request BridgeProtocol.kt에 적힌 요청 스키마 그대로
 *                         { id, type: "call", name, payload }
 * @returns {Promise<object>} 응답 스키마 { id, type: "result", ok, value | error }
 */
async function forwardToNative(request) {
  // 네이티브에는 객체가 아니라 JSON **문자열**을 보낸다.
  // 이렇게 해야 코틀린 쪽이 org.json 없이 kotlinx.serialization만으로 파싱할 수 있고,
  // 그 결과 BridgeProtocol이 순수 코틀린으로 남아 단위 테스트가 가능해진다(plan.md §3.1).
  const rawResponse = await browser.runtime.sendNativeMessage(NATIVE_APP, JSON.stringify(request));
  return JSON.parse(rawResponse);
}

/**
 * 확장 내부에서 오는 모든 메시지의 단일 진입점.
 *
 * 보내는 쪽이 확장 페이지(bridge-client.js)든 외부 사이트의 content script(content.js)든
 * **여기서는 구분하지 않는다.** 구분하는 순간 REQ-006이 말하는 "동일 계약"이 이름뿐이 된다.
 */
browser.runtime.onMessage.addListener((request, sender) => {
  // 요청 형태 검사. 여기서 걸러내면 네이티브까지 갈 필요가 없다.
  if (!request || request.type !== "call" || typeof request.name !== "string") {
    return Promise.resolve({
      id: (request && request.id) || "",
      type: "result",
      ok: false,
      error: { code: "INVALID_REQUEST", message: "요청 형태가 계약과 다르다" },
    });
  }

  if (!isOriginAllowed(sender)) {
    return Promise.resolve({
      id: request.id,
      type: "result",
      ok: false,
      error: { code: "INVALID_REQUEST", message: "허용되지 않은 오리진이다" },
    });
  }

  if (!ALLOWED_FUNCTIONS.includes(request.name)) {
    return Promise.resolve({
      id: request.id,
      type: "result",
      ok: false,
      error: { code: "UNKNOWN_FUNCTION", message: "브리지에 없는 함수명이다: " + request.name },
    });
  }

  // Promise를 그대로 돌려주면 보낸 쪽의 sendMessage Promise가 그 값으로 resolve된다.
  return forwardToNative(request).catch((e) => ({
    id: request.id,
    type: "result",
    ok: false,
    error: { code: "INTERNAL_ERROR", message: String(e) },
  }));
});

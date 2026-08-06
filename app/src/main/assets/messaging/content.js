/*
 * content.js — 외부 웹사이트(naver.com 등)에 주입되는 content script.
 *
 * ┌───────────────────────────────────────────────────────────────────────────────┐
 * │ 왜 page-bridge.js를 따로 주입하는가 — 이 구조는 우회가 아니라 **유일한 방법**이다. │
 * └───────────────────────────────────────────────────────────────────────────────┘
 *
 * content script는 페이지와 DOM은 공유하지만 **JS 스코프는 격리**되어 있고(isolated world),
 * Firefox는 여기에 **Xray vision**이라는 보호막을 씌운다.
 *
 * 그 결과 이 파일에서 `window.NativeBridge = {...}` 라고 대입해도
 * **페이지 세계의 JS는 그 객체를 볼 수 없다.** naver.com의 스크립트 입장에서는
 * window.NativeBridge가 없는 것과 똑같다. (`exportFunction()`이라는 API가 존재하는 이유가 바로 이것이다.)
 *
 * 그래서 브리지 함수를 정의하는 코드(page-bridge.js)를 web_accessible_resources에 등록하고
 * <script> 태그로 페이지에 끼워 넣어 **페이지 세계에서 실행**시킨다.
 * 그리고 두 세계 사이는 DOM 이벤트인 window.postMessage로 잇는다.
 *
 * 이 파일을 "한 단계 줄이자"며 page-bridge.js를 없애면, 겉보기에는 잘 동작하는 것처럼 보이지만
 * 실제 웹페이지에서는 브리지를 전혀 쓸 수 없는 상태가 된다 — 가장 발견하기 어려운 종류의 고장이다.
 *
 * ⚠ 네이티브 API(sendNativeMessage)를 이 파일에서 직접 부르지 말 것.
 *   그것은 mozilla/geckoview#220의 결함 경로다. 자세한 근거는 background.js 상단 주석에 있다.
 *
 * 전송 경로에서의 위치:
 *   page-bridge.js ──postMessage──▶ **이 파일** ──runtime.sendMessage──▶ background.js ──▶ 네이티브
 */
(function () {
  "use strict";

  /*
   * 1) page-bridge.js를 페이지 세계에 주입한다.
   *
   * runtime.getURL: 확장 안의 파일을 페이지가 읽을 수 있는 moz-extension:// URL로 바꿔준다.
   * 이 URL이 동작하려면 manifest.json의 web_accessible_resources에 등록돼 있어야 한다.
   */
  var script = document.createElement("script");
  script.src = browser.runtime.getURL("page-bridge.js");
  script.onload = function () {
    // 실행이 끝나면 태그 자체는 필요 없다. 페이지 DOM을 깨끗하게 되돌린다(AC-010-6).
    script.remove();
  };
  (document.head || document.documentElement).appendChild(script);

  /*
   * 2) 페이지 세계 ↔ 확장 사이의 메시지를 중계한다.
   *
   * postMessage는 페이지의 다른 코드와도 섞이므로 우리 메시지에만 봉투를 씌운다.
   *   { __nativeBridge: true, direction: "req" | "res" | "probe", body: <요청 또는 응답> }
   *
   * event.source !== window 검사: 다른 프레임(iframe)이 보낸 메시지를 걸러낸다.
   * direction 검사: 자기가 보낸 메시지를 자기가 다시 받아 무한 루프가 나는 것을 막는다.
   */
  window.addEventListener("message", function (event) {
    if (event.source !== window) return;
    var data = event.data;
    if (!data || data.__nativeBridge !== true) return;

    if (data.direction === "req") {
      // 봉투를 벗겨 body만 background.js로 보낸다.
      // 그래서 background.js는 확장 페이지에서 온 요청과 이 요청을 구분할 수 없다 — 의도한 바다(AC-003-3).
      browser.runtime.sendMessage(data.body).then(function (response) {
        window.postMessage({ __nativeBridge: true, direction: "res", body: response }, "*");
      }).catch(function (e) {
        window.postMessage({
          __nativeBridge: true,
          direction: "res",
          body: {
            id: data.body && data.body.id,
            type: "result",
            ok: false,
            error: { code: "INTERNAL_ERROR", message: String(e) },
          },
        }, "*");
      });
      return;
    }

    if (data.direction === "probe") {
      markPageWorldIfProven();
    }
  });

  /*
   * 3) "정말로 페이지 세계에서 실행됐는가"를 확인해 배지에 마커를 덧붙인다.
   *
   * window.wrappedJSObject: Xray를 걷어내고 **페이지가 실제로 보는 window**를 읽는 Firefox 전용 통로.
   * 그냥 `window.__bridgeProbeRanInPageWorld`를 읽으면 격리 세계의 window를 보는 것이라 항상 undefined다.
   *
   * 이 검사를 통과해야만 PAGE_WORLD를 붙인다. 이 마커가 없으면
   * "격리 세계 안에서만 동작한 것"이므로 REQ-010 미충족이다.
   */
  function markPageWorldIfProven() {
    var ranInPageWorld = false;
    try {
      ranInPageWorld = window.wrappedJSObject.__bridgeProbeRanInPageWorld === true;
    } catch (e) {
      ranInPageWorld = false;
    }
    if (!ranInPageWorld) return;

    var badge = document.getElementById("__bridge_probe");
    if (badge && badge.textContent.indexOf("PAGE_WORLD") === -1) {
      badge.textContent = badge.textContent + " [PAGE_WORLD]";
    }
  }
})();

/*
 * page-bridge.js — **페이지 세계(page world)에서 실행되는** 브리지 파사드.
 *
 * 이 파일은 content script가 아니다. content.js가 <script> 태그로 페이지에 끼워 넣어
 * 페이지 자신의 JS와 **같은 세계**에서 실행된다. 그래야 naver.com의 스크립트가
 * `window.NativeBridge`를 볼 수 있다(REQ-010).
 *
 * 전송 경로: 이 파일 ──window.postMessage──▶ content.js ──runtime.sendMessage──▶ background.js ──▶ 네이티브
 *
 * 노출하는 함수 모양은 확장 페이지용 bridge-client.js와 **완전히 같다**.
 * 전송 방법만 다르고 계약은 하나다(REQ-006).
 */
(function () {
  "use strict";

  // 이 파일은 web_accessible_resources로 노출되므로 페이지가 두 번 삽입할 수도 있다.
  // 두 번 실행되면 리스너가 중복 등록되어 응답이 두 번 처리된다.
  if (window.NativeBridge) {
    return;
  }

  var pending = {};
  var sequence = 0;

  function nextId() {
    sequence += 1;
    return "page-" + Date.now() + "-" + sequence;
  }

  /*
   * content.js가 보낸 응답을 받는다.
   *
   * `direction`을 확인하는 이유: postMessage는 자기가 보낸 메시지도 자기가 받는다.
   * direction 검사가 없으면 요청을 보내자마자 그 요청을 응답으로 착각해 무한 루프가 난다.
   */
  window.addEventListener("message", function (event) {
    if (event.source !== window) return;
    var data = event.data;
    if (!data || data.__nativeBridge !== true || data.direction !== "res") return;

    var body = data.body;
    if (!body || !pending[body.id]) return;

    var entry = pending[body.id];
    delete pending[body.id];

    if (body.ok) {
      entry.resolve(body.value);
    } else {
      var error = new Error((body.error && body.error.message) || "알 수 없는 오류");
      error.code = (body.error && body.error.code) || "INTERNAL_ERROR";
      entry.reject(error);
    }
  });

  /**
   * 페이지가 쓰는 단일 진입 함수. bridge-client.js의 call과 시그니처가 같다.
   *
   * @param {string} name 브리지 함수명
   * @param {object} [payload] 함수 인자
   * @returns {Promise<any>}
   */
  function call(name, payload) {
    return new Promise(function (resolve, reject) {
      var request = {
        id: nextId(),
        type: "call",
        name: name,
        payload: payload || {},
      };
      pending[request.id] = { resolve: resolve, reject: reject };
      window.postMessage({ __nativeBridge: true, direction: "req", body: request }, "*");
    });
  }

  window.NativeBridge = { call: call };

  /*
   * ── 검증용 프로브 ────────────────────────────────────────────────────────────
   * 여기서부터는 "페이지 세계에서 브리지가 정말 동작하는가"를 화면에서 확인하기 위한 코드다.
   *
   * 이것이 필요한 이유: content script 안에서만 브리지가 동작해도 겉보기에는 완벽한 성공으로 보인다.
   * 페이지 세계에서 실행됐다는 증거를 눈에 보이는 형태로 남기지 않으면
   * "실제 웹페이지는 브리지를 못 쓰는 상태"가 검증을 통과해버린다(AC-010-3).
   */

  // 이 전역 변수가 곧 "이 코드가 페이지 세계에서 돌았다"는 증거다.
  // content script는 이것을 window.wrappedJSObject를 통해서만 읽을 수 있다.
  window.__bridgeProbeRanInPageWorld = true;

  function showBadge(text) {
    var badge = document.getElementById("__bridge_probe");
    if (!badge) {
      badge = document.createElement("div");
      badge.id = "__bridge_probe";
      badge.style.cssText =
        "position:fixed;top:0;left:0;right:0;z-index:2147483647;" +
        "background:#000;color:#0f0;font:14px monospace;padding:6px;";
      document.documentElement.appendChild(badge);
    }
    badge.textContent = text;

    // 배지를 다 쓴 뒤에 content.js에게 알린다.
    // content.js가 페이지 세계 실행 여부를 확인해 PAGE_WORLD 마커를 덧붙인다.
    window.postMessage({ __nativeBridge: true, direction: "probe" }, "*");
  }

  call("getVersionName")
    .then(function (v) { showBadge("versionName = " + v); })
    .catch(function (e) { showBadge("브리지 오류: " + e.code + " / " + e.message); });
})();

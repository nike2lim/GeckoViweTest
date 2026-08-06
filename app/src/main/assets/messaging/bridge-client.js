/*
 * bridge-client.js — **확장 페이지(index.html) 전용** 브리지 어댑터.
 *
 * index.html은 확장 자신의 페이지(moz-extension:// 오리진)이므로
 * content script 없이 `browser.runtime.sendMessage`로 background.js와 바로 대화할 수 있다.
 * 그래서 이 경로는 3단이다:  이 파일 → background.js → 네이티브
 *
 * 외부 사이트(naver 등)는 page-bridge.js + content.js를 앞에 얹은 5단 경로를 쓴다.
 * **두 파일은 전송 방법만 다르고 노출하는 함수 모양은 똑같다** — 그것이 REQ-006이 말하는 "동일 계약"이다.
 *
 * ⚠ 이 파일이 통과한다고 해서 외부 사이트 경로가 동작한다는 뜻이 아니다.
 *   두 경로는 물리적으로 다른 파일이라 한쪽 결과를 다른 쪽 근거로 쓸 수 없다(AC-006-4).
 */
(function () {
  "use strict";

  // 요청마다 고유한 id를 만든다. 응답이 어느 요청의 것인지 짝지으려면 필요하다.
  let sequence = 0;
  function nextId() {
    sequence += 1;
    return "ext-" + Date.now() + "-" + sequence;
  }

  /**
   * 페이지가 쓰는 단일 진입 함수.
   *
   * 함수별 분기를 두지 않고 name을 그대로 흘려보낸다.
   * 덕분에 네이티브에 함수를 추가해도 페이지가 바꾸는 것은 **함수명 문자열뿐**이다(AC-006-3).
   *
   * @param {string} name 브리지 함수명
   * @param {object} [payload] 함수 인자
   * @returns {Promise<any>} 성공하면 value로 resolve, 실패하면 Error로 reject
   */
  function call(name, payload) {
    const request = {
      id: nextId(),
      type: "call",
      name: name,
      payload: payload || {},
    };

    return browser.runtime.sendMessage(request).then(function (response) {
      if (!response || typeof response !== "object") {
        throw new Error("브리지 응답이 비었다");
      }
      if (response.ok) {
        return response.value;
      }
      // 실패 사유를 페이지가 읽을 수 있어야 한다(AC-006-2).
      const error = new Error((response.error && response.error.message) || "알 수 없는 오류");
      error.code = (response.error && response.error.code) || "INTERNAL_ERROR";
      throw error;
    });
  }

  window.NativeBridge = { call: call };
})();

/*
 * index-page.js — index.html의 화면 동작(버튼 핸들러, 결과 표시).
 *
 * ⚠ 이 코드를 index.html 안의 <script> 태그로 되돌리지 말 것.
 *   확장 페이지(moz-extension://)에는 기본 CSP `script-src 'self' 'wasm-unsafe-eval'`가 걸려 있어
 *   **인라인 스크립트가 실행되지 않는다.** 화면은 정상으로 보이는데 JS만 조용히 죽는다.
 *   실제로 이 프로젝트에서 한 번 겪었고, logcat에 다음 오류가 남았다:
 *     "Content-Security-Policy: The page's settings blocked an inline script (script-src-elem)"
 *   외부 파일로 두는 것은 취향이 아니라 제약이다.
 */
(function () {
  "use strict";

  var statusEl = document.getElementById("bridgeStatus");
  var resultEl = document.getElementById("result");

  // bridge-client.js가 정상 로드됐는지 확인해 화면에 표시한다.
  // 이 줄이 실행되지 않으면 초기 텍스트 "(JS 미실행)"이 그대로 남아
  // "정적 HTML만 그려지고 JS가 죽은 상태"를 눈으로 구분할 수 있다(AC-007-3).
  statusEl.textContent = window.NativeBridge ? "READY" : "브리지 없음";

  function show(text) {
    resultEl.textContent = text;
  }

  document.getElementById("btnVersion").addEventListener("click", function () {
    show("호출 중...");
    window.NativeBridge.call("getVersionName")
      .then(function (v) { show("versionName = " + v); })
      .catch(function (e) { show("오류: " + e.code + " / " + e.message); });
  });

  document.getElementById("btnUnknown").addEventListener("click", function () {
    show("호출 중...");
    window.NativeBridge.call("thisFunctionDoesNotExist")
      .then(function (v) { show("예상과 다르게 성공했다: " + v); })
      .catch(function (e) { show("오류: " + e.code + " / " + e.message); });
  });

  document.getElementById("btnFinish").addEventListener("click", function () {
    show("종료 요청 중...");
    // appFinish는 앱 프로세스를 종료시킨다.
    // 종료가 응답보다 먼저 일어날 수 있으므로 **이 Promise의 resolve에 의존하지 않는다**(plan.md §3.5).
    window.NativeBridge.call("appFinish")
      .catch(function (e) { show("오류: " + e.code + " / " + e.message); });
  });
})();

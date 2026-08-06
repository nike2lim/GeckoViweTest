package com.example.geckoviewtest.gecko

import org.mozilla.geckoview.GeckoSession

/**
 * 페이지 로드의 시작과 끝을 GeckoView로부터 전달받는 델리게이트(REQ-009).
 *
 * 데이터 흐름: GeckoView 엔진 → **이 델리게이트** → `MainViewModel` → `MainUiState.isLoading` → 화면
 *
 * 델리게이트(delegate): "이런 일이 생기면 알려달라"고 GeckoView에 등록해 두는 콜백 묶음.
 * `session.progressDelegate = ...` 로 등록한다.
 *
 * **이 클래스는 View를 직접 건드리지 않는다.** 로딩 표시를 여기서 껐다 켜면
 * 화면 상태의 진실의 원천이 두 곳으로 갈라져, 상태와 화면이 어긋나는 순간이 생긴다.
 * 여기서는 ViewModel에 사실만 전달하고, 화면을 바꾸는 것은 Activity의 `render()` 한 곳이다(plan.md D-07).
 *
 * GeckoView의 이 콜백들은 UI 스레드에서 호출된다(`@UiThread`).
 */
class AppProgressDelegate(
    private val onStart: () -> Unit,
    private val onStop: (success: Boolean) -> Unit,
) : GeckoSession.ProgressDelegate {

    override fun onPageStart(session: GeckoSession, url: String) {
        onStart()
    }

    /**
     * @param success 로드 성공 여부.
     *   **실패해도 반드시 불린다** — 그래서 네트워크가 끊겨도 로딩 표시가 사라진다(AC-009-4).
     */
    override fun onPageStop(session: GeckoSession, success: Boolean) {
        onStop(success)
    }
}

package com.example.geckoviewtest.gecko

import org.mozilla.geckoview.GeckoSession

/**
 * 페이지 이동과 관련된 사실을 GeckoView로부터 전달받는 델리게이트(REQ-011).
 *
 * 데이터 흐름: GeckoView 엔진 → **이 델리게이트** → `MainViewModel` → `MainUiState` → 화면·뒤로가기 콜백
 *
 * [GeckoSession.NavigationDelegate.onCanGoBack]은 "웹 히스토리에 뒤로 갈 곳이 있는가"가
 * 바뀔 때마다 불린다. 이 값이 뒤로가기 버튼의 동작을 가른다:
 *  - 갈 곳이 **있으면** → 뒤로가기 콜백이 켜져 페이지를 한 칸 뒤로 보낸다
 *  - 갈 곳이 **없으면** → 콜백이 꺼지고 **플랫폼 기본 동작**이 수행된다. 우리 코드는 여기서 끝난다.
 *
 * **"플랫폼 기본 동작"이 곧 앱 종료는 아니다 — 안드로이드 버전에 따라 갈린다:**
 *  - **API 31 이상**: Activity가 종료되지 않고 **태스크가 백그라운드로 밀려난다**
 *    (홈 버튼을 누른 것과 비슷하다. 화면에서는 사라지지만 Activity는 살아 있다).
 *    실측: API 33 기기에서 `dumpsys`가 `state=STOPPED finishing=false`.
 *  - **API 30 이하**: 예전 동작이라 **Activity가 종료된다.** 이 앱의 `minSdk`가 26이므로
 *    지원 범위 안에 두 동작이 모두 들어 있다(API 30 이하는 이번 검증에 실기기가 없어 미실측).
 *
 * 앱을 확실히 끝내는 경로는 웹 페이지의 `appFinish` → `MainUiEvent.Finish` → `finish()`이며,
 * 그쪽은 버전과 무관하게 ActivityRecord가 실제로 소멸한다.
 *
 * (AC-011-3) — **이 수용 기준의 문언은 API 31 이상의 동작을 반영하지 못해 정정 대기 중이다.**
 * 구현을 바꿀 사안이 아니다.
 *
 * 이 클래스도 View나 GeckoSession을 직접 조작하지 않고 사실만 위로 전달한다.
 */
class AppNavigationDelegate(
    private val onCanGoBack: (Boolean) -> Unit,
    private val onLocationChanged: (String?) -> Unit,
) : GeckoSession.NavigationDelegate {

    override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
        onCanGoBack(canGoBack)
    }

    override fun onLocationChange(
        session: GeckoSession,
        url: String?,
        perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
        hasUserGesture: Boolean,
    ) {
        onLocationChanged(url)
    }
}

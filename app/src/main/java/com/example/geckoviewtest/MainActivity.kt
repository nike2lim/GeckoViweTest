package com.example.geckoviewtest

import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.geckoviewtest.databinding.ActivityMainBinding
import com.example.geckoviewtest.gecko.AppNavigationDelegate
import com.example.geckoviewtest.gecko.AppProgressDelegate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoSession

/**
 * 앱의 유일한 화면.
 *
 * 역할은 세 가지뿐이다:
 *  1. [App]이 소유한 GeckoRuntime에 [GeckoSession]을 붙여 화면의 GeckoView 위젯에 연결한다.
 *  2. GeckoView가 알려주는 사실(로드 시작/끝 등)을 [MainViewModel]에 전달한다.
 *  3. [MainViewModel]이 내놓는 상태를 [render]로 화면에 옮기고, 이벤트를 받아 세션을 조작한다.
 *
 * 판단은 전부 ViewModel에 있고 여기에는 없다. 세션(탭 하나에 해당)만 이 UI 레이어가 소유하는데,
 * 그것은 GeckoView 위젯과 짝을 이루는 화면 자원이기 때문이다(plan.md D-05).
 */
class MainActivity : AppCompatActivity() {

    // ViewBinding으로 만들어진 클래스. activity_main.xml -> ActivityMainBinding 으로 이름이 정해진다.
    private lateinit var binding: ActivityMainBinding

    private lateinit var session: GeckoSession

    /**
     * 뒤로가기 콜백. 이 콜백의 `isEnabled`가 뒤로가기 동작을 가른다:
     *  - `true`  → 콜백이 실행되어 웹 페이지를 한 칸 뒤로 보낸다
     *  - `false` → 콜백을 건너뛰고 **플랫폼 기본 동작**이 수행된다. **우리 코드는 여기서 끝난다.**
     *
     * ┌ `false`일 때 실제로 무슨 일이 일어나는가 — 안드로이드 버전에 따라 갈린다 ────────────┐
     * │ **API 31 이상**: Activity가 종료되지 않고 **태스크가 백그라운드로 밀려난다.**          │
     * │   (= 홈 버튼을 누른 것과 비슷하다. 앱은 화면에서 사라지지만 메모리에 살아 있고,        │
     * │    다시 실행하면 보던 화면이 그대로 나온다.)                                          │
     * │   실측: API 33 기기에서 `dumpsys activity activities`가                               │
     * │   `state=STOPPED stopped=true finishing=false` — 즉 **소멸하지 않았다.**              │
     * │ **API 30 이하**: 예전 동작이 그대로라 **Activity가 종료된다.**                        │
     * │   이 앱의 `minSdk`가 26이므로 지원 범위 안에 두 동작이 **모두** 들어 있다.            │
     * │   (API 30 이하 실기기는 이번 검증에 없어 문서상 동작이며 실측하지 않았다.)            │
     * └──────────────────────────────────────────────────────────────────────────────────────┘
     *
     * **앱을 확실히 끝내는 경로는 이쪽이 아니다.** 웹 페이지의 `appFinish` 호출이
     * [MainUiEvent.Finish] → [handleEvent]의 `finish()`로 이어지는 경로이며,
     * 그쪽은 버전과 무관하게 실제로 ActivityRecord가 소멸한다(실측: `dumpsys`에서 완전히 사라짐).
     *
     * (AC-011-3) — **다만 이 수용 기준의 문언은 "뒤로가기로 Activity가 종료된다"로 되어 있어
     * API 31 이상의 동작을 반영하지 못한다. 문언 정정 대기 중이며, 구현을 바꿀 사안이 아니다.**
     * 강제로 `finish()`를 부르면 오히려 플랫폼 표준 동작에서 이탈한다.
     *
     * 값을 정하는 것은 ViewModel이고([MainUiState.canGoBack]), 여기서는 [render]가 옮겨 담기만 한다.
     */
    private lateinit var backCallback: OnBackPressedCallback

    /**
     * `by viewModels`: 화면이 다시 만들어져도 같은 ViewModel 인스턴스를 돌려주는 코틀린 위임.
     * 생성자에 인자가 있으므로 만드는 방법(Factory)을 함께 알려준다.
     *
     * `AppContainer`가 아니라 여기서 직접 넘기는 것이 의도다: 이 둘은 **화면에 딸린 값**이라
     * 앱 스코프 컨테이너가 알 이유가 없다(다른 화면이 생기면 각자 다른 URL을 쓴다).
     * 중요한 것은 "누가 넘기느냐"가 아니라 **생성자 주입이라 테스트에서 교체 가능하다**는 점이며,
     * `MainViewModelTest`가 실제로 둘 다 다른 값으로 바꿔 넣어 검증한다.
     */
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(naverUrl = NAVER_URL, dispatcher = Dispatchers.Default)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as App

        session = GeckoSession()
        // open(runtime): 세션을 엔진에 붙여 실제 렌더 프로세스를 띄운다. 이 호출 전에는 로드할 수 없다.
        session.open(app.geckoRuntime)
        binding.geckoView.setSession(session)

        // 델리게이트는 사실만 ViewModel에 전달한다. 여기서 화면을 직접 바꾸지 않는다(plan.md D-07).
        session.progressDelegate = AppProgressDelegate(
            onStart = viewModel::onPageStart,
            onStop = viewModel::onPageStop,
        )

        session.navigationDelegate = AppNavigationDelegate(
            onCanGoBack = viewModel::onCanGoBackChanged,
            onLocationChanged = viewModel::onLocationChanged,
        )

        binding.btnNaver.setOnClickListener { viewModel.onNaverClicked() }

        /*
         * onBackPressedDispatcher: 안드로이드의 뒤로가기 처리 창구.
         * 등록한 콜백이 활성이면 시스템 기본 동작보다 먼저 이 콜백이 불린다.
         *
         * 콜백 안에서 `session.goBack()`을 직접 부르지 않고 ViewModel에 알리기만 하는 것이 핵심이다.
         * 그래야 ViewModel이 androidx.activity 타입을 전혀 모르는 상태로 남고,
         * 뒤로가기 판단(canGoBack 전이, 이벤트 발행)이 실기기 없이 JVM 테스트로 검증된다(plan.md D-08).
         *
         * enabled = false로 시작한다 — 첫 페이지에서는 뒤로 갈 곳이 없기 때문이다.
         * 이후 값은 render()가 uiState에서 옮겨 담는다.
         */
        backCallback = onBackPressedDispatcher.addCallback(this, enabled = false) {
            viewModel.onBackPressed()
        }

        /*
         * 웹 페이지가 브리지로 appFinish를 부르면 여기까지 전달된다(REQ-005).
         *
         * 흐름: page JS → background.js → NativeBridgeHandler → BridgeDispatcher
         *       → [com.example.geckoviewtest.bridge.AppBridgeHost]
         *       → **이 콜백** → ViewModel → MainUiEvent.Finish → handleEvent()의 finish()
         *
         * `AppBridgeHost`는 `bridge` 패키지에 있지만 앱 스코프(프로세스 수명)라 Activity보다 오래 산다.
         * 그래서 화면이 사라질 때 onDestroy에서 반드시 이 등록을 해제한다 —
         * 해제하지 않으면 죽은 Activity의 ViewModel을 붙잡고 있게 된다.
         */
        app.container.bridgeHost.onFinishRequested = { viewModel.onAppFinishRequested() }

        observeViewModel()
        loadExtensionPage(app)
    }

    /**
     * ViewModel의 상태와 이벤트를 구독한다.
     *
     * `repeatOnLifecycle(STARTED)`: 화면이 보이는 동안에만 구독하고, 가려지면 자동으로 멈췄다가
     * 다시 보일 때 재개한다. 그냥 `lifecycleScope.launch`만 쓰면 화면이 안 보일 때도 계속 수집한다.
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { render(it) } }
                launch { viewModel.events.collect { handleEvent(it) } }
            }
        }
    }

    /**
     * 상태를 화면에 옮기는 **유일한** 함수.
     *
     * 화면을 바꾸는 코드가 이 함수 하나로 모여 있어야 "화면은 상태의 투영"이라는 규칙이 강제된다.
     * 다른 곳에서 visibility를 직접 건드리기 시작하면 상태와 화면이 어긋나는 순간이 생기고,
     * 그때부터는 화면만 보고 원인을 짚을 수 없게 된다(plan.md D-06 / D-07).
     */
    private fun render(state: MainUiState) {
        binding.loadingOverlay.isVisible = state.isLoading
        backCallback.isEnabled = state.canGoBack
    }

    /** 한 번만 일어나야 하는 일을 처리한다. 세션 조작이 여기에 모인다. */
    private fun handleEvent(event: MainUiEvent) {
        when (event) {
            is MainUiEvent.Navigate -> session.loadUri(event.url)
            MainUiEvent.NavigateBack -> session.goBack()
            MainUiEvent.Finish -> finish()
        }
    }

    private fun loadExtensionPage(app: App) {
        // lifecycleScope: 이 화면이 살아 있는 동안만 도는 코루틴 스코프.
        // 화면이 사라지면 안에서 돌던 작업이 자동으로 취소된다.
        lifecycleScope.launch {
            val extension = app.bridgeExtension.await()

            /*
             * 로드 대상은 확장 페이지의 URL이다.
             *
             * ┌ 왜 `resource://android/assets/index.html`을 직접 열지 않는가 ─────────────┐
             * │ 결정적인 이유는 크래시 우려(mozilla/geckoview#199)가 **아니라**            │
             * │ **매치 패턴 제약**이다. WebExtension의 content_scripts.matches가          │
             * │ 지원하는 스킴은 http, https, ws, wss, ftp, data, file 뿐이고             │
             * │ `resource://`는 그 집합에 없다 — 넣으면 manifest 자체가 거부된다.         │
             * │ 즉 assets에서 직접 열면 그 페이지에는 브리지를 넣을 방법이 원리적으로 없다. │
             * └──────────────────────────────────────────────────────────────────────────┘
             *
             * metaData.baseUrl은 설치된 확장의 루트 URL(moz-extension://<uuid>/)이다.
             * uuid는 설치 때마다 달라지므로 하드코딩할 수 없고 반드시 여기서 읽어야 한다.
             */
            val url = extension.metaData.baseUrl + "index.html"
            Log.d(TAG, "확장 페이지 로드: $url")
            viewModel.onBridgeReady()
            session.loadUri(url)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 앱 스코프 객체가 사라진 화면을 계속 붙잡지 않도록 등록을 해제한다.
        (application as App).container.bridgeHost.onFinishRequested = null
        // 세션을 닫지 않으면 렌더 프로세스가 남는다.
        if (session.isOpen) {
            session.close()
        }
    }

    private companion object {
        const val TAG = "MainActivity"

        /** REQ-008이 원문에 http로 명시했다. https로 리다이렉트되는 것은 실패가 아니다(AC-008-2는 도메인 기준). */
        const val NAVER_URL = "http://naver.com"
    }
}

package com.example.geckoviewtest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * 메인 화면의 상태를 관리하는 ViewModel.
 *
 * ViewModel: 화면 회전 같은 이유로 Activity가 다시 만들어져도 살아남아 상태를 지켜주는 클래스.
 * 그래서 화면이 사라져도 의미가 있는 값(로딩 여부, 뒤로 갈 수 있는지)을 여기에 둔다.
 *
 * 데이터 흐름:
 * `MainActivity`의 GeckoView 델리게이트들 → **이 ViewModel** → [uiState]를 Activity가 구독해 화면을 그린다.
 *
 * **이 클래스는 안드로이드 프레임워크 타입을 하나도 받지 않는다.**
 * `Context`도, View도, `OnBackPressedCallback`도 모른다. 그래야 JVM 단위 테스트로 검증할 수 있고,
 * 뒤로가기 판단 같은 로직이 실기기 없이도 확인된다(plan.md D-02 / D-08).
 *
 * @param naverUrl naver 버튼이 이동할 주소. 상수를 박지 않고 받는 이유는 테스트에서 바꿔 넣기 위해서다.
 * @param dispatcher 코루틴을 어느 스레드 풀에서 돌릴지. **기본값을 주지 않는다** —
 *   기본값이 있으면 테스트가 주입을 잊어도 통과해버려 주입 장치가 장식이 된다(plan.md D-11).
 */
class MainViewModel(
    private val naverUrl: String,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())

    /**
     * StateFlow: "현재 값"을 항상 가지고 있고 값이 바뀔 때마다 구독자에게 알려주는 흐름.
     * Activity는 이것 하나만 구독해 화면 전체를 다시 그린다.
     */
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    /**
     * 일회성 이벤트 통로.
     *
     * `Channel`을 쓰고 `SharedFlow(replay=0)`를 쓰지 않는 이유가 중요하다:
     * `SharedFlow(replay=0)`는 **그 순간 듣고 있는 구독자가 없으면 이벤트를 그냥 버린다.**
     * 화면이 잠깐 멈춘(STOPPED) 사이에 도착한 종료 이벤트가 사라지면
     * "가끔 종료가 안 되는" 재현하기 어려운 버그가 된다.
     * `Channel`은 구독자가 없어도 버퍼에 담아 두었다가 나중에 전달한다(plan.md D-03).
     */
    private val _events = Channel<MainUiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** 확장 설치가 끝났다. */
    fun onBridgeReady() {
        _uiState.value = _uiState.value.copy(bridgeReady = true)
    }

    /** 페이지 로드가 시작됐다 → 로딩 UI를 켠다. */
    fun onPageStart() {
        _uiState.value = _uiState.value.copy(isLoading = true)
    }

    /**
     * 페이지 로드가 끝났다 → 로딩 UI를 끈다.
     *
     * @param success 로드 성공 여부. **성공이든 실패든 로딩은 반드시 꺼진다.**
     *   실패했을 때 끄지 않으면 네트워크가 없는 상황에서 로딩이 영원히 도는 화면이 된다(AC-009-4).
     */
    fun onPageStop(success: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = false)
    }

    /** 웹 히스토리에 뒤로 갈 곳이 있는지 바뀌었다. Activity가 이 값으로 뒤로가기 콜백을 켜고 끈다. */
    fun onCanGoBackChanged(canGoBack: Boolean) {
        _uiState.value = _uiState.value.copy(canGoBack = canGoBack)
    }

    /** 주소가 바뀌었다. */
    fun onLocationChanged(url: String?) {
        _uiState.value = _uiState.value.copy(currentUrl = url)
    }

    /** naver 버튼을 눌렀다. */
    fun onNaverClicked() {
        emit(MainUiEvent.Navigate(naverUrl))
    }

    /**
     * 뒤로가기를 눌렀다(뒤로 갈 곳이 있을 때만 Activity가 이 함수를 부른다).
     *
     * 여기서 직접 `session.goBack()`을 부르지 않고 이벤트만 내보내는 것이 핵심이다.
     * 그래야 ViewModel이 GeckoSession을 몰라도 되고, 이 판단 로직이 JVM 테스트로 검증된다.
     */
    fun onBackPressed() {
        emit(MainUiEvent.NavigateBack)
    }

    /** 웹 페이지가 브리지로 appFinish를 호출했다(REQ-005). */
    fun onAppFinishRequested() {
        emit(MainUiEvent.Finish)
    }

    private fun emit(event: MainUiEvent) {
        // viewModelScope: 이 ViewModel이 살아 있는 동안만 도는 코루틴 스코프.
        // ViewModel이 정리되면 안에서 돌던 코루틴도 함께 취소된다.
        viewModelScope.launch(dispatcher) {
            _events.send(event)
        }
    }

    /**
     * ViewModel을 만드는 방법을 안드로이드에게 알려주는 객체.
     *
     * 생성자에 인자가 있는 ViewModel은 시스템이 스스로 만들 수 없어서 이 Factory가 필요하다.
     * DI 프레임워크 대신 손으로 배선하는 방식이다(plan.md §2.2).
     */
    class Factory(
        private val naverUrl: String,
        private val dispatcher: CoroutineDispatcher,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(naverUrl, dispatcher) as T
        }
    }
}

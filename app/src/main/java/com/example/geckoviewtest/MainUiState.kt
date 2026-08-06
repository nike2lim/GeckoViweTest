package com.example.geckoviewtest

/**
 * 화면 전체의 상태를 담는 단일 객체.
 *
 * 화면에 보이는 모든 것은 이 객체의 투영이다. 예를 들어 로딩 표시가 보이는지는
 * `ProgressBar.visibility`가 아니라 [isLoading]이 진실의 원천이고,
 * Activity의 `render()` 함수가 그것을 화면에 옮긴다.
 *
 * 상태를 이렇게 한 덩어리로 모으는 이유: 상태가 View 여기저기에 흩어져 있으면
 * "지금 화면이 어떤 상태인지"를 한눈에 알 수 없고, 두 곳이 서로 어긋난 상태가 만들어진다.
 *
 * @property isLoading 페이지 로드가 진행 중인가(REQ-009)
 * @property currentUrl 현재 열려 있는 주소
 * @property canGoBack 웹 히스토리에 뒤로 갈 곳이 있는가(REQ-011)
 * @property bridgeReady 확장 설치가 끝나 브리지를 쓸 수 있는가
 */
data class MainUiState(
    val isLoading: Boolean = false,
    val currentUrl: String? = null,
    val canGoBack: Boolean = false,
    val bridgeReady: Boolean = false,
)

/**
 * **한 번만 일어나야 하는 일**을 UI로 전달하는 통로.
 *
 * 상태(MainUiState)와 분리한 이유: "종료하라"는 상태가 아니라 사건이다.
 * 이것을 상태에 담으면 화면이 다시 만들어질 때 같은 값을 다시 읽어
 * **종료가 두 번 일어나거나 이미 끝난 이동이 되풀이된다.**
 */
sealed interface MainUiEvent {
    /** 지정한 주소로 이동하라. */
    data class Navigate(val url: String) : MainUiEvent

    /** 웹 히스토리에서 한 칸 뒤로 가라. */
    data object NavigateBack : MainUiEvent

    /** Activity를 종료하라(REQ-005). */
    data object Finish : MainUiEvent
}

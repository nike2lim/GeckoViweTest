package com.example.geckoviewtest

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 화면 상태 전이와 일회성 이벤트 발행을 실기기 없이 고정한다.
 *
 * **보장하는 것:** 로딩 표시가 켜지고 꺼지는 전이(성공·실패 **양쪽**), `canGoBack` 전이,
 * naver 이동·뒤로가기·종료가 상태가 아니라 **이벤트**로 나가는지.
 *
 * **보장하지 않는 것:** 이 이벤트를 받은 Activity가 실제로 `session.goBack()`이나 `finish()`를
 * 부르는지는 여기서 알 수 없다. 그 확인은 Step 6·7의 실기기 게이트가 담당한다.
 * 즉 **이 스위트가 초록이어도 뒤로가기가 화면에서 동작한다는 뜻은 아니다.**
 *
 * `Dispatchers.setMain`: ViewModel 안의 `viewModelScope`는 기본적으로 안드로이드 메인 스레드를 쓰는데
 * JVM 테스트에는 그런 스레드가 없어 그대로 두면 예외가 난다. 테스트용 디스패처로 갈아끼운다.
 * `Turbine test { }`: Flow가 내보내는 값을 `awaitItem()`으로 하나씩 꺼내 확인하게 해준다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        // 갈아끼운 메인 디스패처를 되돌리지 않으면 다음 테스트 클래스가 영향을 받는다.
        Dispatchers.resetMain()
    }

    private fun viewModel() = MainViewModel(naverUrl = NAVER_URL, dispatcher = testDispatcher)

    @Test
    fun `초기 상태는 로딩도 아니고 뒤로 갈 곳도 없다`() {
        val state = viewModel().uiState.value

        assertFalse(state.isLoading)
        assertFalse(state.canGoBack)
        assertFalse(state.bridgeReady)
    }

    @Test
    fun `onPageStart는 로딩을 켠다`() {
        val vm = viewModel()

        vm.onPageStart()

        assertTrue(vm.uiState.value.isLoading)
    }

    @Test
    fun `onPageStop은 성공했을 때 로딩을 끈다`() {
        val vm = viewModel()
        vm.onPageStart()

        vm.onPageStop(success = true)

        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `onPageStop은 실패했을 때도 로딩을 끈다`() {
        // 이 케이스가 없으면 네트워크가 끊긴 상황에서 로딩이 영원히 도는 구현이 통과한다(AC-009-4).
        val vm = viewModel()
        vm.onPageStart()

        vm.onPageStop(success = false)

        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `onCanGoBackChanged는 양방향으로 전이한다`() {
        // 한 방향만 확인하면 "항상 켜져 있는" 구현을 잡지 못한다(AC-011-4의 취지).
        val vm = viewModel()

        vm.onCanGoBackChanged(true)
        assertTrue(vm.uiState.value.canGoBack)

        vm.onCanGoBackChanged(false)
        assertFalse(vm.uiState.value.canGoBack)
    }

    @Test
    fun `onLocationChanged는 현재 주소를 반영한다`() {
        val vm = viewModel()

        vm.onLocationChanged("https://example.com/")

        assertEquals("https://example.com/", vm.uiState.value.currentUrl)
    }

    @Test
    fun `onBridgeReady는 브리지 준비 상태를 켠다`() {
        val vm = viewModel()

        vm.onBridgeReady()

        assertTrue(vm.uiState.value.bridgeReady)
    }

    @Test
    fun `naver 버튼은 주입받은 주소로 이동 이벤트를 낸다`() = runTest {
        val vm = viewModel()

        vm.events.test {
            vm.onNaverClicked()

            assertEquals(MainUiEvent.Navigate(NAVER_URL), awaitItem())
        }
    }

    @Test
    fun `뒤로가기는 NavigateBack 이벤트를 낸다`() = runTest {
        // ViewModel이 GeckoSession을 직접 만지지 않고 이벤트만 낸다는 것이 이 설계의 핵심이다.
        val vm = viewModel()

        vm.events.test {
            vm.onBackPressed()

            assertEquals(MainUiEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `appFinish 요청은 Finish 이벤트를 낸다`() = runTest {
        val vm = viewModel()

        vm.events.test {
            vm.onAppFinishRequested()

            assertEquals(MainUiEvent.Finish, awaitItem())
        }
    }

    @Test
    fun `이벤트는 구독자가 붙기 전에 발행돼도 유실되지 않는다`() = runTest {
        // SharedFlow(replay=0)를 썼다면 이 테스트가 실패한다.
        // 화면이 멈춘 사이 도착한 종료 이벤트가 사라지는 비결정적 결함을 막는 근거다(plan.md D-03).
        val vm = viewModel()

        vm.onAppFinishRequested()

        vm.events.test {
            assertEquals(MainUiEvent.Finish, awaitItem())
        }
    }

    @Test
    fun `Factory는 주입값을 그대로 넘겨 ViewModel을 만든다`() = runTest {
        // Factory가 인자를 흘리지 않는지 확인한다. 여기서 naverUrl을 잘못 넘기면
        // 화면에서는 "버튼을 눌러도 엉뚱한 곳으로 간다"로만 드러나 원인을 찾기 어렵다.
        val factory = MainViewModel.Factory(naverUrl = "http://other.example", dispatcher = testDispatcher)

        val vm = factory.create(MainViewModel::class.java)

        vm.events.test {
            vm.onNaverClicked()

            assertEquals(MainUiEvent.Navigate("http://other.example"), awaitItem())
        }
    }

    private companion object {
        const val NAVER_URL = "http://naver.com"
    }
}

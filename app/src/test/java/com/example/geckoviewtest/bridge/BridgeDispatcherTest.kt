package com.example.geckoviewtest.bridge

import com.example.geckoviewtest.data.AppInfoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 함수명에 따라 올바른 곳으로 요청이 넘어가는지, 그리고 **모르는 함수명이 조용히 통과하지 않는지**를 고정한다.
 *
 * **보장하는 것:** `getVersionName` 위임, `appFinish` 위임, 미지원 함수명의 `UNKNOWN_FUNCTION` 처리,
 * Repository가 예외를 던졌을 때 그 예외가 **삼켜지지 않고 전파되는지**,
 * 그리고 코루틴 **취소**가 오류 결과로 둔갑하지 않는지.
 *
 * **보장하지 않는 것:** 전파된 예외를 `INTERNAL_ERROR` 응답으로 바꾸는 것은
 * [NativeBridgeHandler]의 몫이며 그 변환은 여기서 검증하지 못한다(GeckoView 의존).
 * JS 쪽 `ALLOWED_FUNCTIONS` 배열과 이 `when` 분기가 일치하는지도 여기서는 알 수 없다 —
 * 그쪽은 `BridgeWireContractTest`가 파일을 직접 읽어 대조한다.
 *
 * `runTest`: 코루틴 테스트용 빌더. suspend 함수를 테스트에서 부를 수 있게 해주고
 * 안에서 시간을 건너뛰어 지연을 기다리지 않는다.
 * `UnconfinedTestDispatcher`: 코루틴을 즉시 실행해 결과 확인을 단순하게 만드는 테스트용 디스패처.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BridgeDispatcherTest {

    /**
     * 테스트용 가짜 Repository. 진짜 PackageManager 없이 원하는 값을 돌려준다.
     *
     * 던지는 동작을 [throws] 플래그로 **따로** 둔 이유: 프로덕션에서 `null`은 "예외"가 아니라
     * `UNKNOWN` 폴백을 뜻한다(`AppInfoRepositoryImpl`). null을 던짐의 트리거로 쓰면
     * 테스트가 프로덕션과 반대 의미를 학습시킨다.
     */
    private class FakeAppInfoRepository(
        private val version: String = "9.9.9",
        private val throws: Boolean = false,
        private val cancels: Boolean = false,
    ) : AppInfoRepository {
        override fun getVersionName(): String = when {
            cancels -> throw CancellationException("작업이 취소됐다")
            throws -> throw IllegalStateException("버전 조회 실패")
            else -> version
        }
    }

    /** 종료 요청이 실제로 도달했는지만 기록하는 가짜 Host. */
    private class FakeBridgeHost : BridgeHost {
        var finishCallCount = 0
        override fun requestFinish() {
            finishCallCount += 1
        }
    }

    private fun dispatcher(
        repository: AppInfoRepository = FakeAppInfoRepository(),
        host: BridgeHost = FakeBridgeHost(),
    ) = BridgeDispatcher(repository, host, UnconfinedTestDispatcher())

    private fun request(name: String) = BridgeRequest(id = "req-1", name = name)

    @Test
    fun `getVersionName은 Repository의 값을 그대로 돌려준다`() = runTest {
        val result = dispatcher().handle(request("getVersionName"))

        assertEquals(BridgeResult.Success(JsonPrimitive("9.9.9")), result)
    }

    @Test
    fun `appFinish는 Host에 종료를 요청한다`() = runTest {
        val host = FakeBridgeHost()

        val result = dispatcher(host = host).handle(request("appFinish"))

        assertEquals(1, host.finishCallCount)
        assertTrue(result is BridgeResult.Success)
    }

    @Test
    fun `모르는 함수명은 UNKNOWN_FUNCTION으로 거절한다`() = runTest {
        // 이 케이스가 없으면 오타 난 함수명이 조용히 성공으로 처리되는 구현이 통과한다(AC-006-2).
        val result = dispatcher().handle(request("존재하지않는함수"))

        assertEquals(ErrorCode.UNKNOWN_FUNCTION, (result as BridgeResult.Failure).code)
    }

    @Test
    fun `모르는 함수명일 때는 Host를 건드리지 않는다`() = runTest {
        val host = FakeBridgeHost()

        dispatcher(host = host).handle(request("존재하지않는함수"))

        assertEquals(0, host.finishCallCount)
    }

    @Test
    fun `실패 사유 문자열에 함수명이 들어간다`() = runTest {
        // 사유를 읽어도 무엇이 잘못됐는지 모르면 AC-006-2의 "사유 판독"이 의미가 없다.
        val result = dispatcher().handle(request("오타난이름"))

        assertTrue((result as BridgeResult.Failure).message.contains("오타난이름"))
    }

    @Test
    fun `Repository 예외는 삼켜지지 않고 그대로 전파된다`() = runTest {
        // 이 케이스가 없으면 "예외를 조용히 삼키고 성공으로 둔갑시키는" 구현이 통과한다.
        // 전파된 예외를 INTERNAL_ERROR 응답으로 바꾸는 것은 NativeBridgeHandler의 몫이며,
        // 그 변환은 GeckoView 의존성 때문에 여기서 검증하지 못한다.
        val failing = FakeAppInfoRepository(throws = true)

        val thrown = runCatching { dispatcher(repository = failing).handle(request("getVersionName")) }

        assertTrue(thrown.isFailure)
        assertFalse(thrown.isSuccess)
    }

    @Test
    fun `코루틴 취소는 오류 결과로 바뀌지 않고 취소인 채로 전파된다`() = runTest {
        // 취소는 "실패"가 아니라 "그만두라"는 신호다. 이것을 BridgeResult.Failure로 바꿔버리면
        // 취소가 위로 전파되지 않아 스코프가 정리되지 않는다.
        //
        // 범위 주의: 이 케이스는 **BridgeDispatcher 경계까지만** 고정한다.
        // 같은 취지의 방어가 NativeBridgeHandler에도 있지만(`catch (CancellationException) { throw e }`가
        // `catch (Exception)`보다 앞에 있어야 한다), 그 클래스는 GeckoView와 android.util.Log에 묶여 있어
        // JVM 테스트에서 호출 자체가 불가능하다. **여기가 초록이어도 그쪽 catch 순서는 검증되지 않았다.**
        val cancelling = FakeAppInfoRepository(cancels = true)

        val thrown = runCatching { dispatcher(repository = cancelling).handle(request("getVersionName")) }

        assertTrue(thrown.exceptionOrNull() is CancellationException)
    }
}

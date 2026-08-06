package com.example.geckoviewtest.gecko

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.mozilla.geckoview.GeckoResult
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * GeckoView의 비동기 타입인 [GeckoResult]와 코틀린 코루틴을 잇는 변환기.
 *
 * GeckoView API는 결과를 [GeckoResult]로 돌려준다. 이것은 자바의 Future/Promise에 해당하는 타입으로,
 * `then`/`accept`에 콜백을 등록해 결과를 받는다. 코루틴의 `suspend fun`과는 형태가 달라 그대로 섞이지 않는다.
 *
 * **변환 코드를 이 파일 한 곳에만 두는 이유:** 변환이 여기저기 흩어지면 그때마다 `runBlocking`이나
 * [GeckoResult.poll] 같은 "잠깐이면 되니까" 코드가 끼어들고, 그것이 UI 스레드를 멈추게 한다.
 * 진입점을 하나로 좁혀 두면 그런 코드가 들어올 자리가 없다(plan.md D-09).
 */

/**
 * [GeckoResult]가 값을 낼 때까지 코루틴을 멈춰 기다린다.
 *
 * `suspendCancellableCoroutine`: 콜백 기반 API를 suspend 함수로 감싸는 코루틴 표준 도구.
 * 콜백이 오면 `cont.resume(...)`으로 코루틴을 깨운다.
 *
 * 코루틴이 취소되면 [GeckoResult.cancel]도 함께 호출해 엔진 쪽 작업을 정리한다 —
 * 이것을 하지 않으면 화면이 사라진 뒤에도 로드가 계속 돈다.
 *
 * **[GeckoResult.poll]을 쓰지 않는다.** poll은 스레드를 통째로 막는 블로킹 호출이라
 * UI 스레드에서 부르면 화면이 굳는다.
 */
suspend fun <T> GeckoResult<T>.await(): T = suspendCancellableCoroutine { cont ->
    accept(
        { value ->
            @Suppress("UNCHECKED_CAST")
            cont.resume(value as T)
        },
        // 자바 API라 예외가 null일 수 있다고 선언돼 있다. 실제로 null이 오는 경우는 없지만
        // null이면 코루틴이 영원히 깨어나지 않으므로 대체 예외로 바꿔 반드시 재개시킨다.
        { error -> cont.resumeWithException(error ?: IllegalStateException("GeckoResult가 사유 없이 실패했다")) },
    )
    cont.invokeOnCancellation { cancel() }
}

/**
 * 반대 방향 변환. suspend 블록의 결과를 GeckoView가 이해하는 [GeckoResult]로 감싼다.
 *
 * GeckoView의 델리게이트(예: `MessageDelegate.onMessage`)는 반환 타입이 [GeckoResult]인데
 * 우리 쪽 처리 로직은 suspend 함수다. 그 사이를 메우는 것이 이 함수다.
 *
 * 블록이 예외를 던지면 [GeckoResult.completeExceptionally]로 전달되어
 * 확장 쪽 JS Promise가 reject된다 — 예외를 삼키지 않는다.
 */
fun <T> CoroutineScope.geckoResultOf(block: suspend () -> T): GeckoResult<T> {
    val result = GeckoResult<T>()
    launch {
        try {
            result.complete(block())
        } catch (e: Throwable) {
            result.completeExceptionally(e)
        }
    }
    return result
}

package com.example.geckoviewtest.bridge

import android.util.Log
import com.example.geckoviewtest.gecko.geckoResultOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.WebExtension

/**
 * WebExtension의 `background.js`가 보낸 네이티브 메시지를 받는 **얇은 어댑터**.
 *
 * 데이터 흐름에서의 위치:
 * `background.js` → (native messaging) → **이 클래스** → [BridgeDispatcher] → Repository
 *
 * `WebExtension.MessageDelegate`: GeckoView가 제공하는 인터페이스로,
 * `extension.setMessageDelegate(this, nativeApp)`으로 등록해야 호출된다.
 *
 * **이 클래스에는 업무 로직을 두지 않는다.** JSON 문자열을 [BridgeProtocol]에 넘기고
 * 결과를 [GeckoResult]로 되돌리는 변환만 한다. 로직이 여기 들어오면
 * GeckoView 의존성 때문에 JVM 테스트가 불가능해져 커버리지 밖으로 밀려난다(plan.md §7.3).
 */
class NativeBridgeHandler(
    private val scope: CoroutineScope,
    private val dispatcher: BridgeDispatcher,
) : WebExtension.MessageDelegate {

    /**
     * `background.js`의 `browser.runtime.sendNativeMessage(...)` 한 번이 이 함수 한 번에 대응한다.
     *
     * @param nativeApp `setMessageDelegate`의 두 번째 인자로 등록한 식별자가 그대로 들어온다.
     * @param message JS가 보낸 값. 이 설계는 JSON **문자열**을 주고받기로 했다(plan.md §3.1).
     * @return 여기서 돌려주는 [GeckoResult]가 resolve되면 그 값이 곧 확장 쪽 Promise의 결과가 된다.
     */
    override fun onMessage(
        nativeApp: String,
        message: Any,
        sender: WebExtension.MessageSender,
    ): GeckoResult<Any>? {
        // U-04(문자열 payload 허용 여부) 판정 근거를 남긴다.
        // 실제로 어떤 자바 타입으로 도착했는지는 이 로그로만 확인할 수 있다.
        Log.d(TAG, "onMessage nativeApp=$nativeApp javaType=${message.javaClass.name}")

        val raw = message.toString()

        return scope.geckoResultOf {
            val parsed = BridgeProtocol.parseRequest(raw)
            val responseJson = parsed.fold(
                onSuccess = { request ->
                    val result = try {
                        dispatcher.handle(request)
                    } catch (e: CancellationException) {
                        // 코루틴 취소는 "오류"가 아니라 "이 작업은 그만두라"는 신호다.
                        // 이것까지 아래 catch로 잡아 오류 응답으로 바꿔버리면 취소가 위로 전파되지 않아
                        // 스코프가 정리되지 않는다. 그래서 다시 던져 취소 전파를 유지한다.
                        throw e
                    } catch (e: Exception) {
                        // 예외를 삼키지 않고 페이지가 읽을 수 있는 오류로 바꾼다.
                        Log.w(TAG, "브리지 처리 중 예외: name=${request.name}", e)
                        BridgeResult.Failure(ErrorCode.INTERNAL_ERROR, e.message ?: e.javaClass.simpleName)
                    }
                    BridgeProtocol.encodeResult(request.id, result)
                },
                onFailure = { error ->
                    Log.w(TAG, "요청 파싱 실패: $raw", error)
                    BridgeProtocol.encodeInvalidRequest(error.message ?: "요청을 해석할 수 없다")
                },
            )
            responseJson
        }
    }

    private companion object {
        const val TAG = "NativeBridge"
    }
}

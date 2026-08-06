package com.example.geckoviewtest.bridge

import com.example.geckoviewtest.data.AppInfoRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive

/**
 * 함수명을 보고 실제 처리 주체에게 넘기는 **분배기**.
 *
 * 데이터 흐름: [NativeBridgeHandler] → **이 클래스** → [AppInfoRepository] 또는 [BridgeHost]
 *
 * 브리지 함수를 새로 추가할 때 손대는 코틀린 쪽 지점은 아래 `when` 하나다.
 * 다만 **JS 쪽 `background.js`의 `ALLOWED_FUNCTIONS` 배열도 함께 고쳐야 한다.**
 * 한쪽만 고쳤을 때의 결과가 비대칭이라는 점이 중요하다:
 * - JS에만 추가 → 네이티브가 `UNKNOWN_FUNCTION`으로 거절한다(**시끄럽게 실패, 안전**)
 * - 코틀린에만 추가 → JS 화이트리스트가 먼저 막아 아무 일도 안 일어난다(**조용히 실패**)
 *
 * 그래서 순서는 항상 **JS를 먼저** 고치는 쪽이 안전하다.
 *
 * 이 클래스도 안드로이드 타입을 쓰지 않는 순수 코틀린이다 — 인터페이스 두 개에만 의존한다.
 */
class BridgeDispatcher(
    private val appInfoRepository: AppInfoRepository,
    private val host: BridgeHost,
    // CoroutineDispatcher를 생성자로 받고 **기본값을 주지 않는다.**
    // 기본값이 있으면 테스트가 주입을 잊어도 통과해버려서 주입 장치가 장식이 된다(plan.md D-11).
    private val dispatcher: CoroutineDispatcher,
) {

    suspend fun handle(request: BridgeRequest): BridgeResult = withContext(dispatcher) {
        when (request.name) {
            BridgeProtocol.FN_GET_VERSION_NAME ->
                BridgeResult.Success(JsonPrimitive(appInfoRepository.getVersionName()))

            BridgeProtocol.FN_APP_FINISH -> {
                // appFinish는 "성공했다"를 먼저 응답한 뒤 실제 종료가 일어난다.
                // 프로세스가 죽는 중이라 페이지가 이 응답을 못 받을 수도 있는데,
                // 그래서 index.html·page-bridge.js는 이 Promise의 resolve에 의존하지 않는다(plan.md §3.5).
                host.requestFinish()
                BridgeResult.Success(JsonPrimitive(true))
            }

            else -> BridgeResult.Failure(
                ErrorCode.UNKNOWN_FUNCTION,
                "지원하지 않는 함수명이다: ${request.name}",
            )
        }
    }
}

/**
 * 브리지가 "앱 자체에 무언가를 요청"할 때 쓰는 창구.
 *
 * 순수 인터페이스로 두는 이유: [BridgeDispatcher]가 Activity나 ViewModel 같은
 * 안드로이드 타입을 직접 알게 되면 JVM 테스트가 불가능해진다.
 * 테스트에서는 이 인터페이스의 가짜 구현을 넣어 "종료가 요청됐는가"만 확인한다.
 */
interface BridgeHost {
    /** Activity 종료를 요청한다. 실제 종료는 UI 레이어가 수행한다. */
    fun requestFinish()
}

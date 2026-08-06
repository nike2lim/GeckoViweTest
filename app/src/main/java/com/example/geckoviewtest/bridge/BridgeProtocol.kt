package com.example.geckoviewtest.bridge

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * 웹 페이지 ↔ 네이티브 사이를 오가는 메시지의 **계약(contract)**.
 *
 * 이 파일이 정의하는 JSON 형태는 JS 쪽 `background.js`·`bridge-client.js`·`page-bridge.js`와
 * 글자 단위로 맞아야 한다. 어긋나면 컴파일러도 린트도 잡지 못하고 **런타임에 조용히** 실패한다.
 * 그래서 스키마 원문을 아래에 적어 두고, JS 파일들이 이 주석을 단일 원본으로 참조한다.
 *
 * ```
 * 요청 (페이지 → background.js → 네이티브)
 *   { "id": "<클라이언트가 만든 고유 문자열>", "type": "call", "name": "<함수명>", "payload": { } }
 *
 * 응답 (네이티브 → background.js → 페이지)
 *   { "id": "<요청 id 그대로>", "type": "result", "ok": true,  "value": <임의 JSON 값> }
 *   { "id": "<요청 id 그대로>", "type": "result", "ok": false, "error": { "code": "...", "message": "..." } }
 * ```
 *
 * **이 클래스는 안드로이드 타입을 하나도 쓰지 않는 순수 코틀린이다.** 의도적인 설계다 —
 * `org.json`을 쓰면 JVM 단위 테스트에서 실행조차 되지 않아 커버리지를 벌 수 없다(plan.md §7.1).
 * 여기에 `Context`나 `android.*`를 들이는 순간 그 성질이 깨진다.
 */
object BridgeProtocol {

    /**
     * 네이티브 앱 식별자.
     *
     * 이 문자열은 `app/src/main/assets/messaging/background.js`의 `NATIVE_APP` 상수와
     * **반드시 같아야 한다.** 어긋나면 예외도 로그도 없이 아무 일도 일어나지 않는다
     * (GeckoView는 native manifest 파일을 쓰지 않고 이 문자열 자체를 앱 식별자로 삼는다).
     */
    const val NATIVE_APP = "browser"

    /** 브리지가 제공하는 함수명. JS 쪽 `ALLOWED_FUNCTIONS` 배열과 쌍이다. */
    const val FN_GET_VERSION_NAME = "getVersionName"
    const val FN_APP_FINISH = "appFinish"

    const val TYPE_CALL = "call"
    const val TYPE_RESULT = "result"

    /**
     * ignoreUnknownKeys: JS가 모르는 필드를 더 보내도 파싱이 깨지지 않게 한다.
     * explicitNulls=false: null인 필드를 JSON에서 아예 빼서 `value`/`error` 중 하나만 나가게 한다.
     */
    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    /**
     * 네이티브가 받은 JSON 문자열을 [BridgeRequest]로 바꾼다.
     *
     * 실패를 예외로 던지지 않고 [Result]로 감싸 돌려주는 이유:
     * 잘못된 요청 하나가 브리지 전체를 죽이면 안 되고, 호출자가 이것을
     * `INVALID_REQUEST` 응답으로 바꿔 페이지에 되돌려줘야 하기 때문이다.
     */
    fun parseRequest(raw: String): Result<BridgeRequest> = runCatching {
        val request = json.decodeFromString(BridgeRequest.serializer(), raw)
        require(request.id.isNotBlank()) { "id가 비어 있다" }
        require(request.type == TYPE_CALL) { "type이 '$TYPE_CALL'이 아니다: ${request.type}" }
        require(request.name.isNotBlank()) { "name이 비어 있다" }
        request
    }

    /** 처리 결과를 페이지로 돌려보낼 JSON 문자열로 만든다. */
    fun encodeResult(id: String, result: BridgeResult): String {
        val response = when (result) {
            is BridgeResult.Success -> BridgeResponse(id = id, ok = true, value = result.value)
            is BridgeResult.Failure -> BridgeResponse(
                id = id,
                ok = false,
                error = BridgeError(code = result.code, message = result.message),
            )
        }
        return json.encodeToString(BridgeResponse.serializer(), response)
    }

    /**
     * 요청 파싱조차 실패해 id를 모를 때 쓰는 응답.
     * id가 없으면 페이지 쪽에서 어느 요청의 답인지 짝을 지을 수 없으므로 빈 문자열을 쓴다.
     */
    fun encodeInvalidRequest(message: String): String =
        encodeResult("", BridgeResult.Failure(ErrorCode.INVALID_REQUEST, message))
}

/**
 * 오류 코드는 **닫힌 집합**이다. 페이지 쪽 코드가 이 값으로 분기할 수 있어야 하므로
 * 새 코드를 늘릴 때는 JS 쪽과 함께 검토한다.
 */
object ErrorCode {
    /** 화이트리스트에 없는 함수명을 호출했다. AC-006-2가 이 코드를 요구한다. */
    const val UNKNOWN_FUNCTION = "UNKNOWN_FUNCTION"

    /** JSON이 깨졌거나 필수 필드가 없다. */
    const val INVALID_REQUEST = "INVALID_REQUEST"

    /** 네이티브 처리 중 예상하지 못한 예외가 났다. */
    const val INTERNAL_ERROR = "INTERNAL_ERROR"
}

/** 페이지가 보낸 함수 호출 요청. */
@Serializable
data class BridgeRequest(
    val id: String,
    val type: String = BridgeProtocol.TYPE_CALL,
    val name: String,
    val payload: JsonObject? = null,
)

/** 네이티브가 돌려주는 응답. `ok`에 따라 `value` 또는 `error` 중 하나만 채워진다. */
@Serializable
data class BridgeResponse(
    val id: String,
    val type: String = BridgeProtocol.TYPE_RESULT,
    val ok: Boolean,
    val value: JsonElement? = null,
    val error: BridgeError? = null,
)

@Serializable
data class BridgeError(
    val code: String,
    val message: String,
)

/**
 * 네이티브 내부에서 함수 처리 결과를 나르는 타입.
 *
 * `sealed interface`: 구현체가 이 파일 안의 둘로 고정된다는 코틀린 문법.
 * 덕분에 `when`으로 분기할 때 컴파일러가 빠진 경우를 잡아준다.
 */
sealed interface BridgeResult {
    data class Success(val value: JsonElement) : BridgeResult
    data class Failure(val code: String, val message: String) : BridgeResult
}

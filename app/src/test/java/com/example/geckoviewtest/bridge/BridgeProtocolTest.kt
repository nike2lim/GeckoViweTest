package com.example.geckoviewtest.bridge

import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 웹 페이지와 네이티브가 주고받는 JSON 형태(§3.2 스키마)를 고정한다.
 *
 * **보장하는 것:** 요청 JSON이 [BridgeRequest]로 올바르게 해석되는지, 필수 필드가 빠지거나
 * 형태가 어긋난 요청이 예외가 아니라 실패 결과로 처리되는지, 응답 JSON의 필드 이름과 구조가
 * JS 쪽이 기대하는 그대로인지.
 *
 * **보장하지 않는 것 (중요):**
 * - JS 파일들이 실제로 이 형태로 보내는지는 검증하지 못한다. 스키마 불일치는 컴파일러가 잡지 못하고
 *   실기기에서만 드러난다. 그 확인은 Step 2·4의 실기기 게이트가 담당한다.
 * - **`index.html` 경로가 통과한다고 해서 외부 사이트 경로가 동작한다는 뜻이 아니다.**
 *   두 경로는 전송 파일이 물리적으로 다르다(bridge-client.js vs content.js+page-bridge.js).
 *   이 스위트가 초록이어도 REQ-010은 별도로 검증해야 한다(AC-006-4).
 */
class BridgeProtocolTest {

    @Test
    fun `정상 요청 JSON을 파싱한다`() {
        val raw = """{"id":"req-1","type":"call","name":"getVersionName","payload":{}}"""

        val request = BridgeProtocol.parseRequest(raw).getOrThrow()

        assertEquals("req-1", request.id)
        assertEquals("getVersionName", request.name)
    }

    @Test
    fun `payload가 없어도 파싱된다`() {
        // JS 쪽이 인자 없는 함수를 부를 때 payload를 아예 빼고 보낼 수 있다.
        val raw = """{"id":"req-2","type":"call","name":"appFinish"}"""

        assertTrue(BridgeProtocol.parseRequest(raw).isSuccess)
    }

    @Test
    fun `JSON이 깨져 있으면 예외가 아니라 실패 결과를 돌려준다`() {
        // 이 케이스가 없으면 잘못된 요청 하나가 브리지 전체를 죽이는 구현이 통과한다.
        val result = BridgeProtocol.parseRequest("이건 JSON이 아니다")

        assertTrue(result.isFailure)
    }

    @Test
    fun `id가 비어 있으면 실패로 처리한다`() {
        // id가 없으면 페이지 쪽에서 응답을 어느 요청에 짝지을지 알 수 없다.
        val raw = """{"id":"","type":"call","name":"getVersionName"}"""

        assertTrue(BridgeProtocol.parseRequest(raw).isFailure)
    }

    @Test
    fun `name이 비어 있으면 실패로 처리한다`() {
        // 빈 이름을 통과시키면 디스패처까지 내려가 `지원하지 않는 함수명이다: ` 라는
        // 이름이 빠진 사유가 페이지로 돌아간다 — 무엇을 잘못 불렀는지 알 수 없는 응답이 된다.
        val raw = """{"id":"req-7","type":"call","name":"  "}"""

        assertTrue(BridgeProtocol.parseRequest(raw).isFailure)
    }

    @Test
    fun `type이 call이 아니면 실패로 처리한다`() {
        val raw = """{"id":"req-3","type":"result","name":"getVersionName"}"""

        assertTrue(BridgeProtocol.parseRequest(raw).isFailure)
    }

    @Test
    fun `모르는 필드가 섞여 있어도 파싱된다`() {
        // JS 쪽이 나중에 필드를 추가해도 네이티브가 깨지지 않아야 한다.
        val raw = """{"id":"req-4","type":"call","name":"getVersionName","이런필드는모른다":1}"""

        assertTrue(BridgeProtocol.parseRequest(raw).isSuccess)
    }

    @Test
    fun `성공 응답은 value를 담고 error를 담지 않는다`() {
        val json = BridgeProtocol.encodeResult("req-5", BridgeResult.Success(JsonPrimitive("1.0.0")))

        // 필드 이름이 하나라도 바뀌면 JS 쪽이 조용히 undefined를 읽게 된다.
        assertTrue(json.contains(""""id":"req-5""""))
        assertTrue(json.contains(""""ok":true"""))
        assertTrue(json.contains(""""value":"1.0.0""""))
        assertTrue(!json.contains(""""error""""))
    }

    @Test
    fun `실패 응답은 code와 message를 담는다`() {
        // AC-006-2가 "페이지에서 오류 사유를 읽을 수 있을 것"을 요구한다.
        val json = BridgeProtocol.encodeResult(
            "req-6",
            BridgeResult.Failure(ErrorCode.UNKNOWN_FUNCTION, "없는 함수"),
        )

        assertTrue(json.contains(""""ok":false"""))
        assertTrue(json.contains(""""code":"UNKNOWN_FUNCTION""""))
        assertTrue(json.contains(""""message":"없는 함수""""))
    }

    @Test
    fun `파싱 실패용 응답은 INVALID_REQUEST를 쓴다`() {
        val json = BridgeProtocol.encodeInvalidRequest("형태가 틀렸다")

        assertTrue(json.contains(""""code":"INVALID_REQUEST""""))
    }

    @Test
    fun `nativeApp 식별자는 background_js와 같은 값이어야 한다`() {
        // 이 단정은 코틀린 쪽 상수가 바뀌는 것을 막는다.
        // JS 쪽 값까지 대조하지는 못하므로, 불일치의 최종 확인은 Step 2 실기기 게이트가 한다.
        assertEquals("browser", BridgeProtocol.NATIVE_APP)
    }
}

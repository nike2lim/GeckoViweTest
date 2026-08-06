package com.example.geckoviewtest.bridge

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * WebExtension `manifest.json`의 **구조 불변식**을 고정한다.
 *
 * 이 파일은 코드가 아니라 설정이라 컴파일러가 한 글자도 봐주지 않는다. 그런데 여기서 한 줄이
 * 빠지면 브리지는 **깨지지 않고 반쪽만 동작한다** — 이 스위트가 존재하는 이유가 그것이다.
 * 가장 위험한 예: `page-bridge.js`가 `web_accessible_resources`에서 빠지면
 * content script의 **격리 세계 안에서는 브리지가 완벽히 동작하지만** 실제 웹페이지의 JS는
 * 브리지를 전혀 쓸 수 없다(requirements.md §2.6.1). 겉보기 성공이라 사람 눈으로는 잡히지 않는다.
 *
 * **보장하는 것:** 확장이 설치·주입되기 위해 반드시 있어야 하는 항목이 있는지,
 * 그리고 있으면 안 되는 항목(`nativeMessagingFromContent`, `all_frames`)이 없는지.
 *
 * **보장하지 않는 것:** manifest가 형식상 올바르다는 것이지 **GeckoView가 이 확장을 실제로
 * 설치한다는 뜻이 아니다.** `ensureBuiltIn`의 성공, content script의 실제 주입, 페이지 세계 실행은
 * 전부 실기기에서만 확인된다(AC-007-1·AC-010-3). 여기가 초록이어도 확장이 안 뜰 수 있다.
 */
class ExtensionManifestTest {

    // 작업 디렉터리가 `app/`인 이유는 BridgeWireContractTest의 KDoc에 적어 두었다.
    private val manifest = Json
        .parseToJsonElement(File("src/main/assets/messaging/manifest.json").readText())
        .jsonObject

    private fun stringArray(key: String): List<String> =
        manifest.getValue(key).jsonArray.map { it.jsonPrimitive.content }

    @Test
    fun `page-bridge_js가 web_accessible_resources에 등록돼 있다`() {
        // 빠져도 격리 세계에서는 브리지가 동작하므로 AC-010-1·2는 통과해버린다.
        // 실제로 깨지는 것은 AC-010-3(PAGE_WORLD 마커) 하나뿐이고, 그것을 사람이 놓치면 거짓 그린이 된다.
        assertTrue(stringArray("web_accessible_resources").contains("page-bridge.js"))
    }

    @Test
    fun `manifest_version은 2다`() {
        // MV3로 올리면 web_accessible_resources가 **문자열 배열에서 객체 배열로 바뀐다.**
        // 그러면 위 케이스의 파싱이 먼저 깨져 원인을 엉뚱한 곳에서 찾게 된다(requirements.md §2.11, A-14).
        assertEquals(2, manifest.getValue("manifest_version").jsonPrimitive.content.toInt())
    }

    @Test
    fun `네이티브 메시징에 필요한 두 권한이 선언돼 있다`() {
        val permissions = stringArray("permissions")

        assertTrue(permissions.contains("geckoViewAddons"))
        assertTrue(permissions.contains("nativeMessaging"))
    }

    @Test
    fun `nativeMessagingFromContent 권한은 선언하지 않는다`() {
        // 이 권한은 content script가 네이티브 API를 **직접** 부를 때만 필요하다.
        // 본 설계는 background.js 중계이므로 불필요하며, 선언해 두면 다음 사람이
        // "직접 불러도 되는구나"로 읽어 mozilla/geckoview#220의 결함 경로로 되돌린다.
        assertFalse(stringArray("permissions").contains("nativeMessagingFromContent"))
    }

    @Test
    fun `content script는 http와 https 전체에 주입된다`() {
        val contentScript = manifest.getValue("content_scripts").jsonArray.single().jsonObject
        val matches = contentScript.getValue("matches").jsonArray.map { it.jsonPrimitive.content }

        assertEquals(listOf("http://*/*", "https://*/*"), matches)
        assertEquals(listOf("content.js"), contentScript.getValue("js").jsonArray.map { it.jsonPrimitive.content })
    }

    @Test
    fun `content script를 서드파티 iframe에까지 넣지 않는다`() {
        // all_frames를 켜면 광고 iframe에까지 브리지가 열린다. 사용자가 말한 "외부 사이트"는
        // 방문한 사이트를 뜻하므로 이것은 요구 축소가 아니라 §5.1의 완화책이다.
        val contentScript = manifest.getValue("content_scripts").jsonArray.single().jsonObject

        assertFalse(contentScript.getValue("all_frames").jsonPrimitive.content.toBoolean())
    }

    @Test
    fun `빌트인 확장 설치에 필요한 id와 version이 있다`() {
        // 둘 중 하나라도 없으면 ensureBuiltIn이 확장을 설치하지 못한다(requirements.md §2.6).
        val id = manifest.getValue("browser_specific_settings")
            .jsonObject.getValue("gecko")
            .jsonObject.getValue("id")
            .jsonPrimitive.content

        assertTrue(id.isNotBlank())
        assertTrue(manifest.getValue("version").jsonPrimitive.content.isNotBlank())
    }

    @Test
    fun `background script로 background_js를 등록한다`() {
        val scripts = manifest.getValue("background").jsonObject
            .getValue("scripts").jsonArray.map { it.jsonPrimitive.content }

        assertEquals(listOf("background.js"), scripts)
    }
}

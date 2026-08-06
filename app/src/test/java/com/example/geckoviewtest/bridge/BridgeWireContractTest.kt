package com.example.geckoviewtest.bridge

import com.example.geckoviewtest.data.AppInfoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 코틀린과 `background.js`에 **따로 적혀 있는 같은 약속**이 어긋나지 않았는지 고정한다.
 *
 * 이 스위트가 있는 이유: 브리지의 계약은 서로 다른 두 언어의 파일에 나뉘어 있고,
 * 어긋나도 컴파일러도 린트도 아무 말을 하지 않는다. 특히 함수명 목록은 **어긋나는 방향에 따라
 * 증상이 다르다** — JS에만 있으면 네이티브가 `UNKNOWN_FUNCTION`으로 시끄럽게 거절하지만,
 * 코틀린에만 있으면 JS 화이트리스트가 먼저 막아 **예외도 로그도 없이 조용히 실패한다.**
 * 조용한 쪽은 실기기에서도 "버튼을 눌렀는데 아무 일도 안 일어난다"로만 보인다.
 *
 * **보장하는 것:** 함수명 집합이 양방향으로 일치하는지, `nativeApp` 식별자 문자열이 같은지,
 * 오류 코드가 양쪽에 다 있는지, 요청 처리가 송신자에 따라 갈라지지 않는지(AC-003-3),
 * 오리진 훅이 "전체 허용"인 채로 출고되는지(requirements.md §5.1 / plan.md H-1).
 *
 * **보장하지 않는 것 (중요):** 이것은 **텍스트 대조**이지 JS 실행이 아니다. 목록이 일치해도
 * `background.js`가 실제로 동작한다는 뜻은 아니고, 5단 경로(page-bridge → content → background)가
 * 살아 있다는 뜻은 더더욱 아니다. 그 확인은 실기기 게이트(AC-006-4·AC-010-3)만 할 수 있다.
 * **이 스위트가 초록이어도 외부 사이트 브리지가 동작한다는 근거로 쓰지 말 것.**
 *
 * 실행 전제: AGP는 단위 테스트의 작업 디렉터리를 **모듈 폴더(`app/`)** 로 잡는다.
 * 그래서 자산 경로가 `src/main/assets/...`이다(`app/`을 앞에 붙이면 파일을 못 찾는다).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BridgeWireContractTest {

    private val backgroundJs: String = File(MESSAGING_DIR, "background.js").readText()

    /**
     * 주석 안의 문자열이 코드로 오인되지 않게 `//`와 `/* */`를 걷어낸 본문.
     * `background.js` 상단 주석에는 상수 이름과 오류 코드가 설명 목적으로 여러 번 등장한다.
     */
    private val backgroundCode: String = backgroundJs
        .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), " ")
        .replace(Regex("""//[^\n]*"""), " ")

    private fun jsAllowedFunctions(): List<String> {
        val body = Regex("""const\s+ALLOWED_FUNCTIONS\s*=\s*\[([^\]]*)]""")
            .find(backgroundCode)
            ?.groupValues
            ?.get(1)
            ?: error("background.js에서 ALLOWED_FUNCTIONS 배열을 찾지 못했다")
        return Regex(""""([^"]+)"""").findAll(body).map { it.groupValues[1] }.toList()
    }

    /** `BridgeProtocol`의 `FN_` 상수를 리플렉션으로 모은다 — 상수가 늘면 이 테스트가 저절로 따라간다. */
    private fun kotlinFunctionNames(): Set<String> = constantsWithPrefix(BridgeProtocol::class.java, "FN_")

    private fun constantsWithPrefix(type: Class<*>, prefix: String): Set<String> =
        type.declaredFields
            .filter { it.name.startsWith(prefix) && it.type == String::class.java }
            .map { it.isAccessible = true; it.get(null) as String }
            .toSet()

    @Test
    fun `코틀린이 아는 함수명은 모두 background_js 화이트리스트에도 있다`() {
        // 이 방향이 깨지면 조용히 실패한다 — JS가 먼저 막아 네이티브까지 요청이 오지 않으므로
        // logcat에도 아무것도 남지 않는다. 그래서 반대 방향보다 이쪽이 더 위험하다.
        val missing = kotlinFunctionNames() - jsAllowedFunctions().toSet()

        assertEquals(emptySet<String>(), missing)
    }

    @Test
    fun `background_js가 허용한 함수명은 모두 디스패처가 실제로 처리한다`() = runTest {
        // 텍스트 비교가 아니라 진짜 BridgeDispatcher에 태워 UNKNOWN_FUNCTION이 나오는지로 본다.
        // 상수 목록만 대조하면 "상수는 있는데 when 분기가 없는" 상태를 놓친다.
        val dispatcher = BridgeDispatcher(
            appInfoRepository = FakeAppInfoRepository,
            host = FakeBridgeHost,
            dispatcher = UnconfinedTestDispatcher(),
        )

        val rejected = jsAllowedFunctions().filter { name ->
            val result = dispatcher.handle(BridgeRequest(id = "req-contract", name = name))
            result is BridgeResult.Failure && result.code == ErrorCode.UNKNOWN_FUNCTION
        }

        assertEquals(emptyList<String>(), rejected)
    }

    @Test
    fun `nativeApp 식별자가 코틀린과 background_js에서 같다`() {
        // 어긋나면 GeckoView가 델리게이트를 찾지 못해 예외도 로그도 없이 아무 일도 안 일어난다.
        val jsValue = Regex("""const\s+NATIVE_APP\s*=\s*"([^"]*)"""")
            .find(backgroundCode)
            ?.groupValues
            ?.get(1)

        assertEquals(BridgeProtocol.NATIVE_APP, jsValue)
    }

    @Test
    fun `ErrorCode의 모든 오류 코드가 background_js에도 존재한다`() {
        // 오류 코드는 닫힌 집합이고 페이지 쪽 코드가 이 값으로 분기한다(BridgeProtocol.kt 주석).
        // 한쪽에만 코드를 늘리면 페이지의 분기가 어느 쪽에서도 타지 않는 죽은 코드가 된다.
        val codes = constantsWithPrefix(ErrorCode::class.java, "")
        val missing = codes.filterNot { backgroundCode.contains(""""$it"""") }

        assertEquals(emptyList<String>(), missing)
    }

    @Test
    fun `background_js는 송신자 종류로 요청 처리를 가르지 않는다`() {
        // AC-003-3. 확장 페이지(3단)와 외부 사이트(5단)가 같은 계약을 쓴다는 말은
        // background.js가 두 경로를 구분하지 않는다는 뜻이다. 구분하는 순간 "동일 계약"이 이름뿐이 된다.
        // sender의 속성을 읽는 순간 분기가 생긴 것으로 본다.
        val senderPropertyAccess = Regex("""\bsender\s*\.""").findAll(backgroundCode).count()

        assertEquals(0, senderPropertyAccess)
    }

    @Test
    fun `오리진 훅은 전체 허용인 채로 출고된다`() {
        // requirements.md §5.1 / plan.md H-1: 훅을 켠 상태로 내보내면 사용자 결정 A-08
        // ("외부 사이트에서도 브리지가 동작한다")을 코드가 조용히 뒤집는다.
        // 켜는 것은 사용자 결정 사항이므로, 정말 켤 때는 이 테스트도 함께 고쳐야 한다.
        val body = backgroundCode
            .substringAfter("function isOriginAllowed")
            .substringAfter("{")
            .substringBefore("}")

        assertEquals("return true;", body.trim())
    }

    @Test
    fun `background_js는 네이티브로 객체가 아니라 JSON 문자열을 보낸다`() {
        // 이 선택이 BridgeProtocol을 순수 코틀린으로 유지시킨다(plan.md §3.1 / §7.1).
        // 객체를 그대로 보내도록 되돌리면 코틀린이 org.json에 묶여 커버리지 경로가 통째로 무너지는데,
        // 그 사실은 JS 파일만 봐서는 드러나지 않는다.
        assertTrue(
            backgroundCode.contains(Regex("""sendNativeMessage\(\s*NATIVE_APP\s*,\s*JSON\.stringify\(""")),
        )
    }

    private companion object {
        val MESSAGING_DIR = File("src/main/assets/messaging")

        /** 계약 대조가 목적이므로 값 자체는 의미가 없다. 예외만 던지지 않으면 된다. */
        val FakeAppInfoRepository = object : AppInfoRepository {
            override fun getVersionName(): String = "0.0.0-contract"
        }

        val FakeBridgeHost = object : BridgeHost {
            override fun requestFinish() = Unit
        }
    }
}

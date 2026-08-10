package com.example.geckoviewtest.bridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * [AppBridgeHost]가 종료 요청을 "지금 등록돼 있는 콜백에만" 넘기는지 고정한다.
 *
 * **보장하는 것:** 콜백이 등록된 상태에서 [AppBridgeHost.requestFinish]가 그 콜백을 정확히 1회 부르는 것,
 * 콜백이 없는 상태에서 같은 호출이 **예외 없이** 그냥 반환하는 것,
 * 그리고 해제한 뒤 다시 등록했을 때 요청이 **새 콜백으로** 가는 것.
 *
 * **보장하지 않는 것:** 그 콜백이 실제로 Activity를 종료시키는지는 여기서 알 수 없다.
 * 그것은 `MainActivity`가 `onCreate`에서 무엇을 등록했는지에 달린 **배선**이고,
 * GeckoView 네이티브 엔진이 필요해 JVM 테스트로는 원리적으로 관측할 수 없다 — 실기기 검증의 몫이다.
 * 동시성(`@Volatile`이 실제로 다른 스레드에 값을 보이게 하는지)도 이 스위트의 범위 밖이다.
 *
 * 순수 JUnit만 쓴다. 이 클래스는 안드로이드 타입을 하나도 참조하지 않아
 * Robolectric도 `coroutines-test`도 필요 없다 — 넣으면 검증이 아니라 무게만 는다.
 */
class AppBridgeHostTest {

    @Test
    fun `콜백이 등록돼 있으면 requestFinish가 그 콜백을 정확히 1회 호출한다`() {
        val host = AppBridgeHost()
        var callCount = 0
        val callback = { callCount++; Unit }
        host.onFinishRequested = callback

        host.requestFinish()

        assertEquals(1, callCount)
        // 넣은 것이 그대로 조회되는지도 함께 본다. `requestFinish`는 프로퍼티의 backing field를
        // 직접 읽으므로 이 단정이 없으면 **getter가 한 번도 실행되지 않는다.**
        // 반증 경로는 setter다 — 값을 감싸거나 정규화하면(`field = { value.invoke() }`)
        // 래퍼가 원본을 부르므로 호출 횟수는 그대로 1이라 위 단정은 초록인데, 이 단정만 RED가 된다.
        assertSame(callback, host.onFinishRequested)
    }

    @Test
    fun `콜백이 없으면 requestFinish는 예외 없이 아무 일도 하지 않는다`() {
        // 이 케이스가 없으면 `onFinishRequested?.invoke()`의 null 분기가 영영 안 돌아,
        // 누군가 `?.`를 `!!`로 바꿔도 전부 초록으로 통과한다.
        // 실제로 도달하는 경로다 — MainActivity.onDestroy가 등록을 null로 되돌린 뒤에도
        // 브리지는 백그라운드에서 appFinish를 보낼 수 있고, 여기서 터지면 화면 없는 앱이 죽는다.
        val host = AppBridgeHost()
        var callCount = 0
        host.onFinishRequested = { callCount++ }
        host.onFinishRequested = null

        host.requestFinish()

        assertEquals(0, callCount)
    }

    @Test
    fun `해제한 뒤 다시 등록하면 requestFinish는 새 콜백을 부른다`() {
        // `AppBridgeHost`는 `App`이 `by lazy`로 드는 프로세스당 1개(App.kt:49)라
        // 화면이 다시 만들어질 때마다 등록(MainActivity:129)과 해제(:201)를 되풀이해 겪는다.
        // 위 두 케이스는 매번 새 host로 시작해 `requestFinish`를 한 번씩만 부르므로,
        // "종료는 한 번만" 같은 1회성 가드가 들어와도 전부 초록으로 통과한다(실측 확인).
        // 그러면 회전 뒤에 온 appFinish가 조용히 무시된다.
        val host = AppBridgeHost()
        var newCount = 0
        host.onFinishRequested = { }
        host.requestFinish()
        host.onFinishRequested = null

        host.onFinishRequested = { newCount++ }
        host.requestFinish()

        assertEquals(1, newCount)
    }
}

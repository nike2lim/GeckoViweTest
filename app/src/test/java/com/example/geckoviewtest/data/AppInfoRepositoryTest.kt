package com.example.geckoviewtest.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 버전 문자열을 못 구했을 때 화면이 빈칸이 되지 않도록 폴백이 동작하는지 고정한다.
 *
 * **보장하는 것:** provider가 값을 주면 그대로 전달하는지, null이면 `UNKNOWN`으로 바뀌는지.
 *
 * **보장하지 않는 것:** 실제 `PackageManager`가 올바른 값을 주는지는 여기서 알 수 없다.
 * 그 확인은 화면 표시 값과 `dumpsys package`의 `versionName`을 문자 단위로 대조하는
 * 실기기 게이트(AC-004-2)가 담당한다.
 *
 * 이 클래스가 `Context` 없이 테스트되는 것은 우연이 아니다 — `PackageManager` 호출을
 * 람다로 잘라내 밖으로 뺐기 때문이다(plan.md D-02).
 */
class AppInfoRepositoryTest {

    @Test
    fun `provider가 준 값을 그대로 돌려준다`() {
        val repository = AppInfoRepositoryImpl { "1.2.3" }

        assertEquals("1.2.3", repository.getVersionName())
    }

    @Test
    fun `provider가 null이면 UNKNOWN으로 폴백한다`() {
        // 빈 문자열이나 null이 화면에 나가면 "브리지가 죽은 것"과 구분할 수 없게 된다.
        val repository = AppInfoRepositoryImpl { null }

        assertEquals(AppInfoRepositoryImpl.UNKNOWN, repository.getVersionName())
    }

    @Test
    fun `값을 물어볼 때마다 provider를 다시 호출한다`() {
        // 처음 한 번만 읽어 캐시해두면 테스트에서 값을 바꿔도 반영되지 않는다.
        var current = "first"
        val repository = AppInfoRepositoryImpl { current }

        assertEquals("first", repository.getVersionName())
        current = "second"
        assertEquals("second", repository.getVersionName())
    }
}

package com.example.geckoviewtest

import android.app.Application
import com.example.geckoviewtest.bridge.BridgeProtocol
import com.example.geckoviewtest.gecko.await
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.WebExtension

/**
 * 앱 프로세스 전체에서 딱 한 번만 만들어지는 Application 클래스.
 *
 * Android는 앱 프로세스가 시작될 때 이 클래스를 가장 먼저 생성한다(Activity보다 먼저).
 * 그래서 "프로세스당 하나만 존재해야 하는 것"을 여기에 둔다 — [GeckoRuntime]과 [AppContainer]가 그렇다.
 *
 * AndroidManifest.xml의 `<application android:name=".App">` 등록과 이 파일은 **쌍**이다.
 * 한쪽만 있으면 런타임이 초기화되지 않거나 Activity에서 생성되어 화면 회전 시 크래시한다(plan.md D-04 / R-07).
 */
class App : Application() {

    /**
     * Gecko 엔진 본체. "GeckoRuntime can only be initialized once per process" 제약이 있어
     * Activity가 아니라 여기서 소유한다.
     *
     * `by lazy`: 프로퍼티를 처음 읽는 순간 한 번만 초기화하는 코틀린 문법.
     *
     * `onCreate()`에서 미리 만들지 않는 이유는 **"무거우니까 나중에"가 아니다** —
     * 실제로는 `MainActivity.onCreate`에서 곧바로 읽으므로 그 비용은 회피되지 않고 옮겨질 뿐이다.
     * 진짜 이유는 **엔진이 필요 없는 프로세스 시작에서는 아예 만들지 않기 위해서**다.
     * 안드로이드는 화면 없이 프로세스를 띄우기도 하는데(예: 백업·브로드캐스트 수신),
     * 그때까지 229.5 MiB짜리 네이티브 엔진을 올릴 이유가 없다.
     */
    val geckoRuntime: GeckoRuntime by lazy {
        val settings = GeckoRuntimeSettings.Builder()
            // consoleOutput: 웹 페이지의 console.log를 logcat으로 빼준다.
            // remoteDebuggingEnabled: PC의 Firefox에서 원격 디버깅 접속을 허용한다.
            // 이 앱은 JS가 실행되는 세계가 3곳(확장 페이지 / content script 격리 세계 / background)이라
            // 이걸 켜지 않으면 "어디서 죽었는지"를 분리하는 것 자체가 불가능하다(plan.md B-11).
            // BuildConfig: 빌드 시 자동 생성되는 클래스. DEBUG는 debug 빌드에서만 true다.
            .consoleOutput(BuildConfig.DEBUG)
            .remoteDebuggingEnabled(BuildConfig.DEBUG)
            .build()
        GeckoRuntime.create(this, settings)
    }

    /** 앱의 객체 조립 담당. DI 프레임워크(Hilt 등) 없이 손으로 배선한다(plan.md §2.2). */
    val container: AppContainer by lazy { AppContainer(this) }

    /**
     * 내장 WebExtension을 설치하고 네이티브 메시지 델리게이트를 붙인 결과.
     *
     * `ensureBuiltIn`: 이미 설치돼 있으면 그대로 쓰고 없으면 설치하는 GeckoView API.
     * 매번 `installBuiltIn`을 부르면 앱 재시작마다 재설치가 일어나 느려진다.
     * uri는 반드시 `resource://android/` 로 시작해야 한다 — 이것이 앱 assets를 가리키는 규약이다.
     *
     * `Deferred`: "나중에 값이 나오는 코루틴 작업". `await()`를 **여러 번** 호출할 수 있어서
     * 화면이 다시 만들어질 때마다 재설치하지 않고 같은 결과를 나눠 쓸 수 있다.
     * (GeckoResult는 리스너를 하나만 붙일 수 있어 그대로 재사용하기 어렵다.)
     */
    val bridgeExtension: Deferred<WebExtension> by lazy {
        container.applicationScope.async {
            val extension = geckoRuntime.webExtensionController
                .ensureBuiltIn(EXTENSION_URI, EXTENSION_ID)
                .await()
            // 이 등록이 없으면 background.js의 sendNativeMessage가 아무 데도 도달하지 않는다.
            // 두 번째 인자는 BridgeProtocol.NATIVE_APP과 background.js의 NATIVE_APP 셋이 모두 같아야 한다.
            extension.setMessageDelegate(container.nativeBridgeHandler, BridgeProtocol.NATIVE_APP)
            extension
        }
    }

    companion object {
        /** 확장 폴더 위치. `app/src/main/assets/messaging/` 에 대응한다. */
        const val EXTENSION_URI = "resource://android/assets/messaging/"

        /** manifest.json의 browser_specific_settings.gecko.id와 같아야 한다. */
        const val EXTENSION_ID = "geckoviewtest@example.com"
    }
}

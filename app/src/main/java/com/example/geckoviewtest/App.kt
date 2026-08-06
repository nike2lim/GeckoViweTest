package com.example.geckoviewtest

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import com.example.geckoviewtest.bridge.BridgeDispatcher
import com.example.geckoviewtest.bridge.BridgeHost
import com.example.geckoviewtest.bridge.BridgeProtocol
import com.example.geckoviewtest.bridge.NativeBridgeHandler
import com.example.geckoviewtest.data.AppInfoRepository
import com.example.geckoviewtest.data.AppInfoRepositoryImpl
import com.example.geckoviewtest.gecko.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

/**
 * 수동 DI 컨테이너 — "무엇이 무엇에 의존하는가"가 이 파일 하나에 그대로 보이게 하는 것이 목적이다.
 *
 * Hilt 같은 DI 프레임워크를 쓰지 않은 이유: 화면 1개에 주입 대상 5개뿐이라 손익분기점 아래이고,
 * 이미 229.5 MiB짜리 AAR을 들이는 중이라 애너테이션 처리 단계를 더 얹으면 빌드 루프가 느려진다.
 * `architecture.md`가 요구하는 것은 "생성자 주입"과 "Dispatcher 주입"이지 특정 프레임워크가 아니다.
 */
class AppContainer(private val app: Application) {

    /**
     * Activity 수명과 무관하게 살아 있어야 하는 코루틴 스코프.
     *
     * 브리지 요청은 화면이 없는 순간에도 도착할 수 있어 `viewModelScope`가 맞지 않는다.
     * 그렇다고 `GlobalScope`를 쓰면 금지 사항 위반이므로 스코프를 직접 만든다(plan.md D-10).
     *
     * `SupervisorJob`: 자식 코루틴 하나가 실패해도 형제들을 죽이지 않는다.
     * `Dispatchers.Main.immediate`: GeckoView API 상당수가 UI 스레드 호출을 요구한다.
     */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * **`PackageManager`가 이 앱에서 등장하는 유일한 곳이다.**
     * 이 호출을 Repository 안에 두면 그 클래스가 안드로이드에 묶여 JVM 테스트가 불가능해지므로,
     * 람다로 잘라내 여기서만 안드로이드를 만진다(plan.md D-02).
     */
    val appInfoRepository: AppInfoRepository = AppInfoRepositoryImpl {
        try {
            val packageManager = app.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // API 33부터 int 플래그를 받던 오버로드가 deprecated되고 PackageInfoFlags를 쓴다.
                packageManager.getPackageInfo(app.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(app.packageName, 0)
            }
            info.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            // 자기 자신의 패키지를 못 찾는 일은 실무상 없지만, 예외를 삼키지 않고 null로 바꿔
            // Repository의 UNKNOWN 폴백이 동작하게 한다.
            null
        }
    }

    val bridgeHost: AppBridgeHost = AppBridgeHost()

    val bridgeDispatcher: BridgeDispatcher = BridgeDispatcher(
        appInfoRepository = appInfoRepository,
        host = bridgeHost,
        // Dispatchers.Default: CPU 작업용 스레드 풀. 브리지 처리는 UI 스레드를 막으면 안 된다.
        dispatcher = Dispatchers.Default,
    )

    val nativeBridgeHandler: NativeBridgeHandler = NativeBridgeHandler(
        scope = applicationScope,
        dispatcher = bridgeDispatcher,
    )
}

/**
 * 브리지가 "앱을 종료해 달라"고 요청할 때의 창구 구현.
 *
 * 이 객체는 앱 스코프(프로세스 수명)인데 실제로 종료를 수행하는 Activity는 그보다 짧게 살고
 * 여러 번 다시 만들어진다. 그래서 직접 Activity를 들지 않고 현재 화면이 등록해 둔 콜백만 호출한다.
 * Activity를 필드로 들면 화면이 사라진 뒤에도 참조가 남아 메모리 누수가 된다.
 */
class AppBridgeHost : BridgeHost {

    /** `@Volatile`: 다른 스레드가 바꾼 값을 즉시 보이게 한다(브리지는 백그라운드 스레드에서 호출된다). */
    @Volatile
    var onFinishRequested: (() -> Unit)? = null

    override fun requestFinish() {
        onFinishRequested?.invoke()
    }
}


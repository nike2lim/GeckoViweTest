package com.example.geckoviewtest

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import com.example.geckoviewtest.bridge.AppBridgeHost
import com.example.geckoviewtest.bridge.BridgeDispatcher
import com.example.geckoviewtest.bridge.NativeBridgeHandler
import com.example.geckoviewtest.data.AppInfoRepository
import com.example.geckoviewtest.data.AppInfoRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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

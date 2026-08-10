package com.example.geckoviewtest.bridge

/**
 * 브리지가 "앱을 종료해 달라"고 요청할 때의 창구 구현.
 *
 * 이 객체는 앱 스코프(프로세스 수명)인데 실제로 종료를 수행하는 Activity는 그보다 짧게 살고
 * 여러 번 다시 만들어진다. 그래서 직접 Activity를 들지 않고 현재 화면이 등록해 둔 콜백만 호출한다.
 * Activity를 필드로 들면 화면이 사라진 뒤에도 참조가 남아 메모리 누수가 된다.
 *
 * 수명은 앱 스코프인데 파일은 왜 `bridge` 패키지에 있는가 — **수명과 소속은 다른 축**이기 때문이다.
 * 이 클래스가 아는 것은 같은 패키지의 [BridgeHost] 하나뿐이고 안드로이드 타입은 하나도 쓰지 않는다.
 * 즉 브리지의 호스트 포트를 구현하는 것이 정체성이고, "앱 스코프"는 그것을 누가 얼마나 오래 들고 있느냐의 문제다.
 * 실제로 이 인스턴스를 만들어 소유하는 것은 루트 패키지의 `AppContainer`이고,
 * 종료 콜백을 등록·해제하는 것은 `MainActivity`다(`onCreate`에서 등록, `onDestroy`에서 null).
 */
class AppBridgeHost : BridgeHost {

    /** `@Volatile`: 다른 스레드가 바꾼 값을 즉시 보이게 한다(브리지는 백그라운드 스레드에서 호출된다). */
    @Volatile
    var onFinishRequested: (() -> Unit)? = null

    override fun requestFinish() {
        onFinishRequested?.invoke()
    }
}

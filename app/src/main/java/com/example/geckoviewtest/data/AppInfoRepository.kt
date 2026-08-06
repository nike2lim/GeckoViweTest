package com.example.geckoviewtest.data

/**
 * 앱 자신에 대한 정보를 알려주는 데이터 계층 인터페이스.
 *
 * 데이터 흐름: `BridgeDispatcher` → **이 인터페이스** → 구현체가 실제 값을 가져온다.
 *
 * 인터페이스를 따로 두는 이유는 상위 계층이 "값을 어디서 가져오는지"를 모르게 하기 위해서다.
 * 덕분에 테스트에서는 가짜 구현으로 갈아끼울 수 있다.
 */
interface AppInfoRepository {
    /** 앱의 버전 문자열(build.gradle.kts의 `versionName`). */
    fun getVersionName(): String
}

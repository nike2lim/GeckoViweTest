package com.example.geckoviewtest.data

/**
 * [AppInfoRepository]의 실제 구현.
 *
 * **여기에 `Context`를 받지 않는 것이 이 클래스 설계의 핵심이다.**
 * 버전 문자열은 원래 `context.packageManager.getPackageInfo(...)`로 얻는데,
 * 그 호출을 이 클래스가 직접 하면 안드로이드 프레임워크에 묶여
 * JVM 단위 테스트에서 실행조차 되지 않는다(커버리지 분모에 0%로 얹힌다).
 *
 * 그래서 `Context`를 쓰는 부분만 [versionNameProvider] 람다로 잘라내 밖으로 뺐다.
 * `PackageManager` 호출은 `App.kt`가 이 람다를 만들 때 단 한 번 등장한다(plan.md D-02).
 *
 * @param versionNameProvider 버전 문자열을 돌려주는 함수. 값을 못 구하면 null을 돌려준다.
 */
class AppInfoRepositoryImpl(
    private val versionNameProvider: () -> String?,
) : AppInfoRepository {

    override fun getVersionName(): String = versionNameProvider() ?: UNKNOWN

    companion object {
        /**
         * 버전을 못 구했을 때 돌려주는 값.
         * 빈 문자열이나 null을 돌려주면 페이지 화면에 아무것도 안 나와서
         * "브리지가 죽은 것"과 "버전을 못 읽은 것"을 구분할 수 없게 된다.
         */
        const val UNKNOWN = "UNKNOWN"
    }
}

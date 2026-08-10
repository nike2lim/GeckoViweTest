// app 모듈의 빌드 스크립트.
// 안드로이드 앱 하나가 어떻게 컴파일·패키징되는지를 정의한다.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    // jacoco: 테스트가 코드의 어느 줄을 실행했는지 세어주는 커버리지 도구.
    jacoco
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

android {
    // namespace: 생성되는 R 클래스와 BuildConfig 클래스가 놓일 패키지.
    namespace = "com.example.geckoviewtest"
    // compileSdk: 컴파일할 때 참조하는 안드로이드 SDK 버전. 기기 요구사항이 아니다.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.geckoviewtest"
        // minSdk 26은 선택이 아니라 하한이다.
        // GeckoView AAR의 AndroidManifest.xml이 <uses-sdk android:minSdkVersion="26"/>을 선언하고 있어
        // 그보다 낮추면 매니페스트 병합 단계에서 빌드가 실패한다(plan.md B-01).
        minSdk = 26
        // targetSdk: 앱이 "이 버전의 동작 변경을 이해하고 있다"고 선언하는 값.
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ndk.abiFilters: APK에 어떤 CPU 아키텍처용 네이티브 라이브러리(.so)를 담을지 고르는 설정.
        // GeckoView AAR은 arm64-v8a / armeabi-v7a / x86_64 세 벌을 동봉하고 있는데,
        // AGP는 minSdk >= 23에서 .so를 "비압축"으로 패키징하므로 세 벌을 다 담으면 합계가 483.1 MiB가 된다.
        // 검증 기기(SM-G981N)가 arm64-v8a이므로 그 한 벌만 남긴다(plan.md R-01 / B-07).
        // 이 설정은 GeckoView 의존성과 반드시 같은 커밋에 있어야 한다.
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        debug {
            // enableUnitTestCoverage: JVM 단위 테스트 실행 시 커버리지 데이터(.exec)를 수집한다.
            enableUnitTestCoverage = true
            // release 빌드(서명·R8·AAB)는 이번 작업의 스코프 밖이다(plan.md §10).
        }
    }

    testCoverage {
        jacocoVersion = libs.versions.jacoco.get()
    }

    compileOptions {
        // GeckoView 공식 quick-start가 Java 17 호환 플래그를 요구한다(plan.md B-06).
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        // ViewBinding: XML 레이아웃마다 타입 안전한 바인딩 클래스를 자동 생성해준다.
        // activity_main.xml -> ActivityMainBinding. findViewById를 직접 쓰지 않아도 된다.
        viewBinding = true
        // buildConfig: BuildConfig 클래스(BuildConfig.DEBUG 등) 생성 스위치.
        // AGP 8부터는 기본값이 false라 명시하지 않으면 "Unresolved reference 'BuildConfig'"로 컴파일이 실패한다.
        buildConfig = true
        // buildFeatures.compose는 의도적으로 설정하지 않는다 — REQ-002가 XML View를 명시한다(AC-002-2).
    }
}

dependencies {
    implementation(libs.geckoview)
    // activity-ktx: OnBackPressedDispatcher에 코틀린 람다로 콜백을 등록하는 addCallback 확장을 제공한다.
    // GeckoView도 appcompat도 이 아티팩트를 끌고 오지 않으므로 직접 선언해야 한다.
    //
    // ⚠ 의존성 트리에서 `androidx.compose.runtime`을 보더라도 놀라지 말고 **exclude하지 말 것.**
    //   유입 경로: activity 1.13.0 → androidx.navigationevent 1.0.0 → compose.runtime:runtime-annotation
    //   이것은 **애너테이션 전용 아티팩트이고 Compose UI가 아니다.**
    //   근거(실측): 빌드된 APK의 dex 7개를 전수 조사한 결과 androidx/compose 정의 클래스는 6개뿐이고
    //   전부 애너테이션 타입 + R이다 — Stable / Immutable / StableMarker /
    //   annotation.RememberInComposition / annotation.FrequentlyChangingValue / annotation.R.
    //   compose.ui · foundation · material · runtime 본체는 0건이며 Compose 컴파일러도 적용되지 않았다.
    //   따라서 REQ-002(XML View 사용, Compose 미사용) 위반이 아니다.
    //   exclude하면 navigationevent가 깨질 수 있고, 얻는 것은 의존성 목록의 겉보기뿐이다.
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    // lifecycle-*-ktx는 ViewModel과 repeatOnLifecycle을 제공하면서
    // kotlinx-coroutines-android를 전이 의존성으로 함께 끌고 온다.
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    // coroutines-test: runTest / Dispatchers.setMain / TestDispatcher 제공.
    testImplementation(libs.kotlinx.coroutines.test)
    // turbine: Flow가 내보내는 값을 순서대로 꺼내 단정할 수 있게 해주는 테스트 도구.
    testImplementation(libs.turbine)
}

/*
 * 커버리지 집계에서 제외할 클래스들.
 *
 * 제외 기준은 "테스트하기 귀찮아서"가 아니라 **JVM 테스트로 실행 자체가 불가능한가**다.
 * GeckoView 델리게이트와 Activity는 실기기가 없으면 한 줄도 돌지 않으므로
 * 분모에 넣어봤자 영원히 0%로 남아 수치만 왜곡한다. 이들은 실기기 게이트로 검증한다(plan.md §7.3).
 *
 * 이 목록이 코드에 박혀 있으므로 누군가 제외 범위를 넓히면 diff에 그대로 드러난다.
 *
 * ┌ AppContainer는 제외인데 AppBridgeHost는 왜 아닌가 — 형제처럼 보이지만 기준의 양쪽이다 ──┐
 * │ AppContainer: `Application`·`app.packageManager`·`Dispatchers.Main.immediate`에 의존한다. │
 * │   특히 마지막 것은 `Dispatchers.setMain` 없이는 **생성자 실행 자체가 실패**하므로          │
 * │   위 기준("JVM 테스트로 실행 자체가 불가능한가")을 충족한다 → 제외 유지.                   │
 * │ AppBridgeHost: 안드로이드 타입을 **하나도** 쓰지 않는 순수 Kotlin 4줄짜리라 기준에 미달한다. │
 * │   제외할 이유가 없어 목록에서 뺐고, AppBridgeHostTest가 그 4줄·2분기를 덮는다.             │
 * └──────────────────────────────────────────────────────────────────────────────────────────┘
 */
val coverageExclusions = listOf(
    "com/example/geckoviewtest/App.class",
    "com/example/geckoviewtest/App$*.class",
    "com/example/geckoviewtest/AppContainer*.class",
    "com/example/geckoviewtest/MainActivity*.class",
    "com/example/geckoviewtest/gecko/**",
    "com/example/geckoviewtest/bridge/NativeBridgeHandler*.class",
    "**/BuildConfig.*",
    "**/R.class",
    "**/R$*.class",
    "**/databinding/**",
)

tasks.register<JacocoReport>("jvmCoverageReport") {
    dependsOn("testDebugUnitTest")
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) { exclude(coverageExclusions) }
    )
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(
        layout.buildDirectory.file("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
    )
}

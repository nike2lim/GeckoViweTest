// 루트 프로젝트의 빌드 스크립트.
// 여기서는 플러그인을 "선언만" 하고 적용하지 않는다(apply false).
// 실제 적용은 각 모듈(app/build.gradle.kts)에서 한다 — 루트에 적용하면 루트가 안드로이드 모듈이 되어버린다.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

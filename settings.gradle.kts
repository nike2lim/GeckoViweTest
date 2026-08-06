// Gradle 설정 스크립트.
// 이 파일은 "어떤 모듈이 이 빌드에 참여하는가"와 "플러그인·라이브러리를 어느 저장소에서 받아오는가"를 정한다.
// 안드로이드 프로젝트에서 build.gradle.kts보다 먼저 평가되는 유일한 파일이다.

pluginManagement {
    repositories {
        // google(): AGP(Android Gradle Plugin)와 androidx 아티팩트가 있는 구글 저장소
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // 모듈별 build.gradle.kts에서 repositories를 다시 선언하는 것을 막는다.
    // 저장소 목록이 한 곳에만 존재해야 "이 의존성이 어디서 왔는가"를 추적할 수 있다.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // GeckoView는 Google/Maven Central에 없고 Mozilla가 직접 운영하는 저장소에만 있다(plan.md L-02).
        maven {
            url = uri("https://maven.mozilla.org/maven2/")
            content {
                // 이 저장소는 org.mozilla.* 그룹만 담당하게 제한한다.
                // 제한하지 않으면 모든 의존성 조회가 이 저장소에도 한 번씩 가서 빌드가 느려진다.
                includeGroupByRegex("org\\.mozilla.*")
            }
        }
    }
}

rootProject.name = "GeckoViewTest"
// 단일 app 모듈 구성이다. 모듈을 추가하면 impact-report.json 화이트리스트를 벗어나므로
// impact-analyzer 재실행이 필요하다(plan.md §2.3).
include(":app")

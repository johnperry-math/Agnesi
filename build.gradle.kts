plugins {
    kotlin("js") version "1.7.10"
}

group = "cantanima.name"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test-js"))
    implementation(kotlin("stdlib-js"))
}

kotlin {
    js(LEGACY) {
        browser {
            binaries.executable()
            webpackTask {
                cssSupport.enabled = true
            }
            runTask {
                cssSupport.enabled = true
            }
            testTask {
                useKarma {
                    useChromeHeadless()
                    webpackConfig.cssSupport.enabled = true
                }
            }
            dceTask {
                keep("Agnesi.new_graph")
            }
        }
    }
}
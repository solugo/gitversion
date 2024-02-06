import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    kotlin("multiplatform") version "1.9.22"
}

group = "de.solugo.gitversion"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin {

    linuxX64("linuxX64") {
        val main by compilations.getting
        val git by main.cinterops.creating

        binaries {
            executable {
                entryPoint = "main"
                baseName = "gitversion"
            }
            all {
                freeCompilerArgs = freeCompilerArgs + listOf(
                    "-Xoverride-konan-properties=linkerGccFlags=-lgcc -lgcc_eh -lc",
                )

                linkerOpts("--as-needed")
            }
        }
    }

    mingwX64("windowsX64") {
        val main by compilations.getting
        val git by main.cinterops.creating

        binaries {
            executable {
                entryPoint = "main"
                baseName = "gitversion"
                freeCompilerArgs = freeCompilerArgs + listOf(
                    "-Xoverride-konan-properties=linkerGccFlags=-lgcc -lgcc_eh -lc",
                )
            }
        }
    }

    sourceSets {
        val nativeMain by creating {
            dependencies {
                implementation("com.squareup.okio:okio:3.7.0")
                implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")
                implementation("me.archinamon:file-io:1.3.5")
            }
        }
        val nativeTest by creating {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test:1.9.22")
                implementation("com.willowtreeapps.assertk:assertk:0.28.0")
            }
        }
        val linuxX64Main by getting
        val linuxX64Test by getting

        val windowsX64Main by getting
        val windowsX64Test by getting

    }
}

tasks.withType<AbstractTestTask> {
    testLogging {
        events(TestLogEvent.PASSED, TestLogEvent.FAILED)
        showExceptions = true
        exceptionFormat = TestExceptionFormat.FULL
    }
}
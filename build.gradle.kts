import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    kotlin("multiplatform") version "1.8.20"
}

group = "de.solugo.gitversion"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/releases")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

kotlin {

    val linuxX64 = linuxX64("linuxX64") {
        compilations.getByName("main") {
            cinterops {
                val git by creating
            }
        }
        binaries {
            executable {
                entryPoint = "main"
                baseName = "gitversion"
                optimized = true
            }
            all {
                freeCompilerArgs = freeCompilerArgs + listOf(
                    "-Xoverride-konan-properties=linkerGccFlags=-lgcc -lgcc_eh -lc",
                )

                linkerOpts("--as-needed", "--defsym=isnan=isnan", "-s", "-static")
            }
        }
    }

    val windowsX64 = mingwX64("windowsX64") {
        compilations.getByName("main") {
            cinterops {
                val git by creating
            }
        }
        binaries {
            executable {
                entryPoint = "main"
                baseName = "gitversion"
                optimized = true
                freeCompilerArgs = freeCompilerArgs + listOf(
                    "-Xoverride-konan-properties=linkerGccFlags=-lgcc -lgcc_eh -lc",
                )
            }
        }
    }

    sourceSets {
        val nativeMain by creating {
            dependencies {
                implementation("com.squareup.okio:okio:3.3.0")
                implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
                implementation("me.archinamon:file-io:1.3.5")
            }
        }
        val nativeTest by creating {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test:1.7.0")
                implementation("com.willowtreeapps.assertk:assertk:0.26-SNAPSHOT")
            }
        }
        val linuxX64Main by getting {
            dependsOn(nativeMain)
        }
        val linuxX64Test by getting {
            dependsOn(nativeTest)
        }

        val windowsX64Main by getting {
            dependsOn(nativeMain)
        }
        val windowsX64Test by getting {
            dependsOn(nativeTest)
        }

    }
}

tasks.withType<AbstractTestTask> {
    testLogging {
        events(TestLogEvent.PASSED, TestLogEvent.FAILED)
        showExceptions = true
        exceptionFormat = TestExceptionFormat.FULL
    }
}
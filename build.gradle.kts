plugins {
    kotlin("multiplatform") version "1.7.0"
}

group = "de.solugo.gitversion"
version = "1.0.0"

repositories {
    mavenCentral()
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
                freeCompilerArgs = freeCompilerArgs + listOf("-Xoverride-konan-properties=linkerGccFlags=-lgcc -lgcc_eh -lc")

                linkerOpts("--as-needed", "--defsym=isnan=isnan", "-s", "-static", "./libs/linuxX64/libgit2.a")
            }
        }
    }

//    val windowsX64 = mingwX64("windowsX64") {
//        compilations.getByName("main") {
//            cinterops {
//                val git by creating
//            }
//        }
//        binaries {
//            executable {
//                entryPoint = "main"
//                baseName = "gitversion"
//                optimized = true
//                freeCompilerArgs = freeCompilerArgs + listOf("-Xoverride-konan-properties=linkerGccFlags=-lgcc -lgcc_eh -lc")
//
//                linkerOpts("-s", "-static", "./libs/windowsX64/libgit2.a")
//            }
//        }
//    }

    sourceSets {
        val nativeMain by creating {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.4")
                implementation("me.archinamon:file-io:1.3.5")
            }
        }
        val linuxX64Main by getting {
            dependsOn(nativeMain)
        }

//        val windowsX64Main by getting {
//            dependsOn(nativeMain)
//        }
    }
}

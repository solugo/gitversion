plugins {
    kotlin("multiplatform") version "1.6.10"
}

group = "de.solugo.gitversion"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
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
                linkerOpts("--as-needed", "--defsym=isnan=isnan")
                freeCompilerArgs = freeCompilerArgs + listOf("-Xoverride-konan-properties=linkerGccFlags=-lgcc -lgcc_eh -lc")
                linkerOpts.add("-s")
                linkerOpts.add("-static")
                linkerOpts.add("./libs/libgit2.a")
            }
        }
    }
    sourceSets {
        val nativeMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.4")
                implementation("me.archinamon:file-io:1.3.5")
            }
        }
        val nativeTest by getting
    }
}

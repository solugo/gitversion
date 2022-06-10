package gitversion

import kotlinx.cinterop.toKStringFromUtf8
import me.archinamon.fileio.File
import me.archinamon.fileio.appendText
import platform.posix.getenv

object Pipeline {
    val modifiers = listOf(
        Modifier("azure") {
            val buildId = getenv("BUILD_BUILDID")?.toKStringFromUtf8()
            when {
                buildId != null -> {
                    environment["VERSION"]?.also { println("##vso[build.updatebuildnumber]$it") }
                    environment.entries.forEach { (key, value) -> println("##vso[task.setvariable variable=$key]$value") }
                    true
                }
                else -> false
            }
        },
        Modifier("github") {
            val env = getenv("GITHUB_ENV")?.toKStringFromUtf8()
            when {
                env != null -> {
                    writeLinesToFile(env, environment.entries.map { "${it.key}=${it.value}" })
                    true
                }
                else -> false
            }
        },
    )

    private fun writeLinesToFile(file: String, lines: Collection<String>) {
        File(file).appendText(lines.joinToString(separator = "\n", postfix = "\n"))
    }


    data class Context(val environment: Map<String, String?>)
    data class Modifier(val name: String, val modify: Context.() -> Boolean)
}
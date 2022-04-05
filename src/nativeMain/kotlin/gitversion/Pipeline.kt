package gitversion

import kotlinx.cinterop.toKStringFromUtf8
import me.archinamon.fileio.File
import me.archinamon.fileio.appendText
import platform.posix.getenv

object Pipeline {

    fun applyAzure(environment: Map<String, String?>) {
        getenv("BUILD_BUILDID")?.toKStringFromUtf8()?.also {
            environment["VERSION"]?.also { println("##vso[build.updatebuildnumber]$it") }
            environment.entries.forEach { (key, value) -> println("##vso[task.setvariable variable=$key]$value") }
        }
    }

    fun applyGithub(environment: Map<String, String?>) {
        getenv("GITHUB_ENV")?.toKStringFromUtf8()?.also {
            writeLinesToFile(it, environment.entries.map { "${it.key}=${it.value}" })
        }
    }

    private fun writeLinesToFile(file: String, lines: Collection<String>) {
        File(file).appendText(lines.joinToString(separator = "\n", postfix = "\n"))
    }
}
package gitversion

import me.archinamon.fileio.File
import me.archinamon.fileio.appendText
import me.archinamon.fileio.writeText

object Pipeline {
    val modifiers = listOf(
        Modifier("azure") {
            env("BUILD_BUILDID") ?: return@Modifier false

            environment["VERSION"]?.also { err("##vso[build.updatebuildnumber]$it") }
            environment.entries.forEach { (key, value) -> err("##vso[task.setvariable variable=$key]$value") }
            true
        },
        Modifier("github") {
            env("GITHUB_ACTION") ?: return@Modifier false

            var modified = false

            modified = modified or run {
                val envFile = env("GITHUB_ENV") ?: return@run false
                writeLinesToFile(envFile, environment.entries.map { "${it.key}=${it.value}" })
                true
            }

            modified = modified or run {
                val outputFile = env("GITHUB_OUTPUT") ?: return@run false
                writeLinesToFile(outputFile, environment.entries.map { "${it.key}=${it.value}" })
                true
            }
            modified = modified or run {
                val level = params["version-annotation"]?.takeUnless { it == "none" } ?: return@run false
                val version = params["version"] ?: return@run false
                err("::$level title=GitVersion::Calculated version is $version")
                true
            }

            modified
        },
        Modifier("gitlab") {
            env("GITLAB_CI") ?: return@Modifier false

            val dotenv = params["dotenv"] ?: return@Modifier false
            writeLinesToFile(dotenv, environment.entries.map { "${it.key}=${it.value}" })
            true
        },
    )

    private fun writeLinesToFile(file: String, lines: Collection<String>) {
        val content = lines.joinToString(separator = "\n", postfix = "\n")

        File(file).apply {
            when {
                exists() -> appendText(content)
                else -> writeText(content)
            }
        }
    }


    data class Context(
        val env: (String) -> String?,
        val environment: Map<String, String?>,
        val out: (String) -> Unit,
        val err: (String) -> Unit,
        val params: Map<String, String?>,
    )

    data class Modifier(
        val name: String,
        val modify: Context.() -> Boolean,
    )

}
package gitversion

import me.archinamon.fileio.File
import me.archinamon.fileio.appendText

object Pipeline {
    val modifiers = listOf(
        Modifier("azure") {
            val buildId = env("BUILD_BUILDID")
            when {
                buildId != null -> {
                    environment["VERSION"]?.also { out("##vso[build.updatebuildnumber]$it") }
                    environment.entries.forEach { (key, value) -> out("##vso[task.setvariable variable=$key]$value") }
                    true
                }

                else -> false
            }
        },
        Modifier("github") {
            var modified = false

            env("GITHUB_ENV")?.also { envFile ->
                writeLinesToFile(envFile, environment.entries.map { "${it.key}=${it.value}" })
                modified = true
            }
            env("GITHUB_OUTPUT")?.also { outputFile ->
                writeLinesToFile(outputFile, environment.entries.map { "${it.key}=${it.value}" })
                modified = true
            }

            modified
        },
        Modifier("gitlab") {
            val env = env("GITLAB_CI")
            when {
                env != null -> {
                    writeLinesToFile(params.getValue("dotenv"), environment.entries.map { "${it.key}=${it.value}" })
                    true
                }

                else -> false
            }
        },
    )

    private fun writeLinesToFile(file: String, lines: Collection<String>) {
        File(file).appendText(lines.joinToString(separator = "\n", postfix = "\n"))
    }


    data class Context(
        val env: (String) -> String?,
        val environment: Map<String, String?>,
        val out: (String) -> Unit,
        val params: Map<String, String>,
    )

    data class Modifier(
        val name: String,
        val modify: Context.() -> Boolean,
    )

}
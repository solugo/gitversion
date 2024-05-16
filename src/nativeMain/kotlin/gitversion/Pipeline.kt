package gitversion

import okio.Path.Companion.toPath

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
                envFile.toPath().writeLines(environment.entries.map { "${it.key}=${it.value}" })
                true
            }

            modified = modified or run {
                val outputFile = env("GITHUB_OUTPUT") ?: return@run false
                outputFile.toPath().writeLines(environment.entries.map { "${it.key}=${it.value}" })
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
            dotenv.toPath().writeLines(environment.entries.map { "${it.key}=${it.value}" })
            true
        },
    )


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
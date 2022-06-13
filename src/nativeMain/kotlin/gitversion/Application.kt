package gitversion

class Application(
    private val out: (String) -> Unit,
    private val env: (String) -> String?,
) {

    fun execute(args: Array<String>) {
        val configuration = Configuration(args)
        val component = configuration.component?.takeUnless(String::isEmpty)
        val versionRegex = configuration.versionPattern.toRegex(RegexOption.DOT_MATCHES_ALL)
        val tagRegex = configuration.tagPattern.takeUnless(String::isEmpty).let {
            when {
                component == null -> it
                else -> "$component-$it"
            }
        }?.toRegex(RegexOption.DOT_MATCHES_ALL)
        val majorRegex = configuration.majorPattern?.takeUnless(String::isEmpty)?.toRegex(RegexOption.DOT_MATCHES_ALL)
        val minorRegex = configuration.minorPattern?.takeUnless(String::isEmpty)?.toRegex(RegexOption.DOT_MATCHES_ALL)
        val patchRegex = configuration.patchPattern.takeUnless(String::isEmpty)?.toRegex(RegexOption.DOT_MATCHES_ALL)
        val directory = configuration.directory?.takeUnless(String::isEmpty).let {
            when {
                component == null -> it
                it == null -> component
                else -> "$component/$it"
            }
        }
        val stacktrace = configuration.stacktrace
        val verbosity = when {
            configuration.verbose2 -> 2
            configuration.verbose1 -> 1
            else -> 0
        }

        try {
            val version = Version()
            val git = Git(configuration.path)
            val tags = hashMapOf<String, MutableList<Git.Tag>>()
            val changes = arrayListOf<VersionChange>()

            step("Searching for tags", verbosity >= 2) { log ->
                var found = false
                git.consumeTags(tagRegex) { tag ->
                    found = true
                    log("Found $tag")
                    tags.getOrPut(tag.commit, ::arrayListOf).add(tag)
                    true
                }
                if (!found) log("No tags found")
            }

            step("Searching for commits", verbosity >= 2) { log ->
                var found = false
                git.consumeCommits(directory = directory) { commit ->
                    found = true
                    log("Found $commit")

                    tags[commit.id]?.forEach { tag ->
                        if (tag.matches) {
                            tagRegex?.find(tag.name)?.also { match ->
                                val versionString = match.groupValues.getOrNull(1) ?: tag.name
                                val versionParts = versionRegex.find(versionString)?.groupValues ?: error(
                                    "Could find version in '$versionString' using $versionRegex"
                                )

                                changes.add(VersionChange(tag) {
                                    major += versionParts.getOrNull(1)?.takeUnless(String::isEmpty)?.toInt() ?: 0
                                    minor += versionParts.getOrNull(2)?.takeUnless(String::isEmpty)?.toInt() ?: 0
                                    patch += versionParts.getOrNull(3)?.takeUnless(String::isEmpty)?.toInt() ?: 0
                                })

                                log("Reached $tag")
                            }


                            return@consumeCommits false
                        }
                    }

                    if (commit.matches) {

                        majorRegex?.find(commit.message)?.also {
                            changes.add(VersionChange(commit) {
                                major += 1
                                minor = 0
                                patch = 0
                            })
                            return@consumeCommits true
                        }

                        minorRegex?.find(commit.message)?.also {
                            changes.add(VersionChange(commit) {
                                minor += 1
                                patch = 0
                            })
                            return@consumeCommits true
                        }

                        patchRegex?.find(commit.message)?.also {
                            changes.add(VersionChange(commit) {
                                patch += 1
                            })
                            return@consumeCommits true
                        }

                        changes.add(
                            VersionChange(commit, null)
                        )
                    }
                    return@consumeCommits true
                }
                if (!found) log("No commits found")
            }

            step("Applying changes", verbosity >= 1) { log ->
                var changed = false
                changes.reversed().forEach { change ->
                    change.modification?.invoke(version)
                    changed = true
                    log("Set version to $version due to ${change.reason}")
                }
                if (!changed) log("No changes applied")
            }

            step("Modifying pipeline", verbosity >= 2) { log ->
                val environment = mapOf(
                    "VERSION" to version.toString(),
                )

                var pipelineModified = false
                Pipeline.modifiers.forEach { modifier ->
                    when (configuration.pipeline) {
                        "auto", modifier.name -> {
                            val modified = modifier.modify(Pipeline.Context(env = env, environment = environment))
                            pipelineModified = pipelineModified || modified
                            if (modified) log("Applied pipeline modifier '${modifier.name}'")
                        }
                    }
                }
                if (!pipelineModified) log("No pipeline modifier applied")
            }

            git.close()

            out(version.toString())

        } catch (ex: Exception) {
            if (stacktrace) {
                ex.printStackTrace()
            } else {
                out(ex.toString())
            }
        }

    }

    private inline fun step(description: String, log: Boolean, block: (log: (message: String) -> Unit) -> Unit) {
        if (log) out(description)
        block { if (log) out("  $it") }
        if (log) out("")
    }

    data class VersionChange(val reason: Any, val modification: (Version.() -> Unit)?)

    data class Version(var major: Int = 0, var minor: Int = 0, var patch: Int = 0) {
        override fun toString(): String = "$major.$minor.$patch"
    }

}
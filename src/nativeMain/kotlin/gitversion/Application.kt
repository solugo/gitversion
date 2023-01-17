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
        val majorOverride = configuration.majorOverride
        val minorRegex = configuration.minorPattern?.takeUnless(String::isEmpty)?.toRegex(RegexOption.DOT_MATCHES_ALL)
        val minorOverride = configuration.minorOverride
        val patchRegex = configuration.patchPattern.takeUnless(String::isEmpty)?.toRegex(RegexOption.DOT_MATCHES_ALL)
        val patchOverride = configuration.patchOverride
        val dirtyIgnore = configuration.dirtyIgnore
        val dirtySuffix = configuration.dirtySuffix.takeUnless { it.isEmpty() }
        val suffixOverride = configuration.suffixOverride
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

            val git = try {
                Git(configuration.path)
            } catch (ex: Exception) {
                throw ExecutionError(
                    message = "Could not open git repository in folder ${configuration.path}",
                    reason = ExecutionError.Reason.NO_REPOSITORY,
                )
            }
            val tags = hashMapOf<String, MutableList<Git.Tag>>()
            val changes = arrayListOf<VersionChange>()
            var dirty = false

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
                            tagRegex?.matchEntire(tag.name)?.also { match ->
                                val value = match.groupValues.getOrNull(1) ?: match.value
                                val parts = versionRegex.matchEntire(value)?.groupValues ?: return@forEach

                                changes.add(VersionChange(tag) {
                                    major += parts.getOrNull(1)?.takeUnless(String::isEmpty)?.toInt() ?: 0
                                    minor += parts.getOrNull(2)?.takeUnless(String::isEmpty)?.toInt() ?: 0
                                    patch += parts.getOrNull(3)?.takeUnless(String::isEmpty)?.toInt() ?: 0
                                })

                                log("Reached $tag")

                                return@consumeCommits false
                            }
                        }
                    }

                    if (commit.matches) {

                        majorRegex?.matchEntire(commit.message)?.also {
                            changes.add(VersionChange(commit) {
                                major += 1
                                minor = 0
                                patch = 0
                            })
                            return@consumeCommits true
                        }

                        minorRegex?.matchEntire(commit.message)?.also {
                            changes.add(VersionChange(commit) {
                                minor += 1
                                patch = 0
                            })
                            return@consumeCommits true
                        }

                        patchRegex?.matchEntire(commit.message)?.also {
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

            if (!dirtyIgnore) {
                var found = false
                step("Check for modifications", verbosity >= 2) { log ->
                    git.consumeModifications {
                        found = true
                        dirty = true
                        log("Found ${it.file} ${it.status}")
                        false
                    }
                    if (!found) log("No modification found")
                }
            }

            step("Applying changes", verbosity >= 1) { log ->
                var changed = false
                changes.reversed().forEach { change ->
                    change.modification?.invoke(version)
                    changed = true
                    log("Set version to $version due to ${change.reason}")
                }
                if (dirty) {
                    version.apply {
                        patch += 1
                        suffix = dirtySuffix ?: suffix
                    }
                    changed = true
                    log("Set version to $version due to uncommitted modifications")
                }
                if (!changed) throw ExecutionError(
                    message = "No git history found",
                    reason = ExecutionError.Reason.NO_HISTORY,
                )
            }

            if (majorOverride != null || minorOverride != null || patchOverride != null || suffixOverride != null) {
                step("Applying overrides", verbosity >= 1) { log ->
                    version.major = majorOverride ?: version.major
                    version.minor = minorOverride ?: version.minor
                    version.patch = patchOverride ?: version.patch
                    version.suffix = suffixOverride ?: version.suffix

                    log("Set version to $version due to overrides")
                }
            }

            step("Modifying pipeline", verbosity >= 2) { log ->
                val environment = mapOf(
                    "VERSION" to version.toString(),
                )

                var pipelineModified = false
                Pipeline.modifiers.forEach { modifier ->
                    when (configuration.pipeline) {
                        "auto", modifier.name -> {
                            val modified = modifier.modify(
                                Pipeline.Context(
                                    out = out,
                                    env = env,
                                    environment = environment,
                                    params = when (modifier.name) {
                                        "gitlab" -> mapOf(
                                            "dotenv" to configuration.pipelineGitlabDotenv
                                        )

                                        else -> emptyMap()
                                    }
                                ),
                            )
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

    data class Version(var major: Int = 0, var minor: Int = 0, var patch: Int = 0, var suffix: String? = null) {
        override fun toString(): String = "$major.$minor.$patch${suffix?.let { "-$it" } ?: ""}"
    }

    class ExecutionError(message: String, val reason: Reason, cause: Exception? = null) : Error(message, cause) {
        enum class Reason(val status: Int) {
            NO_REPOSITORY(1),
            NO_HISTORY(2)
        }
    }


}
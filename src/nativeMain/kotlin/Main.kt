import gitversion.Configuration
import gitversion.Git
import gitversion.Pipeline

fun main(args: Array<String>) {

    val configuration = Configuration(args)

    val versionRegex = configuration.versionPattern.toRegex(RegexOption.DOT_MATCHES_ALL)
    val tagRegex = configuration.tagPattern.takeUnless(String::isEmpty)?.toRegex(RegexOption.DOT_MATCHES_ALL)
    val majorRegex = configuration.majorPattern?.takeUnless(String::isEmpty)?.toRegex(RegexOption.DOT_MATCHES_ALL)
    val minorRegex = configuration.minorPattern?.takeUnless(String::isEmpty)?.toRegex(RegexOption.DOT_MATCHES_ALL)
    val patchRegex = configuration.patchPattern.takeUnless(String::isEmpty)?.toRegex(RegexOption.DOT_MATCHES_ALL)

    val git = Git(configuration.path)
    val tags = hashMapOf<String, MutableList<Git.Tag>>()
    val changes = arrayListOf<VersionChange>()

    git.consumeTags { tag ->
        tags.getOrPut(tag.commit, ::arrayListOf).add(tag)
        true
    }

    git.consumeCommits { commit ->
        tags[commit.id]?.forEach { tag ->
            tagRegex?.matchEntire(tag.name)?.takeIf { it.groupValues.size > 1 }?.also { match ->
                match.groupValues.getOrNull(1)?.let(versionRegex::matchEntire)?.also { versionMatch ->
                    changes.add(
                        VersionChange(tag) {
                            major += versionMatch.groupValues.getOrNull(1)?.toInt() ?: 0
                            minor += versionMatch.groupValues.getOrNull(2)?.toInt() ?: 0
                            patch += versionMatch.groupValues.getOrNull(3)?.toInt() ?: 0
                        }
                    )

                    return@consumeCommits false
                }
            }

        }

        majorRegex?.matchEntire(commit.message)?.also {
            changes.add(
                VersionChange(commit) {
                    major += 1
                }
            )
            return@consumeCommits true
        }

        minorRegex?.matchEntire(commit.message)?.also {
            changes.add(
                VersionChange(commit) {
                    minor += 1
                }
            )
            return@consumeCommits true
        }

        patchRegex?.matchEntire(commit.message)?.also {
            changes.add(
                VersionChange(commit) {
                    patch += 1
                }
            )
            return@consumeCommits true
        }

        changes.add(
            VersionChange(commit, null)
        )

        return@consumeCommits true
    }

    val version = Version()

    changes.reversed().forEach { change ->
        change.modification?.invoke(version)

        if (configuration.verbose) println("$version because of ${change.reason}")
    }

    if (configuration.pipeline) {
        val environment = mapOf(
            "VERSION" to version.toString(),
        )

        Pipeline.applyAzure(environment)
        Pipeline.applyGithub(environment)
    }

    println(version)

}


data class VersionChange(val reason: Any, val modification: (Version.() -> Unit)?)

data class Version(var major: Int = 0, var minor: Int = 0, var patch: Int = 0) {
    override fun toString(): String = "$major.$minor.$patch"
}
